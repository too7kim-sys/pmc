<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">취약점 재발 현황</h2>

<div class="card">
    <p class="muted">서버별 미조치(OPEN/RECURRED) 취약점과 재발(RECURRED) 건수. 재발은 조치 후 다시 탐지된 항목입니다.</p>
    <table class="tbl">
        <tr><th>호스트</th><th>미조치</th><th>재발(RECURRED)</th><th>상등급 미조치</th><th>누적 재발횟수</th><th>최근탐지</th></tr>
        <c:forEach var="s" items="${summary}">
            <tr>
                <td><c:out value="${s.hostname}"/></td>
                <td>${s.openCount}</td>
                <td><c:choose><c:when test="${s.recurredCount > 0}"><span class="st CRITICAL">${s.recurredCount}</span></c:when><c:otherwise>0</c:otherwise></c:choose></td>
                <td><c:choose><c:when test="${s.highOpen > 0}"><span class="st WARN">${s.highOpen}</span></c:when><c:otherwise>0</c:otherwise></c:choose></td>
                <td>${s.recurTotal}</td>
                <td>${s.lastDetectedAt}</td>
            </tr>
        </c:forEach>
        <c:if test="${empty summary}"><tr><td colspan="6" class="muted">미조치/재발 취약점이 없습니다.</td></tr></c:if>
    </table>
    <p><a class="btn" href="<c:url value='/pmc/vuln/list.do'/>">목록</a></p>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
