package egovframework.let.pmc.agent.service;

import java.util.List;
import java.util.Map;

/**
 * 대상 서버/Agent 등록·원격제어 서비스.
 */
public interface AgentService {

    // 웹 UI : 서버/Agent 관리
    Long registerServer(AgentVO vo);
    List<AgentVO> getServerList();
    AgentVO getServer(Long serverId);

    List<AgentVO> getAgentList();
    AgentVO getAgent(String agentId);

    /** 대상 서버에 대해 Agent 등록 토큰 발급(INACTIVE agent 생성). 반환=enrollToken */
    String issueEnrollToken(Long serverId, Long policyId);

    /** 원격 명령 발행(UI) */
    void issueCommand(String agentId, String commandType, String params, String requestedBy);
    List<AgentCommandVO> getCommandHistory(String agentId);

    // Agent API
    /** register : enrollToken 검증 → apiKey 발급 → 활성화. 반환 data 맵 */
    Map<String, Object> enroll(String enrollToken, String hostname, String ipAddr,
                               String osType, String osVersion, String agentVersion);

    /** heartbeat : 갱신 + 대기 명령/정책버전 반환 */
    Map<String, Object> heartbeat(String agentId, String agentVersion, Integer policyVersion);

    /** 대기 명령 조회(폴링) */
    List<AgentCommandVO> getPendingCommands(String agentId);

    /** 명령 결과 회신(agentId 로 명령 소유권 스코프) */
    void ackCommand(String agentId, Long commandId, String status, String resultMsg);
}
