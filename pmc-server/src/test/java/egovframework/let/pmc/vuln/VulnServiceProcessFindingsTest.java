package egovframework.let.pmc.vuln;

import egovframework.let.pmc.ingest.service.InspectionRunVO;
import egovframework.let.pmc.notify.service.AlertChannelVO;
import egovframework.let.pmc.notify.service.AlertLogVO;
import egovframework.let.pmc.notify.service.AlertService;
import egovframework.let.pmc.vuln.service.*;
import egovframework.let.pmc.vuln.service.impl.VulnServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VulnService 상태머신 단위테스트(무DB). 스텁 VulnMapper(HashMap) + 스텁 AlertService 로
 * 동일 취약점 추적/재발방지 전이를 검증한다.
 */
class VulnServiceProcessFindingsTest {

    private StubMapper mapper;
    private StubAlert alert;
    private VulnServiceImpl svc;

    private static final Long SID = 1L;

    @BeforeEach
    void init() {
        mapper = new StubMapper();
        alert = new StubAlert();
        svc = new VulnServiceImpl(mapper, alert);
    }

    private VulnFinding f(String code, String sev) {
        return new VulnFinding(SID, code, "BUILTIN", "SEC", code + " title", sev);
    }

    @Test
    void newFinding_insertsOpen_noAlertByDefault() {
        svc.processFindings(SID, "r1", "BUILTIN", Collections.singletonList(f("C1", "상")), true);
        VulnFindingVO vo = mapper.find(SID, "C1", "BUILTIN");
        assertNotNull(vo);
        assertEquals("OPEN", vo.getStatus());
        assertEquals(1, vo.getOccurrenceCount());
        assertTrue(alert.raised.isEmpty(), "VulnNewAlert 기본 비활성 → 알림 없음");
    }

    @Test
    void newFinding_high_withFlag_raisesVulnNew() throws Exception {
        setFlag(true);
        svc.processFindings(SID, "r1", "BUILTIN", Collections.singletonList(f("C1", "상")), true);
        assertEquals(1, alert.raised.size());
        assertEquals("VULN_NEW", alert.raised.get(0)[0]);
    }

    @Test
    void repeatOpen_incrementsOccurrence_noAlert() {
        svc.processFindings(SID, "r1", "BUILTIN", Collections.singletonList(f("C1", "중")), true);
        svc.processFindings(SID, "r2", "BUILTIN", Collections.singletonList(f("C1", "중")), true);
        VulnFindingVO vo = mapper.find(SID, "C1", "BUILTIN");
        assertEquals("OPEN", vo.getStatus());
        assertEquals(2, vo.getOccurrenceCount());
        assertTrue(alert.raised.isEmpty());
    }

    @Test
    void fixedThenDetected_recurs_escalates_andAlertsOnce() {
        // 1회 탐지 → OPEN
        svc.processFindings(SID, "r1", "BUILTIN", Collections.singletonList(f("C1", "중")), true);
        // 조치되어 사라짐 → 부재 → FIXED
        svc.processFindings(SID, "r2", "BUILTIN", Collections.emptyList(), true);
        assertEquals("FIXED", mapper.find(SID, "C1", "BUILTIN").getStatus());
        // 재탐지 → RECURRED + 등급 에스컬레이션(중→상) + VULN_RECUR 알림
        svc.processFindings(SID, "r3", "BUILTIN", Collections.singletonList(f("C1", "중")), true);
        VulnFindingVO vo = mapper.find(SID, "C1", "BUILTIN");
        assertEquals("RECURRED", vo.getStatus());
        assertEquals("상", vo.getSeverity(), "재발 시 등급 상향");
        assertEquals(1, vo.getRecurCount());
        assertEquals(1, alert.raised.size());
        assertEquals("VULN_RECUR", alert.raised.get(0)[0]);
        assertEquals("CRITICAL", alert.raised.get(0)[1]);
    }

