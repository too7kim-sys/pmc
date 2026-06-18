package egovframework.let.pmc.agent.service;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 대상 서버 + Agent 통합 표시/입력 VO.
 */
public class AgentVO implements Serializable {

    // 서버
    private Long serverId;
    private String hostname;
    private String ipAddr;
    private String osType;
    private String osVersion;
    private String deptCode;
    private String serviceName;
    private String location;

    // Agent
    private String agentId;
    private String agentVersion;
    private String status;
    private Long policyId;
    private String scheduleCron;
    private String enrollToken;
    private OffsetDateTime lastHeartbeat;
    private OffsetDateTime lastRunAt;

    // 등록 응답용(평문 키는 1회만)
    private String apiKey;

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getIpAddr() { return ipAddr; }
    public void setIpAddr(String ipAddr) { this.ipAddr = ipAddr; }
    public String getOsType() { return osType; }
    public void setOsType(String osType) { this.osType = osType; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getAgentVersion() { return agentVersion; }
    public void setAgentVersion(String agentVersion) { this.agentVersion = agentVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getScheduleCron() { return scheduleCron; }
    public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }
    public String getEnrollToken() { return enrollToken; }
    public void setEnrollToken(String enrollToken) { this.enrollToken = enrollToken; }
    public OffsetDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(OffsetDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public OffsetDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(OffsetDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
