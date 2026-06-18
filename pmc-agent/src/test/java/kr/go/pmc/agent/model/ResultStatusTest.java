package kr.go.pmc.agent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultStatusTest {

    @Test
    void severityOrdering() {
        // NORMAL < NA < WARN < CRITICAL < ERROR
        assertEquals(ResultStatus.NA, ResultStatus.worst(ResultStatus.NORMAL, ResultStatus.NA));
        assertEquals(ResultStatus.WARN, ResultStatus.worst(ResultStatus.NA, ResultStatus.WARN));
        assertEquals(ResultStatus.CRITICAL, ResultStatus.worst(ResultStatus.WARN, ResultStatus.CRITICAL));
        assertEquals(ResultStatus.ERROR, ResultStatus.worst(ResultStatus.CRITICAL, ResultStatus.ERROR));
        assertEquals(ResultStatus.ERROR, ResultStatus.worst(ResultStatus.ERROR, ResultStatus.NORMAL));
    }

    @Test
    void worstHandlesNulls() {
        assertEquals(ResultStatus.WARN, ResultStatus.worst(null, ResultStatus.WARN));
        assertEquals(ResultStatus.WARN, ResultStatus.worst(ResultStatus.WARN, null));
    }

    @Test
    void worstSymmetric() {
        assertEquals(ResultStatus.worst(ResultStatus.WARN, ResultStatus.CRITICAL),
                ResultStatus.worst(ResultStatus.CRITICAL, ResultStatus.WARN));
    }
}
