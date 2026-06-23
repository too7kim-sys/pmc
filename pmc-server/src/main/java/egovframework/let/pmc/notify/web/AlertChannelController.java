package egovframework.let.pmc.notify.web;

import egovframework.let.pmc.notify.service.AlertChannelVO;
import egovframework.let.pmc.notify.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * 알림 채널 관리(ADMIN, /pmc/admin/** 권한). 다채널 Webhook + 심각도 임계/유형 필터.
 */
@Controller
@RequestMapping("/pmc/admin/alertChannel")
public class AlertChannelController {

    private final AlertService alertService;

    @Autowired
    public AlertChannelController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping(".do")
    public String list(Model model) {
        model.addAttribute("channels", alertService.getChannels());
        return "egovframework/let/pmc/notify/alertChannelList";
    }

    @PostMapping("/regist.do")
    public String regist(@ModelAttribute AlertChannelVO vo, Principal principal) {
        vo.setRegUser(principal != null ? principal.getName() : "admin");
        if (vo.getEnabled() == null) vo.setEnabled("Y");
        alertService.addChannel(vo);
        return "redirect:/pmc/admin/alertChannel.do";
    }

    @PostMapping("/toggle.do")
    public String toggle(@RequestParam Long channelId, @RequestParam String enabled) {
        alertService.setChannelEnabled(channelId, enabled);
        return "redirect:/pmc/admin/alertChannel.do";
    }

    @PostMapping("/delete.do")
    public String delete(@RequestParam Long channelId) {
        alertService.deleteChannel(channelId);
        return "redirect:/pmc/admin/alertChannel.do";
    }
}
