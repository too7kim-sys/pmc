package egovframework.let.pmc.report.service.impl;

import egovframework.let.pmc.ingest.service.IngestService;
import egovframework.let.pmc.ingest.service.InspectionRunVO;
import egovframework.let.pmc.ingest.service.ResultItemVO;
import egovframework.let.pmc.plan.service.PlanService;
import egovframework.let.pmc.plan.service.PlanTargetVO;
import egovframework.let.pmc.plan.service.PlanVO;
import egovframework.let.pmc.report.generator.ReportData;
import egovframework.let.pmc.report.generator.ReportGenerator;
import egovframework.let.pmc.report.service.ReportMapper;
import egovframework.let.pmc.report.service.ReportService;
import egovframework.let.pmc.report.service.ReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final IngestService ingestService;
    private final PlanService planService;
    private final Map<String, ReportGenerator> generators;

    @Value("${Globals.ReportDir}")
    private String reportDir;

    @Autowired
    public ReportServiceImpl(ReportMapper reportMapper, IngestService ingestService,
                             PlanService planService, List<ReportGenerator> generatorList) {
        this.reportMapper = reportMapper;
        this.ingestService = ingestService;
        this.planService = planService;
        this.generators = new java.util.HashMap<>();
        for (ReportGenerator g : generatorList) {
            generators.put(g.type(), g);
        }
    }

    @Override
    public List<ReportVO> getReportList() {
        return reportMapper.selectReportList();
    }

    @Override
    public ReportVO getReport(Long reportId) {
        return reportMapper.selectReport(reportId);
    }

    @Override
    public ReportVO generateRunReport(String runId, String type) {
        InspectionRunVO run = ingestService.getRunDetail(runId);
        if (run == null) {
            throw new IllegalArgumentException("점검 실행을 찾을 수 없습니다: " + runId);
        }
        ReportData data = new ReportData("정보시스템 장애예방 점검표");
        data.addHeader("점검대상(호스트)", run.getHostname());
        data.addHeader("점검일시", fmt(run.getStartedAt()));
        data.addHeader("종합판정", run.getOverallStatus());
        data.addHeader("점검항목 수", String.valueOf(run.getItemCount()));
        data.addHeader("주의/위험/오류", run.getWarnCount() + " / " + run.getCriticalCount() + " / " + run.getErrorCount());

        // 분류별 섹션
        String currentCat = null;
        ReportData.Section sec = null;
        if (run.getItems() != null) {
            for (ResultItemVO it : run.getItems()) {
                if (!java.util.Objects.equals(it.getCategory(), currentCat)) {
                    currentCat = it.getCategory();
                    sec = data.addSection("[" + currentCat + "]", "점검항목", "수집값", "단위", "기준(주의/위험)", "판정");
                }
                sec.addRow(nz(it.getItemName()), nz(it.getValue()), nz(it.getUnit()),
                        nz(it.getThresholdWarn()) + "/" + nz(it.getThresholdCritical()), nz(it.getStatus()));
            }
        }
        ReportVO vo = saveAndRecord(data, type, "SINGLE_RUN", null, run.getServerId(), runId, "inspection-sheet");
        return vo;
    }

    @Override
    public ReportVO generatePlanReport(Long planId, String type) {
        PlanVO plan = planService.getPlanDetail(planId);
        if (plan == null) {
            throw new IllegalArgumentException("점검 계획을 찾을 수 없습니다: " + planId);
        }
        ReportData data = new ReportData("정기점검 결과보고서");
        data.addHeader("계획명", plan.getPlanName());
        data.addHeader("주기", plan.getCycle());
        data.addHeader("점검자", nz(plan.getInspectorId()));
        data.addHeader("결재자", nz(plan.getApproverId()));
        data.addHeader("결재상태", nz(plan.getApproveStatus()));
        data.addHeader("이행률", plan.getCompliance() + "% (" + plan.getDoneCnt() + "/" + plan.getTargetCnt() + ")");

        ReportData.Section sec = data.addSection("대상 서버별 점검 실적",
                "호스트", "OS", "서비스", "실적상태", "종합판정", "완료일시");
        if (plan.getTargets() != null) {
            for (PlanTargetVO t : plan.getTargets()) {
                sec.addRow(nz(t.getHostname()), nz(t.getOsType()), nz(t.getServiceName()),
                        nz(t.getResultStatus()), nz(t.getOverallStatus()), fmt(t.getDoneDt()));
            }
        }
        return saveAndRecord(data, type, "PLAN", planId, null, null, "plan-report");
    }

    private ReportVO saveAndRecord(ReportData data, String type, String scope, Long planId,
                                   Long serverId, String runId, String namePrefix) {
        ReportGenerator gen = generators.get(type);
        if (gen == null) {
            throw new IllegalArgumentException("지원하지 않는 보고서 유형: " + type);
        }
        byte[] bytes = gen.generate(data);
        String ts = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileName = namePrefix + "-" + ts + "." + gen.extension();
        try {
            Path dir = Paths.get(reportDir);
            Files.createDirectories(dir);
            Path file = dir.resolve(fileName);
            Files.write(file, bytes);

            ReportVO vo = new ReportVO();
            vo.setReportType(gen.type());
            vo.setScopeType(scope);
            vo.setPlanId(planId);
            vo.setServerId(serverId);
            vo.setRunId(runId);
            vo.setFilePath(file.toString());
            vo.setFileName(fileName);
            vo.setFileSize((long) bytes.length);
            vo.setGenStatus("DONE");
            reportMapper.insertReport(vo);
            return vo;
        } catch (Exception e) {
            throw new RuntimeException("보고서 저장 실패", e);
        }
    }

    private String fmt(java.time.OffsetDateTime dt) {
        return dt == null ? "" : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
