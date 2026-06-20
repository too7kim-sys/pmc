package egovframework.let.pmc.dq.service.impl;

import egovframework.let.pmc.dq.service.DqMapper;
import egovframework.let.pmc.dq.service.DqService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DqServiceImpl implements DqService {

    private static final Logger log = LoggerFactory.getLogger(DqServiceImpl.class);

    private final DqMapper dqMapper;
    private final DqQueryExecutor queryExecutor;

    @Autowired
    public DqServiceImpl(DqMapper dqMapper, DqQueryExecutor queryExecutor) {
        this.dqMapper = dqMapper;
        this.queryExecutor = queryExecutor;
    }

    @Override
    public List<Map<String, Object>> getRules() {
        return dqMapper.selectRules();
    }

    @Override
    public List<Map<String, Object>> getResults() {
        return dqMapper.selectResults();
    }

    @Override
    @Transactional
    public int runAll() {
        int cnt = 0;
        for (Map<String, Object> rule : dqMapper.selectRules()) {
            if (!"Y".equals(String.valueOf(rule.get("useYn")))) {
                continue;
            }
            Long ruleId = ((Number) rule.get("ruleId")).longValue();
            String sql = String.valueOf(rule.get("checkSql"));
            // 방어적 가드: 점검 룰은 위반 건수를 세는 단일 SELECT 만 허용(DML/DDL 차단)
            if (!isSafeSelect(sql)) {
                log.warn("데이터품질 룰 차단(SELECT 아님) ruleId={}", ruleId);
                dqMapper.insertResult(ruleId, 0L, "ERROR");
                cnt++;
                continue;
            }
            try {
                // SELECT 는 읽기전용 트랜잭션에서 실행(DB가 DML 차단)
                long v = queryExecutor.countViolations(sql);
                dqMapper.insertResult(ruleId, v, v > 0 ? "FAIL" : "PASS");
            } catch (Exception e) {
                log.warn("데이터품질 룰 실행 실패 ruleId={}", ruleId, e);
                dqMapper.insertResult(ruleId, 0L, "ERROR");
            }
            cnt++;
        }
        return cnt;
    }

    /**
     * 점검 룰이 안전한 단일 조회(SELECT/WITH...SELECT) 인지 검사.
     * 세미콜론(다중 구문)·주석(--, /* *&#47;)·DML/DDL 키워드를 차단한다.
     * (실행 자체는 읽기전용 트랜잭션으로 DB가 한 번 더 차단 — 본 검사는 방어선 1차)
     */
    boolean isSafeSelect(String sql) {
        if (sql == null) return false;
        String s = sql.trim();
        if (s.isEmpty()) return false;
        String lower = s.toLowerCase();
        // 단일 조회만 허용: select 또는 with(CTE) 로 시작
        if (!(lower.startsWith("select") || lower.startsWith("with"))) return false;
        // 세미콜론 전면 차단(구문 이어붙이기 방지)
        if (s.indexOf(';') >= 0) return false;
        // 주석 차단(-- , /* */ 로 검사 우회 방지)
        if (lower.contains("--") || lower.contains("/*")) return false;
        // 데이터 변경/권한/실행 계열 키워드 단어 차단
        String[] banned = {"insert", "update", "delete", "drop", "alter", "create", "truncate",
                "grant", "revoke", "merge", "copy", "call", "do", "vacuum", "analyze",
                "into", "returning", "pg_sleep", "lo_import", "lo_export", "dblink"};
        for (String kw : banned) {
            if (containsWord(lower, kw)) return false;
        }
        return true;
    }

    /** 단어 경계 기준 포함 검사(부분 문자열 오탐 방지: 예 'created_at' 의 'create'). */
    private boolean containsWord(String haystack, String word) {
        int from = 0;
        while (true) {
            int idx = haystack.indexOf(word, from);
            if (idx < 0) return false;
            boolean leftOk = idx == 0 || !isWordChar(haystack.charAt(idx - 1));
            int end = idx + word.length();
            boolean rightOk = end >= haystack.length() || !isWordChar(haystack.charAt(end));
            if (leftOk && rightOk) return true;
            from = idx + word.length();
        }
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
