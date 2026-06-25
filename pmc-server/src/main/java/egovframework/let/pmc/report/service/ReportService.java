package egovframework.let.pmc.report.service;

import java.io.File;
import java.util.List;

public interface ReportService {
    List<ReportVO> getReportList();
    ReportVO getReport(Long reportId);

    /**
     * 다운로드 가능한 보고서 파일을 반환. 저장 경로가 보고서 디렉터리({@code Globals.ReportDir})
     * 하위에 있고 실제 존재할 때만 File 을 돌려준다(경로 우회 차단). 그 외에는 null.
     */
    File resolveDownloadableFile(Long reportId);

    /** 단일 점검(run) 보고서 생성. type=PDF/XLSX/CSV */
    ReportVO generateRunReport(String runId, String type);

    /** 정기점검 계획(plan) 결과보고서 생성 */
    ReportVO generatePlanReport(Long planId, String type);

    /** 문제가능성(위험) 분석 보고서 생성. days = 분석 기간(일) */
    ReportVO generateRiskReport(int days, String type);

    /** 취약점 진단 현황 보고서 생성(OPEN/RECURRED 취약점 + 등급별). */
    ReportVO generateVulnReport(String type);
}
