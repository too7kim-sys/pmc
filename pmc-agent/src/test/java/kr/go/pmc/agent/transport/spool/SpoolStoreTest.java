package kr.go.pmc.agent.transport.spool;

import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.InspectionResult;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpoolStoreTest {

    private InspectionResult sample() {
        InspectionResult r = new InspectionResult();
        r.setRunId(UUID.randomUUID().toString());
        r.setAgentId("agent-1");
        r.setHostname("web01");
        r.setPlatform("LINUX");
        r.setStartedAt(Instant.now());
        r.setFinishedAt(Instant.now());
        ResultItem it = new ResultItem();
        it.setCategory(Category.OS);
        it.setItemCode("OS_CPU_USAGE");
        it.setName("CPU 사용률");
        it.setValue("42.5");
        it.setUnit("%");
        it.setStatus(ResultStatus.NORMAL);
        it.setCollectedAt(Instant.now());
        r.getItems().add(it);
        return r;
    }

    @Test
    void writeReadDeleteRoundTrip(@TempDir Path dir) throws Exception {
        SpoolStore store = new SpoolStore(dir.toString());
        InspectionResult original = sample();

        Path file = store.write(original);
        assertNotNull(file);
        assertEquals(1, store.count());

        InspectionResult loaded = store.read(file);
        assertEquals(original.getRunId(), loaded.getRunId());
        assertEquals("web01", loaded.getHostname());
        assertEquals(1, loaded.getItems().size());
        assertEquals("OS_CPU_USAGE", loaded.getItems().get(0).getItemCode());
        assertEquals(Category.OS, loaded.getItems().get(0).getCategory());
        assertEquals(ResultStatus.NORMAL, loaded.getItems().get(0).getStatus());

        assertTrue(store.delete(file));
        assertEquals(0, store.count());
        assertFalse(store.delete(file)); // 이미 삭제됨
    }

    @Test
    void listFifoOrder(@TempDir Path dir) throws Exception {
        SpoolStore store = new SpoolStore(dir.toString());
        Path f1 = store.write(sample());
        Thread.sleep(20);
        Path f2 = store.write(sample());
        assertEquals(2, store.listFifo().size());
        // FIFO: 먼저 쓴 것이 먼저
        assertEquals(f1.getFileName(), store.listFifo().get(0).getFileName());
        assertEquals(f2.getFileName(), store.listFifo().get(1).getFileName());
    }

    @Test
    void purgeRecentKeepsFiles(@TempDir Path dir) {
        SpoolStore store = new SpoolStore(dir.toString());
        store.write(sample());
        // 방금 쓴 파일은 7일 이내이므로 purge 되지 않음
        assertEquals(0, store.purgeOlderThan(7));
        assertEquals(1, store.count());
    }
}
