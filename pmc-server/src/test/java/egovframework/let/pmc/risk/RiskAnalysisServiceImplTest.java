package egovframework.let.pmc.risk;

import egovframework.let.pmc.risk.service.RiskMapper;
import egovframework.let.pmc.risk.service.RiskScoreVO;
import egovframework.let.pmc.risk.service.impl.RiskAnalysisServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 규칙 기반 위험 점수 산정 단위테스트(스텁 매퍼, DB 무).
 */
class RiskAnalysisServiceImplTest {

    private Map<String, Object> facts(long id, String host, String last,
                                      int c7, int w7, int svc, int hbStale, int noRun7) {
        Map<String, Object> m = new HashMap<>();
        m.put("serverId", id);
        m.put("hostname", host);
        m.put("serviceName", "svc");
        m.put("lastOverall", last);
        m.put("c7", c7);
        m.put("w7", w7);
        m.put("svcFail7", svc);
        m.put("hbStale", hbStale);
        m.put("noRun7", noRun7);
        return m;
    }

    @Test
    void scoresAndLevelsAndSorting() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(facts(1, "h1", "CRITICAL", 5, 0, 0, 0, 0)); // 25 + min(30,30)=30 = 55 → MEDIUM
        rows.add(facts(2, "h2", "NORMAL", 0, 0, 0, 1, 1));    // 30 + 20 = 50 → MEDIUM
        rows.add(facts(3, "h3", "NORMAL", 0, 1, 0, 0, 0));    // min(20,2)=2 → LOW
        rows.add(facts(4, "h4", "NORMAL", 0, 0, 0, 0, 0));    // 0 → NONE
        RiskMapper stub = days -> rows;

        List<RiskScoreVO> r = new RiskAnalysisServiceImpl(stub).analyze(7);
        assertEquals(4, r.size());
        // 내림차순 정렬
        assertEquals(55, r.get(0).getScore());
        assertEquals("MEDIUM", r.get(0).getLevel());
        assertEquals(50, r.get(1).getScore());
        assertEquals("MEDIUM", r.get(1).getLevel());
        assertEquals(2, r.get(2).getScore());
        assertEquals("LOW", r.get(2).getLevel());
        assertEquals(0, r.get(3).getScore());
        assertEquals("NONE", r.get(3).getLevel());
        // 사유 누적 확인
        assertTrue(r.get(0).getReasons().size() >= 2);
    }

    @Test
    void highLevelWhenManySignals() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(facts(1, "h1", "CRITICAL", 10, 10, 3, 1, 1)); // 25+30+20+30+20+15 → 100 → HIGH
        RiskScoreVO v = new RiskAnalysisServiceImpl(days -> rows).analyze(7).get(0);
        assertEquals(100, v.getScore());
        assertEquals("HIGH", v.getLevel());
        assertEquals("CRITICAL", v.getCssClass());
    }
}
