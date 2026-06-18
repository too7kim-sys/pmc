package egovframework.let.pmc.policy.service.impl;

import egovframework.let.pmc.policy.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PolicyServiceImpl implements PolicyService {

    private final PolicyMapper policyMapper;

    @Autowired
    public PolicyServiceImpl(PolicyMapper policyMapper) {
        this.policyMapper = policyMapper;
    }

    @Override
    public List<PolicyVO> getPolicyList() {
        return policyMapper.selectPolicyList();
    }

    @Override
    public PolicyVO getPolicyDetail(Long policyId) {
        return buildPolicyDoc(policyId);
    }

    @Override
    public PolicyVO buildPolicyDoc(Long policyId) {
        PolicyVO policy = policyMapper.selectPolicy(policyId);
        if (policy == null) {
            return null;
        }
        List<PolicyItemVO> items = policyMapper.selectPolicyItems(policyId);
        List<ThresholdVO> thresholds = policyMapper.selectThresholds(policyId);

        // itemCode -> thresholds 그룹
        Map<String, List<ThresholdVO>> byItem = new LinkedHashMap<>();
        for (ThresholdVO t : thresholds) {
            byItem.computeIfAbsent(t.getItemCode(), k -> new ArrayList<>()).add(t);
        }
        for (PolicyItemVO it : items) {
            it.setThresholds(byItem.getOrDefault(it.getItemCode(), new ArrayList<>()));
        }
        policy.setItems(items);
        policy.setSvcTargets(policyMapper.selectSvcTargets(policyId));
        return policy;
    }

    @Override
    @Transactional
    public Long createPolicy(PolicyVO vo) {
        policyMapper.insertPolicy(vo);
        return vo.getPolicyId();
    }

    @Override
    @Transactional
    public void updatePolicy(PolicyVO vo) {
        policyMapper.updatePolicy(vo);
        policyMapper.bumpVersion(vo.getPolicyId());
    }

    @Override
    @Transactional
    public void saveItem(PolicyItemVO item, List<ThresholdVO> thresholds) {
        if (item.getPolicyItemId() == null) {
            policyMapper.insertItem(item);
        } else {
            policyMapper.updateItem(item);
            policyMapper.deleteThresholdsByItem(item.getPolicyItemId());
        }
        if (thresholds != null) {
            for (ThresholdVO t : thresholds) {
                if (t.getOperator() == null || t.getOperator().isEmpty()) {
                    continue;
                }
                t.setPolicyItemId(item.getPolicyItemId());
                policyMapper.insertThreshold(t);
            }
        }
        policyMapper.bumpVersion(item.getPolicyId());
    }

    @Override
    @Transactional
    public void addSvcTarget(SvcTargetVO vo) {
        policyMapper.insertSvcTarget(vo);
        policyMapper.bumpVersion(vo.getPolicyId());
    }

    @Override
    @Transactional
    public void deleteSvcTarget(Long svcTargetId, Long policyId) {
        policyMapper.deleteSvcTarget(svcTargetId);
        if (policyId != null) {
            policyMapper.bumpVersion(policyId);
        }
    }
}
