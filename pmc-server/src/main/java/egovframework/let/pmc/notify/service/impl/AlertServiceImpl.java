package egovframework.let.pmc.notify.service.impl;

import egovframework.let.pmc.ingest.service.InspectionRunVO;
import egovframework.let.pmc.notify.WebhookSender;
import egovframework.let.pmc.notify.service.AlertChannelMapper;
import egovframework.let.pmc.notify.service.AlertChannelVO;
import egovframework.let.pmc.notify.service.AlertLogVO;
import egovframework.let.pmc.notify.service.AlertMapper;
import egovframework.let.pmc.notify.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    private final AlertMapper alertMapper;
    private final AlertChannelMapper alertChannelMapper;
    private final WebhookSender webhookSender;

    @Value("${Globals.AlertEnabled:false}")
    private boolean alertEnabled;
    @Value("${Globals.AlertWebhookUrl:}")
    private String webhookUrl;
    @Value("${Globals.AlertDedupeMin:30}")
    private int dedupeMin;

    @Autowired
    public AlertServiceImpl(AlertMapper alertMapper, AlertChannelMapper alertChannelMapper,
                            WebhookSender webhookSender) {
        this.alertMapper = alertMapper;
        this.alertChannelMapper = alertChannelMapper;
        this.webhookSender = webhookSender;
    }

    @Override
    public void raiseRunAlert(InspectionRunVO run, int svcFailCount) {
        if (!alertEnabled || run == null) return;
        try {
            String host = run.getHostname() != null ? run.getHostname() : ("server#" + run.getServerId());
            if ("CRITICAL".equals(run.getOverallStatus())) {
                raise("RUN_CRITICAL", run.getServerId(), run.getRunId(), "CRITICAL",
                        "[위험] 점검 종합판정 CRITICAL — " + host,
                        host + " 점검에서 위험 " + run.getCriticalCount() + "건, 주의 " + run.getWarnCount()
                                + "건, 오류 " + run.getErrorCount() + "건이 감지되었습니다.");
            }
            if (svcFailCount > 0) {
                raise("SVC_FAIL", run.getServerId(), run.getRunId(), "CRITICAL",
                        "[웹서비스] 접속 실패 감지 — " + host,
                        host + " 의 웹서비스(SVC) 점검에서 접속 실패 " + svcFailCount + "건이 감지되었습니다.");
            }
        } catch (Exception e) {
            log.warn("run 알림 처리 실패: {}", e.getMessage());
        }
    }

    @Override
    public void raiseHeartbeatAlerts() {
        if (!alertEnabled) return;
        try {
            List<Map<String, Object>> stale = alertMapper.selectStaleHeartbeats(24);
            for (Map<String, Object> s : stale) {
                Long serverId = s.get("serverId") == null ? null : ((Number) s.get("serverId")).longValue();
                String host = String.valueOf(s.get("hostname"));
                raise("HB_STALE", serverId, null, "CRITICAL",
                        "[Agent] heartbeat 누락 — " + host,
                        host + " Agent 가 24시간 이상 heartbeat 를 보내지 않았습니다(마지막: "
                                + s.get("lastHeartbeat") + ").");
            }
        } catch (Exception e) {
            log.warn("heartbeat 알림 처리 실패: {}", e.getMessage());
        }
    }

    @Override
    public void raise(String alertType, Long serverId, String refId, String severity,
                      String title, String message) {
        if (!alertEnabled) return;
        // 중복억제: 같은 type+server 가 dedupe 창 내 이미 발송됐으면 skip
        if (alertMapper.countRecent(alertType, serverId, dedupeMin) > 0) {
            return;
        }
        // 매칭 채널(심각도 임계·유형 필터) 전부 발송. 채널이 없으면 globals webhook 폴백(하위호환).
        List<String> targets = resolveTargetUrls(alertType, severity);
        boolean anySent = false;
        boolean attempted = false;
        for (String url : targets) {
            attempted = true;
            if (webhookSender.send(url, title + "\n" + message)) anySent = true;
        }
        String sentStatus = !attempted ? "SKIPPED" : (anySent ? "SENT" : "FAILED");
        AlertLogVO vo = new AlertLogVO();
        vo.setAlertType(alertType);
        vo.setServerId(serverId);
        vo.setRefId(refId);
        vo.setSeverity(severity);
        vo.setTitle(title);
        vo.setMessage(message);
        vo.setChannel("WEBHOOK");
        vo.setSentStatus(sentStatus);
        alertMapper.insertAlert(vo);
    }

    /** 알림 severity·type 에 매칭되는 채널 URL 목록. 채널 미구성 시 globals 단일 URL 폴백. */
    private List<String> resolveTargetUrls(String alertType, String severity) {
        List<String> urls = new java.util.ArrayList<>();
        List<AlertChannelVO> channels = alertChannelMapper.selectEnabledChannels();
        if (channels != null && !channels.isEmpty()) {
            int sev = severityRank(severity);
            for (AlertChannelVO ch : channels) {
                if (ch.getUrl() == null || ch.getUrl().trim().isEmpty()) continue;
                if (sev < severityRank(ch.getMinSeverity())) continue;        // 임계 미달
                if (!typeMatches(ch.getAlertTypes(), alertType)) continue;     // 유형 필터
                urls.add(ch.getUrl().trim());
            }
            return urls;
        }
        // 폴백: globals 단일 webhook
        if (webhookUrl != null && !webhookUrl.trim().isEmpty()) {
            urls.add(webhookUrl.trim());
        }
        return urls;
    }

    private int severityRank(String s) {
        if ("CRITICAL".equals(s) || "ERROR".equals(s)) return 2;
        if ("WARN".equals(s)) return 1;
        return 0; // INFO/NORMAL/NA
    }

    /** alertTypes 가 비면 전체 허용, CSV 면 포함 여부. */
    private boolean typeMatches(String alertTypes, String alertType) {
        if (alertTypes == null || alertTypes.trim().isEmpty()) return true;
        for (String t : alertTypes.split(",")) {
            if (t.trim().equalsIgnoreCase(alertType)) return true;
        }
        return false;
    }

    @Override
    public List<AlertLogVO> getAlertLog(int limit) {
        return alertMapper.selectAlertLog(limit);
    }

    @Override
    public List<AlertChannelVO> getChannels() {
        return alertChannelMapper.selectChannels();
    }

    @Override
    public void addChannel(AlertChannelVO vo) {
        alertChannelMapper.insertChannel(vo);
    }

    @Override
    public void setChannelEnabled(Long channelId, String enabled) {
        alertChannelMapper.updateEnabled(channelId, "Y".equals(enabled) ? "Y" : "N");
    }

    @Override
    public void deleteChannel(Long channelId) {
        alertChannelMapper.deleteChannel(channelId);
    }

    @Override
    public boolean testChannel(Long channelId) {
        String url = null;
        for (AlertChannelVO c : alertChannelMapper.selectChannels()) {
            if (c.getChannelId() != null && c.getChannelId().equals(channelId)) {
                url = c.getUrl();
                break;
            }
        }
        return testSend(url, "채널 #" + channelId);
    }

    @Override
    public boolean testGlobalWebhook() {
        return testSend(webhookUrl, "globals 폴백 URL");
    }

    /** 테스트 발송: 활성/중복억제와 무관하게 즉시 발송하고 TEST 이력으로 기록. */
    private boolean testSend(String url, String target) {
        boolean ok = false;
        if (url != null && !url.trim().isEmpty()) {
            ok = webhookSender.send(url.trim(), "[PMC 테스트] 알림 연결 테스트 — " + target);
        }
        AlertLogVO vo = new AlertLogVO();
        vo.setAlertType("TEST");
        vo.setSeverity("INFO");
        vo.setTitle("[테스트] " + target);
        vo.setMessage("알림 연결 테스트 발송");
        vo.setChannel("WEBHOOK");
        vo.setSentStatus(url == null || url.trim().isEmpty() ? "SKIPPED" : (ok ? "SENT" : "FAILED"));
        alertMapper.insertAlert(vo);
        return ok;
    }
}
