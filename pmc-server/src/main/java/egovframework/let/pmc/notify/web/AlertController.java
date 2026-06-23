package egovframework.let.pmc.notify.web;

import egovframework.let.pmc.notify.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 이상 알림 이력 화면.
 */
@Controller
@RequestMapping("/pmc/alert")
public class AlertController {

    private final AlertService alertService;

    @Autowired
    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/list.do")
    public String list(Model model) {
        model.addAttribute("alerts", alertService.getAlertLog(200));
        return "egovframework/let/pmc/notify/alertList";
    }
}
