package egovframework.let.pmc.agent.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 대상 서버/Agent/명령 큐 매퍼.
 */
@Mapper
public interface AgentMapper {

    // 서버
    void insertServer(AgentVO vo);
    List<AgentVO> selectServerList();
    AgentVO selectServer(@Param("serverId") Long serverId);
    void updateServer(AgentVO vo);

    // Agent
    void insertAgentEnroll(AgentVO vo);                 // INACTIVE + enroll_token
    AgentVO selectAgentByEnrollToken(@Param("token") String token);
    void activateAgent(AgentVO vo);                     // api_key_hash, agent_version, status=ACTIVE
    AgentVO selectAgent(@Param("agentId") String agentId);
    List<AgentVO> selectAgentList();
    String selectActiveAgentIdByServer(@Param("serverId") Long serverId);
    void updateHeartbeat(@Param("agentId") String agentId, @Param("agentVersion") String agentVersion);
    void updateAgentSchedule(@Param("agentId") String agentId, @Param("scheduleCron") String scheduleCron);
    void updateAgentStatus(@Param("agentId") String agentId, @Param("status") String status);
    void touchLastRun(@Param("agentId") String agentId);

    // 명령 큐
    void insertCommand(AgentCommandVO vo);
    List<AgentCommandVO> selectPendingCommands(@Param("agentId") String agentId);
    List<AgentCommandVO> selectCommandHistory(@Param("agentId") String agentId);
    void markCommandsSent(@Param("agentId") String agentId);
    void ackCommand(@Param("agentId") String agentId, @Param("commandId") Long commandId,
                    @Param("status") String status, @Param("resultMsg") String resultMsg);
}
