package egovframework.let.pmc.risk.web;

import egovframework.let.pmc.report.service.ReportService;
import egovframework.let.pmc.risk.service.RiskAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 문제 가능성(위험) 점검 화면 + 위험분석 보고서 생성.
 */
@Controller
@RequestMapping("/pmc/risk")
public class RiskController {

    private final RiskAnalysisService riskAnalysisService;
    private final ReportService reportService;

    @Autowired
    public RiskController(RiskAnalysisService riskAnalysisService, ReportService reportService) {
        this.riskAnalysisService = riskAnalysisService;
        this.reportService = reportService;
    }

    @GetMapping("/list.do")
    public String list(@RequestParam(defaultValue = "7") int days, Model model) {
        model.addAttribute("days", days);
        model.addAttribute("scores", riskAnalysisService.analyze(days));
        return "egovframework/let/pmc/risk/riskList";
    }

    @PostMapping("/report.do")
    public String report(@RequestParam(defaultValue = "7") int days,
                         @RequestParam(defaultValue = "PDF") String type,
                         RedirectAttributes ra) {
        reportService.generateRiskReport(days, type);
        ra.addFlashAttribute("msg", "위험분석 보고서(" + type + ")를 생성했습니다.");
        return "redirect:/pmc/report/list.do";
    }
}
