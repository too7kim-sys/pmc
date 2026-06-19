<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">점검 실행 이력</h2>
<div class="card">
    <table class="tbl">
        <tr><th>호스트</th><th>유형</th><th>종합판정</th><th>항목</th><th>주의</th><th>위험</th><th>오류</th><th>수신시각</th><th></th></tr>
        <c:forEach var="r" items="${runs}">
            <tr>
                <td><c:out value="${r.hostname}"/></td><td>${r.runType}</td>
                <td><span class="st ${r.overallStatus}">${r.overallStatus}</span></td>
                <td>${r.itemCount}</td><td>${r.warnCount}</td><td>${r.criticalCount}</td><td>${r.errorCount}</td>
                <td>${r.receivedAt}</td>
                <td><a class="btn" href="<c:url value='/pmc/inspection/detail.do'/>?runId=${r.runId}">상세</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty runs}"><tr><td colspan="9" class="muted">점검 이력 없음</td></tr></c:if>
    </table>
</div>
<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
