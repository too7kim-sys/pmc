package kr.go.pmc.agent;

import kr.go.pmc.agent.config.AgentConfig;
import kr.go.pmc.agent.config.ConfigLoader;
import kr.go.pmc.agent.control.CommandHandler;
import kr.go.pmc.agent.engine.InspectionEngine;
import kr.go.pmc.agent.model.InspectionResult;
import kr.go.pmc.agent.model.PolicyDoc;
import kr.go.pmc.agent.schedule.InspectionScheduler;
import kr.go.pmc.agent.transport.ServerClient;
import kr.go.pmc.agent.transport.spool.SpoolReplayer;
import kr.go.pmc.agent.transport.spool.SpoolStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent 데몬 라이프사이클. 서비스 래퍼(systemd/procrun/init.d)에서 start()/stop() 으로 사용.
 * commons-daemon 의존성 없이 단순 스레드 루프로 구성한다.
 *
 * <p>루프: heartbeat → 명령 처리 → (정책 변경 시 재풀) → 스풀 드레인. 스케줄 점검은 별도
 * 스케줄러 스레드에서 실행된다.</p>
 */
public class AgentDaemon {

    private static final Logger log = LoggerFactory.getLogger(AgentDaemon.class);
    private static final int SPOOL_RETENTION_DAYS = 7;

    private final String configPath;
    private final AgentConfig cfg;
    private final ServerClient client;
    private final InspectionEngine engine;
    private final SpoolStore spool;
    private final SpoolReplayer replayer;
    private final InspectionScheduler scheduler;
    private final CommandHandler commandHandler;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile PolicyDoc policy;
    private volatile Integer policyVersion;
    private Thread loopThread;

    public AgentDaemon(String configPath, AgentConfig cfg) {
        this.configPath = configPath;
        this.cfg = cfg;
        this.client = new ServerClient(cfg);
        this.engine = new InspectionEngine(cfg);
        this.spool = new SpoolStore(cfg.getSpoolDir());
        this.replayer = new SpoolReplayer(spool, client, SPOOL_RETENTION_DAYS);
        this.scheduler = new InspectionScheduler(this::runAndPush);
        this.commandHandler = new CommandHandler(this);
    }

    // ---- 접근자 (CommandHandler 용) ----

    public AgentConfig config() {
        return cfg;
    }

    public String configPath() {
        return configPath;
    }

    public ServerClient client() {
        return client;
    }

    public InspectionScheduler scheduler() {
        return scheduler;
    }

    public PolicyDoc policy() {
        return policy;
    }

    public void setPolicy(PolicyDoc p) {
        this.policy = p;
        if (p != null) this.policyVersion = p.getVersion();
    }

    public Integer policyVersion() {
        return policyVersion;
    }

    // ---- 점검 실행 + 전송(스풀 폴백) ----

    /**
     * 지정 카테고리 점검 후 서버 전송. 실패 시 스풀에 저장한다.
     */
    public InspectionResult runAndPush(List<String> categories) {
        String runType = (categories != null && !categories.isEmpty()) ? "MANUAL" : "AUTO";
        InspectionResult result = engine.run(policy, categories, runType);
        boolean ok = client.pushResult(result);
        if (!ok) {
            spool.write(result);
            log.info("전송 실패 — 스풀 저장 runId={}", result.getRunId());
        } else {
            // 전송 성공했으니 밀린 스풀도 함께 비움
            replayer.drain();
        }
        return result;
    }

    public int drainSpool() {
        return replayer.drain();
    }

    // ---- 등록 ----

    /**
     * 미등록이면 등록을 시도하고 성공 시 설정에 agentId/apiKey 저장.
     * @return 등록(또는 기존 등록) 성공 여부
     */
    public boolean ensureRegistered() {
        if (cfg.isRegistered()) {
            return true;
        }
        if (cfg.getEnrollToken() == null || cfg.getEnrollToken().isEmpty()) {
            log.error("미등록 상태이며 enrollToken 도 없음 — 등록 불가");
            return false;
        }
        ServerClient.RegisterResult rr = client.register();
        if (rr == null || rr.agentId == null || rr.apiKey == null) {
            log.error("등록 실패");
            return false;
        }
        cfg.setAgentId(rr.agentId);
        cfg.setApiKey(rr.apiKey);
        ConfigLoader.save(configPath, cfg);
        log.info("등록 성공 agentId={} (apiKey 저장됨)", rr.agentId);
        return true;
    }

    /** 정책 강제 풀. 변경 시 적용. */
    public boolean pullPolicy() {
        ServerClient.PolicyPullResult ppr = client.pullPolicy(policyVersion);
        if (ppr.error) {
            log.warn("정책 풀 실패");
            return false;
        }
        if (ppr.changed && ppr.policy != null) {
            setPolicy(ppr.policy);
            log.info("정책 갱신 v{}", ppr.policy.getVersion());
            // 정책에 cron 이 있으면 스케줄 반영
            if (ppr.policy.getScheduleCron() != null && !ppr.policy.getScheduleCron().isEmpty()) {
                scheduler.setSchedule(ppr.policy.getScheduleCron());
                cfg.setScheduleCron(ppr.policy.getScheduleCron());
                ConfigLoader.save(configPath, cfg);
            }
        }
        return true;
    }

    // ---- 라이프사이클 ----

    /** --once 모드: 등록→정책풀→전체점검→전송→스풀드레인. */
    public InspectionResult runOnce() {
        if (!ensureRegistered()) {
            log.warn("미등록 — 오프라인 점검 후 스풀만 수행");
        } else {
            pullPolicy();
        }
        InspectionResult result = runAndPush(null);
        drainSpool();
        return result;
    }

    /** --daemon 모드 시작. */
    public synchronized void start() {
        if (running.getAndSet(true)) {
            return;
        }
        log.info("PMC Agent 데몬 시작");
        ensureRegistered();
        pullPolicy();
        // 스케줄러 기동(정책 cron 우선, 없으면 설정 cron/인터벌)
        String cron = policy != null && policy.getScheduleCron() != null
                ? policy.getScheduleCron() : cfg.getScheduleCron();
        scheduler.start(cron, cfg.getIntervalSeconds());

        loopThread = new Thread(this::heartbeatLoop, "pmc-heartbeat");
        loopThread.setDaemon(false);
        loopThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "pmc-shutdown"));
    }

    private void heartbeatLoop() {
        while (running.get()) {
            try {
                ServerClient.HeartbeatResult hr = client.heartbeat(policyVersion);
                if (hr != null) {
                    if (hr.policyChanged) {
                        pullPolicy();
                    }
                    if (!hr.commands.isEmpty()) {
                        commandHandler.handleAll(hr.commands);
                    }
                    // 재연결 성공 신호 — 스풀 드레인
                    drainSpool();
                }
            } catch (RuntimeException e) {
                log.warn("Heartbeat 루프 오류: {}", e.getMessage());
            }
            sleep(cfg.getHeartbeatSeconds() * 1000L);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }

    /** 데몬 종료. */
    public synchronized void stop() {
        if (!running.getAndSet(false)) {
            return;
        }
        log.info("PMC Agent 데몬 종료");
        scheduler.shutdown();
        if (loopThread != null) {
            loopThread.interrupt();
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
