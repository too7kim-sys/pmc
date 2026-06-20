package egovframework.let.pmc.agent.web;

import egovframework.let.pmc.agent.service.AgentService;
import egovframework.let.pmc.common.ApiException;
import egovframework.let.pmc.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
        requireSelf(agentId);
        String ver = body != null ? str(body, "agentVersion") : null;
        Integer pv = body != null ? intval(body, "policyVersion") : null;
        return ApiResponse.ok(agentService.heartbeat(agentId, ver, pv));
    }

    /** 대기 명령 조회(폴링 대안) */
    @GetMapping("/{agentId}/commands")
    public ApiResponse<List<?>> commands(@PathVariable String agentId) {
        requireSelf(agentId);
        return ApiResponse.ok(agentService.getPendingCommands(agentId));
    }

    /** 명령 결과 회신 */
    @PostMapping("/{agentId}/commands/{commandId}/ack")
    public ApiResponse<Void> ack(@PathVariable String agentId, @PathVariable Long commandId,
                                 @RequestBody Map<String, Object> body) {
        requireSelf(agentId);
        String status = str(body, "status");
        if (!ACK_STATUSES.contains(status)) {
            throw new ApiException("INVALID_STATUS", "허용되지 않은 명령 상태입니다: " + status);
        }
        agentService.ackCommand(agentId, commandId, status, str(body, "resultMsg"));
        return ApiResponse.ok(null);
    }

    /** ack 로 허용되는 명령 상태(Agent CommandHandler 가 회신하는 값). */
    private static final List<String> ACK_STATUSES = Arrays.asList("DONE", "FAILED");

    /**
     * path {agentId} 가 인증된 주체(토큰으로 해석된 agentId)와 일치하는지 검증.
     * 불일치 시 403(FORBIDDEN). 다른 Agent 의 큐 조회·명령 위변조(IDOR) 방지.
     */
    private void requireSelf(String agentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principal = auth != null ? auth.getName() : null;
        if (principal == null || !principal.equals(agentId)) {
            throw new ApiException("FORBIDDEN", "해당 Agent 에 대한 권한이 없습니다.");
        }
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
