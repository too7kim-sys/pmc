package egovframework.let.pmc.vuln;

import egovframework.let.pmc.support.H2SchemaTestSupport;
import egovframework.let.pmc.vuln.service.*;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 인메모리 H2(전체 스키마)에서 VulnMapper 실제 쿼리 정합성 검증.
 * 운영은 PostgreSQL — 동일 매퍼 XML 로 H2 적재 후 실행한다(Docker 불필요).
 */
class VulnMapperH2Test {

    private static H2SchemaTestSupport h2;
    private static SqlSessionFactory sf;
    private static Long serverId;

    @BeforeAll
    static void setUp() throws Exception {
        h2 = H2SchemaTestSupport.create("vulnmapper");
        sf = h2.sqlSessionFactory();
        try (Connection c = h2.dataSource().getConnection(); Statement st = c.createStatement()) {
            st.execute("INSERT INTO pmc_server(hostname, ip_addr, os_type, use_yn) VALUES('h1','10.0.0.1','LINUX','Y')");
            try (ResultSet rs = st.executeQuery("SELECT server_id FROM pmc_server WHERE hostname='h1'")) {
                rs.next();
                serverId = rs.getLong(1);
            }
        }
    }

    @Test
    void findingCrudAndQueriesRun() {
        try (SqlSession s = sf.openSession(true)) {
            VulnMapper m = s.getMapper(VulnMapper.class);

            // insert
            VulnFindingVO vo = new VulnFindingVO();
            vo.setServerId(serverId);
            vo.setCheckCode("SEC_U01_ROOT_REMOTE");
            vo.setSource("BUILTIN");
            vo.setCategory("SEC");
            vo.setTitle("root 원격 접속 제한");
            vo.setSeverity("상");
            vo.setStatus("OPEN");
            vo.setRegUser("system");
            assertEquals(1, m.insertFinding(vo));
            assertNotNull(vo.getFindingId());

            // selectFinding(지문)
            VulnFindingVO got = m.selectFinding(serverId, "SEC_U01_ROOT_REMOTE", "BUILTIN");
            assertNotNull(got);
            assertEquals("OPEN", got.getStatus());
            assertEquals(1, got.getOccurrenceCount());

            // updateFindingDetected — 재발(occurrence+1, recur+1)
            m.updateFindingDetected(vo.getFindingId(), "RECURRED", "상", null, 1, "system");
            got = m.selectFindingById(vo.getFindingId());
            assertEquals("RECURRED", got.getStatus());
            assertEquals(2, got.getOccurrenceCount());
            assertEquals(1, got.getRecurCount());
            assertEquals("h1", got.getHostname());

            // selectFindings(필터) — 조인/정렬/where
            List<VulnFindingVO> list = m.selectFindings(serverId, "상", "RECURRED");
            assertEquals(1, list.size());

            // selectRecurrenceSummary — sum(CASE) 집계(H2/PG 호환)
            List<Map<String, Object>> summary = m.selectRecurrenceSummary();
            assertFalse(summary.isEmpty());

            // markFixedForServerExcept — <foreach> NOT IN (현재 코드 유지 → FIXED 안 됨)
            int fixed = m.markFixedForServerExcept(serverId, "BUILTIN",
                    java.util.Collections.singletonList("SEC_U01_ROOT_REMOTE"), "system");
            assertEquals(0, fixed);
            // 빈 목록 → 전부 FIXED
            fixed = m.markFixedForServerExcept(serverId, "BUILTIN",
                    java.util.Collections.emptyList(), "system");
            assertEquals(1, fixed);
            assertEquals("FIXED", m.selectFindingById(vo.getFindingId()).getStatus());
        }
    }

    @Test
    void exceptionAndActionQueriesRun() {
        try (SqlSession s = sf.openSession(true)) {
            VulnMapper m = s.getMapper(VulnMapper.class);
            VulnFindingVO vo = new VulnFindingVO();
            vo.setServerId(serverId);
            vo.setCheckCode("SEC_U05_PASSWD_PERM");
            vo.setSource("BUILTIN");
            vo.setCategory("SEC");
            vo.setTitle("/etc/passwd 권한");
            vo.setSeverity("상");
            vo.setStatus("OPEN");
            m.insertFinding(vo);

            // 예외 등록 + 활성 예외 조회
            VulnExceptionVO ex = new VulnExceptionVO();
            ex.setFindingId(vo.getFindingId());
            ex.setReason("위험 수용(임시)");
            ex.setApprover("admin");
            ex.setStatus("ACTIVE");
            ex.setExpiresAt(OffsetDateTime.now().plusDays(7));
            m.insertException(ex);
            assertNotNull(m.selectActiveException(vo.getFindingId()));

            // 만료 예외(과거) → selectExpiredExceptions 가 잡아야 함
            VulnExceptionVO past = new VulnExceptionVO();
            past.setFindingId(vo.getFindingId());
            past.setReason("만료건");
            past.setStatus("ACTIVE");
            past.setExpiresAt(OffsetDateTime.now().minusDays(1));
            m.insertException(past);
            List<VulnExceptionVO> expired = m.selectExpiredExceptions();
            assertEquals(1, expired.size());
            assertEquals(1, m.expireException(expired.get(0).getExceptionId()));

            // 조치이력
            VulnActionVO act = new VulnActionVO();
            act.setFindingId(vo.getFindingId());
            act.setActionUser("oper");
            act.setActionDesc("권한 644 로 수정");
            act.setResultingStatus("FIXED");
            m.insertAction(act);
            assertEquals(1, m.selectActions(vo.getFindingId()).size());

            m.updateFindingStatus(vo.getFindingId(), "FIXED", "oper");
            assertEquals("FIXED", m.selectFindingById(vo.getFindingId()).getStatus());
        }
    }
}
