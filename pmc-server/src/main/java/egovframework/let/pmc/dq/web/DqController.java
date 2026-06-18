package egovframework.let.pmc.dq.web;

import egovframework.let.pmc.dq.service.DqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 데이터 품질 점검 현황 화면.
 */
@Controller
@RequestMapping("/pmc/admin")
public class DqController {

    private final DqService dqService;

    @Autowired
    public DqController(DqService dqService) {
        this.dqService = dqService;
    }

    @GetMapping("/dq.do")
    public String dq(Model model) {
        model.addAttribute("rules", dqService.getRules());
        model.addAttribute("results", dqService.getResults());
        return "egovframework/let/pmc/admin/dqList";
    }

    @PostMapping("/dqRun.do")
    public String run() {
        dqService.runAll();
        return "redirect:/pmc/admin/dq.do";
    }
}
