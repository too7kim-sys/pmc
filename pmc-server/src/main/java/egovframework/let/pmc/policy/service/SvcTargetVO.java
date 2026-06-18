package egovframework.let.pmc.policy.service;

import java.io.Serializable;

public class SvcTargetVO implements Serializable {
    private Long svcTargetId;
    private Long policyId;
    private String svcName;
    private String url;
    private String httpMethod;
    private Integer expectedStatus;
    private String expectedContent;
    private Integer timeoutMs;
    private String sslCheckYn;
    private String useYn;

    public Long getSvcTargetId() { return svcTargetId; }
    public void setSvcTargetId(Long svcTargetId) { this.svcTargetId = svcTargetId; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getSvcName() { return svcName; }
    public void setSvcName(String svcName) { this.svcName = svcName; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public Integer getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(Integer expectedStatus) { this.expectedStatus = expectedStatus; }
    public String getExpectedContent() { return expectedContent; }
    public void setExpectedContent(String expectedContent) { this.expectedContent = expectedContent; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public String getSslCheckYn() { return sslCheckYn; }
    public void setSslCheckYn(String sslCheckYn) { this.sslCheckYn = sslCheckYn; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
}
