package egovframework.let.pmc.policy.web;

import egovframework.let.pmc.agent.service.AgentMapper;
import egovframework.let.pmc.agent.service.AgentVO;
import egovframework.let.pmc.common.ApiResponse;
import egovframework.let.pmc.policy.service.PolicyService;
import egovframework.let.pmc.policy.service.PolicyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * Agent 정책 풀 REST.
 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyApiController {

    private final PolicyService policyService;
    private final AgentMapper agentMapper;

    @Autowired
    public PolicyApiController(PolicyService policyService, AgentMapper agentMapper) {
        this.policyService = policyService;
        this.agentMapper = agentMapper;
    }

    /** 활성 정책 조회. ver 일치 시 changed=false */
    @GetMapping("/active")
    public ApiResponse<Object> active(@RequestParam String agentId,
                                      @RequestParam(required = false) Integer ver) {
        AgentVO agent = agentMapper.selectAgent(agentId);
        if (agent == null || agent.getPolicyId() == null) {
            return ApiResponse.ok(Collections.singletonMap("changed", false));
        }
        PolicyVO doc = policyService.buildPolicyDoc(agent.getPolicyId());
        if (doc == null) {
            return ApiResponse.ok(Collections.singletonMap("changed", false));
        }
        if (ver != null && ver.equals(doc.getVersion())) {
            return ApiResponse.ok(Collections.singletonMap("changed", false));
        }
        return ApiResponse.ok(doc);
    }
}
