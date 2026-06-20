package egovframework.let.pmc.ingest.service.impl;

import egovframework.let.pmc.agent.service.AgentMapper;
import egovframework.let.pmc.agent.service.AgentVO;
import egovframework.let.pmc.ingest.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 점검 결과 수신(ingest). run_id 멱등, 서버측 카운트/판정 집계, 정기점검 계획 연계.
 */
@Service
public class IngestServiceImpl implements IngestService {

    private final InspectionMapper inspectionMapper;
    private final AgentMapper agentMapper;

    @Autowired
    public IngestServiceImpl(InspectionMapper inspectionMapper, AgentMapper agentMapper) {
        this.inspectionMapper = inspectionMapper;
        this.agentMapper = agentMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> ingest(IncomingResult r, String sourceIp) {
        Map<String, Object> data = new HashMap<>();
        data.put("runId", r.runId);

        // 멱등 : 동일 runId 존재 시 무시
        if (r.runId != null && inspectionMapper.countRun(r.runId) > 0) {
            data.put("duplicated", true);
            InspectionRunVO existing = inspectionMapper.selectRun(r.runId);
            data.put("overallStatus", existing != null ? existing.getOverallStatus() : null);
            return data;
        }

        // 대상 서버 식별(agent → server)
        Long serverId = null;
        Long policyId = r.policyId;
        AgentVO agent = r.agentId != null ? agentMapper.selectAgent(r.agentId) : null;
        if (agent != null) {
            serverId = agent.getServerId();
            if (policyId == null) {
                policyId = agent.getPolicyId();
            }
        }

        // run 헤더 + 항목, 서버측 집계
        InspectionRunVO run = new InspectionRunVO();
        run.setRunId(r.runId);
        run.setAgentId(r.agentId);
        run.setServerId(serverId);
        run.setPolicyId(policyId);
        run.setPolicyVersion(r.policyVersion);
        run.setPlanId(r.planId);
        run.setRunType(r.runType != null ? r.runType : "AUTO");
        run.setStartedAt(parse(r.startedAt));
        run.setFinishedAt(parse(r.finishedAt));
        run.setSourceIp(sourceIp);

        int warn = 0, crit = 0, err = 0;
        List<ResultItemVO> items = new ArrayList<>();
        if (r.items != null) {
            for (IncomingResult.IncomingItem it : r.items) {
                ResultItemVO vo = new ResultItemVO();
                vo.setRunId(r.runId);
                vo.setCategory(it.category);
                vo.setItemCode(it.itemCode);
                vo.setItemName(it.name);
                vo.setValue(it.value);
                vo.setUnit(it.unit);
                vo.setStatus(it.status != null ? it.status : "NORMAL");
                vo.setSource("AUTO");
                vo.setThresholdWarn(it.thresholdWarn);
                vo.setThresholdCritical(it.thresholdCritical);
                vo.setRawText(it.raw);
                vo.setErrorText(it.error);
                vo.setCollectedAt(parse(it.collectedAt));
                items.add(vo);
                if ("WARN".equals(vo.getStatus())) warn++;
                else if ("CRITICAL".equals(vo.getStatus())) crit++;
                else if ("ERROR".equals(vo.getStatus())) err++;
            }
        }
        run.setItemCount(items.size());
        run.setWarnCount(warn);
        run.setCriticalCount(crit);
        run.setErrorCount(err);
        run.setOverallStatus(worstOf(err, crit, warn));

        // 멱등 INSERT : ON CONFLICT(run_id) DO NOTHING. 영향행 0이면 동시 중복 → 결과항목 적재 생략.
        int inserted = inspectionMapper.insertRun(run);
        if (inserted == 0) {
            data.put("duplicated", true);
            InspectionRunVO existing = inspectionMapper.selectRun(r.runId);
            data.put("overallStatus", existing != null ? existing.getOverallStatus() : null);
            return data;
        }
        for (ResultItemVO vo : items) {
            inspectionMapper.insertResultItem(vo);
        }
        if (r.agentId != null) {
            agentMapper.touchLastRun(r.agentId);
        }
        // 정기점검 계획 연계
        if (r.planId != null && serverId != null) {
            inspectionMapper.linkPlanTarget(r.planId, serverId, r.runId);
        }

        data.put("duplicated", false);
        data.put("overallStatus", run.getOverallStatus());
        return data;
    }

    @Override
    public List<InspectionRunVO> getRunList(Long serverId) {
        return inspectionMapper.selectRunList(serverId);
    }

    @Override
    public InspectionRunVO getRunDetail(String runId) {
        InspectionRunVO run = inspectionMapper.selectRun(runId);
        if (run != null) {
            run.setItems(inspectionMapper.selectResultItems(runId));
        }
        return run;
    }

    @Override
    @Transactional
    public void saveManualRun(InspectionRunVO run, List<ResultItemVO> items) {
        int warn = 0, crit = 0, err = 0;
        for (ResultItemVO it : items) {
            it.setRunId(run.getRunId());
            it.setSource("MANUAL");
            if ("WARN".equals(it.getStatus())) warn++;
            else if ("CRITICAL".equals(it.getStatus())) crit++;
            else if ("ERROR".equals(it.getStatus())) err++;
        }
        run.setRunType("MANUAL");
        run.setItemCount(items.size());
        run.setWarnCount(warn);
        run.setCriticalCount(crit);
        run.setErrorCount(err);
        run.setOverallStatus(worstOf(err, crit, warn));
        run.setStartedAt(OffsetDateTime.now());
        run.setFinishedAt(OffsetDateTime.now());
        inspectionMapper.insertRun(run);
        for (ResultItemVO it : items) {
            inspectionMapper.insertResultItem(it);
        }
        if (run.getPlanId() != null && run.getServerId() != null) {
            inspectionMapper.linkPlanTarget(run.getPlanId(), run.getServerId(), run.getRunId());
        }
    }

    private String worstOf(int err, int crit, int warn) {
        if (err > 0) return "ERROR";
        if (crit > 0) return "CRITICAL";
        if (warn > 0) return "WARN";
        return "NORMAL";
    }

    private OffsetDateTime parse(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            return OffsetDateTime.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }
}