    @Test
    void absentInScan_autoFixed() {
        svc.processFindings(SID, "r1", "BUILTIN", Arrays.asList(f("C1", "중"), f("C2", "하")), true);
        // 다음 스캔에 C1 만 존재 → C2 는 FIXED
        svc.processFindings(SID, "r2", "BUILTIN", Collections.singletonList(f("C1", "중")), true);
        assertEquals("OPEN", mapper.find(SID, "C1", "BUILTIN").getStatus());
        assertEquals("FIXED", mapper.find(SID, "C2", "BUILTIN").getStatus());
    }

    @Test
    void exemptedWithActiveException_noAlert_keepsExempted() {
        svc.processFindings(SID, "r1", "BUILTIN", Collections.singletonList(f("C1", "상")), true);
        Long fid = mapper.find(SID, "C1", "BUILTIN").getFindingId();
        svc.registerException(fid, "수용", "admin", OffsetDateTime.now().plusDays(7), "admin");
        assertEquals("EXEMPTED", mapper.find(SID, "C1", "BUILTIN").getStatus());
        alert.raised.clear();
        // 재탐지 — 유효 예외 → 알림 없음, 상태 유지
        svc.processFindings(SID, "r2", "BUILTIN", Collections.singletonList(f("C1", "상")), true);
        assertEquals("EXEMPTED", mapper.find(SID, "C1", "BUILTIN").getStatus());
        assertTrue(alert.raised.isEmpty());
    }

    @Test
    void exemptedExpired_detected_recursAndAlerts() {
        svc.processFindings(SID, "r1", "BUILTIN", Collections.singletonList(f("C1", "중")), true);
        Long fid = mapper.find(SID, "C1", "BUILTIN").getFindingId();
        svc.registerException(fid, "수용", "admin", OffsetDateTime.now().minusDays(1), "admin"); // 이미 만료
        alert.raised.clear();
        svc.processFindings(SID, "r2", "BUILTIN", Collections.singletonList(f("C1", "중")), true);
        assertEquals("RECURRED", mapper.find(SID, "C1", "BUILTIN").getStatus());
        assertEquals(1, alert.raised.size());
        assertEquals("VULN_RECUR", alert.raised.get(0)[0]);
    }

    @Test
    void expireExceptions_reactivatesFinding() {
        svc.processFindings(SID, "r1", "BUILTIN", Collections.singletonList(f("C1", "중")), true);
        Long fid = mapper.find(SID, "C1", "BUILTIN").getFindingId();
        svc.registerException(fid, "수용", "admin", OffsetDateTime.now().minusHours(1), "admin"); // 만료
        int n = svc.expireExceptions();
        assertEquals(1, n);
        assertEquals("OPEN", mapper.find(SID, "C1", "BUILTIN").getStatus(), "만료 → EXEMPTED→OPEN 재활성");
    }

    private void setFlag(boolean v) throws Exception {
        Field fld = VulnServiceImpl.class.getDeclaredField("vulnNewAlertEnabled");
        fld.setAccessible(true);
        fld.set(svc, v);
    }

    // ---- 스텁 ----

    static class StubMapper implements VulnMapper {
        private final Map<String, VulnFindingVO> findings = new LinkedHashMap<>();
        private final Map<Long, VulnFindingVO> byId = new HashMap<>();
        private final List<VulnExceptionVO> exceptions = new ArrayList<>();
        private final List<VulnActionVO> actions = new ArrayList<>();
        private long seqF = 0, seqE = 0, seqA = 0;

        private String key(Long s, String c, String src) { return s + "|" + c + "|" + src; }
        VulnFindingVO find(Long s, String c, String src) { return findings.get(key(s, c, src)); }

