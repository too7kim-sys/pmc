package egovframework.let.pmc.policy.service;

import java.util.List;

public interface PolicyService {
    List<PolicyVO> getPolicyList();
    PolicyVO getPolicyDetail(Long policyId);     // items+thresholds+svcTargets 포함
    PolicyVO buildPolicyDoc(Long policyId);      // Agent pull용(동일 구조)

    Long createPolicy(PolicyVO vo);
    void updatePolicy(PolicyVO vo);
    void saveItem(PolicyItemVO item, List<ThresholdVO> thresholds);
    void addSvcTarget(SvcTargetVO vo);
    void deleteSvcTarget(Long svcTargetId, Long policyId);
}
