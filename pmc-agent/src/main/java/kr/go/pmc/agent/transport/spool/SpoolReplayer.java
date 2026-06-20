package kr.go.pmc.agent.transport.spool;

import kr.go.pmc.agent.model.InspectionResult;
import kr.go.pmc.agent.transport.ServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 재연결 시 스풀에 쌓인 결과를 FIFO 순으로 전송하고 성공분을 삭제한다.
 * 보관 기한이 지난 항목은 purge.
 */
public class SpoolReplayer {

    private static final Logger log = LoggerFactory.getLogger(SpoolReplayer.class);

    private final SpoolStore store;
    private final ServerClient client;
    private final int retentionDays;

    public SpoolReplayer(SpoolStore store, ServerClient client, int retentionDays) {
        this.store = store;
        this.client = client;
        this.retentionDays = retentionDays <= 0 ? 7 : retentionDays;
    }

    /**
     * 스풀을 비운다. 전송 성공 건수 반환. 한 건이라도 전송 실패하면 즉시 중단(서버 다운 추정).
     * 스케줄러·heartbeat 스레드가 동시에 호출할 수 있으므로 synchronized 로 단일 실행 보장
     * (동일 파일 이중 전송·이중 삭제 race 방지).
     */
    public synchronized int drain() {
        store.purgeOlderThan(retentionDays);
        List<Path> files = store.listFifo();
        if (files.isEmpty()) return 0;

        log.info("스풀 재전송 시작: {}건", files.size());
        int sent = 0;
        for (Path f : files) {
            InspectionResult result;
            try {
                result = store.read(f);
            } catch (IOException e) {
                log.warn("스풀 읽기 실패 — 손상 파일 삭제: {} ({})", f, e.getMessage());
                store.delete(f);
                continue;
            }
            boolean ok = client.pushResult(result);
            if (ok) {
                store.delete(f);
                sent++;
            } else {
                log.warn("스풀 재전송 실패 — 중단(다음 기회에 재시도): {}", f);
                break;
            }
        }
        if (sent > 0) log.info("스풀 재전송 완료: {}건", sent);
        return sent;
    }
}
