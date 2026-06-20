package kr.go.pmc.agent.transport.spool;

import kr.go.pmc.agent.model.InspectionResult;
import kr.go.pmc.agent.transport.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 미전송 점검 결과를 디스크에 보관한다. 경로: {spoolDir}/pending/&lt;runId&gt;.json.gz
 */
public class SpoolStore {

    private static final Logger log = LoggerFactory.getLogger(SpoolStore.class);

    private final Path pendingDir;

    public SpoolStore(String spoolDir) {
        this.pendingDir = Paths.get(spoolDir, "pending");
        try {
            Files.createDirectories(pendingDir);
        } catch (IOException e) {
            log.warn("스풀 디렉토리 생성 실패({})", pendingDir, e);
        }
    }

    public Path pendingDir() {
        return pendingDir;
    }

    /** 결과를 gzip JSON 으로 저장. 저장된 파일 경로 반환(실패 시 null). */
    public Path write(InspectionResult result) {
        String runId = result.getRunId() == null ? "unknown-" + System.nanoTime() : result.getRunId();
        // 파일명 안전화: 영숫자/_/- 외 문자는 '_' 로 치환(경로 우회 '/','..' 차단)
        String safe = runId.replaceAll("[^A-Za-z0-9_-]", "_");
        Path file = pendingDir.resolve(safe + ".json.gz");
        try {
            byte[] json = JsonMapper.get().writeValueAsBytes(result);
            try (OutputStream os = Files.newOutputStream(file);
                 GZIPOutputStream gz = new GZIPOutputStream(os)) {
                gz.write(json);
            }
            log.info("스풀 저장: {}", file);
            return file;
        } catch (IOException e) {
            log.warn("스풀 저장 실패({})", file, e);
            return null;
        }
    }

    /** 파일에서 결과 복원. */
    public InspectionResult read(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file);
             GZIPInputStream gz = new GZIPInputStream(is)) {
            byte[] data = readAll(gz);
            return JsonMapper.get().readValue(
                    new String(data, StandardCharsets.UTF_8), InspectionResult.class);
        }
    }

    /** 보관된 파일 목록(생성 시각 오름차순 = FIFO). */
    public List<Path> listFifo() {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(pendingDir)) return files;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(pendingDir, "*.json.gz")) {
            for (Path p : ds) {
                files.add(p);
            }
        } catch (IOException e) {
            log.warn("스풀 목록 조회 실패: {}", e.getMessage());
            return files;
        }
        files.sort(Comparator.comparingLong(this::mtime));
        return files;
    }

    /** 전송 성공 시 삭제. */
    public boolean delete(Path file) {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("스풀 삭제 실패({}): {}", file, e.getMessage());
            return false;
        }
    }

    /** N일 이전 파일 purge. 삭제 개수 반환. */
    public int purgeOlderThan(int days) {
        int removed = 0;
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        for (Path p : listFifo()) {
            if (Instant.ofEpochMilli(mtime(p)).isBefore(cutoff)) {
                if (delete(p)) removed++;
            }
        }
        if (removed > 0) log.info("오래된 스풀 {}건 purge", removed);
        return removed;
    }

    public int count() {
        return listFifo().size();
    }

    private long mtime(Path p) {
        try {
            FileTime ft = Files.getLastModifiedTime(p);
            return ft.toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private byte[] readAll(InputStream in) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        byte[] buf = new byte[8192];
        int n;
        int total = 0;
        while ((n = in.read(buf)) != -1) {
            byte[] c = new byte[n];
            System.arraycopy(buf, 0, c, 0, n);
            chunks.add(c);
            total += n;
        }
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, out, pos, c.length);
            pos += c.length;
        }
        return out;
    }
}
