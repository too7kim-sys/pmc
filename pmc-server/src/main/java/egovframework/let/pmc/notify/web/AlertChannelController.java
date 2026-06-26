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
@RequestMapping("/pmc/admin")
public class AlertChannelController {

    private final AlertService alertService;

    @Autowired
    public AlertChannelController(AlertService alertService) {
        this.alertService = alertService;
    }

    // 주의: 목록 경로는 클래스("/pmc/admin") + "/alertChannel.do" 로 둬야 한다.
    // 클래스에 "/pmc/admin/alertChannel" 을 두고 메서드에 ".do"(슬래시 없음)를 쓰면
    // Spring 경로 결합이 "/pmc/admin/alertChannel/.do" 로 만들어 메뉴/redirect 와 불일치(404)된다.
    @GetMapping("/alertChannel.do")
    public String list(Model model) {
        model.addAttribute("channels", alertService.getChannels());
        return "egovframework/let/pmc/notify/alertChannelList";
    }

    @PostMapping("/alertChannel/regist.do")
    public String regist(@ModelAttribute AlertChannelVO vo, Principal principal) {
        vo.setRegUser(principal != null ? principal.getName() : "admin");
        if (vo.getEnabled() == null) vo.setEnabled("Y");
        alertService.addChannel(vo);
        return "redirect:/pmc/admin/alertChannel.do";
    }

    @PostMapping("/alertChannel/toggle.do")
    public String toggle(@RequestParam Long channelId, @RequestParam String enabled) {
        alertService.setChannelEnabled(channelId, enabled);
        return "redirect:/pmc/admin/alertChannel.do";
    }

    @PostMapping("/alertChannel/delete.do")
    public String delete(@RequestParam Long channelId) {
        alertService.deleteChannel(channelId);
        return "redirect:/pmc/admin/alertChannel.do";
    }

    @PostMapping("/alertChannel/test.do")
    public String test(@RequestParam Long channelId,
                       org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        boolean ok = alertService.testChannel(channelId);
        ra.addFlashAttribute("msg", "채널 #" + channelId + " 테스트 발송 " + (ok ? "성공" : "실패(설정·URL 확인)"));
        return "redirect:/pmc/admin/alertChannel.do";
    }

    @PostMapping("/alertChannel/testGlobal.do")
    public String testGlobal(org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        boolean ok = alertService.testGlobalWebhook();
        ra.addFlashAttribute("msg", "globals 폴백 URL 테스트 발송 " + (ok ? "성공" : "실패(URL 미설정·연결 확인)"));
        return "redirect:/pmc/admin/alertChannel.do";
    }
}
