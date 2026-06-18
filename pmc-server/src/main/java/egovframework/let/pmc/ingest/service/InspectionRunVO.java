package egovframework.let.pmc.ingest.service;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

public class InspectionRunVO implements Serializable {
    private String runId;
    private String agentId;
    private Long serverId;
    private Long policyId;
    private Integer policyVersion;
    private Long planId;
    private String runType;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String overallStatus;
    private int itemCount;
    private int warnCount;
    private int criticalCount;
    private int errorCount;
    private String sourceIp;
    private OffsetDateTime receivedAt;

    // 표시용
    private String hostname;
    private List<ResultItemVO> items;

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public Integer getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(Integer policyVersion) { this.policyVersion = policyVersion; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getRunType() { return runType; }
    public void setRunType(String runType) { this.runType = runType; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public int getWarnCount() { return warnCount; }
    public void setWarnCount(int warnCount) { this.warnCount = warnCount; }
    public int getCriticalCount() { return criticalCount; }
    public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(OffsetDateTime receivedAt) { this.receivedAt = receivedAt; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public List<ResultItemVO> getItems() { return items; }
    public void setItems(List<ResultItemVO> items) { this.items = items; }
}
