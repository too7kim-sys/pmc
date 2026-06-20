package egovframework.let.pmc.report.web;

import egovframework.let.pmc.report.service.ReportService;
import egovframework.let.pmc.report.service.ReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 보고서 생성/목록/다운로드 화면.
 */
@Controller
@RequestMapping("/pmc/report")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/list.do")
    public String list(Model model) {
        model.addAttribute("reports", reportService.getReportList());
        return "egovframework/let/pmc/report/reportList";
    }

    /** 단일 점검 보고서 생성 */
    @PostMapping("/generateRun.do")
    public String generateRun(@RequestParam String runId, @RequestParam String type) {
        reportService.generateRunReport(runId, type);
        return "redirect:/pmc/report/list.do";
    }

    /** 정기점검 결과보고서 생성 */
    @PostMapping("/generatePlan.do")
    public String generatePlan(@RequestParam Long planId, @RequestParam String type) {
        reportService.generatePlanReport(planId, type);
        return "redirect:/pmc/report/list.do";
    }

    @GetMapping("/download.do")
    public ResponseEntity<FileSystemResource> download(@RequestParam Long reportId) {
        ReportVO vo = reportService.getReport(reportId);
        // 경로 우회(../) 차단 : 보고서 디렉터리 하위의 실제 파일만 반환
        File f = reportService.resolveDownloadableFile(reportId);
        if (vo == null || f == null) {
            return ResponseEntity.notFound().build();
        }
        String encoded = URLEncoder.encode(vo.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(f));
    }
}
