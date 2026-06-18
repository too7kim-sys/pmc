package kr.go.pmc.agent.config;

import java.util.Arrays;
import java.util.List;

/**
 * Agent 실행 설정 POJO. YAML/환경변수로부터 로딩되며 등록 성공 시 agentId/apiKey 가 채워진다.
 */
public class AgentConfig {

    /** 전체 카테고리 기본값(7종). */
    public static final List<String> ALL_CATEGORIES =
            Arrays.asList("OS", "WEB", "WAS", "DB", "SW", "NW", "SVC");

    private String serverUrl = "http://localhost:8080";
    private String agentId;           // 등록 전 null
    private String apiKey;            // 등록 전 null
    private String enrollToken;       // 최초 등록용 1회 토큰
    private String hostname;          // null 이면 자동 탐지
    private String ipAddr;            // null 이면 자동 탐지
    private String osType;            // null 이면 플랫폼 자동 탐지
    private List<String> categories = ALL_CATEGORIES;
    private String scheduleCron;      // 예: "0 0 6 * * *" (있으면 interval 보다 우선)
    private int intervalSeconds = 300;
    private int heartbeatSeconds = 60;
    private int timeoutMs = 10000;    // 명령/HTTP 기본 타임아웃
    private String spoolDir = "./spool";
    private String logLevel = "INFO";

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEnrollToken() {
        return enrollToken;
    }

    public void setEnrollToken(String enrollToken) {
        this.enrollToken = enrollToken;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        if (categories != null && !categories.isEmpty()) {
            this.categories = categories;
        }
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public int getHeartbeatSeconds() {
        return heartbeatSeconds;
    }

    public void setHeartbeatSeconds(int heartbeatSeconds) {
        this.heartbeatSeconds = heartbeatSeconds;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getSpoolDir() {
        return spoolDir;
    }

    public void setSpoolDir(String spoolDir) {
        this.spoolDir = spoolDir;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isRegistered() {
        return agentId != null && !agentId.isEmpty()
                && apiKey != null && !apiKey.isEmpty();
    }
}
