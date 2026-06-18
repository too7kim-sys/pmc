package kr.go.pmc.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * SVC 카테고리 점검 대상(외부 URL).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SvcTarget {

    private String svcName;
    private String url;
    private String httpMethod = "GET";
    private Integer expectedStatus = 200;
    private String expectedContent;
    private Integer timeoutMs = 5000;
    private String sslCheckYn = "N";

    public String getSvcName() {
        return svcName;
    }

    public void setSvcName(String svcName) {
        this.svcName = svcName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(Integer expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public String getExpectedContent() {
        return expectedContent;
    }

    public void setExpectedContent(String expectedContent) {
        this.expectedContent = expectedContent;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getSslCheckYn() {
        return sslCheckYn;
    }

    public void setSslCheckYn(String sslCheckYn) {
        this.sslCheckYn = sslCheckYn;
    }

    public boolean isSslCheck() {
        return "Y".equalsIgnoreCase(sslCheckYn);
    }
}
