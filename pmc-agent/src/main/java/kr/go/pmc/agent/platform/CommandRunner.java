package kr.go.pmc.agent.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 안전한 외부 명령 실행 래퍼.
 * <p>
 * 보안: 정책/사용자 입력을 셸 문자열로 보간(interpolation)하지 않는다. 명령은 항상 argv 배열로
 * 전달한다. 셸 빌트인이 필요한 경우에만 {@link #runShell(long, String)} 을 사용하되, 이는
 * 내부 하드코딩 명령에만 사용해야 한다.
 */
public class CommandRunner {

    private static final Logger log = LoggerFactory.getLogger(CommandRunner.class);

    private Charset charset = StandardCharsets.UTF_8;

    public void setCharset(Charset charset) {
        if (charset != null) {
            this.charset = charset;
        }
    }

    public Charset getCharset() {
        return charset;
    }

    /**
     * argv 배열을 그대로 실행한다(셸 비경유). 외부에서 받은 값이 argv 의 한 원소로 들어가더라도
     * 셸 메타문자 해석이 일어나지 않으므로 안전하다.
     */
    public Result run(long timeoutMs, String... argv) {
        if (argv == null || argv.length == 0) {
            return new Result("", "empty argv", -1, false);
        }
        ProcessBuilder pb = new ProcessBuilder(argv);
        pb.redirectErrorStream(false);
        return exec(pb, timeoutMs, Arrays.toString(argv));
    }

    /**
     * 내부 하드코딩 명령에 한해 셸을 경유해 실행한다(파이프/리다이렉트 필요시).
     * 외부 입력을 절대 전달하지 말 것.
     */
    public Result runShell(long timeoutMs, String hardcodedCmd) {
        String[] argv;
        if (PlatformDetector.detect() == Platform.WINDOWS) {
            argv = new String[]{"cmd.exe", "/c", hardcodedCmd};
        } else {
            argv = new String[]{"/bin/sh", "-c", hardcodedCmd};
        }
        return run(timeoutMs, argv);
    }

    public Result run(long timeoutMs, List<String> argv) {
        return run(timeoutMs, argv.toArray(new String[0]));
    }

    private Result exec(ProcessBuilder pb, long timeoutMs, String desc) {
        Process proc = null;
        try {
            proc = pb.start();
            // 자식 stdin 을 즉시 닫아 EOF 를 알린다(입력 대기 명령의 무한 행 + 디스크립터 누수 방지)
            try {
                proc.getOutputStream().close();
            } catch (IOException ignore) {
                // stdin close 실패는 무시(명령 실행에는 영향 없음)
            }
            // stdout/stderr 를 별도 스레드로 비동기 소비(파이프 버퍼 데드락 방지)
            StreamGobbler outG = new StreamGobbler(proc.getInputStream(), charset);
            StreamGobbler errG = new StreamGobbler(proc.getErrorStream(), charset);
            Thread to = new Thread(outG, "cmd-out");
            Thread te = new Thread(errG, "cmd-err");
            to.setDaemon(true);
            te.setDaemon(true);
            to.start();
            te.start();

            boolean finished = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                proc.destroyForcibly();
                proc.waitFor(2, TimeUnit.SECONDS);
                to.join(1000);
                te.join(1000);
                log.warn("명령 타임아웃({}ms): {}", timeoutMs, desc);
                return new Result(outG.text(), errG.text(), -1, true);
            }
            to.join(2000);
            te.join(2000);
            return new Result(outG.text(), errG.text(), proc.exitValue(), false);
        } catch (IOException e) {
            return new Result("", "IOException: " + e.getMessage(), -1, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result("", "interrupted", -1, true);
        } catch (RuntimeException e) {
            return new Result("", "error: " + e.getMessage(), -1, false);
        } finally {
            // 정상/예외 모든 경로에서 살아있는 프로세스 정리(핸들 누수 방지)
            if (proc != null && proc.isAlive()) {
                proc.destroyForcibly();
            }
        }
    }

    private static final class StreamGobbler implements Runnable {
        private final InputStream in;
        private final Charset cs;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

        StreamGobbler(InputStream in, Charset cs) {
            this.in = in;
            this.cs = cs;
        }

        @Override
        public void run() {
            byte[] tmp = new byte[4096];
            int n;
            try {
                while ((n = in.read(tmp)) != -1) {
                    buf.write(tmp, 0, n);
                }
            } catch (IOException ignore) {
                // 프로세스 강제 종료 시 발생 가능 — 무시
            }
        }

        String text() {
            return new String(buf.toByteArray(), cs);
        }
    }

    /**
     * 명령 실행 결과.
     */
    public static final class Result {
        private final String stdout;
        private final String stderr;
        private final int exitCode;
        private final boolean timedOut;

        public Result(String stdout, String stderr, int exitCode, boolean timedOut) {
            this.stdout = stdout == null ? "" : stdout;
            this.stderr = stderr == null ? "" : stderr;
            this.exitCode = exitCode;
            this.timedOut = timedOut;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public int getExitCode() {
            return exitCode;
        }

        public boolean isTimedOut() {
            return timedOut;
        }

        public boolean isSuccess() {
            return !timedOut && exitCode == 0;
        }

        /** stdout 첫 줄(없으면 빈 문자열). */
        public String firstLine() {
            String s = stdout.trim();
            if (s.isEmpty()) return "";
            int nl = s.indexOf('\n');
            return nl < 0 ? s : s.substring(0, nl).trim();
        }
    }
}
