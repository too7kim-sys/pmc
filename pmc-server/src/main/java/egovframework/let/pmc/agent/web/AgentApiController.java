package egovframework.let.pmc.agent.web;

import egovframework.let.pmc.agent.service.AgentService;
import egovframework.let.pmc.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent 연동 REST API (/api/v1/agents). 토큰 인증(AgentTokenAuthFilter), register 제외.
 */
@RestController
@RequestMapping("/api/v1/agents")
public class AgentApiController {

    private final AgentService agentService;

    @Autowired
    public AgentApiController(AgentService agentService) {
        this.agentService = agentService;
    }

    /** 등록 : enrollToken → apiKey 발급 */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        Map<String, Object> data = agentService.enroll(
                str(body, "enrollToken"), str(body, "hostname"), str(body, "ipAddr"),
                str(body, "osType"), str(body, "osVersion"), str(body, "agentVersion"));
        return ApiResponse.ok(data);
    }

    /** Heartbeat : 대기 명령/정책버전 반환 */
    @PostMapping("/{agentId}/heartbeat")
    public ApiResponse<Map<String, Object>> heartbeat(@PathVariable String agentId,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        String ver = body != null ? str(body, "agentVersion") : null;
        Integer pv = body != null ? intval(body, "policyVersion") : null;
        return ApiResponse.ok(agentService.heartbeat(agentId, ver, pv));
    }

    /** 대기 명령 조회(폴링 대안) */
    @GetMapping("/{agentId}/commands")
    public ApiResponse<List<?>> commands(@PathVariable String agentId) {
        return ApiResponse.ok(agentService.getPendingCommands(agentId));
    }

    /** 명령 결과 회신 */
    @PostMapping("/{agentId}/commands/{commandId}/ack")
    public ApiResponse<Void> ack(@PathVariable String agentId, @PathVariable Long commandId,
                                 @RequestBody Map<String, Object> body) {
        agentService.ackCommand(commandId, str(body, "status"), str(body, "resultMsg"));
        return ApiResponse.ok(null);
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    private Integer intval(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        try {
            return Integer.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
