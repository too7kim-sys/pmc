package egovframework.com.cmm.web;

import egovframework.com.cmm.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 감사 로그 조회(ADMIN, /pmc/admin/**).
 */
@Controller
public class AuditController {

    private final AuditService auditService;

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/pmc/admin/auditLog.do")
    public String list(Model model) {
        model.addAttribute("auditLogs", auditService.recent(300));
        return "egovframework/com/cmm/auditLogList";
    }
}
