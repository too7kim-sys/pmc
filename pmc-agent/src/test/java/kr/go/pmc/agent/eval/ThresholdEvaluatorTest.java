package kr.go.pmc.agent.eval;

import kr.go.pmc.agent.model.PolicyItem;
import kr.go.pmc.agent.model.ResultStatus;
import kr.go.pmc.agent.model.ThresholdRule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThresholdEvaluatorTest {

    private final ThresholdEvaluator ev = new ThresholdEvaluator();

    private ThresholdRule rule(String level, String op, String cv) {
        ThresholdRule r = new ThresholdRule();
        r.setLevel(level);
        r.setOperator(op);
        r.setCompareValue(cv);
        return r;
    }

    @Test
    void gteOperators() {
        assertTrue(ev.matches("90", rule("WARN", "GTE", "80")));
        assertTrue(ev.matches("80", rule("WARN", "GTE", "80")));
        assertFalse(ev.matches("79", rule("WARN", "GTE", "80")));
        assertTrue(ev.matches("81", rule("WARN", "GT", "80")));
        assertFalse(ev.matches("80", rule("WARN", "GT", "80")));
    }

    @Test
    void ltOperators() {
        assertTrue(ev.matches("5", rule("CRITICAL", "LT", "10")));
        assertFalse(ev.matches("10", rule("CRITICAL", "LT", "10")));
        assertTrue(ev.matches("10", rule("CRITICAL", "LTE", "10")));
    }

    @Test
    void eqOperatorNumericAndString() {
        assertTrue(ev.matches("0", rule("WARN", "EQ", "0")));
        assertTrue(ev.matches("0.0", rule("WARN", "EQ", "0"))); // 수치 동등
        assertTrue(ev.matches("DOWN", rule("CRITICAL", "EQ", "DOWN")));
        assertFalse(ev.matches("UP", rule("CRITICAL", "EQ", "DOWN")));
    }

    @Test
    void rangeOperator() {
        ThresholdRule r = new ThresholdRule();
        r.setLevel("WARN");
        r.setOperator("RANGE");
        r.setRangeLow("10");
        r.setRangeHigh("20");
        assertTrue(ev.matches("15", r));
        assertTrue(ev.matches("10", r));
        assertTrue(ev.matches("20", r));
        assertFalse(ev.matches("21", r));
        assertFalse(ev.matches("9", r));
    }

    @Test
    void regexOperator() {
        assertTrue(ev.matches("UNSYNCED", rule("WARN", "REGEX", "UNSYNC.*")));
        assertFalse(ev.matches("SYNCED", rule("WARN", "REGEX", "^UNSYNC")));
    }

    @Test
    void unitSuffixTolerated() {
        assertTrue(ev.matches("85%", rule("WARN", "GTE", "80")));
        assertTrue(ev.matches("120 ms", rule("WARN", "GT", "100")));
    }

    @Test
    void signAndExponentParsed() {
        // 선행 '+' 부호(이전 파서는 null 처리되어 오분류)
        assertTrue(ev.matches("+90", rule("WARN", "GTE", "80")));
        // 지수 표기(1.5e3 = 1500)
        assertTrue(ev.matches("1.5e3", rule("WARN", "GT", "1000")));
        // 음수 비교
        assertTrue(ev.matches("-5", rule("WARN", "LT", "0")));
    }

    @Test
    void criticalTakesPrecedenceOverWarn() {
        // 두 규칙 모두 만족(95 >= 80, 95 >= 90) → CRITICAL
        ResultStatus s = ev.evaluate("95", Arrays.asList(
                rule("WARN", "GTE", "80"),
                rule("CRITICAL", "GTE", "90")));
        assertEquals(ResultStatus.CRITICAL, s);
    }

    @Test
    void onlyWarnMatched() {
        ResultStatus s = ev.evaluate("85", Arrays.asList(
                rule("WARN", "GTE", "80"),
                rule("CRITICAL", "GTE", "90")));
        assertEquals(ResultStatus.WARN, s);
    }

    @Test
    void noneMatchedIsNormal() {
        ResultStatus s = ev.evaluate("50", Arrays.asList(
                rule("WARN", "GTE", "80"),
                rule("CRITICAL", "GTE", "90")));
        assertEquals(ResultStatus.NORMAL, s);
    }

    @Test
    void emptyRulesIsNormal() {
        assertEquals(ResultStatus.NORMAL, ev.evaluate("100", Collections.emptyList()));
        assertEquals(ResultStatus.NORMAL, ev.evaluate("100", (PolicyItem) null));
    }
}
