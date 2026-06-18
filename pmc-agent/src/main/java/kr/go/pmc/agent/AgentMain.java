package kr.go.pmc.agent;

import kr.go.pmc.agent.config.AgentConfig;
import kr.go.pmc.agent.config.ConfigLoader;
import kr.go.pmc.agent.platform.Platform;
import kr.go.pmc.agent.platform.PlatformDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;

/**
 * PMC Agent 진입점.
 * <pre>
 *   --config &lt;path&gt;   설정 파일 경로 (기본 ./conf/agent.yml)
 *   --once             등록(필요시)→정책풀→전체 점검 1회→전송→스풀 비우기→종료
 *   --daemon           등록 후 heartbeat/명령/스케줄 점검 루프 상주
 *   --help             도움말
 * </pre>
 */
public final class AgentMain {

    private static final Logger log = LoggerFactory.getLogger(AgentMain.class);

    private AgentMain() {
    }

    public static void main(String[] args) {
        String configPath = "./conf/agent.yml";
        String mode = null;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--config":
                    if (i + 1 < args.length) {
                        configPath = args[++i];
                    }
                    break;
                case "--once":
                    mode = "once";
                    break;
                case "--daemon":
                    mode = "daemon";
                    break;
                case "--help":
                case "-h":
                    printHelp();
                    return;
                default:
                    System.err.println("알 수 없는 인자: " + a);
                    printHelp();
                    return;
            }
        }

        if (mode == null) {
            printHelp();
            return;
        }

        // 설정 파일 경로가 없고 기본값도 존재하지 않으면 classpath agent.yml 사용
        if (!new File(configPath).exists()) {
            log.info("설정 파일이 없어 기본값/클래스패스 사용: {}", configPath);
        }
        AgentConfig cfg = ConfigLoader.load(configPath);
        autoFill(cfg);

        AgentDaemon daemon = new AgentDaemon(configPath, cfg);

        if ("once".equals(mode)) {
            log.info("=== --once 모드 실행 ===");
            daemon.runOnce();
            log.info("=== --once 완료 ===");
        } else {
            log.info("=== --daemon 모드 실행 ===");
            daemon.start();
            // 메인 스레드 대기 (heartbeat 스레드는 non-daemon)
            try {
                while (daemon.isRunning()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                daemon.stop();
            }
        }
    }

    /** hostname/ipAddr/osType 자동 보정. */
    static void autoFill(AgentConfig cfg) {
        if (cfg.getHostname() == null || cfg.getHostname().isEmpty()) {
            cfg.setHostname(detectHostname());
        }
        if (cfg.getIpAddr() == null || cfg.getIpAddr().isEmpty()) {
            cfg.setIpAddr(detectIp());
        }
        if (cfg.getOsType() == null || cfg.getOsType().isEmpty()) {
            Platform p = PlatformDetector.detect();
            cfg.setOsType(p.name());
        }
    }

    private static String detectHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            String env = System.getenv("HOSTNAME");
            return env != null ? env : "unknown-host";
        }
    }

    private static String detectIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private static void printHelp() {
        System.out.println(
                "PMC Agent 1.0.0 — 장애예방점검 자동화 수집 데몬\n" +
                "\n사용법: java -jar pmc-agent-1.0.0.jar [옵션]\n" +
                "\n옵션:\n" +
                "  --config <path>   설정 파일 경로 (기본 ./conf/agent.yml)\n" +
                "  --once            등록(필요시)→정책풀→전체 점검 1회→전송→스풀 비우기→종료\n" +
                "  --daemon          상주 모드 (heartbeat→명령→스케줄 점검 루프)\n" +
                "  --help, -h        이 도움말\n" +
                "\n환경변수 오버라이드: PMC_SERVER_URL, PMC_API_KEY, PMC_AGENT_ID\n");
    }
}
