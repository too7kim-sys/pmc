package kr.go.pmc.agent.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kr.go.pmc.agent.config.AgentConfig;
import kr.go.pmc.agent.model.AgentCommand;
import kr.go.pmc.agent.model.InspectionResult;
import kr.go.pmc.agent.model.PolicyDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * api-contract.md 의 엔드포인트를 감싼 고수준 서버 클라이언트.
 */
public class ServerClient {

    private static final Logger log = LoggerFactory.getLogger(ServerClient.class);
    private static final String AGENT_VERSION = "1.0.0";

    private final AgentConfig cfg;
    private final HttpTransport http;

    public ServerClient(AgentConfig cfg) {
        this.cfg = cfg;
        this.http = new HttpTransport(cfg.getTimeoutMs());
    }

    public ServerClient(AgentConfig cfg, HttpTransport http) {
        this.cfg = cfg;
        this.http = http;
    }

    private String base() {
        String u = cfg.getServerUrl();
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    /** 1. 등록. 성공 시 결과(RegisterResult) 반환, 실패 시 null. */
    public RegisterResult register() {
        try {
            ObjectNode req = JsonMapper.get().createObjectNode();
            req.put("enrollToken", cfg.getEnrollToken());
            req.put("hostname", cfg.getHostname());
            req.put("ipAddr", cfg.getIpAddr());
            req.put("osType", cfg.getOsType());
            req.put("agentVersion", AGENT_VERSION);
            String body = JsonMapper.get().writeValueAsString(req);
            // 등록은 Bearer 불필요
            String resp = http.postJson(base() + "/api/v1/agents/register", body, null);
            ApiResponse api = parse(resp);
            if (api == null || !api.isSuccess() || api.getData() == null) {
                log.warn("등록 실패 응답: {}", resp);
                return null;
            }
            JsonNode d = api.getData();
            RegisterResult rr = new RegisterResult();
            rr.agentId = text(d, "agentId");
            rr.apiKey = text(d, "apiKey");
            rr.policyId = d.has("policyId") && !d.get("policyId").isNull()
                    ? d.get("policyId").asInt() : null;
            return rr;
        } catch (IOException e) {
            log.warn("등록 통신 실패", e);
            return null;
        } catch (RuntimeException e) {
            log.warn("등록 처리 오류", e);
            return null;
        }
    }

    /** 2. Heartbeat. 명령 리스트 반환(없으면 빈 리스트, 통신 실패 시 null). */
    public HeartbeatResult heartbeat(Integer policyVersion) {
        try {
            ObjectNode req = JsonMapper.get().createObjectNode();
            req.put("agentVersion", AGENT_VERSION);
            if (policyVersion != null) req.put("policyVersion", policyVersion);
            String body = JsonMapper.get().writeValueAsString(req);
            String url = base() + "/api/v1/agents/" + cfg.getAgentId() + "/heartbeat";
            String resp = http.postJson(url, body, cfg.getApiKey());
            ApiResponse api = parse(resp);
            if (api == null || !api.isSuccess() || api.getData() == null) {
                return null;
            }
            JsonNode d = api.getData();
            HeartbeatResult hr = new HeartbeatResult();
            hr.policyVersion = d.has("policyVersion") && !d.get("policyVersion").isNull()
                    ? d.get("policyVersion").asInt() : null;
            hr.policyChanged = d.path("policyChanged").asBoolean(false);
            JsonNode cmds = d.get("commands");
            if (cmds != null && cmds.isArray()) {
                for (JsonNode c : cmds) {
                    AgentCommand cmd = new AgentCommand();
                    if (c.has("commandId")) cmd.setCommandId(c.get("commandId").asLong());
                    cmd.setCommandType(text(c, "commandType"));
                    cmd.setParams(text(c, "params"));
                    hr.commands.add(cmd);
                }
            }
            return hr;
        } catch (IOException e) {
            log.warn("Heartbeat 통신 실패", e);
            return null;
        } catch (RuntimeException e) {
            log.warn("Heartbeat 처리 오류", e);
            return null;
        }
    }

    /**
     * 3. 정책 풀. 변경 없으면 null 반환(이때 changedFlag=false 의미), 변경 시 PolicyDoc 반환.
     * 통신 실패 시 PolicyPullResult.error=true.
     */
    public PolicyPullResult pullPolicy(Integer currentVersion) {
        PolicyPullResult out = new PolicyPullResult();
        try {
            String url = base() + "/api/v1/policies/active?agentId=" + cfg.getAgentId()
                    + "&ver=" + (currentVersion == null ? 0 : currentVersion);
            String resp = http.getJson(url, cfg.getApiKey());
            ApiResponse api = parse(resp);
            if (api == null || !api.isSuccess() || api.getData() == null) {
                out.error = true;
                return out;
            }
            JsonNode d = api.getData();
            // 변경 없음: { "changed": false }
            if (d.has("changed") && !d.get("changed").asBoolean(true)) {
                out.changed = false;
                return out;
            }
            out.changed = true;
            out.policy = JsonMapper.get().treeToValue(d, PolicyDoc.class);
            return out;
        } catch (IOException e) {
            log.warn("정책 풀 통신 실패", e);
            out.error = true;
            return out;
        } catch (RuntimeException e) {
            log.warn("정책 풀 처리 오류", e);
            out.error = true;
            return out;
        }
    }

    /** 4. 결과 전송. 성공 여부 반환. */
    public boolean pushResult(InspectionResult result) {
        try {
            String body = JsonMapper.get().writeValueAsString(result);
            String resp = http.postJson(base() + "/api/v1/inspections/results", body, cfg.getApiKey());
            ApiResponse api = parse(resp);
            boolean ok = api != null && api.isSuccess();
            if (ok) {
                log.info("결과 전송 성공 runId={}", result.getRunId());
            } else {
                log.warn("결과 전송 거부 응답: {}", resp);
            }
            return ok;
        } catch (IOException e) {
            log.warn("결과 전송 통신 실패", e);
            return false;
        } catch (RuntimeException e) {
            log.warn("결과 전송 처리 오류", e);
            return false;
        }
    }

    /** 5. 명령 결과 회신. */
    public boolean ackCommand(long commandId, String status, String resultMsg) {
        try {
            ObjectNode req = JsonMapper.get().createObjectNode();
            req.put("status", status);
            req.put("resultMsg", resultMsg == null ? "" : resultMsg);
            String body = JsonMapper.get().writeValueAsString(req);
            String url = base() + "/api/v1/agents/" + cfg.getAgentId()
                    + "/commands/" + commandId + "/ack";
            String resp = http.postJson(url, body, cfg.getApiKey());
            ApiResponse api = parse(resp);
            return api != null && api.isSuccess();
        } catch (IOException e) {
            log.warn("명령 ack 통신 실패(cmd={})", commandId, e);
            return false;
        } catch (RuntimeException e) {
            log.warn("명령 ack 처리 오류(cmd={})", commandId, e);
            return false;
        }
    }

    private ApiResponse parse(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            return JsonMapper.get().readValue(json, ApiResponse.class);
        } catch (IOException e) {
            log.warn("응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private String text(JsonNode n, String field) {
        JsonNode f = n.get(field);
        return f == null || f.isNull() ? null : f.asText();
    }

    // ---- 결과 DTO ----

    public static class RegisterResult {
        public String agentId;
        public String apiKey;
        public Integer policyId;
    }

    public static class HeartbeatResult {
        public Integer policyVersion;
        public boolean policyChanged;
        public List<AgentCommand> commands = new ArrayList<>();
    }

    public static class PolicyPullResult {
        public boolean changed;
        public boolean error;
        public PolicyDoc policy;
    }
}
