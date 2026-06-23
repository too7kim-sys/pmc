package egovframework.let.pmc.notify.service.impl;

import egovframework.let.pmc.ingest.service.InspectionRunVO;
import egovframework.let.pmc.notify.WebhookSender;
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
    private final WebhookSender webhookSender;

    @Value("${Globals.AlertEnabled:false}")
    private boolean alertEnabled;
    @Value("${Globals.AlertWebhookUrl:}")
    private String webhookUrl;
    @Value("${Globals.AlertDedupeMin:30}")
    private int dedupeMin;

    @Autowired
    public AlertServiceImpl(AlertMapper alertMapper, WebhookSender webhookSender) {
        this.alertMapper = alertMapper;
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
        boolean ok = webhookSender.send(webhookUrl, title + "\n" + message);
        AlertLogVO vo = new AlertLogVO();
        vo.setAlertType(alertType);
        vo.setServerId(serverId);
        vo.setRefId(refId);
        vo.setSeverity(severity);
        vo.setTitle(title);
        vo.setMessage(message);
        vo.setChannel("WEBHOOK");
        vo.setSentStatus(ok ? "SENT" : "FAILED");
        alertMapper.insertAlert(vo);
    }

    @Override
    public List<AlertLogVO> getAlertLog(int limit) {
        return alertMapper.selectAlertLog(limit);
    }
}
