package egovframework.com.cmm.web;

import egovframework.com.cmm.service.CommonMapper;
import egovframework.com.cmm.service.MenuNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
    public List<MenuNode> gnbMenus(HttpServletRequest request) {
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
            List<MenuNode> tree = buildTree(commonMapper.selectNavMenus(roles));
            markActive(tree, currentPath(request));
            return tree;
        } catch (Exception e) {
            // 네비게이션 조회 실패가 화면 렌더를 막지 않도록 빈 목록 폴백
            log.warn("GNB 메뉴 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 평면 메뉴 행(menu_ordr 정렬)을 2단계 트리로 구성. 상위가 접근목록에 없으면 해당 노드를 최상위로 승격. */
    private List<MenuNode> buildTree(List<Map<String, Object>> rows) {
        Map<Long, MenuNode> byNo = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            Long no = toLong(r.get("menuNo"));
            byNo.put(no, new MenuNode(no, str(r.get("menuNm")), str(r.get("menuUrl"))));
        }
        List<MenuNode> roots = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            MenuNode node = byNo.get(toLong(r.get("menuNo")));
            Long upper = toLong(r.get("upperMenuNo"));
            if (upper != null && byNo.containsKey(upper)) {
                byNo.get(upper).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        // URL 도 없고 하위도 없는 빈 그룹 노드는 제외
        List<MenuNode> result = new ArrayList<>();
        for (MenuNode n : roots) {
            if (n.getMenuUrl() != null || !n.getChildren().isEmpty()) {
                result.add(n);
            }
        }
        return result;
    }

    /** 컨텍스트패스를 제외한 현재 요청 경로. */
    private String currentPath(HttpServletRequest req) {
        if (req == null) return "";
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri != null && uri.startsWith(ctx)) {
            return uri.substring(ctx.length());
        }
        return uri == null ? "" : uri;
    }

    /** 현재 경로와 일치하는 메뉴(및 그 상위 그룹)에 active 표시. 정확 일치 우선. */
    private void markActive(List<MenuNode> roots, String cur) {
        if (cur == null || cur.isEmpty()) return;
        for (MenuNode root : roots) {
            boolean childActive = false;
            for (MenuNode ch : root.getChildren()) {
                boolean a = cur.equals(ch.getMenuUrl());
                ch.setActive(a);
                childActive = childActive || a;
            }
            boolean selfActive = root.getMenuUrl() != null && cur.equals(root.getMenuUrl());
            root.setActive(selfActive || childActive);
        }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long) return (Long) o;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
