package egovframework.let.pmc.plan.web;

import egovframework.let.pmc.agent.service.AgentService;
import egovframework.let.pmc.ingest.service.ResultItemVO;
import egovframework.let.pmc.plan.service.PlanService;
import egovframework.let.pmc.plan.service.PlanVO;
import egovframework.let.pmc.policy.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * 정기점검 계획·결과 관리 화면.
 */
@Controller
@RequestMapping("/pmc/plan")
public class PlanController {

    private final PlanService planService;
    private final AgentService agentService;
    private final PolicyService policyService;

    @Autowired
    public PlanController(PlanService planService, AgentService agentService, PolicyService policyService) {
        this.planService = planService;
        this.agentService = agentService;
        this.policyService = policyService;
    }

    @GetMapping("/list.do")
    public String list(Model model) {
        // OVERDUE 전환은 PlanOverdueScheduler 배치가 처리(조회 GET 에서 쓰기 제거)
        model.addAttribute("plans", planService.getPlanList());
        return "egovframework/let/pmc/plan/planList";
    }

    @GetMapping("/regist.do")
    public String registForm(Model model) {
        model.addAttribute("servers", agentService.getServerList());
        model.addAttribute("policies", policyService.getPolicyList());
        return "egovframework/let/pmc/plan/planRegist";
    }

    @PostMapping("/regist.do")
    public String regist(@ModelAttribute PlanVO vo,
                         @RequestParam(value = "serverIds", required = false) List<Long> serverIds) {
        Long id = planService.createPlan(vo, serverIds);
        return "redirect:/pmc/plan/detail.do?planId=" + id;
    }

    @GetMapping("/detail.do")
    public String detail(@RequestParam Long planId, Model model) {
        model.addAttribute("plan", planService.getPlanDetail(planId));
        return "egovframework/let/pmc/plan/planDetail";
    }

    @PostMapping("/runAuto.do")
    public String runAuto(@RequestParam Long planId, Principal principal,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        int issued = planService.runAuto(planId, principal != null ? principal.getName() : "admin");
        ra.addFlashAttribute("msg", "자동 점검 명령 " + issued + "건 발행(활성 Agent 미존재 대상은 제외)");
        return "redirect:/pmc/plan/detail.do?planId=" + planId;
    }

    @PostMapping("/link.do")
    public String link(@RequestParam Long planId, @RequestParam Long serverId) {
        planService.linkLatestRun(planId, serverId);
        return "redirect:/pmc/plan/detail.do?planId=" + planId;
    }

    /** 수동 점검 결과 입력 (병렬 배열) */
    @PostMapping("/manualInput.do")
    public String manualInput(@RequestParam Long planId, @RequestParam Long serverId,
                              @RequestParam(value = "category", required = false) List<String> category,
                              @RequestParam(value = "itemCode", required = false) List<String> itemCode,
                              @RequestParam(value = "itemName", required = false) List<String> itemName,
                              @RequestParam(value = "value", required = false) List<String> value,
                              @RequestParam(value = "status", required = false) List<String> status,
                              Principal principal) {
        List<ResultItemVO> items = new ArrayList<>();
        if (itemCode != null) {
            for (int i = 0; i < itemCode.size(); i++) {
                ResultItemVO vo = new ResultItemVO();
                vo.setCategory(get(category, i));
                vo.setItemCode(itemCode.get(i));
                vo.setItemName(get(itemName, i));
                vo.setValue(get(value, i));
                vo.setStatus(get(status, i) != null ? get(status, i) : "NORMAL");
                items.add(vo);
            }
        }
        planService.saveManual(planId, serverId, items, principal != null ? principal.getName() : "admin");
        return "redirect:/pmc/plan/detail.do?planId=" + planId;
    }

    @PostMapping("/requestApproval.do")
    public String requestApproval(@RequestParam Long planId) {
        planService.requestApproval(planId);
        return "redirect:/pmc/plan/detail.do?planId=" + planId;
    }

    @PostMapping("/approve.do")
    public String approve(@RequestParam Long planId, @RequestParam(required = false) String opinion) {
        planService.approve(planId, opinion);
        return "redirect:/pmc/plan/detail.do?planId=" + planId;
    }

    @PostMapping("/reject.do")
    public String reject(@RequestParam Long planId, @RequestParam(required = false) String opinion) {
        planService.reject(planId, opinion);
        return "redirect:/pmc/plan/detail.do?planId=" + planId;
    }

    private String get(List<String> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : null;
    }
}
