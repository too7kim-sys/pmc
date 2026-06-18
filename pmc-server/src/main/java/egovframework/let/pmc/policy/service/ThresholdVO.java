package egovframework.let.pmc.policy.service;

import java.io.Serializable;

public class ThresholdVO implements Serializable {
    private Long thresholdId;
    private Long policyItemId;
    private String itemCode;   // 조인 표시용
    private String level;      // WARN/CRITICAL
    private String operator;   // GT/GTE/LT/LTE/EQ/RANGE/REGEX
    private String compareValue;
    private String rangeLow;
    private String rangeHigh;

    public Long getThresholdId() { return thresholdId; }
    public void setThresholdId(Long thresholdId) { this.thresholdId = thresholdId; }
    public Long getPolicyItemId() { return policyItemId; }
    public void setPolicyItemId(Long policyItemId) { this.policyItemId = policyItemId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getCompareValue() { return compareValue; }
    public void setCompareValue(String compareValue) { this.compareValue = compareValue; }
    public String getRangeLow() { return rangeLow; }
    public void setRangeLow(String rangeLow) { this.rangeLow = rangeLow; }
    public String getRangeHigh() { return rangeHigh; }
    public void setRangeHigh(String rangeHigh) { this.rangeHigh = rangeHigh; }
}
