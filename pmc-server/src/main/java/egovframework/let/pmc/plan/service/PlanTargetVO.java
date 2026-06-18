package egovframework.let.pmc.plan.service;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class PlanTargetVO implements Serializable {
    private Long planTargetId;
    private Long planId;
    private Long serverId;
    private String resultStatus;   // PENDING/DONE/SKIPPED
    private String runId;
    private OffsetDateTime doneDt;

    // 표시용
    private String hostname;
    private String osType;
    private String serviceName;
    private String overallStatus;  // 연계된 run의 판정

    public Long getPlanTargetId() { return planTargetId; }
    public void setPlanTargetId(Long planTargetId) { this.planTargetId = planTargetId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public OffsetDateTime getDoneDt() { return doneDt; }
    public void setDoneDt(OffsetDateTime doneDt) { this.doneDt = doneDt; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getOsType() { return osType; }
    public void setOsType(String osType) { this.osType = osType; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
}
