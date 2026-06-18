package kr.go.pmc.agent.control;

import com.fasterxml.jackson.databind.JsonNode;
import kr.go.pmc.agent.AgentDaemon;
import kr.go.pmc.agent.config.AgentConfig;
import kr.go.pmc.agent.config.ConfigLoader;
import kr.go.pmc.agent.model.AgentCommand;
import kr.go.pmc.agent.transport.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * heartbeat 으로 받은 {@link AgentCommand} 들을 처리하고 결과를 ackCommand 로 회신한다.
 * 지원 명령: RUN_NOW, START, STOP, SET_SCHEDULE, UPDATE_CONFIG, UPDATE_POLICY.
 */
public class CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    private final AgentDaemon daemon;

    public CommandHandler(AgentDaemon daemon) {
        this.daemon = daemon;
    }

    public void handleAll(List<AgentCommand> commands) {
        if (commands == null) return;
        for (AgentCommand cmd : commands) {
            handle(cmd);
        }
    }

    public void handle(AgentCommand cmd) {
        if (cmd == null || cmd.getCommandType() == null) return;
        String type = cmd.getCommandType().trim().toUpperCase();
        String msg;
        boolean ok = true;
        try {
            switch (type) {
                case "RUN_NOW":
                    msg = doRunNow(cmd);
                    break;
                case "START":
                    daemon.scheduler().resume();
                    msg = "스케줄러 재개";
                    break;
                case "STOP":
                    daemon.scheduler().stop();
                    msg = "스케줄러 일시정지";
                    break;
                case "SET_SCHEDULE":
                    msg = doSetSchedule(cmd);
                    break;
                case "UPDATE_CONFIG":
                    msg = doUpdateConfig(cmd);
                    break;
                case "UPDATE_POLICY":
                    boolean pulled = daemon.pullPolicy();
                    ok = pulled;
                    msg = pulled ? "정책 갱신 완료" : "정책 갱신 실패";
                    break;
                default:
                    ok = false;
                    msg = "알 수 없는 명령: " + type;
            }
        } catch (RuntimeException e) {
            ok = false;
            msg = "명령 처리 예외: " + e.getMessage();
            log.warn("명령 처리 실패 type={}: {}", type, e.getMessage());
        }

        if (cmd.getCommandId() != null) {
            daemon.client().ackCommand(cmd.getCommandId(), ok ? "DONE" : "FAILED", msg);
        }
        log.info("명령 처리 결과 type={} id={} status={} msg={}",
                type, cmd.getCommandId(), ok ? "DONE" : "FAILED", msg);
    }

    private String doRunNow(AgentCommand cmd) {
        List<String> categories = parseCategories(cmd.getParams());
        daemon.runAndPush(categories.isEmpty() ? null : categories);
        return "점검 실행 categories=" + (categories.isEmpty() ? "ALL" : categories);
    }

    private String doSetSchedule(AgentCommand cmd) {
        JsonNode p = parse(cmd.getParams());
        String spec = null;
        if (p != null) {
            if (p.hasNonNull("cron")) {
                spec = p.get("cron").asText();
            } else if (p.hasNonNull("interval")) {
                spec = "intervalSeconds:" + p.get("interval").asInt();
            } else if (p.hasNonNull("intervalSeconds")) {
                spec = "intervalSeconds:" + p.get("intervalSeconds").asInt();
            }
        }
        if (spec == null) {
            return "스케줄 파라미터 없음";
        }
        daemon.scheduler().setSchedule(spec);
        // 설정 파일에 영속화
        AgentConfig cfg = daemon.config();
        if (spec.startsWith("intervalSeconds:")) {
            cfg.setScheduleCron(null);
            Integer iv = Integer.valueOf(spec.substring("intervalSeconds:".length()));
            cfg.setIntervalSeconds(iv);
        } else {
            cfg.setScheduleCron(spec);
        }
        ConfigLoader.save(daemon.configPath(), cfg);
        return "스케줄 변경: " + spec;
    }

    private String doUpdateConfig(AgentCommand cmd) {
        JsonNode p = parse(cmd.getParams());
        if (p == null) return "설정 파라미터 없음";
        AgentConfig cfg = daemon.config();
        List<String> changed = new ArrayList<>();
        if (p.hasNonNull("serverUrl")) {
            cfg.setServerUrl(p.get("serverUrl").asText());
            changed.add("serverUrl");
        }
        if (p.hasNonNull("heartbeatSeconds")) {
            cfg.setHeartbeatSeconds(p.get("heartbeatSeconds").asInt());
            changed.add("heartbeatSeconds");
        }
        if (p.hasNonNull("intervalSeconds")) {
            cfg.setIntervalSeconds(p.get("intervalSeconds").asInt());
            changed.add("intervalSeconds");
        }
        if (p.hasNonNull("timeoutMs")) {
            cfg.setTimeoutMs(p.get("timeoutMs").asInt());
            changed.add("timeoutMs");
        }
        if (p.hasNonNull("logLevel")) {
            cfg.setLogLevel(p.get("logLevel").asText());
            changed.add("logLevel");
        }
        if (p.has("categories") && p.get("categories").isArray()) {
            List<String> cats = new ArrayList<>();
            for (JsonNode c : p.get("categories")) cats.add(c.asText());
            cfg.setCategories(cats);
            changed.add("categories");
        }
        ConfigLoader.save(daemon.configPath(), cfg);
        return "설정 변경: " + changed;
    }

    private List<String> parseCategories(String params) {
        List<String> out = new ArrayList<>();
        JsonNode p = parse(params);
        if (p != null && p.has("categories") && p.get("categories").isArray()) {
            for (JsonNode c : p.get("categories")) {
                out.add(c.asText());
            }
        }
        return out;
    }

    private JsonNode parse(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            return JsonMapper.get().readTree(json);
        } catch (Exception e) {
            log.warn("명령 params 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
