package egovframework.let.pmc.monitoring.web;

import egovframework.let.pmc.monitoring.MonitoringSseRegistry;
import egovframework.let.pmc.monitoring.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 실시간 시스템 상태 SSE 스트림. 세션 평면(/pmc/**, 인증 필요). GET 이라 CSRF 무관.
 * 연결 직후 1회 스냅샷 전송, 이후 MonitoringBroadcaster 가 5초 주기로 푸시.
 */
@RestController
public class MonitoringStreamController {

    private final MonitoringSseRegistry registry;
    private final MonitoringService monitoringService;

    @Autowired
    public MonitoringStreamController(MonitoringSseRegistry registry, MonitoringService monitoringService) {
        this.registry = registry;
        this.monitoringService = monitoringService;
    }

    @GetMapping("/pmc/monitoring/stream")
    public SseEmitter stream() {
        SseEmitter emitter = registry.create();
        registry.sendTo(emitter, "snapshot", monitoringService.getRealtimeSnapshot());
        return emitter;
    }
}
