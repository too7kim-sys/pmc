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
import egovframework.let.pmc.risk.service.RiskAnalysisService;
import egovframework.let.pmc.risk.service.RiskScoreVO;
import egovframework.let.pmc.vuln.service.VulnFindingVO;
import egovframework.let.pmc.vuln.service.VulnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final ReportMapper reportMapper;
    private final IngestService ingestService;
    private final PlanService planService;
    private final RiskAnalysisService riskAnalysisService;
    private final VulnService vulnService;
    private final Map<String, ReportGenerator> generators;

    @Value("${Globals.ReportDir}")
    private String reportDir;

    @Autowired
    public ReportServiceImpl(ReportMapper reportMapper, IngestService ingestService,
                             PlanService planService, RiskAnalysisService riskAnalysisService,
                             VulnService vulnService, List<ReportGenerator> generatorList) {
        this.reportMapper = reportMapper;
        this.ingestService = ingestService;
        this.planService = planService;
        this.riskAnalysisService = riskAnalysisService;
        this.vulnService = vulnService;
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
    public File resolveDownloadableFile(Long reportId) {
        ReportVO vo = reportMapper.selectReport(reportId);
        if (vo == null || vo.getFilePath() == null) {
            return null;
        }
        try {
            File base = new File(reportDir).getCanonicalFile();
            File f = new File(vo.getFilePath()).getCanonicalFile();
            String basePath = base.getPath() + File.separator;
            // 정규화된 경로가 보고서 디렉터리 하위인지 확인(../ 등 경로 우회 차단)
            if (!f.getPath().startsWith(basePath)) {
                log.warn("보고서 다운로드 경로 우회 차단 reportId={} path={}", reportId, vo.getFilePath());
                return null;
            }
            return f.isFile() ? f : null;
        } catch (java.io.IOException e) {
            log.warn("보고서 경로 확인 실패 reportId={}: {}", reportId, e.getMessage());
            return null;
        }
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
        ReportVO vo = saveAndRecord(data, type, "SINGLE_RUN", null, run.getServerId(), runId,
                null, null, "inspection-sheet");
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
        return saveAndRecord(data, type, "PLAN", planId, null, null,
                plan.getPeriodFrom(), plan.getPeriodTo(), "plan-report");
    }

    @Override
    public ReportVO generateRiskReport(int days, String type) {
        List<RiskScoreVO> scores = riskAnalysisService.analyze(days);
        int high = 0, medium = 0, low = 0;
        for (RiskScoreVO r : scores) {
            if ("HIGH".equals(r.getLevel())) high++;
            else if ("MEDIUM".equals(r.getLevel())) medium++;
            else if ("LOW".equals(r.getLevel())) low++;
        }
        ReportData data = new ReportData("문제 가능성 점검 보고서");
        data.addHeader("생성일시", java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        data.addHeader("분석기간", "최근 " + days + "일");
        data.addHeader("대상 서버", String.valueOf(scores.size()));
        data.addHeader("위험도 분포", "HIGH " + high + " / MEDIUM " + medium + " / LOW " + low);

        ReportData.Section sec = data.addSection("서버별 위험도",
                "호스트", "서비스", "점수", "위험도", "주요 사유");
        for (RiskScoreVO r : scores) {
            sec.addRow(nz(r.getHostname()), nz(r.getServiceName()),
                    String.valueOf(r.getScore()), nz(r.getLevel()), r.getReasonText());
        }
        return saveAndRecord(data, type, "RISK", null, null, null, null, null, "risk-report");
    }

    @Override
    public ReportVO generateVulnReport(String type) {
        // OPEN/RECURRED 취약점(현재 미조치)만 보고 대상
        List<VulnFindingVO> findings = vulnService.getFindings(null, null, null);
        int high = 0, mid = 0, low = 0, recurred = 0, open = 0;
        for (VulnFindingVO f : findings) {
            boolean active = "OPEN".equals(f.getStatus()) || "RECURRED".equals(f.getStatus());
            if (!active) continue;
            open++;
            if ("RECURRED".equals(f.getStatus())) recurred++;
            if ("상".equals(f.getSeverity())) high++;
            else if ("중".equals(f.getSeverity())) mid++;
            else low++;
        }
        ReportData data = new ReportData("취약점 진단 현황 보고서");
        data.addHeader("생성일시", java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        data.addHeader("미조치 취약점", String.valueOf(open));
        data.addHeader("등급 분포", "상 " + high + " / 중 " + mid + " / 하 " + low);
        data.addHeader("재발(RECURRED)", String.valueOf(recurred));

        ReportData.Section sec = data.addSection("서버별 취약점(미조치)",
                "호스트", "점검코드", "취약점", "등급", "상태", "발생/재발");
        for (VulnFindingVO f : findings) {
            if (!"OPEN".equals(f.getStatus()) && !"RECURRED".equals(f.getStatus())) continue;
            sec.addRow(nz(f.getHostname()), nz(f.getCheckCode()), nz(f.getTitle()),
                    nz(f.getSeverity()), nz(f.getStatus()),
                    f.getOccurrenceCount() + "/" + f.getRecurCount());
        }
        return saveAndRecord(data, type, "VULN", null, null, null, null, null, "vuln-report");
    }

    private ReportVO saveAndRecord(ReportData data, String type, String scope, Long planId,
                                   Long serverId, String runId,
                                   java.time.LocalDate periodFrom, java.time.LocalDate periodTo,
                                   String namePrefix) {
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
            vo.setPeriodFrom(periodFrom);
            vo.setPeriodTo(periodTo);
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
