package egovframework.let.pmc.notify;

import egovframework.let.pmc.notify.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * heartbeat 누락 주기 점검 알림(기존 pmcScheduler 사용). 5분 주기, 중복억제는 AlertService 가 처리.
 */
@Component
public class AlertScheduler {

    private final AlertService alertService;

    @Autowired
    public AlertScheduler(AlertService alertService) {
        this.alertService = alertService;
    }

    @Scheduled(fixedRate = 300000)
    public void checkHeartbeats() {
        alertService.raiseHeartbeatAlerts();
    }
}
