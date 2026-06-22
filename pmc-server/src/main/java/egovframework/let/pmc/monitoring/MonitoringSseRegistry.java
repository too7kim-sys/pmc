package egovframework.let.pmc.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 실시간 모니터링 SSE 연결 레지스트리(루트 컨텍스트 싱글톤).
 * 컨트롤러가 연결을 등록하고, 스케줄 브로드캐스터가 등록된 연결로 스냅샷을 전송한다.
 */
@Component
public class MonitoringSseRegistry {

    private static final Logger log = LoggerFactory.getLogger(MonitoringSseRegistry.class);
    /** SSE 연결 타임아웃(ms). 클라이언트 EventSource 는 자동 재연결한다. */
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter create() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> { emitter.complete(); emitters.remove(emitter); });
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    /** 단일 연결에 즉시 1회 전송(연결 직후 초기 스냅샷용). */
    public void sendTo(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }

    /** 등록된 모든 연결로 브로드캐스트. 실패한 연결은 정리. */
    public void broadcast(String event, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public int size() {
        return emitters.size();
    }
}
