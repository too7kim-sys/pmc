package egovframework.com.cmm.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 진입 → 대시보드.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/pmc/dashboard.do";
    }

    @GetMapping("/login.do")
    public String login() {
        return "egovframework/com/cmm/login";
    }
}
