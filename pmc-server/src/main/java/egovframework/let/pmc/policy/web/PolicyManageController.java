package egovframework.let.pmc.policy.web;

import egovframework.let.pmc.policy.service.PolicyService;
import egovframework.let.pmc.policy.service.PolicyVO;
import egovframework.let.pmc.policy.service.SvcTargetVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 점검 정책 관리 화면 (정책/항목/임계치/웹서비스 대상).
 */
@Controller
@RequestMapping("/pmc/policy")
public class PolicyManageController {

    private final PolicyService policyService;

    @Autowired
    public PolicyManageController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/list.do")
    public String list(Model model) {
        model.addAttribute("policies", policyService.getPolicyList());
        return "egovframework/let/pmc/policy/policyList";
    }

    @GetMapping("/detail.do")
    public String detail(@RequestParam Long policyId, Model model) {
        model.addAttribute("policy", policyService.getPolicyDetail(policyId));
        return "egovframework/let/pmc/policy/policyDetail";
    }

    @PostMapping("/regist.do")
    public String regist(@ModelAttribute PolicyVO vo) {
        Long id = policyService.createPolicy(vo);
        return "redirect:/pmc/policy/detail.do?policyId=" + id;
    }

    @PostMapping("/svcTarget.do")
    public String addSvcTarget(@ModelAttribute SvcTargetVO vo) {
        policyService.addSvcTarget(vo);
        return "redirect:/pmc/policy/detail.do?policyId=" + vo.getPolicyId();
    }

    @PostMapping("/svcTargetDelete.do")
    public String delSvcTarget(@RequestParam Long svcTargetId, @RequestParam Long policyId) {
        policyService.deleteSvcTarget(svcTargetId, policyId);
        return "redirect:/pmc/policy/detail.do?policyId=" + policyId;
    }
}
