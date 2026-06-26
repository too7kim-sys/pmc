<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">권한 관리</h2>

<c:if test="${not empty msg}"><div class="card"><span class="st NORMAL"><c:out value="${msg}"/></span></div></c:if>

<div class="card">
    <h3><c:choose><c:when test="${not empty editAuthor}">권한 수정</c:when><c:otherwise>권한 등록</c:otherwise></c:choose></h3>
    <form method="post" action="<c:url value='/pmc/admin/authority/save.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="isNew" value="${empty editAuthor ? 'true' : 'false'}"/>
        <p>권한코드:
            <c:choose>
                <c:when test="${not empty editAuthor}">
                    <b><c:out value="${editAuthor.authorCode}"/></b><input type="hidden" name="authorCode" value="${editAuthor.authorCode}"/>
                </c:when>
                <c:otherwise><input name="authorCode" required placeholder="예 ROLE_MANAGER"/></c:otherwise>
            </c:choose>
            권한명: <input name="authorNm" value="<c:out value='${editAuthor.authorNm}'/>" required/>
            설명: <input name="authorDe" value="<c:out value='${editAuthor.authorDe}'/>" style="width:240px"/>
            <button class="btn">저장</button>
            <c:if test="${not empty editAuthor}"><a class="btn" href="<c:url value='/pmc/admin/authority.do'/>">취소</a></c:if>
        </p>
    </form>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>권한코드</th><th>권한명</th><th>설명</th><th>관리</th></tr>
        <c:forEach var="r" items="${roles}">
            <tr>
                <td><code><c:out value="${r.authorCode}"/></code></td>
                <td><c:out value="${r.authorNm}"/></td>
                <td><c:out value="${r.authorDe}"/></td>
                <td>
                    <a class="btn" href="<c:url value='/pmc/admin/authority.do'/>?authorCode=${r.authorCode}">수정·메뉴권한</a>
                    <form class="inline" method="post" action="<c:url value='/pmc/admin/authority/delete.do'/>" onsubmit="return confirm('삭제하시겠습니까?');">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <input type="hidden" name="authorCode" value="${r.authorCode}"/>
                        <button class="btn">삭제</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty roles}"><tr><td colspan="4" class="muted">등록된 권한이 없습니다.</td></tr></c:if>
    </table>
</div>

<c:if test="${not empty editAuthor}">
<div class="card">
    <h3><c:out value="${editAuthor.authorCode}"/> 메뉴 접근 권한</h3>
    <form method="post" action="<c:url value='/pmc/admin/authority/menus.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="authorCode" value="${editAuthor.authorCode}"/>
        <table class="tbl">
            <tr><th>접근</th><th>메뉴번호</th><th>메뉴명</th><th>URL</th></tr>
            <c:forEach var="m" items="${menus}">
                <c:set var="chk" value=""/>
                <c:forEach var="am" items="${authorMenus}"><c:if test="${am == m.menuNo}"><c:set var="chk" value="checked"/></c:if></c:forEach>
                <tr>
                    <td><input type="checkbox" name="menuNos" value="${m.menuNo}" ${chk}/></td>
                    <td>${m.menuNo}</td>
                    <td><c:if test="${not empty m.upperMenuNo}">&nbsp;&nbsp;└ </c:if><c:out value="${m.menuNm}"/></td>
                    <td><code><c:out value="${m.menuUrl}"/></code></td>
                </tr>
            </c:forEach>
        </table>
        <p><button class="btn">메뉴 권한 저장</button>
           <span class="muted">※ URL 보안(접근통제)은 context-security.xml 의 intercept-url 이 1차 적용되며, 본 매핑은 메뉴 노출/권한 모델 관리용입니다.</span></p>
    </form>
</div>
</c:if>
<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
