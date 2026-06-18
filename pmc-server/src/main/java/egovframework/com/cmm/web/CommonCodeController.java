package egovframework.com.cmm.web;

import egovframework.com.cmm.service.CommonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 공통코드 / 사용자 관리(조회) 화면.
 */
@Controller
public class CommonCodeController {

    private final CommonMapper commonMapper;

    @Autowired
    public CommonCodeController(CommonMapper commonMapper) {
        this.commonMapper = commonMapper;
    }

    @GetMapping("/cmm/cmmnCode/list.do")
    public String codeList(Model model) {
        model.addAttribute("codes", commonMapper.selectCommonCodes());
        return "egovframework/com/cmm/cmmnCodeList";
    }

    @GetMapping("/pmc/admin/user.do")
    public String userList(Model model) {
        model.addAttribute("users", commonMapper.selectUsers());
        return "egovframework/com/cmm/userList";
    }
}
