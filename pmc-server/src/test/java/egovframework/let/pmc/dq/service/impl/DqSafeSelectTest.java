package egovframework.let.pmc.dq.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DqServiceImpl.isSafeSelect 강화 가드 단위테스트.
 * (DB 의존 없이 순수 문자열 검사만 검증 — 매퍼/executor 는 null 로 충분)
 */
class DqSafeSelectTest {

    private final DqServiceImpl svc = new DqServiceImpl(null, null);

    @Test
    void allowsPlainCountSelect() {
        assertTrue(svc.isSafeSelect("SELECT count(*) FROM pmc_server"));
        assertTrue(svc.isSafeSelect("select COUNT(*) from pmc_inspection_run r WHERE r.server_id IS NOT NULL"));
        // 단어 경계: created_at 의 'create' 는 오탐하지 않음
        assertTrue(svc.isSafeSelect("SELECT count(*) FROM pmc_report WHERE created_at > now() - interval '1 day'"));
        // NOT IN / NOT EXISTS 서브쿼리(시드 룰 형태)
        assertTrue(svc.isSafeSelect(
                "SELECT count(*) FROM pmc_server s WHERE s.os_type NOT IN (SELECT code FROM comtccmmncodedetail)"));
        // CTE(WITH ... SELECT) 허용
        assertTrue(svc.isSafeSelect("WITH x AS (SELECT 1 AS n) SELECT count(*) FROM x"));
    }

    @Test
    void blocksNonSelect() {
        assertFalse(svc.isSafeSelect(null));
        assertFalse(svc.isSafeSelect("   "));
        assertFalse(svc.isSafeSelect("UPDATE pmc_server SET hostname='x'"));
        assertFalse(svc.isSafeSelect("DELETE FROM pmc_server"));
        assertFalse(svc.isSafeSelect("DROP TABLE pmc_server"));
    }

    @Test
    void blocksStackedAndComments() {
        assertFalse(svc.isSafeSelect("SELECT 1; DROP TABLE pmc_server"));
        assertFalse(svc.isSafeSelect("SELECT count(*) FROM pmc_server;"));
        assertFalse(svc.isSafeSelect("SELECT count(*) FROM pmc_server -- comment"));
        assertFalse(svc.isSafeSelect("SELECT count(*) /* x */ FROM pmc_server"));
    }

    @Test
    void blocksDataModifyingCteAndFunctions() {
        assertFalse(svc.isSafeSelect("WITH d AS (DELETE FROM pmc_server RETURNING 1) SELECT count(*) FROM d"));
        assertFalse(svc.isSafeSelect("SELECT pg_sleep(10)"));
        assertFalse(svc.isSafeSelect("SELECT count(*) INTO x FROM pmc_server"));
    }
}
