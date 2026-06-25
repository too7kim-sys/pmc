package egovframework.let.pmc.vuln.service;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 취약점 예외(수용/면제) — pmc_vuln_exception.
 */
public class VulnExceptionVO implements Serializable {
    private Long exceptionId;
    private Long findingId;
    private String reason;
    private String approver;
    private String status;          // ACTIVE/EXPIRED/REVOKED
    private OffsetDateTime expiresAt;
    private String regUser;
    private OffsetDateTime regDt;

    public Long getExceptionId() { return exceptionId; }
    public void setExceptionId(Long exceptionId) { this.exceptionId = exceptionId; }
    public Long getFindingId() { return findingId; }
    public void setFindingId(Long findingId) { this.findingId = findingId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getRegUser() { return regUser; }
    public void setRegUser(String regUser) { this.regUser = regUser; }
    public OffsetDateTime getRegDt() { return regDt; }
    public void setRegDt(OffsetDateTime regDt) { this.regDt = regDt; }
}
