package egovframework.let.pmc.vuln.web;

import egovframework.let.pmc.agent.service.AgentService;
import egovframework.let.pmc.report.service.ReportService;
import egovframework.let.pmc.vuln.service.VulnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * 취약점 진단 현황(동일 취약점 관리·재발방지) 화면. 인증 사용자 접근(/pmc/**).
 */
@Controller
@RequestMapping("/pmc/vuln")
public class VulnController {

    private final VulnService vulnService;
    private final AgentService agentService;
    private final ReportService reportService;

    @Autowired
    public VulnController(VulnService vulnService, AgentService agentService, ReportService reportService) {
        this.vulnService = vulnService;
        this.agentService = agentService;
        this.reportService = reportService;
    }

    private static final int PAGE_SIZE = 20;

    @GetMapping("/list.do")
    public String list(@RequestParam(required = false) Long serverId,
                       @RequestParam(required = false) String severity,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        int totalCount = vulnService.countFindings(serverId, severity, status, kw);
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) PAGE_SIZE));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int offset = (page - 1) * PAGE_SIZE;
        model.addAttribute("findings",
                vulnService.getFindingsPaged(serverId, severity, status, kw, PAGE_SIZE, offset));
        model.addAttribute("servers", agentService.getServerList());
        model.addAttribute("fServerId", serverId);
        model.addAttribute("fSeverity", severity);
        model.addAttribute("fStatus", status);
        model.addAttribute("fKeyword", kw);
        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);
        return "egovframework/let/pmc/vuln/vulnList";
    }

    @GetMapping("/detail.do")
    public String detail(@RequestParam Long findingId, Model model) {
        model.addAttribute("finding", vulnService.getFinding(findingId));
        model.addAttribute("actions", vulnService.getActions(findingId));
        model.addAttribute("exception", vulnService.getActiveException(findingId));
        return "egovframework/let/pmc/vuln/vulnDetail";
    }

    @GetMapping("/recurrence.do")
    public String recurrence(Model model) {
        model.addAttribute("summary", vulnService.getRecurrenceSummary());
        return "egovframework/let/pmc/vuln/vulnRecurrence";
    }

    /** 조치이력 등록(담당자/조치내용) + 선택적 상태 변경(예: FIXED). */
    @PostMapping("/action.do")
    public String action(@RequestParam Long findingId,
                         @RequestParam(required = false) String actionDesc,
                         @RequestParam(required = false) String resultingStatus,
                         Principal principal, RedirectAttributes ra) {
        String who = principal != null ? principal.getName() : "user";
        vulnService.registerAction(findingId, who, actionDesc, emptyToNull(resultingStatus), who);
        ra.addFlashAttribute("msg", "조치이력을 등록했습니다.");
        return "redirect:/pmc/vuln/detail.do?findingId=" + findingId;
    }

    @PostMapping("/report.do")
    public String report(@RequestParam(defaultValue = "PDF") String type, RedirectAttributes ra) {
        reportService.generateVulnReport(type);
        ra.addFlashAttribute("msg", "취약점 진단 보고서(" + type + ")를 생성했습니다.");
        return "redirect:/pmc/report/list.do";
    }

    private String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
