package egovframework.let.pmc.vuln.service;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 취약점 조치이력 — pmc_vuln_action.
 */
public class VulnActionVO implements Serializable {
    private Long actionId;
    private Long findingId;
    private String actionUser;      // 담당자
    private String actionDesc;      // 조치내용
    private OffsetDateTime actionDt;
    private String resultingStatus; // FIXED/OPEN/EXEMPTED (nullable)
    private String regUser;
    private OffsetDateTime regDt;

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public Long getFindingId() { return findingId; }
    public void setFindingId(Long findingId) { this.findingId = findingId; }
    public String getActionUser() { return actionUser; }
    public void setActionUser(String actionUser) { this.actionUser = actionUser; }
    public String getActionDesc() { return actionDesc; }
    public void setActionDesc(String actionDesc) { this.actionDesc = actionDesc; }
    public OffsetDateTime getActionDt() { return actionDt; }
    public void setActionDt(OffsetDateTime actionDt) { this.actionDt = actionDt; }
    public String getResultingStatus() { return resultingStatus; }
    public void setResultingStatus(String resultingStatus) { this.resultingStatus = resultingStatus; }
    public String getRegUser() { return regUser; }
    public void setRegUser(String regUser) { this.regUser = regUser; }
    public OffsetDateTime getRegDt() { return regDt; }
    public void setRegDt(OffsetDateTime regDt) { this.regDt = regDt; }
}
