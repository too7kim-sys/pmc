package egovframework.let.pmc.ingest.web;

import egovframework.let.pmc.ingest.service.IngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 점검 실행 이력/상세 화면.
 */
@Controller
@RequestMapping("/pmc/inspection")
public class InspectionController {

    private final IngestService ingestService;

    @Autowired
    public InspectionController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @GetMapping("/list.do")
    public String list(@RequestParam(required = false) Long serverId, Model model) {
        model.addAttribute("runs", ingestService.getRunList(serverId));
        return "egovframework/let/pmc/inspection/runList";
    }

    @GetMapping("/detail.do")
    public String detail(@RequestParam String runId, Model model) {
        model.addAttribute("run", ingestService.getRunDetail(runId));
        return "egovframework/let/pmc/inspection/runDetail";
    }
}
