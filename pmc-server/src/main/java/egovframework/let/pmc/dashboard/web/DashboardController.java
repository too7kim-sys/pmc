package egovframework.let.pmc.dashboard.web;

import egovframework.let.pmc.dashboard.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * 장애예방점검 종합 대시보드.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/pmc/dashboard.do")
    public String dashboard(Model model) {
        Map<String, Object> d = dashboardService.getDashboard();
        model.addAllAttributes(d);
        return "egovframework/let/pmc/dashboard/dashboardMain";
    }
}
