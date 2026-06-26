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

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공통코드 / 사용자 관리(조회 + 등록/수정/삭제). ADMIN 전용(/cmm/**, /pmc/admin/**).
 */
@Controller
public class CommonCodeController {

    private final CommonMapper commonMapper;
    private final CommonAdminService commonAdminService;

    @Autowired
    public CommonCodeController(CommonMapper commonMapper, CommonAdminService commonAdminService) {
        this.commonMapper = commonMapper;
        this.commonAdminService = commonAdminService;
    }

    // ===== 공통코드 =====

    @GetMapping("/cmm/cmmnCode/list.do")
    public String codeList(@RequestParam(required = false) String clCode,
                           @RequestParam(required = false) String code,
                           Model model) {
        model.addAttribute("codes", commonMapper.selectCommonCodes());
        model.addAttribute("groups", commonMapper.selectCodeGroups());
        if (clCode != null && code != null) {
            model.addAttribute("editCode", commonMapper.selectCode(clCode, code));
        }
        return "egovframework/com/cmm/cmmnCodeList";
    }

    @PostMapping("/cmm/cmmnCode/save.do")
    public String codeSave(@RequestParam String clCode,
                           @RequestParam(required = false) String newGroupNm,
                           @RequestParam String code,
                           @RequestParam String codeNm,
                           @RequestParam(required = false) Integer sortOrdr,
                           @RequestParam(required = false) String useYn,
                           @RequestParam(defaultValue = "true") boolean isNew,
                           RedirectAttributes ra) {
        try {
            commonAdminService.saveCode(clCode, newGroupNm, code, codeNm, sortOrdr, useYn, isNew);
            ra.addFlashAttribute("msg", "공통코드를 저장했습니다.");
        } catch (ApiException e) {
            ra.addFlashAttribute("msg", "저장 실패: " + e.getMessage());
        }
        return "redirect:/cmm/cmmnCode/list.do";
    }

    @PostMapping("/cmm/cmmnCode/delete.do")
    public String codeDelete(@RequestParam String clCode, @RequestParam String code,
                             RedirectAttributes ra) {
        commonAdminService.deleteCode(clCode, code);
        ra.addFlashAttribute("msg", "공통코드를 삭제했습니다.");
        return "redirect:/cmm/cmmnCode/list.do";
    }

    // ===== 사용자 =====

    @GetMapping("/pmc/admin/user.do")
    public String userList(@RequestParam(required = false) String emplyrId, Model model) {
        model.addAttribute("users", commonMapper.selectUsers());
        model.addAttribute("roles", commonMapper.selectRoles());
        if (emplyrId != null) {
            model.addAttribute("editUser", commonMapper.selectUserOne(emplyrId));
            model.addAttribute("editUserRoles", commonMapper.selectUserRoles(emplyrId));
        }
        return "egovframework/com/cmm/userList";
    }

    @PostMapping("/pmc/admin/user/save.do")
    public String userSave(@RequestParam String emplyrId,
                           @RequestParam String userNm,
                           @RequestParam(required = false) String password,
                           @RequestParam(required = false) String emailAdres,
                           @RequestParam(required = false) String ofcpsNm,
                           @RequestParam(required = false) String deptCode,
                           @RequestParam(required = false) String emplyrSttus,
                           @RequestParam(required = false) String lockAt,
                           @RequestParam(required = false) List<String> roles,
                           @RequestParam(defaultValue = "true") boolean isNew,
                           RedirectAttributes ra) {
        Map<String, Object> user = new HashMap<>();
        user.put("emplyrId", emplyrId);
        user.put("userNm", userNm);
        user.put("emailAdres", emailAdres);
        user.put("ofcpsNm", ofcpsNm);
        user.put("deptCode", deptCode);
        user.put("emplyrSttus", emplyrSttus);
        user.put("lockAt", lockAt);
        try {
            commonAdminService.saveUser(user, password, roles, isNew);
            ra.addFlashAttribute("msg", "사용자를 저장했습니다.");
        } catch (ApiException e) {
            ra.addFlashAttribute("msg", "저장 실패: " + e.getMessage());
        }
        return "redirect:/pmc/admin/user.do";
    }

    @PostMapping("/pmc/admin/user/delete.do")
    public String userDelete(@RequestParam String emplyrId, Principal principal,
                             RedirectAttributes ra) {
        String me = principal != null ? principal.getName() : null;
        if (emplyrId.equals(me)) {
            ra.addFlashAttribute("msg", "본인 계정은 삭제할 수 없습니다.");
        } else {
            commonAdminService.deleteUser(emplyrId);
            ra.addFlashAttribute("msg", "사용자를 삭제했습니다.");
        }
        return "redirect:/pmc/admin/user.do";
    }
}
