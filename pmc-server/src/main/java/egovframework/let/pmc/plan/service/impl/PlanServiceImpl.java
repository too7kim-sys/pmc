package egovframework.let.pmc.plan.service.impl;

import egovframework.let.pmc.agent.service.AgentMapper;
import egovframework.let.pmc.agent.service.AgentService;
import egovframework.let.pmc.ingest.service.InspectionMapper;
import egovframework.let.pmc.ingest.service.InspectionRunVO;
import egovframework.let.pmc.ingest.service.IngestService;
import egovframework.let.pmc.ingest.service.ResultItemVO;
import egovframework.let.pmc.plan.service.PlanMapper;
import egovframework.let.pmc.plan.service.PlanService;
import egovframework.let.pmc.plan.service.PlanTargetVO;
import egovframework.let.pmc.plan.service.PlanVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PlanServiceImpl implements PlanService {

    private final PlanMapper planMapper;
    private final AgentMapper agentMapper;
    private final AgentService agentService;
    private final IngestService ingestService;
    private final InspectionMapper inspectionMapper;

    @Autowired
    public PlanServiceImpl(PlanMapper planMapper, AgentMapper agentMapper, AgentService agentService,
                           IngestService ingestService, InspectionMapper inspectionMapper) {
        this.planMapper = planMapper;
        this.agentMapper = agentMapper;
        this.agentService = agentService;
        this.ingestService = ingestService;
        this.inspectionMapper = inspectionMapper;
    }

    @Override
    public List<PlanVO> getPlanList() {
        return planMapper.selectPlanList();
    }

    @Override
    public PlanVO getPlanDetail(Long planId) {
        PlanVO plan = planMapper.selectPlan(planId);
        if (plan != null) {
            plan.setTargets(planMapper.selectTargets(planId));
        }
        return plan;
    }

    @Override
    @Transactional
    public Long createPlan(PlanVO vo, List<Long> serverIds) {
        planMapper.insertPlan(vo);
        if (serverIds != null) {
            for (Long sid : serverIds) {
                PlanTargetVO t = new PlanTargetVO();
                t.setPlanId(vo.getPlanId());
                t.setServerId(sid);
                planMapper.insertTarget(t);
            }
        }
        return vo.getPlanId();
    }

    @Override
    @Transactional
    public int runAuto(Long planId, String requestedBy) {
        int issued = 0;
        List<PlanTargetVO> targets = planMapper.selectTargets(planId);
        for (PlanTargetVO t : targets) {
            String agentId = agentMapper.selectActiveAgentIdByServer(t.getServerId());
            if (agentId != null) {
                String params = "{\"planId\":" + planId + "}";
                agentService.issueCommand(agentId, "RUN_NOW", params, requestedBy);
                issued++;
            }
        }
        planMapper.updateStatus(planId, "IN_PROGRESS");
        return issued;
    }

    @Override
    @Transactional
    public void linkLatestRun(Long planId, Long serverId) {
        String runId = planMapper.selectLatestRunIdByServer(serverId);
        if (runId != null) {
            inspectionMapper.linkPlanTarget(planId, serverId, runId);
        }
    }

    @Override
    @Transactional
    public void saveManual(Long planId, Long serverId, List<ResultItemVO> items, String inputUser) {
        InspectionRunVO run = new InspectionRunVO();
        run.setRunId(UUID.randomUUID().toString());
        run.setServerId(serverId);
        run.setPlanId(planId);
        for (ResultItemVO it : items) {
            it.setInputUser(inputUser);
        }
        ingestService.saveManualRun(run, items);
    }

    @Override
    @Transactional
    public void requestApproval(Long planId) {
        planMapper.updateStatus(planId, "DONE");
        planMapper.updateApprove(planId, "REQUESTED", null);
    }

    @Override
    @Transactional
    public void approve(Long planId, String opinion) {
        planMapper.updateApprove(planId, "APPROVED", opinion);
    }

    @Override
    @Transactional
    public void reject(Long planId, String opinion) {
        planMapper.updateApprove(planId, "REJECTED", opinion);
    }

    @Override
    @Transactional
    public void markOverdue() {
        planMapper.markOverdue();
    }
}
