package egovframework.com.cmm.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * GNB 렌더용 메뉴 노드(2단계 트리). 최상위 + 하위(children).
 */
public class MenuNode implements Serializable {
    private Long menuNo;
    private String menuNm;
    private String menuUrl;
    private boolean active;   // 현재 페이지 해당 메뉴(또는 하위 포함) 여부
    private final List<MenuNode> children = new ArrayList<>();

    public MenuNode() { }

    public MenuNode(Long menuNo, String menuNm, String menuUrl) {
        this.menuNo = menuNo;
        this.menuNm = menuNm;
        this.menuUrl = menuUrl;
    }

    public Long getMenuNo() { return menuNo; }
    public void setMenuNo(Long menuNo) { this.menuNo = menuNo; }
    public String getMenuNm() { return menuNm; }
    public void setMenuNm(String menuNm) { this.menuNm = menuNm; }
    public String getMenuUrl() { return menuUrl; }
    public void setMenuUrl(String menuUrl) { this.menuUrl = menuUrl; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<MenuNode> getChildren() { return children; }
}
