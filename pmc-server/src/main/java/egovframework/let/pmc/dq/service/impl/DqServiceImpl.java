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

    @Autowired
    public DqServiceImpl(DqMapper dqMapper) {
        this.dqMapper = dqMapper;
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
                Long violations = dqMapper.executeCount(sql);
                long v = violations == null ? 0 : violations;
                dqMapper.insertResult(ruleId, v, v > 0 ? "FAIL" : "PASS");
            } catch (Exception e) {
                log.warn("데이터품질 룰 실행 실패 ruleId={}", ruleId, e);
                dqMapper.insertResult(ruleId, 0L, "ERROR");
            }
            cnt++;
        }
        return cnt;
    }

    /** 단일 SELECT 문인지 검사(세미콜론으로 구문을 이어붙이는 인젝션 차단). */
    private boolean isSafeSelect(String sql) {
        if (sql == null) return false;
        String s = sql.trim();
        if (!s.toLowerCase().startsWith("select")) return false;
        // 끝의 세미콜론 1개는 허용하되, 중간 세미콜론(다중 구문)은 차단
        int semi = s.indexOf(';');
        return semi < 0 || semi == s.length() - 1;
    }
}
