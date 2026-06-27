package egovframework.let.pmc.report;

import egovframework.let.pmc.notify.service.AlertService;
import egovframework.let.pmc.report.service.ReportService;
import egovframework.let.pmc.report.service.ReportVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기(매일 06:00) 위험분석 보고서 자동 생성. 기본 비활성(Globals.AutoReportEnabled).
 * 생성물은 /pmc/report/list.do 에 노출되며, 알림 활성 시 Webhook 으로 생성 사실을 통지한다.
 */
@Component
public class ReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);

    private final ReportService reportService;
    private final AlertService alertService;

    @Value("${Globals.AutoReportEnabled:false}")
    private boolean enabled;
    @Value("${Globals.AutoReportType:PDF}")
    private String type;
    @Value("${Globals.AutoReportDays:7}")
    private int days;

    @Autowired
    public ReportScheduler(ReportService reportService, AlertService alertService) {
        this.reportService = reportService;
        this.alertService = alertService;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void generateDailyRiskReport() {
        if (!enabled) return;
        try {
            ReportVO vo = reportService.generateRiskReport(days, type);
            log.info("정기 위험분석 보고서 생성: {}", vo.getFileName());
            try {
                alertService.raise("AUTO_REPORT", null, null, "INFO",
                        "[정기보고서] 위험분석 보고서 생성",
                        "최근 " + days + "일 위험분석 보고서(" + type + ")가 생성되었습니다: " + vo.getFileName());
            } catch (Exception ignore) {
                // 통지 실패는 보고서 생성에 영향 없음
            }
        } catch (Exception e) {
            log.warn("정기 위험분석 보고서 생성 실패: {}", e.getMessage());
        }
        // 정기 취약점 진단 보고서도 함께 생성(독립 try — 한쪽 실패가 다른 쪽에 영향 없음)
        try {
            ReportVO vo = reportService.generateVulnReport(type);
            log.info("정기 취약점 진단 보고서 생성: {}", vo.getFileName());
            try {
                alertService.raise("AUTO_REPORT", null, null, "INFO",
                        "[정기보고서] 취약점 진단 보고서 생성",
                        "취약점 진단 현황 보고서(" + type + ")가 생성되었습니다: " + vo.getFileName());
            } catch (Exception ignore) {
                // 통지 실패는 보고서 생성에 영향 없음
            }
        } catch (Exception e) {
            log.warn("정기 취약점 진단 보고서 생성 실패: {}", e.getMessage());
        }
    }
}
