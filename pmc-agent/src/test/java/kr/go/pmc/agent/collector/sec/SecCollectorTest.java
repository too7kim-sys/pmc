package kr.go.pmc.agent.collector.sec;

import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;
import kr.go.pmc.agent.platform.CommandRunner;
import kr.go.pmc.agent.platform.Platform;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 보안 진단 수집기 공통 헬퍼 + 리눅스 수집기 스모크.
 */
class SecCollectorTest {

    /** 헬퍼 노출용 구체 서브클래스. */
    static class Probe extends AbstractSecCollector {
        public boolean supports(Platform p) { return true; }
        public List<ResultItem> collect(CollectContext ctx) { return newList(); }
    }

    private final Probe probe = new Probe();

    @Test
    void severityMapping_andSevEncoding() {
        ResultItem high = probe.vuln("C", "n", "상", "evi");
        assertEquals(ResultStatus.CRITICAL, high.getStatus());
        assertTrue(high.getRaw().startsWith("SEV=상; "));
        assertEquals("취약", high.getValue());

        assertEquals(ResultStatus.WARN, probe.vuln("C", "n", "중", "e").getStatus());
        assertEquals(ResultStatus.WARN, probe.vuln("C", "n", "하", "e").getStatus());

        ResultItem ok = probe.good("C", "n", "상", "e");
        assertEquals(ResultStatus.NORMAL, ok.getStatus());
        assertEquals("양호", ok.getValue());

        ResultItem na = probe.notApplicable("C", "n", "없음");
        assertEquals(ResultStatus.NA, na.getStatus());
        assertEquals(Category.SEC, na.getCategory());
    }

    @Test
    void findActiveLine_skipsCommentsAndBlanks() {
        List<String> lines = Arrays.asList("# comment", "", "  PermitRootLogin yes  ", "Other x");
        String hit = probe.findActiveLine(lines, "(?i)^permitrootlogin\\s+.*");
        assertEquals("PermitRootLogin yes", hit);
        assertNull(probe.findActiveLine(lines, "(?i)^nomatch.*"));
    }

    @Test
    void octalPerm_readsPosixPermissions() throws Exception {
        Path tmp = Files.createTempFile("sec", ".txt");
        try {
            Files.setPosixFilePermissions(tmp, PosixFilePermissions.fromString("rw-r--r--")); // 644
            assertEquals("644", probe.octalPerm(tmp.toString()));
            Files.setPosixFilePermissions(tmp, PosixFilePermissions.fromString("rw-------")); // 600
            assertEquals("600", probe.octalPerm(tmp.toString()));
        } finally {
            Files.deleteIfExists(tmp);
        }
        assertNull(probe.octalPerm("/no/such/file/xyz"));
    }

    @Test
    void linuxCollector_neverThrows_andAllItemsAreSec() {
        LinuxSecCollector c = new LinuxSecCollector();
        assertTrue(c.supports(Platform.LINUX));
        assertFalse(c.supports(Platform.WINDOWS));
        CollectContext ctx = new CollectContext(Platform.LINUX, new CommandRunner(), null, 3000);
        List<ResultItem> items = c.collect(ctx); // 실제 /etc 파일 읽음 — 예외 없이 SEC 항목 반환
        assertNotNull(items);
        assertFalse(items.isEmpty());
        for (ResultItem it : items) {
            assertEquals(Category.SEC, it.getCategory());
            assertNotNull(it.getStatus());
        }
    }

    @Test
    void platformSupportMatrix() {
        assertTrue(new WindowsSecCollector().supports(Platform.WINDOWS));
        assertTrue(new AixSecCollector().supports(Platform.AIX));
        assertTrue(new HpuxSecCollector().supports(Platform.HPUX));
        assertFalse(new WindowsSecCollector().supports(Platform.LINUX));
    }
}
