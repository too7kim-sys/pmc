package egovframework.let.pmc.vuln.service;

import java.io.Serializable;

/**
 * 진단 결과 1건(전달 객체). 내장 점검(result_item) 또는 외부 스캐너 import 를
 * 동일한 형태로 정규화해 {@link VulnService#processFindings} 에 전달한다.
 */
public class VulnFinding implements Serializable {
    private Long serverId;
    private String checkCode;   // SEC_U01_ROOT_REMOTE / CVE-xxxx / 스캐너 rule id
    private String source;      // BUILTIN / SCANNER / MANUAL
    private String category;    // 기본 SEC
    private String title;
    private String severity;    // 상 / 중 / 하

    public VulnFinding() { }

    public VulnFinding(Long serverId, String checkCode, String source, String category,
                       String title, String severity) {
        this.serverId = serverId;
        this.checkCode = checkCode;
        this.source = source;
        this.category = category;
        this.title = title;
        this.severity = severity;
    }

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
}
