package egovframework.let.pmc.policy.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PolicyMapper {

    List<PolicyVO> selectPolicyList();
    PolicyVO selectPolicy(@Param("policyId") Long policyId);
    Integer selectPolicyVersion(@Param("policyId") Long policyId);

    List<PolicyItemVO> selectPolicyItems(@Param("policyId") Long policyId);
    List<ThresholdVO> selectThresholds(@Param("policyId") Long policyId);
    List<SvcTargetVO> selectSvcTargets(@Param("policyId") Long policyId);

    void insertPolicy(PolicyVO vo);
    void updatePolicy(PolicyVO vo);
    void bumpVersion(@Param("policyId") Long policyId);

    void insertItem(PolicyItemVO vo);
    void updateItem(PolicyItemVO vo);
    void deleteThresholdsByItem(@Param("policyItemId") Long policyItemId);
    void insertThreshold(ThresholdVO vo);

    void insertSvcTarget(SvcTargetVO vo);
    void deleteSvcTarget(@Param("svcTargetId") Long svcTargetId);
}
