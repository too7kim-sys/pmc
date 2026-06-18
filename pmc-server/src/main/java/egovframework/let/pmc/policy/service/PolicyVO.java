package egovframework.let.pmc.policy.service;

import java.io.Serializable;
import java.util.List;

public class PolicyVO implements Serializable {
    private Long policyId;
    private String policyName;
    private String description;
    private Integer version;
    private String scheduleCron;
    private String osScope;
    private String useYn;
    private List<PolicyItemVO> items;
    private List<SvcTargetVO> svcTargets;

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getScheduleCron() { return scheduleCron; }
    public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }
    public String getOsScope() { return osScope; }
    public void setOsScope(String osScope) { this.osScope = osScope; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public List<PolicyItemVO> getItems() { return items; }
    public void setItems(List<PolicyItemVO> items) { this.items = items; }
    public List<SvcTargetVO> getSvcTargets() { return svcTargets; }
    public void setSvcTargets(List<SvcTargetVO> svcTargets) { this.svcTargets = svcTargets; }
}
