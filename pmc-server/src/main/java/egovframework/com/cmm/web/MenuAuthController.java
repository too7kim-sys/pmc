package egovframework.com.cmm.web;

import egovframework.com.cmm.service.CommonAdminService;
import egovframework.com.cmm.service.CommonMapper;
import egovframework.let.pmc.common.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 메뉴관리 / 권한관리(권한 CRUD + 권한별 메뉴 접근 매핑). ADMIN 전용(/pmc/admin/**).
 */
@Controller
public class MenuAuthController {

    private final CommonMapper commonMapper;
    private final CommonAdminService commonAdminService;

    @Autowired
    public MenuAuthController(CommonMapper commonMapper, CommonAdminService commonAdminService) {
        this.commonMapper = commonMapper;
        this.commonAdminService = commonAdminService;
    }

    // ===== 메뉴관리 =====

    @GetMapping("/pmc/admin/menu.do")
    public String menuList(@RequestParam(required = false) Long menuNo, Model model) {
        model.addAttribute("menus", commonMapper.selectMenus());
        if (menuNo != null) {
            model.addAttribute("editMenu", commonMapper.selectMenuOne(menuNo));
        }
        return "egovframework/com/cmm/menuList";
    }

    @PostMapping("/pmc/admin/menu/save.do")
    public String menuSave(@RequestParam Long menuNo,
                           @RequestParam String menuNm,
                           @RequestParam(required = false) String menuUrl,
                           @RequestParam(required = false) Long upperMenuNo,
                           @RequestParam(required = false) Integer menuOrdr,
                           @RequestParam(required = false) String useYn,
                           @RequestParam(defaultValue = "true") boolean isNew,
                           RedirectAttributes ra) {
        try {
            commonAdminService.saveMenu(menuNo, menuNm, menuUrl, upperMenuNo, menuOrdr, useYn, isNew);
            ra.addFlashAttribute("msg", "메뉴를 저장했습니다.");
        } catch (ApiException e) {
            ra.addFlashAttribute("msg", "저장 실패: " + e.getMessage());
        }
        return "redirect:/pmc/admin/menu.do";
    }

    @PostMapping("/pmc/admin/menu/delete.do")
    public String menuDelete(@RequestParam Long menuNo, RedirectAttributes ra) {
        try {
            commonAdminService.deleteMenu(menuNo);
            ra.addFlashAttribute("msg", "메뉴를 삭제했습니다.");
        } catch (ApiException e) {
            ra.addFlashAttribute("msg", "삭제 실패: " + e.getMessage());
        }
        return "redirect:/pmc/admin/menu.do";
    }

    // ===== 권한관리 =====

    @GetMapping("/pmc/admin/authority.do")
    public String authorityList(@RequestParam(required = false) String authorCode, Model model) {
        model.addAttribute("roles", commonMapper.selectRoles());
        model.addAttribute("menus", commonMapper.selectMenus());
        if (authorCode != null) {
            model.addAttribute("editAuthor", commonMapper.selectAuthorityOne(authorCode));
            model.addAttribute("authorMenus", commonMapper.selectAuthorMenus(authorCode));
        }
        return "egovframework/com/cmm/authorityList";
    }

    @PostMapping("/pmc/admin/authority/save.do")
    public String authoritySave(@RequestParam String authorCode,
                                @RequestParam String authorNm,
                                @RequestParam(required = false) String authorDe,
                                @RequestParam(defaultValue = "true") boolean isNew,
                                RedirectAttributes ra) {
        try {
            commonAdminService.saveAuthority(authorCode, authorNm, authorDe, isNew);
            ra.addFlashAttribute("msg", "권한을 저장했습니다.");
        } catch (ApiException e) {
            ra.addFlashAttribute("msg", "저장 실패: " + e.getMessage());
        }
        return "redirect:/pmc/admin/authority.do";
    }

    @PostMapping("/pmc/admin/authority/delete.do")
    public String authorityDelete(@RequestParam String authorCode, RedirectAttributes ra) {
        try {
            commonAdminService.deleteAuthority(authorCode);
            ra.addFlashAttribute("msg", "권한을 삭제했습니다.");
        } catch (ApiException e) {
            ra.addFlashAttribute("msg", "삭제 실패: " + e.getMessage());
        }
        return "redirect:/pmc/admin/authority.do";
    }

    /** 권한별 메뉴 접근 매핑 저장. */
    @PostMapping("/pmc/admin/authority/menus.do")
    public String saveRoleMenus(@RequestParam String authorCode,
                                @RequestParam(required = false) List<Long> menuNos,
                                RedirectAttributes ra) {
        try {
            commonAdminService.saveRoleMenus(authorCode, menuNos);
            ra.addFlashAttribute("msg", authorCode + " 의 메뉴 접근 권한을 저장했습니다.");
        } catch (ApiException e) {
            ra.addFlashAttribute("msg", "저장 실패: " + e.getMessage());
        }
        return "redirect:/pmc/admin/authority.do?authorCode=" + authorCode;
    }
}
