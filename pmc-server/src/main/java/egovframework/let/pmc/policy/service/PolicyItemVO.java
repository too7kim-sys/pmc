package egovframework.let.pmc.policy.service;

import java.io.Serializable;
import java.util.List;

public class PolicyItemVO implements Serializable {
    private Long policyItemId;
    private Long policyId;
    private String category;
    private String midCategory;
    private String itemCode;
    private String itemName;
    private String unit;
    private String checkCycle;
    private String checkMethod;
    private String checkType;     // AUTO/MANUAL
    private String judgeCriteria;
    private String collectYn;
    private Integer sortOrder;
    private List<ThresholdVO> thresholds;

    public Long getPolicyItemId() { return policyItemId; }
    public void setPolicyItemId(Long policyItemId) { this.policyItemId = policyItemId; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getMidCategory() { return midCategory; }
    public void setMidCategory(String midCategory) { this.midCategory = midCategory; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getCheckCycle() { return checkCycle; }
    public void setCheckCycle(String checkCycle) { this.checkCycle = checkCycle; }
    public String getCheckMethod() { return checkMethod; }
    public void setCheckMethod(String checkMethod) { this.checkMethod = checkMethod; }
    public String getCheckType() { return checkType; }
    public void setCheckType(String checkType) { this.checkType = checkType; }
    public String getJudgeCriteria() { return judgeCriteria; }
    public void setJudgeCriteria(String judgeCriteria) { this.judgeCriteria = judgeCriteria; }
    public String getCollectYn() { return collectYn; }
    public void setCollectYn(String collectYn) { this.collectYn = collectYn; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public List<ThresholdVO> getThresholds() { return thresholds; }
    public void setThresholds(List<ThresholdVO> thresholds) { this.thresholds = thresholds; }
}
