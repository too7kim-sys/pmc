package egovframework.let.pmc.monitoring;

import egovframework.let.pmc.monitoring.service.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 실시간 시스템 상태 스냅샷을 5초 주기로 SSE 연결에 푸시(연결이 있을 때만 조회).
 * 기존 pmcScheduler(task:scheduler) 로 구동.
 */
@Component
public class MonitoringBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(MonitoringBroadcaster.class);

    private final MonitoringSseRegistry registry;
    private final MonitoringService monitoringService;

    @Autowired
    public MonitoringBroadcaster(MonitoringSseRegistry registry, MonitoringService monitoringService) {
        this.registry = registry;
        this.monitoringService = monitoringService;
    }

    @Scheduled(fixedRate = 5000)
    public void tick() {
        if (registry.size() == 0) {
            return; // 구독자 없으면 DB 조회 생략
        }
        try {
            registry.broadcast("snapshot", monitoringService.getRealtimeSnapshot());
        } catch (Exception e) {
            log.warn("실시간 스냅샷 브로드캐스트 실패: {}", e.getMessage());
        }
    }
}
