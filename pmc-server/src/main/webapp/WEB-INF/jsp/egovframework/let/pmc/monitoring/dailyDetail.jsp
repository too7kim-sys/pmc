<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">일자별 점검 상세 — <c:out value="${day}"/></h2>

<div class="card">
    <a class="btn gray" href="<c:url value='/pmc/monitoring/daily.do'/>">← 일자별 목록</a>
</div>

<div class="card">
    <table class="tbl">
        <tr>
            <th>시각</th><th>호스트</th><th>유형</th><th>종합판정</th>
            <th>항목</th><th>주의</th><th>위험</th><th>오류</th><th></th>
        </tr>
        <c:forEach var="r" items="${runs}">
            <tr>
                <td>${r.receivedAt}</td>
                <td><c:out value="${r.hostname}"/></td>
                <td><c:out value="${r.runType}"/></td>
                <td><span class="st ${empty r.overallStatus ? 'NA' : r.overallStatus}"><c:out value="${r.overallStatus}"/></span></td>
                <td>${r.itemCount}</td>
                <td>${r.warnCount}</td>
                <td>${r.criticalCount}</td>
                <td>${r.errorCount}</td>
                <td><a class="btn" href="<c:url value='/pmc/inspection/detail.do'/>?runId=${r.runId}">점검상세</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty runs}"><tr><td colspan="9" class="muted">해당 일자 점검 실행이 없습니다.</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
