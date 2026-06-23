package egovframework.let.pmc.notify.service;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class AlertChannelVO implements Serializable {
    private Long channelId;
    private String name;
    private String channelType;
    private String url;
    private String minSeverity;   // WARN / CRITICAL
    private String alertTypes;    // NULL=전체, CSV
    private String enabled;       // Y/N
    private String regUser;
    private OffsetDateTime regDt;

    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMinSeverity() { return minSeverity; }
    public void setMinSeverity(String minSeverity) { this.minSeverity = minSeverity; }
    public String getAlertTypes() { return alertTypes; }
    public void setAlertTypes(String alertTypes) { this.alertTypes = alertTypes; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getRegUser() { return regUser; }
    public void setRegUser(String regUser) { this.regUser = regUser; }
    public OffsetDateTime getRegDt() { return regDt; }
    public void setRegDt(OffsetDateTime regDt) { this.regDt = regDt; }
}
