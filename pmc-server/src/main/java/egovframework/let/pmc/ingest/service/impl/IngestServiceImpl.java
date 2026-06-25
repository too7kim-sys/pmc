package egovframework.let.pmc.ingest.service.impl;

import egovframework.let.pmc.agent.service.AgentMapper;
import egovframework.let.pmc.agent.service.AgentVO;
import egovframework.let.pmc.common.ApiException;
import egovframework.let.pmc.ingest.service.*;
import egovframework.let.pmc.notify.service.AlertService;
import egovframework.let.pmc.vuln.service.VulnFinding;
import egovframework.let.pmc.vuln.service.VulnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(IngestServiceImpl.class);

    private final InspectionMapper inspectionMapper;
    private final AgentMapper agentMapper;
    private final AlertService alertService;
    private final VulnService vulnService;

    @Autowired
    public IngestServiceImpl(InspectionMapper inspectionMapper, AgentMapper agentMapper,
                             AlertService alertService, VulnService vulnService) {
        this.inspectionMapper = inspectionMapper;
        this.agentMapper = agentMapper;
        this.alertService = alertService;
        this.vulnService = vulnService;
    }

    @Override
    @Transactional
    public Map<String, Object> ingest(IncomingResult r, String sourceIp) {
        // 입력 검증 : runId 필수·UUID 형식(잘못된 UUID 캐스팅으로 인한 500 방지)
        if (r.runId == null || !isUuid(r.runId)) {
            throw new ApiException("INVALID_REQUEST", "runId 가 유효한 UUID 가 아닙니다.");
        }
        if (r.agentId != null && !isUuid(r.agentId)) {
            throw new ApiException("INVALID_REQUEST", "agentId 가 유효한 UUID 가 아닙니다.");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("runId", r.runId);

        // 멱등 : 동일 runId 존재 시 무시
        if (inspectionMapper.countRun(r.runId) > 0) {
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
        run.setRunType(normalizeRunType(r.runType));
        run.setStartedAt(parse(r.startedAt));
        run.setFinishedAt(parse(r.finishedAt));
        run.setSourceIp(sourceIp);

        int warn = 0, crit = 0, err = 0, svcFail = 0;
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
                vo.setStatus(normalizeStatus(it.status));
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
                if ("SVC".equalsIgnoreCase(vo.getCategory()) && "SVC_URL_STATUS".equals(vo.getItemCode())
                        && ("WARN".equals(vo.getStatus()) || "CRITICAL".equals(vo.getStatus())
                            || "ERROR".equals(vo.getStatus()))) {
                    svcFail++;
                }
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

        // 취약점(SEC) 진단 결과 → 동일 취약점 추적/재발방지(비차단: 실패해도 수신 처리에 영향 없음)
        if (serverId != null) {
            try {
                List<VulnFinding> findings = new ArrayList<>();
                for (ResultItemVO vo : items) {
                    if (!"SEC".equalsIgnoreCase(vo.getCategory())) continue;
                    String st = vo.getStatus();
                    if (!"WARN".equals(st) && !"CRITICAL".equals(st)) continue; // 취약만 finding
                    findings.add(new VulnFinding(serverId, vo.getItemCode(), "BUILTIN", "SEC",
                            vo.getItemName(), parseSeverity(vo.getRawText(), st)));
                }
                // 이번 run 에 SEC 항목이 하나라도 있으면 전체 스캔으로 보고 부재 항목 자동 FIXED
                boolean hasSec = false;
                for (ResultItemVO vo : items) {
                    if ("SEC".equalsIgnoreCase(vo.getCategory())) { hasSec = true; break; }
                }
                if (hasSec) {
                    vulnService.processFindings(serverId, r.runId, "BUILTIN", findings, true);
                }
            } catch (Exception e) {
                // 취약점 추적 실패는 수신 처리에 영향 주지 않음(비차단). 추적용 로깅만 남긴다.
                log.warn("취약점 추적 처리 실패(runId={}): {}", r.runId, e.getMessage());
            }
        }

        // 이상 알림(Webhook) — 비활성/중복억제는 AlertService 가 처리, ingest 실패에 영향 없음
        try {
            alertService.raiseRunAlert(run, svcFail);
        } catch (Exception e) {
            // 알림 실패는 수신 처리에 영향 주지 않음(비차단). 추적용 로깅만 남긴다.
            log.warn("이상 알림 발송 실패(runId={}): {}", r.runId, e.getMessage());
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

    /** SEC 항목 raw 의 "SEV=상; ..." 토큰에서 등급 추출. 없으면 판정값으로 추정(CRITICAL→상, WARN→중). */
    private String parseSeverity(String raw, String status) {
        if (raw != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("SEV=([상중하])").matcher(raw);
            if (m.find()) return m.group(1);
        }
        return "CRITICAL".equals(status) ? "상" : "중";
    }

    private String worstOf(int err, int crit, int warn) {
        if (err > 0) return "ERROR";
        if (crit > 0) return "CRITICAL";
        if (warn > 0) return "WARN";
        return "NORMAL";
    }

    private static final java.util.Set<String> VALID_STATUS = new java.util.HashSet<>(
            java.util.Arrays.asList("NORMAL", "WARN", "CRITICAL", "ERROR", "NA"));

    /** 항목 판정값을 허용 집합으로 정규화(null→NORMAL, 미지정 값→NA). 임의 문자열 DB 유입 차단. */
    private String normalizeStatus(String status) {
        if (status == null) return "NORMAL";
        String s = status.trim().toUpperCase();
        return VALID_STATUS.contains(s) ? s : "NA";
    }

    /** 실행 유형 정규화(AUTO/MANUAL 외에는 AUTO). */
    private String normalizeRunType(String runType) {
        if (runType == null) return "AUTO";
        String s = runType.trim().toUpperCase();
        return ("AUTO".equals(s) || "MANUAL".equals(s)) ? s : "AUTO";
    }

    private boolean isUuid(String s) {
        try {
            java.util.UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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
