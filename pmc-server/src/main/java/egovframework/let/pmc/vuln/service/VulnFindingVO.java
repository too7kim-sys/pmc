package egovframework.let.pmc.vuln.service;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 취약점 추적 엔티티(pmc_vuln_finding 행 매핑 + 화면 표시용 조인 필드).
 */
public class VulnFindingVO implements Serializable {
    private Long findingId;
    private Long serverId;
    private String checkCode;
    private String source;            // BUILTIN/SCANNER/MANUAL
    private String category;
    private String title;
    private String severity;          // 상/중/하
    private String status;            // OPEN/FIXED/RECURRED/EXEMPTED
    private OffsetDateTime firstDetectedAt;
    private OffsetDateTime lastDetectedAt;
    private int occurrenceCount;
    private int recurCount;
    private String lastRunId;
    private String regUser;
    private OffsetDateTime regDt;
    private String updUser;
    private OffsetDateTime updDt;

    // 표시용(조인)
    private String hostname;
    private String serviceName;

    public Long getFindingId() { return findingId; }
    public void setFindingId(Long findingId) { this.findingId = findingId; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public String getCheckCode() { return checkCode; }
    public void setCheckCode(String checkCode) { this.checkCode = checkCode; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getFirstDetectedAt() { return firstDetectedAt; }
    public void setFirstDetectedAt(OffsetDateTime firstDetectedAt) { this.firstDetectedAt = firstDetectedAt; }
    public OffsetDateTime getLastDetectedAt() { return lastDetectedAt; }
    public void setLastDetectedAt(OffsetDateTime lastDetectedAt) { this.lastDetectedAt = lastDetectedAt; }
    public int getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(int occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    public int getRecurCount() { return recurCount; }
    public void setRecurCount(int recurCount) { this.recurCount = recurCount; }
    public String getLastRunId() { return lastRunId; }
    public void setLastRunId(String lastRunId) { this.lastRunId = lastRunId; }
    public String getRegUser() { return regUser; }
    public void setRegUser(String regUser) { this.regUser = regUser; }
    public OffsetDateTime getRegDt() { return regDt; }
    public void setRegDt(OffsetDateTime regDt) { this.regDt = regDt; }
    public String getUpdUser() { return updUser; }
    public void setUpdUser(String updUser) { this.updUser = updUser; }
    public OffsetDateTime getUpdDt() { return updDt; }
    public void setUpdDt(OffsetDateTime updDt) { this.updDt = updDt; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    /** 등급 → pmc.css 상태 배지 클래스(상=CRITICAL, 중=WARN, 하=NA). */
    public String getCssClass() {
        if ("상".equals(severity)) return "CRITICAL";
        if ("중".equals(severity)) return "WARN";
        if ("하".equals(severity)) return "NA";
        return "NORMAL";
    }

    /** 상태 → 배지 클래스(OPEN/RECURRED=취약 강조, FIXED=정상, EXEMPTED=중립). */
    public String getStatusCssClass() {
        if ("RECURRED".equals(status)) return "CRITICAL";
        if ("OPEN".equals(status)) return "WARN";
        if ("FIXED".equals(status)) return "NORMAL";
        return "NA"; // EXEMPTED
    }
}
