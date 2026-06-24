package egovframework.let.pmc.notify;

import egovframework.let.pmc.notify.service.AlertChannelMapper;
import egovframework.let.pmc.notify.service.AlertChannelVO;
import egovframework.let.pmc.notify.service.AlertLogVO;
import egovframework.let.pmc.notify.service.AlertMapper;
import egovframework.let.pmc.notify.service.impl.AlertServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 알림 발행(채널 매칭·임계·dedupe·비활성) 단위테스트(스텁, DB/네트워크 무).
 */
class AlertServiceImplTest {

    private final List<AlertLogVO> inserted = new ArrayList<>();
    private final List<String> sentUrls = new ArrayList<>();
    private int recentCount = 0;
    private List<AlertChannelVO> enabledChannels = Collections.emptyList();

    private AlertMapper alertMapper() {
        return new AlertMapper() {
            public void insertAlert(AlertLogVO vo) { inserted.add(vo); }
            public int countRecent(String alertType, Long serverId, int min) { return recentCount; }
            public List<Map<String, Object>> selectStaleHeartbeats(int hours) { return Collections.emptyList(); }
            public List<AlertLogVO> selectAlertLog(int limit) { return Collections.emptyList(); }
        };
    }

    private AlertChannelMapper channelMapper() {
        return new AlertChannelMapper() {
            public List<AlertChannelVO> selectChannels() { return enabledChannels; }
            public List<AlertChannelVO> selectEnabledChannels() { return enabledChannels; }
            public void insertChannel(AlertChannelVO vo) { }
            public void updateEnabled(Long channelId, String enabled) { }
            public void deleteChannel(Long channelId) { }
        };
    }

    private WebhookSender sender() {
        return new WebhookSender() {
            @Override public boolean send(String url, String text) { sentUrls.add(url); return true; }
        };
    }

    private AlertChannelVO channel(String url, String minSev, String types) {
        AlertChannelVO c = new AlertChannelVO();
        c.setUrl(url); c.setMinSeverity(minSev); c.setAlertTypes(types); c.setEnabled("Y");
        return c;
    }

    private AlertServiceImpl service(boolean enabled, String globalsUrl) throws Exception {
        AlertServiceImpl svc = new AlertServiceImpl(alertMapper(), channelMapper(), sender());
        set(svc, "alertEnabled", enabled);
        set(svc, "webhookUrl", globalsUrl);
        set(svc, "dedupeMin", 30);
        return svc;
    }

    private void set(Object o, String f, Object v) throws Exception {
        Field fl = AlertServiceImpl.class.getDeclaredField(f);
        fl.setAccessible(true);
        fl.set(o, v);
    }

    @Test
    void disabledSendsNothing() throws Exception {
        AlertServiceImpl svc = service(false, "http://x");
        svc.raise("RUN_CRITICAL", 1L, "r", "CRITICAL", "t", "m");
        assertTrue(sentUrls.isEmpty());
        assertTrue(inserted.isEmpty());
    }

    @Test
    void channelMatchBySeverity() throws Exception {
        enabledChannels = Collections.singletonList(channel("http://ch1", "CRITICAL", null));
        AlertServiceImpl svc = service(true, "");
        // CRITICAL → 채널 임계(CRITICAL) 충족 → 발송 SENT
        svc.raise("RUN_CRITICAL", 1L, "r", "CRITICAL", "t", "m");
        assertEquals(1, sentUrls.size());
        assertEquals("http://ch1", sentUrls.get(0));
        assertEquals("SENT", inserted.get(0).getSentStatus());
    }

    @Test
    void belowSeverityThresholdSkipped() throws Exception {
        enabledChannels = Collections.singletonList(channel("http://ch1", "CRITICAL", null));
        AlertServiceImpl svc = service(true, "");
        // WARN < 채널 임계(CRITICAL) → 매칭 채널 없음 → SKIPPED, 미발송
        svc.raise("RUN_CRITICAL", 1L, "r", "WARN", "t", "m");
        assertTrue(sentUrls.isEmpty());
        assertEquals("SKIPPED", inserted.get(0).getSentStatus());
    }

    @Test
    void typeFilterApplied() throws Exception {
        enabledChannels = Collections.singletonList(channel("http://ch1", "WARN", "SVC_FAIL"));
        AlertServiceImpl svc = service(true, "");
        svc.raise("RUN_CRITICAL", 1L, "r", "CRITICAL", "t", "m"); // 유형 불일치 → skip
        assertTrue(sentUrls.isEmpty());
    }

    @Test
    void dedupeSkips() throws Exception {
        recentCount = 1; // 최근 동일 알림 존재
        enabledChannels = Collections.singletonList(channel("http://ch1", "WARN", null));
        AlertServiceImpl svc = service(true, "");
        svc.raise("RUN_CRITICAL", 1L, "r", "CRITICAL", "t", "m");
        assertTrue(sentUrls.isEmpty());
        assertTrue(inserted.isEmpty());
    }

    @Test
    void globalsFallbackWhenNoChannels() throws Exception {
        enabledChannels = Collections.emptyList();
        AlertServiceImpl svc = service(true, "http://fallback");
        svc.raise("RUN_CRITICAL", 1L, "r", "CRITICAL", "t", "m");
        assertEquals(1, sentUrls.size());
        assertEquals("http://fallback", sentUrls.get(0));
    }

    @Test
    void testChannelSendsRegardlessOfEnabled() throws Exception {
        AlertChannelVO c = channel("http://ch9", "CRITICAL", null);
        c.setChannelId(9L);
        enabledChannels = Collections.singletonList(c);
        AlertServiceImpl svc = service(false, ""); // 마스터 비활성이어도 테스트 발송은 동작
        boolean ok = svc.testChannel(9L);
        assertTrue(ok);
        assertEquals(1, sentUrls.size());
        assertEquals("http://ch9", sentUrls.get(0));
        assertEquals("TEST", inserted.get(0).getAlertType());
        assertEquals("SENT", inserted.get(0).getSentStatus());
    }

    @Test
    void testGlobalSkippedWhenNoUrl() throws Exception {
        AlertServiceImpl svc = service(false, "");
        boolean ok = svc.testGlobalWebhook();
        assertTrue(!ok);
        assertTrue(sentUrls.isEmpty());
        assertEquals("SKIPPED", inserted.get(0).getSentStatus());
    }
}
