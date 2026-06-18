package egovframework.let.pmc.agent.service;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 원격 명령 큐 항목.
 */
public class AgentCommandVO implements Serializable {

    private Long commandId;
    private String agentId;
    private String commandType;   // RUN_NOW/START/STOP/SET_SCHEDULE/UPDATE_CONFIG/UPDATE_POLICY
    private String params;        // JSON
    private String status;        // PENDING/SENT/ACKED/DONE/FAILED
    private String resultMsg;
    private String requestedBy;
    private OffsetDateTime requestedAt;
    private OffsetDateTime pickedAt;
    private OffsetDateTime completedAt;

    public Long getCommandId() { return commandId; }
    public void setCommandId(Long commandId) { this.commandId = commandId; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultMsg() { return resultMsg; }
    public void setResultMsg(String resultMsg) { this.resultMsg = resultMsg; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public OffsetDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(OffsetDateTime requestedAt) { this.requestedAt = requestedAt; }
    public OffsetDateTime getPickedAt() { return pickedAt; }
    public void setPickedAt(OffsetDateTime pickedAt) { this.pickedAt = pickedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}
