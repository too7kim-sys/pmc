package egovframework.let.pmc.report.service;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ReportVO implements Serializable {
    private Long reportId;
    private String reportType;
    private String scopeType;
    private Long planId;
    private Long serverId;
    private String runId;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private String genStatus;
    private OffsetDateTime regDt;

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public LocalDate getPeriodFrom() { return periodFrom; }
    public void setPeriodFrom(LocalDate periodFrom) { this.periodFrom = periodFrom; }
    public LocalDate getPeriodTo() { return periodTo; }
    public void setPeriodTo(LocalDate periodTo) { this.periodTo = periodTo; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getGenStatus() { return genStatus; }
    public void setGenStatus(String genStatus) { this.genStatus = genStatus; }
    public OffsetDateTime getRegDt() { return regDt; }
    public void setRegDt(OffsetDateTime regDt) { this.regDt = regDt; }
}
