package egovframework.com.cmm.service.impl;

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
 * 공통코드/사용자 관리 서비스 구현. 비밀번호는 bcrypt(PasswordEncoder)로 해시.
 */
@Service
public class CommonAdminServiceImpl implements CommonAdminService {

    private final CommonMapper commonMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CommonAdminServiceImpl(CommonMapper commonMapper, PasswordEncoder passwordEncoder) {
        this.commonMapper = commonMapper;
        this.passwordEncoder = passwordEncoder;
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
    }

    @Override
    @Transactional
    public void deleteCode(String clCode, String code) {
        commonMapper.deleteCode(clCode, code);
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
            user.put("password", passwordEncoder.encode(rawPassword));
            commonMapper.insertUser(user);
        } else {
            commonMapper.updateUser(user);
            if (!isBlank(rawPassword)) {
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
    }

    @Override
    @Transactional
    public void deleteUser(String emplyrId) {
        commonMapper.deleteUserRoles(emplyrId);
        commonMapper.deleteUser(emplyrId);
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
