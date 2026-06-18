package egovframework.let.pmc.ingest.service;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class ResultItemVO implements Serializable {
    private Long resultItemId;
    private String runId;
    private String category;
    private String itemCode;
    private String itemName;
    private String value;
    private String unit;
    private String status;
    private String source;
    private String inputUser;
    private String thresholdWarn;
    private String thresholdCritical;
    private String rawText;
    private String errorText;
    private OffsetDateTime collectedAt;

    public Long getResultItemId() { return resultItemId; }
    public void setResultItemId(Long resultItemId) { this.resultItemId = resultItemId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getInputUser() { return inputUser; }
    public void setInputUser(String inputUser) { this.inputUser = inputUser; }
    public String getThresholdWarn() { return thresholdWarn; }
    public void setThresholdWarn(String thresholdWarn) { this.thresholdWarn = thresholdWarn; }
    public String getThresholdCritical() { return thresholdCritical; }
    public void setThresholdCritical(String thresholdCritical) { this.thresholdCritical = thresholdCritical; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public String getErrorText() { return errorText; }
    public void setErrorText(String errorText) { this.errorText = errorText; }
    public OffsetDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(OffsetDateTime collectedAt) { this.collectedAt = collectedAt; }
}
