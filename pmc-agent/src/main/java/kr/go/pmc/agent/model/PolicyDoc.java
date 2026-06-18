package kr.go.pmc.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 서버에서 내려받은 점검 정책 전체.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyDoc {

    private Integer policyId;
    private Integer version;
    private String scheduleCron;
    private List<PolicyItem> items = new ArrayList<>();
    private List<SvcTarget> svcTargets = new ArrayList<>();

    public Integer getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Integer policyId) {
        this.policyId = policyId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public List<PolicyItem> getItems() {
        return items;
    }

    public void setItems(List<PolicyItem> items) {
        this.items = items;
    }

    public List<SvcTarget> getSvcTargets() {
        return svcTargets;
    }

    public void setSvcTargets(List<SvcTarget> svcTargets) {
        this.svcTargets = svcTargets;
    }

    /** itemCode 로 PolicyItem 조회 (없으면 null). */
    public PolicyItem findItem(String itemCode) {
        if (itemCode == null || items == null) return null;
        for (PolicyItem it : items) {
            if (itemCode.equals(it.getItemCode())) return it;
        }
        return null;
    }

    /** 해당 itemCode 가 수집 대상(collectYn=Y)인지. 정책에 없으면 기본 true(전체 수집). */
    public boolean isItemEnabled(String itemCode) {
        PolicyItem it = findItem(itemCode);
        return it == null || it.isEnabled();
    }
}
