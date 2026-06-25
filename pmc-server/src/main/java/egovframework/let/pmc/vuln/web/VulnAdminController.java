package egovframework.let.pmc.vuln.web;

import egovframework.let.pmc.agent.service.AgentService;
import egovframework.let.pmc.common.ApiException;
import egovframework.let.pmc.vuln.service.ScannerImportService;
import egovframework.let.pmc.vuln.service.VulnFinding;
import egovframework.let.pmc.vuln.service.VulnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 취약점 관리 — 관리자 전용(/pmc/admin/** → hasRole('ADMIN')).
 * 외부 스캐너 결과 import, 예외(수용/면제) 등록.
 */
@Controller
@RequestMapping("/pmc/admin")
public class VulnAdminController {

    private final ScannerImportService scannerImportService;
    private final VulnService vulnService;
    private final AgentService agentService;

    @Autowired
    public VulnAdminController(ScannerImportService scannerImportService, VulnService vulnService,
                              AgentService agentService) {
        this.scannerImportService = scannerImportService;
        this.vulnService = vulnService;
        this.agentService = agentService;
    }

    /** 외부 스캐너 결과 import 화면. */
    @GetMapping("/vulnImport.do")
    public String importForm(Model model) {
        model.addAttribute("servers", agentService.getServerList());
        return "egovframework/let/pmc/vuln/vulnImport";
    }

    /** 외부 스캐너 결과(JSON) 적재 → 동일 취약점 추적 상태머신 반영(source=SCANNER, 전체 스캔). */
    @PostMapping("/vulnImport.do")
    public String doImport(@RequestParam String scannerType,
                           @RequestParam Long serverId,
                           @RequestParam String json,
                           RedirectAttributes ra) {
        try {
            List<VulnFinding> findings = scannerImportService.parse(scannerType, serverId, json);
            vulnService.processFindings(serverId, null, "SCANNER", findings, true);
            ra.addFlashAttribute("msg", scannerType + " 결과 " + findings.size() + "건을 반영했습니다.");
        } catch (ApiException e) {
            ra.addFlashAttribute("msg", "가져오기 실패: " + e.getMessage());
        }
        return "redirect:/pmc/vuln/list.do";
    }

    /** 예외(수용/면제) 등록 → finding EXEMPTED 전환. */
    @PostMapping("/vulnException.do")
    public String registerException(@RequestParam Long findingId,
                                    @RequestParam String reason,
                                    @RequestParam(required = false) String approver,
                                    @RequestParam String expiresOn,
                                    Principal principal, RedirectAttributes ra) {
        String who = principal != null ? principal.getName() : "admin";
        try {
            OffsetDateTime expiresAt = LocalDate.parse(expiresOn)
                    .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
            vulnService.registerException(findingId, reason, approver, expiresAt, who);
            ra.addFlashAttribute("msg", "예외(수용/면제)를 등록했습니다. 만료: " + expiresOn);
        } catch (Exception e) {
            ra.addFlashAttribute("msg", "예외 등록 실패: " + e.getMessage());
        }
        return "redirect:/pmc/vuln/detail.do?findingId=" + findingId;
    }
}
