package egovframework.let.pmc.notify.service;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class AlertLogVO implements Serializable {
    private Long alertId;
    private String alertType;
    private Long serverId;
    private String refId;
    private String severity;
    private String title;
    private String message;
    private String channel;
    private String sentStatus;
    private OffsetDateTime createdAt;

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getSentStatus() { return sentStatus; }
    public void setSentStatus(String sentStatus) { this.sentStatus = sentStatus; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
