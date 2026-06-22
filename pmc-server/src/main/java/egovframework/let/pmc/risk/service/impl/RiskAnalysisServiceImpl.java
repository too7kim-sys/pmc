package egovframework.let.pmc.risk.service.impl;

import egovframework.let.pmc.risk.service.RiskAnalysisService;
import egovframework.let.pmc.risk.service.RiskMapper;
import egovframework.let.pmc.risk.service.RiskScoreVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 규칙 기반 위험 점수 산정(결정적·설명가능).
 * score = min(100,
 *   25·[최신=CRITICAL] + 10·[최신=WARN]
 *   + min(30, c7·6) + min(20, w7·2)
 *   + 30·heartbeat누락 + 20·미점검7일 + min(15, svcFail·5))
 * level: ≥70 HIGH / ≥40 MEDIUM / ≥1 LOW / else NONE
 */
@Service
public class RiskAnalysisServiceImpl implements RiskAnalysisService {

    private final RiskMapper riskMapper;

    @Autowired
    public RiskAnalysisServiceImpl(RiskMapper riskMapper) {
        this.riskMapper = riskMapper;
    }

    @Override
    public List<RiskScoreVO> analyze(int days) {
        List<RiskScoreVO> out = new ArrayList<>();
        for (Map<String, Object> f : riskMapper.selectServerRiskFacts(days)) {
            out.add(score(f, days));
        }
        out.sort(Comparator.comparingInt(RiskScoreVO::getScore).reversed());
        return out;
    }

    private RiskScoreVO score(Map<String, Object> f, int days) {
        String last = str(f.get("lastOverall"));
        int c7 = num(f.get("c7")), w7 = num(f.get("w7")), svcFail = num(f.get("svcFail7"));
        boolean hbStale = num(f.get("hbStale")) == 1;
        boolean noRun7 = num(f.get("noRun7")) == 1;

        int score = 0;
        List<String> reasons = new ArrayList<>();
        if ("CRITICAL".equals(last)) {
            score += 25; reasons.add("최신 점검 종합판정 위험(CRITICAL)");
        } else if ("WARN".equals(last)) {
            score += 10; reasons.add("최신 점검 종합판정 주의(WARN)");
        }
        if (c7 > 0) {
            score += Math.min(30, c7 * 6); reasons.add("최근 " + days + "일 위험 항목 " + c7 + "건");
        }
        if (w7 > 0) {
            score += Math.min(20, w7 * 2); reasons.add("최근 " + days + "일 주의 항목 " + w7 + "건");
        }
        if (hbStale) {
            score += 30; reasons.add("Agent heartbeat 24시간 이상 누락");
        }
        if (noRun7) {
            score += 20; reasons.add("최근 7일 내 점검 실행 없음");
        }
        if (svcFail > 0) {
            score += Math.min(15, svcFail * 5); reasons.add("웹서비스 접속 실패 " + svcFail + "건");
        }
        score = Math.min(100, score);

        RiskScoreVO vo = new RiskScoreVO();
        vo.setServerId(f.get("serverId") == null ? null : ((Number) f.get("serverId")).longValue());
        vo.setHostname(str(f.get("hostname")));
        vo.setServiceName(str(f.get("serviceName")));
        vo.setScore(score);
        vo.setLevel(level(score));
        vo.setReasons(reasons);
        return vo;
    }

    private String level(int score) {
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        if (score >= 1) return "LOW";
        return "NONE";
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private int num(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }
}
