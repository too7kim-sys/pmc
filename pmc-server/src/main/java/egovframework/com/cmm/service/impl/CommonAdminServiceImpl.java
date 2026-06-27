package egovframework.com.cmm.service.impl;

import egovframework.com.cmm.service.AuditService;
import egovframework.com.cmm.service.CommonAdminService;
import egovframework.com.cmm.service.CommonMapper;
import egovframework.let.pmc.common.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공통코드/사용자 관리 서비스 구현. 비밀번호는 bcrypt(PasswordEncoder)로 해시,
 * 복잡도 정책 검증 + 주요 변경 감사로그 기록.
 */
@Service
public class CommonAdminServiceImpl implements CommonAdminService {

    private final CommonMapper commonMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Autowired
    public CommonAdminServiceImpl(CommonMapper commonMapper, PasswordEncoder passwordEncoder,
                                  AuditService auditService) {
        this.commonMapper = commonMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /** 비밀번호 복잡도(행안부 권장): 9자 이상 + 영문/숫자/특수 중 2종류 이상. */
    private void validatePassword(String pw) {
        if (pw == null || pw.length() < 9) {
            throw new ApiException("WEAK_PASSWORD", "비밀번호는 9자 이상이어야 합니다.");
        }
        int classes = 0;
        if (pw.matches(".*[A-Za-z].*")) classes++;
        if (pw.matches(".*[0-9].*")) classes++;
        if (pw.matches(".*[^A-Za-z0-9].*")) classes++;
        if (classes < 2) {
            throw new ApiException("WEAK_PASSWORD", "영문·숫자·특수문자 중 2종류 이상을 포함해야 합니다.");
        }
    }

    @Override
    @Transactional
    public void saveCode(String clCode, String newGroupNm, String code, String codeNm,
                         Integer sortOrdr, String useYn, boolean isNew) {
        if (isBlank(clCode) || isBlank(code) || isBlank(codeNm)) {
            throw new ApiException("INVALID_REQUEST", "그룹코드·코드·코드명은 필수입니다.");
        }
        // 새 그룹명이 주어지고 아직 그룹이 없으면 그룹 먼저 생성
        if (!isBlank(newGroupNm) && commonMapper.countGroup(clCode) == 0) {
            commonMapper.insertGroup(clCode, newGroupNm);
        }
        Map<String, Object> p = new HashMap<>();
        p.put("clCode", clCode);
        p.put("code", code);
        p.put("codeNm", codeNm);
        p.put("sortOrdr", sortOrdr);
        p.put("useYn", normalizeYn(useYn));
        if (isNew) {
            if (commonMapper.selectCode(clCode, code) != null) {
                throw new ApiException("DUPLICATE", "이미 존재하는 코드입니다: " + clCode + "/" + code);
            }
            commonMapper.insertCode(p);
        } else {
            commonMapper.updateCode(p);
        }
        auditService.log(isNew ? "CODE_CREATE" : "CODE_UPDATE", "CODE", clCode + "/" + code, codeNm);
    }

    @Override
    @Transactional
    public void deleteCode(String clCode, String code) {
        commonMapper.deleteCode(clCode, code);
        auditService.log("CODE_DELETE", "CODE", clCode + "/" + code, null);
    }

    @Override
    @Transactional
    public void saveUser(Map<String, Object> user, String rawPassword, List<String> roles, boolean isNew) {
        String emplyrId = str(user.get("emplyrId"));
        if (isBlank(emplyrId) || isBlank(str(user.get("userNm")))) {
            throw new ApiException("INVALID_REQUEST", "ID·성명은 필수입니다.");
        }
        user.put("emplyrSttus", isBlank(str(user.get("emplyrSttus"))) ? "P" : str(user.get("emplyrSttus")));
        user.put("lockAt", normalizeYn(str(user.get("lockAt"))));

        if (isNew) {
            if (commonMapper.countUser(emplyrId) > 0) {
                throw new ApiException("DUPLICATE", "이미 존재하는 사용자 ID 입니다: " + emplyrId);
            }
            if (isBlank(rawPassword)) {
                throw new ApiException("INVALID_REQUEST", "신규 사용자는 비밀번호가 필수입니다.");
            }
            validatePassword(rawPassword);
            user.put("password", passwordEncoder.encode(rawPassword));
            commonMapper.insertUser(user);
        } else {
            commonMapper.updateUser(user);
            if (!isBlank(rawPassword)) {
                validatePassword(rawPassword);
                commonMapper.updateUserPassword(emplyrId, passwordEncoder.encode(rawPassword));
            }
        }
        // 권한 동기화(삭제 후 재삽입)
        commonMapper.deleteUserRoles(emplyrId);
        if (roles != null) {
            for (String r : roles) {
                if (!isBlank(r)) commonMapper.insertUserRole(emplyrId, r.trim());
            }
        }
        auditService.log(isNew ? "USER_CREATE" : "USER_UPDATE", "USER", emplyrId,
                "roles=" + (roles == null ? "" : roles));
    }

    @Override
    @Transactional
    public void deleteUser(String emplyrId) {
        commonMapper.deleteUserRoles(emplyrId);
        commonMapper.deleteUser(emplyrId);
        auditService.log("USER_DELETE", "USER", emplyrId, null);
    }

    // ===== 메뉴관리 =====

    @Override
    @Transactional
    public void saveMenu(Long menuNo, String menuNm, String menuUrl, Long upperMenuNo,
                         Integer menuOrdr, String useYn, boolean isNew) {
        if (menuNo == null || isBlank(menuNm)) {
            throw new ApiException("INVALID_REQUEST", "메뉴번호·메뉴명은 필수입니다.");
        }
        if (menuNo.equals(upperMenuNo)) {
            throw new ApiException("INVALID_REQUEST", "상위메뉴를 자기 자신으로 지정할 수 없습니다.");
        }
        Map<String, Object> p = new HashMap<>();
        p.put("menuNo", menuNo);
        p.put("menuNm", menuNm);
        p.put("menuUrl", emptyToNull(menuUrl));
        p.put("upperMenuNo", upperMenuNo);
        p.put("menuOrdr", menuOrdr);
        p.put("useYn", normalizeYn(useYn));
        if (isNew) {
            if (commonMapper.countMenu(menuNo) > 0) {
                throw new ApiException("DUPLICATE", "이미 존재하는 메뉴번호입니다: " + menuNo);
            }
            commonMapper.insertMenu(p);
        } else {
            commonMapper.updateMenu(p);
        }
        auditService.log(isNew ? "MENU_CREATE" : "MENU_UPDATE", "MENU", String.valueOf(menuNo), menuNm);
    }

    @Override
    @Transactional
    public void deleteMenu(Long menuNo) {
        if (commonMapper.countChildMenu(menuNo) > 0) {
            throw new ApiException("CONFLICT", "하위 메뉴가 있어 삭제할 수 없습니다. 하위 메뉴를 먼저 정리하세요.");
        }
        commonMapper.deleteAuthorMenuByMenu(menuNo); // 권한 매핑 정리(FK)
        commonMapper.deleteMenu(menuNo);
        auditService.log("MENU_DELETE", "MENU", String.valueOf(menuNo), null);
    }

    // ===== 권한관리 =====

    @Override
    @Transactional
    public void saveAuthority(String authorCode, String authorNm, String authorDe, boolean isNew) {
        if (isBlank(authorCode) || isBlank(authorNm)) {
            throw new ApiException("INVALID_REQUEST", "권한코드·권한명은 필수입니다.");
        }
        Map<String, Object> p = new HashMap<>();
        p.put("authorCode", authorCode);
        p.put("authorNm", authorNm);
        p.put("authorDe", emptyToNull(authorDe));
        if (isNew) {
            if (commonMapper.countAuthority(authorCode) > 0) {
                throw new ApiException("DUPLICATE", "이미 존재하는 권한코드입니다: " + authorCode);
            }
            commonMapper.insertAuthority(p);
        } else {
            commonMapper.updateAuthority(p);
        }
        auditService.log(isNew ? "AUTH_CREATE" : "AUTH_UPDATE", "AUTHORITY", authorCode, authorNm);
    }

    @Override
    @Transactional
    public void deleteAuthority(String authorCode) {
        if ("ROLE_ADMIN".equals(authorCode) || "ROLE_USER".equals(authorCode)) {
            throw new ApiException("FORBIDDEN", "기본 권한(" + authorCode + ")은 삭제할 수 없습니다.");
        }
        if (commonMapper.countUsersOfAuthority(authorCode) > 0) {
            throw new ApiException("CONFLICT", "해당 권한을 가진 사용자가 있어 삭제할 수 없습니다.");
        }
        commonMapper.deleteAuthorMenus(authorCode); // 메뉴 매핑 정리(FK)
        commonMapper.deleteAuthority(authorCode);
        auditService.log("AUTH_DELETE", "AUTHORITY", authorCode, null);
    }

    @Override
    @Transactional
    public void saveRoleMenus(String authorCode, List<Long> menuNos) {
        if (isBlank(authorCode)) {
            throw new ApiException("INVALID_REQUEST", "권한코드가 필요합니다.");
        }
        commonMapper.deleteAuthorMenus(authorCode);
        if (menuNos != null) {
            for (Long m : menuNos) {
                if (m != null) commonMapper.insertAuthorMenu(authorCode, m);
            }
        }
        auditService.log("AUTH_MENUS", "AUTHORITY", authorCode,
                "menus=" + (menuNos == null ? 0 : menuNos.size()));
    }

    private String emptyToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private String normalizeYn(String v) {
        return "Y".equalsIgnoreCase(v) ? "Y" : "N";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
