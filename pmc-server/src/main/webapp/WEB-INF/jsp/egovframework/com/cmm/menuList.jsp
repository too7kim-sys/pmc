<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">메뉴 관리</h2>

<c:if test="${not empty msg}"><div class="card"><span class="st NORMAL"><c:out value="${msg}"/></span></div></c:if>

<div class="card">
    <h3><c:choose><c:when test="${not empty editMenu}">메뉴 수정</c:when><c:otherwise>메뉴 등록</c:otherwise></c:choose></h3>
    <form method="post" action="<c:url value='/pmc/admin/menu/save.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="isNew" value="${empty editMenu ? 'true' : 'false'}"/>
        <p>메뉴번호:
            <c:choose>
                <c:when test="${not empty editMenu}">
                    <b>${editMenu.menuNo}</b><input type="hidden" name="menuNo" value="${editMenu.menuNo}"/>
                </c:when>
                <c:otherwise><input type="number" name="menuNo" required style="width:110px" placeholder="예 9000"/></c:otherwise>
            </c:choose>
            메뉴명: <input name="menuNm" value="<c:out value='${editMenu.menuNm}'/>" required/>
            URL: <input name="menuUrl" value="<c:out value='${editMenu.menuUrl}'/>" placeholder="/pmc/...do" style="width:200px"/>
        </p>
        <p>상위메뉴:
            <select name="upperMenuNo">
                <option value="">(최상위)</option>
                <c:forEach var="m" items="${menus}">
                    <c:if test="${empty editMenu or m.menuNo != editMenu.menuNo}">
                        <option value="${m.menuNo}" ${editMenu.upperMenuNo == m.menuNo ? 'selected' : ''}>
                            ${m.menuNo} - <c:out value="${m.menuNm}"/></option>
                    </c:if>
                </c:forEach>
            </select>
            순서: <input type="number" name="menuOrdr" value="${empty editMenu ? 0 : editMenu.menuOrdr}" style="width:70px"/>
            사용:
            <select name="useYn">
                <option value="Y" ${editMenu.useYn=='N' ? '' : 'selected'}>Y</option>
                <option value="N" ${editMenu.useYn=='N' ? 'selected' : ''}>N</option>
            </select>
            <button class="btn">저장</button>
            <c:if test="${not empty editMenu}"><a class="btn" href="<c:url value='/pmc/admin/menu.do'/>">취소</a></c:if>
        </p>
    </form>
    <p class="muted">상위메뉴가 있으면 하위(2차) 메뉴로 분류됩니다. 하위 메뉴가 있는 메뉴는 삭제할 수 없습니다.</p>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>메뉴번호</th><th>메뉴명</th><th>URL</th><th>상위메뉴</th><th>순서</th><th>사용</th><th>관리</th></tr>
        <c:forEach var="m" items="${menus}">
            <tr>
                <td>${m.menuNo}</td>
                <td><c:if test="${not empty m.upperMenuNo}">&nbsp;&nbsp;└ </c:if><c:out value="${m.menuNm}"/></td>
                <td><code><c:out value="${m.menuUrl}"/></code></td>
                <td><c:out value="${m.upperMenuNm}"/></td>
                <td>${m.menuOrdr}</td>
                <td>${m.useYn}</td>
                <td>
                    <a class="btn" href="<c:url value='/pmc/admin/menu.do'/>?menuNo=${m.menuNo}">수정</a>
                    <form class="inline" method="post" action="<c:url value='/pmc/admin/menu/delete.do'/>" onsubmit="return confirm('삭제하시겠습니까?');">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <input type="hidden" name="menuNo" value="${m.menuNo}"/>
                        <button class="btn">삭제</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty menus}"><tr><td colspan="7" class="muted">등록된 메뉴가 없습니다.</td></tr></c:if>
    </table>
</div>
<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
