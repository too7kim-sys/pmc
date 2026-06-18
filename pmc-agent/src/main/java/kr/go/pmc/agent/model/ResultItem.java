package kr.go.pmc.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * 개별 점검 항목 결과. inspection-result.schema.json 의 items[] 요소와 필드명이 일치한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultItem {

    private Category category;
    private String itemCode;
    private String name;
    private String value;
    private String unit;
    private ResultStatus status;
    private String thresholdWarn;
    private String thresholdCritical;
    private String raw;
    private Instant collectedAt;
    private String error;

    public ResultItem() {
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public ResultStatus getStatus() {
        return status;
    }

    public void setStatus(ResultStatus status) {
        this.status = status;
    }

    public String getThresholdWarn() {
        return thresholdWarn;
    }

    public void setThresholdWarn(String thresholdWarn) {
        this.thresholdWarn = thresholdWarn;
    }

    public String getThresholdCritical() {
        return thresholdCritical;
    }

    public void setThresholdCritical(String thresholdCritical) {
        this.thresholdCritical = thresholdCritical;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
