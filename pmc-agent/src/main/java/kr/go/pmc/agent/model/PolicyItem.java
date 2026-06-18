package kr.go.pmc.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 정책상의 점검 항목 정의.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyItem {

    private String category;
    private String itemCode;
    private String itemName;
    private String unit;
    private String checkType;          // AUTO | MANUAL
    private String collectYn = "Y";    // 수집 여부
    private List<ThresholdRule> thresholds = new ArrayList<>();

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getCollectYn() {
        return collectYn;
    }

    public void setCollectYn(String collectYn) {
        this.collectYn = collectYn;
    }

    public List<ThresholdRule> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<ThresholdRule> thresholds) {
        this.thresholds = thresholds;
    }

    public boolean isEnabled() {
        return collectYn == null || "Y".equalsIgnoreCase(collectYn);
    }
}
