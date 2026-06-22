package egovframework.let.pmc.risk.service;

import java.io.Serializable;
import java.util.List;

/**
 * 서버 1대의 위험 점수 산정 결과.
 */
public class RiskScoreVO implements Serializable {
    private Long serverId;
    private String hostname;
    private String serviceName;
    private int score;            // 0~100
    private String level;         // HIGH / MEDIUM / LOW / NONE
    private List<String> reasons; // 가산 사유(사람이 읽는 문자열)

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    /** 레벨 → pmc.css 상태 배지 클래스 매핑(화면 색상 재사용). */
    public String getCssClass() {
        if ("HIGH".equals(level)) return "CRITICAL";
        if ("MEDIUM".equals(level)) return "WARN";
        if ("LOW".equals(level)) return "NA";
        return "NORMAL";
    }

    /** 사유를 한 줄로(보고서/요약용). */
    public String getReasonText() {
        return reasons == null || reasons.isEmpty() ? "-" : String.join(" · ", reasons);
    }
}
