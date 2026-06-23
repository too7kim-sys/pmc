package egovframework.let.pmc.retention;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

/**
 * 보존기간 경과 데이터 정리 배치(매일 03:00). 보존일이 0 이하인 항목은 건너뜀.
 * DB·디스크 증가를 막아 운영 지속성을 확보한다.
 */
@Component
public class RetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    private final RetentionMapper retentionMapper;

    @Value("${Globals.RetentionRunDays:0}")
    private int runDays;
    @Value("${Globals.RetentionReportDays:0}")
    private int reportDays;
    @Value("${Globals.RetentionAlertDays:0}")
    private int alertDays;
    @Value("${Globals.ReportDir:}")
    private String reportDir;

    @Autowired
    public RetentionScheduler(RetentionMapper retentionMapper) {
        this.retentionMapper = retentionMapper;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purge() {
        try {
            if (reportDays > 0) {
                // 보고서 파일 먼저 삭제(경로가 ReportDir 하위인 경우만), 이후 행 삭제
                for (String path : retentionMapper.selectOldReportPaths(reportDays)) {
                    deleteReportFile(path);
                }
                int rep = retentionMapper.deleteReportsBefore(reportDays);
                if (rep > 0) log.info("보존정리: 보고서 {}건 삭제(>{}일)", rep, reportDays);
            }
            if (runDays > 0) {
                int runs = retentionMapper.deleteRunsBefore(runDays);
                if (runs > 0) log.info("보존정리: 점검 run {}건 삭제(>{}일, 계획연계 제외)", runs, runDays);
            }
            if (alertDays > 0) {
                int al = retentionMapper.deleteAlertsBefore(alertDays);
                if (al > 0) log.info("보존정리: 알림 이력 {}건 삭제(>{}일)", al, alertDays);
            }
        } catch (Exception e) {
            log.warn("보존정리 배치 실패: {}", e.getMessage());
        }
    }

    /** ReportDir 하위 경로 검증 후 파일 삭제(경로 우회 방지). */
    private void deleteReportFile(String path) {
        if (path == null || reportDir == null || reportDir.isEmpty()) return;
        try {
            File base = new File(reportDir).getCanonicalFile();
            File f = new File(path).getCanonicalFile();
            if (f.getPath().startsWith(base.getPath() + File.separator) && f.isFile()) {
                if (!f.delete()) {
                    log.warn("보고서 파일 삭제 실패: {}", f);
                }
            }
        } catch (Exception e) {
            log.warn("보고서 파일 정리 오류({}): {}", path, e.getMessage());
        }
    }
}
