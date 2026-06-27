<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">감사 로그</h2>
<div class="card">
    <p class="muted">로그인(성공/실패·잠금)·관리자 작업 이력. 최근 300건.</p>
    <table class="tbl">
        <tr><th>일시</th><th>행위자</th><th>액션</th><th>대상유형</th><th>대상ID</th><th>상세</th><th>IP</th></tr>
        <c:forEach var="a" items="${auditLogs}">
            <tr>
                <td>${a.regDt}</td>
                <td><c:out value="${a.actorId}"/></td>
                <td><span class="st ${a.action=='LOGIN_FAIL' ? 'WARN' : (a.action=='LOGIN_SUCCESS' ? 'NORMAL' : 'NA')}"><c:out value="${a.action}"/></span></td>
                <td><c:out value="${a.targetType}"/></td>
                <td><c:out value="${a.targetId}"/></td>
                <td><c:out value="${a.details}"/></td>
                <td><c:out value="${a.actorIp}"/></td>
            </tr>
        </c:forEach>
        <c:if test="${empty auditLogs}"><tr><td colspan="7" class="muted">감사 로그가 없습니다.</td></tr></c:if>
    </table>
</div>
<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
