<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">이상 알림 이력</h2>

<div class="card">
    <a class="btn gray" href="<c:url value='/pmc/monitoring/realtime.do'/>">실시간 상태</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/svc.do'/>">웹서비스(URL)</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/daily.do'/>">일자별 점검</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/capacity.do'/>">용량점검</a>
    <a class="btn" href="<c:url value='/pmc/alert/list.do'/>">알림이력</a>
    <span class="muted">알림은 globals.properties 의 Globals.AlertEnabled / Globals.AlertWebhookUrl 설정 시 발송됩니다.</span>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>시각</th><th>유형</th><th>심각도</th><th>제목</th><th>발송</th></tr>
        <c:forEach var="a" items="${alerts}">
            <tr>
                <td>${a.createdAt}</td>
                <td>${a.alertType}</td>
                <td><span class="st ${empty a.severity ? 'NA' : a.severity}"><c:out value="${a.severity}"/></span></td>
                <td><c:out value="${a.title}"/></td>
                <td><span class="st ${a.sentStatus == 'SENT' ? 'NORMAL' : (a.sentStatus == 'FAILED' ? 'CRITICAL' : 'NA')}">${a.sentStatus}</span></td>
            </tr>
        </c:forEach>
        <c:if test="${empty alerts}"><tr><td colspan="5" class="muted">알림 이력이 없습니다.</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