        public VulnFindingVO selectFinding(Long serverId, String checkCode, String source) {
            return findings.get(key(serverId, checkCode, source));
        }
        public int insertFinding(VulnFindingVO vo) {
            vo.setFindingId(++seqF);
            if (vo.getOccurrenceCount() == 0) vo.setOccurrenceCount(1);
            findings.put(key(vo.getServerId(), vo.getCheckCode(), vo.getSource()), vo);
            byId.put(vo.getFindingId(), vo);
            return 1;
        }
        public int updateFindingDetected(Long findingId, String status, String severity,
                                         String lastRunId, int recurInc, String updUser) {
            VulnFindingVO vo = byId.get(findingId);
            if (vo == null) return 0;
            vo.setStatus(status);
            vo.setSeverity(severity);
            vo.setOccurrenceCount(vo.getOccurrenceCount() + 1);
            vo.setRecurCount(vo.getRecurCount() + recurInc);
            vo.setLastRunId(lastRunId);
            return 1;
        }
        public int markFixedForServerExcept(Long serverId, String source, List<String> presentCodes, String updUser) {
            int n = 0;
            for (VulnFindingVO vo : findings.values()) {
                if (!serverId.equals(vo.getServerId()) || !source.equals(vo.getSource())) continue;
                if (!"OPEN".equals(vo.getStatus()) && !"RECURRED".equals(vo.getStatus())) continue;
                if (presentCodes != null && presentCodes.contains(vo.getCheckCode())) continue;
                vo.setStatus("FIXED");
                n++;
            }
            return n;
        }
        public int updateFindingStatus(Long findingId, String status, String updUser) {
            VulnFindingVO vo = byId.get(findingId);
            if (vo == null) return 0;
            vo.setStatus(status);
            return 1;
        }
        public List<VulnFindingVO> selectFindings(Long serverId, String severity, String status) {
            return new ArrayList<>(findings.values());
        }
        public VulnFindingVO selectFindingById(Long findingId) { return byId.get(findingId); }
        public List<Map<String, Object>> selectRecurrenceSummary() { return Collections.emptyList(); }
        public VulnExceptionVO selectActiveException(Long findingId) {
            VulnExceptionVO best = null;
            for (VulnExceptionVO e : exceptions) {
                if (findingId.equals(e.getFindingId()) && "ACTIVE".equals(e.getStatus())
                        && e.getExpiresAt() != null && e.getExpiresAt().isAfter(OffsetDateTime.now())) {
                    best = e;
                }
            }
            return best;
        }
        public int insertException(VulnExceptionVO vo) {
            vo.setExceptionId(++seqE);
            if (vo.getStatus() == null) vo.setStatus("ACTIVE");
            exceptions.add(vo);
            return 1;
        }
        public List<VulnExceptionVO> selectExpiredExceptions() {
            List<VulnExceptionVO> out = new ArrayList<>();
            for (VulnExceptionVO e : exceptions) {
                if ("ACTIVE".equals(e.getStatus()) && e.getExpiresAt() != null
                        && !e.getExpiresAt().isAfter(OffsetDateTime.now())) {
                    out.add(e);
                }
            }
            return out;
        }
        public int expireException(Long exceptionId) {
            for (VulnExceptionVO e : exceptions) {
                if (exceptionId.equals(e.getExceptionId())) { e.setStatus("EXPIRED"); return 1; }
            }
            return 0;
        }
        public int insertAction(VulnActionVO vo) { vo.setActionId(++seqA); actions.add(vo); return 1; }
        public List<VulnActionVO> selectActions(Long findingId) {
            List<VulnActionVO> out = new ArrayList<>();
            for (VulnActionVO a : actions) if (findingId.equals(a.getFindingId())) out.add(a);
            return out;
        }
    }

    static class StubAlert implements AlertService {
        final List<String[]> raised = new ArrayList<>(); // [type, severity, title]
        public void raise(String alertType, Long serverId, String refId, String severity, String title, String message) {
            raised.add(new String[]{alertType, severity, title});
        }
        public void raiseRunAlert(InspectionRunVO run, int svcFailCount) { }
        public void raiseHeartbeatAlerts() { }
        public List<AlertLogVO> getAlertLog(int limit) { return Collections.emptyList(); }
        public List<AlertChannelVO> getChannels() { return Collections.emptyList(); }
        public void addChannel(AlertChannelVO vo) { }
        public void setChannelEnabled(Long channelId, String enabled) { }
        public void deleteChannel(Long channelId) { }
        public boolean testChannel(Long channelId) { return true; }
        public boolean testGlobalWebhook() { return true; }
    }
}
