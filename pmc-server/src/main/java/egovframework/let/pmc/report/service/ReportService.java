package egovframework.let.pmc.report.service;

import java.util.List;

public interface ReportService {
    List<ReportVO> getReportList();
    ReportVO getReport(Long reportId);

    /** 단일 점검(run) 보고서 생성. type=PDF/XLSX/CSV */
    ReportVO generateRunReport(String runId, String type);

    /** 정기점검 계획(plan) 결과보고서 생성 */
    ReportVO generatePlanReport(Long planId, String type);
}
