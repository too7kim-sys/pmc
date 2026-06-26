package egovframework.com.cmm.web;

import egovframework.com.cmm.service.CommonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 로그인 사용자의 롤이 접근 가능한 메뉴(comtnauthormenu)를 모든 화면 모델에 주입한다.
 * header.jspf 의 GNB 가 이 {@code gnbMenus} 로 데이터 구동 렌더된다.
 * <p>
 * 사람 세션(ROLE_ADMIN/ROLE_USER 등)만 조회하고, Agent(ROLE_AGENT)·익명·오류 시엔 빈 목록을 반환해
 * API/요청 처리에 영향을 주지 않는다.
 */
@ControllerAdvice
public class GlobalMenuAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalMenuAdvice.class);

    private final CommonMapper commonMapper;

    @Autowired
    public GlobalMenuAdvice(CommonMapper commonMapper) {
        this.commonMapper = commonMapper;
    }

    @ModelAttribute("gnbMenus")
    public List<Map<String, Object>> gnbMenus() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Collections.emptyList();
            }
            List<String> roles = new ArrayList<>();
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String a = ga.getAuthority();
                if (a != null && a.startsWith("ROLE_") && !"ROLE_AGENT".equals(a) && !"ROLE_ANONYMOUS".equals(a)) {
                    roles.add(a);
                }
            }
            if (roles.isEmpty()) {
                return Collections.emptyList();
            }
            return commonMapper.selectNavMenus(roles);
        } catch (Exception e) {
            // 네비게이션 조회 실패가 화면 렌더를 막지 않도록 빈 목록 폴백
            log.warn("GNB 메뉴 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
