package egovframework.let.pmc.agent.service.impl;

import egovframework.let.pmc.agent.service.AgentCommandVO;
import egovframework.let.pmc.agent.service.AgentMapper;
import egovframework.let.pmc.agent.service.AgentService;
import egovframework.let.pmc.agent.service.AgentVO;
import egovframework.let.pmc.common.ApiException;
import egovframework.let.pmc.common.security.Hashing;
import egovframework.let.pmc.policy.service.PolicyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 등록·원격제어 서비스 구현.
 */
@Service
public class AgentServiceImpl implements AgentService {

    private final AgentMapper agentMapper;
    private final PolicyMapper policyMapper;

    /** 등록 토큰 유효시간(분). 이 윈도우 내 enroll 재시도는 키 재발급(회전)으로 멱등 처리. */
    @Value("${Globals.EnrollTokenTtlMin}")
    private int enrollTokenTtlMin;

    @Autowired
    public AgentServiceImpl(AgentMapper agentMapper, PolicyMapper policyMapper) {
        this.agentMapper = agentMapper;
        this.policyMapper = policyMapper;
    }

    @Override
    @Transactional
    public Long registerServer(AgentVO vo) {
        agentMapper.insertServer(vo);
        return vo.getServerId();
    }

    @Override
    public List<AgentVO> getServerList() {
        return agentMapper.selectServerList();
    }

    @Override
    public AgentVO getServer(Long serverId) {
        return agentMapper.selectServer(serverId);
    }

    @Override
    public List<AgentVO> getAgentList() {
        return agentMapper.selectAgentList();
    }

    @Override
    public AgentVO getAgent(String agentId) {
        return agentMapper.selectAgent(agentId);
    }

    @Override
    @Transactional
    public String issueEnrollToken(Long serverId, Long policyId) {
        AgentVO vo = new AgentVO();
        vo.setAgentId(UUID.randomUUID().toString());
        vo.setServerId(serverId);
        vo.setPolicyId(policyId);
        vo.setEnrollToken(Hashing.randomToken(16));
        agentMapper.insertAgentEnroll(vo);
        return vo.getEnrollToken();
    }

    @Override
    @Transactional
    public void issueCommand(String agentId, String commandType, String params, String requestedBy) {
        AgentCommandVO cmd = new AgentCommandVO();
        cmd.setAgentId(agentId);
        cmd.setCommandType(commandType);
        cmd.setParams(params);
        cmd.setRequestedBy(requestedBy);
        agentMapper.insertCommand(cmd);
        // 실행주기 변경은 서버 레지스트리에도 즉시 반영(표시용)
        if ("SET_SCHEDULE".equals(commandType) && params != null) {
            String cron = extractJsonValue(params, "cron");
            if (cron != null) {
                agentMapper.updateAgentSchedule(agentId, cron);
            }
        }
    }

    @Override
    public List<AgentCommandVO> getCommandHistory(String agentId) {
        return agentMapper.selectCommandHistory(agentId);
    }

    @Override
    @Transactional
    public Map<String, Object> enroll(String enrollToken, String hostname, String ipAddr,
                                      String osType, String osVersion, String agentVersion) {
        // TTL 윈도우 내에서만 토큰 유효(만료/소진 토큰 거부). 재시도 시 키를 회전해 멱등 동작.
        AgentVO agent = agentMapper.selectAgentByEnrollToken(enrollToken, enrollTokenTtlMin);
        if (agent == null) {
            throw new ApiException("INVALID_TOKEN", "유효하지 않거나 만료된 등록 토큰입니다.");
        }
        String apiKey = Hashing.randomToken(24);
        AgentVO upd = new AgentVO();
        upd.setAgentId(agent.getAgentId());
        upd.setApiKey(Hashing.sha256(apiKey)); // 저장은 해시(이전 키 무효화=회전)
        upd.setAgentVersion(agentVersion);
        agentMapper.activateAgent(upd);

        Map<String, Object> data = new HashMap<>();
        data.put("agentId", agent.getAgentId());
        data.put("apiKey", apiKey); // 평문은 1회만 노출
        data.put("policyId", agent.getPolicyId());
        return data;
    }

    @Override
    @Transactional
    public Map<String, Object> heartbeat(String agentId, String agentVersion, Integer policyVersion) {
        agentMapper.updateHeartbeat(agentId, agentVersion);
        AgentVO agent = agentMapper.selectAgent(agentId);

        // 재전달 한도 소진 명령 만료 후, 대기/재전달 대상 조회 → 전달 표시
        agentMapper.expireStaleCommands(agentId);
        List<AgentCommandVO> pending = agentMapper.selectPendingCommands(agentId);
        if (!pending.isEmpty()) {
            agentMapper.markCommandsSent(agentId);
        }
        Integer currentVer = (agent != null && agent.getPolicyId() != null)
                ? policyMapper.selectPolicyVersion(agent.getPolicyId()) : null;

        Map<String, Object> data = new HashMap<>();
        data.put("policyVersion", currentVer);
        data.put("policyChanged", currentVer != null && !currentVer.equals(policyVersion));
        List<Map<String, Object>> cmds = new ArrayList<>();
        for (AgentCommandVO c : pending) {
            Map<String, Object> m = new HashMap<>();
            m.put("commandId", c.getCommandId());
            m.put("commandType", c.getCommandType());
            m.put("params", c.getParams());
            cmds.add(m);
        }
        data.put("commands", cmds);
        return data;
    }

    @Override
    public List<AgentCommandVO> getPendingCommands(String agentId) {
        return agentMapper.selectPendingCommands(agentId);
    }

    @Override
    @Transactional
    public void ackCommand(String agentId, Long commandId, String status, String resultMsg) {
        agentMapper.ackCommand(agentId, commandId, status, resultMsg);
    }

    /** 아주 단순한 JSON 값 추출(평탄 객체 전제). 의존성 추가 없이 사용. */
    private String extractJsonValue(String json, String key) {
        String pat = "\"" + key + "\"";
        int i = json.indexOf(pat);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + pat.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }
}
