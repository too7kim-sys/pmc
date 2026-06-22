<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">웹서비스(URL) 응답시간 · 접속 실패 확인</h2>

<div class="card">
    <a class="btn gray" href="<c:url value='/pmc/monitoring/realtime.do'/>">실시간 상태</a>
    <a class="btn" href="<c:url value='/pmc/monitoring/svc.do'/>">웹서비스(URL)</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/daily.do'/>">일자별 점검</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/capacity.do'/>">용량점검</a>
</div>

<div class="card">
    <form class="inline" method="get" action="<c:url value='/pmc/monitoring/svc.do'/>">
        조회기간:
        <select name="days" onchange="this.form.submit()">
            <option value="1"  ${days==1 ?'selected':''}>최근 1일</option>
            <option value="7"  ${days==7 ?'selected':''}>최근 7일</option>
            <option value="30" ${days==30?'selected':''}>최근 30일</option>
        </select>
        <span class="muted">서비스명은 정책의 SVC 점검대상에서 정의됩니다.</span>
    </form>
</div>

<div class="card">
    <table class="tbl">
        <tr>
            <th>서비스</th><th>URL</th><th>최신 응답코드</th><th>응답시간</th>
            <th>SSL 만료</th><th>기간 실패율</th><th>최근 점검</th>
        </tr>
        <c:forEach var="s" items="${svcList}">
            <tr>
                <td><c:out value="${s.svcKey}"/></td>
                <td class="muted"><c:out value="${s.url}"/></td>
                <td>
                    <span class="st ${empty s.statusJudge ? 'NA' : s.statusJudge}"><c:out value="${empty s.httpStatus ? '-' : s.httpStatus}"/></span>
                    <c:if test="${not empty s.error}"><span class="muted"><c:out value="${s.error}"/></span></c:if>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty s.responseMs}"><c:out value="${s.responseMs}"/> ms</c:when>
                        <c:otherwise><span class="muted">-</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty s.sslDays}"><c:out value="${s.sslDays}"/>일</c:when>
                        <c:otherwise><span class="muted">-</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:set var="fr" value="${s.failRate}"/>
                    <div class="bar" style="width:120px; display:inline-block; vertical-align:middle;">
                        <span style="width:${fr}%; background:${fr >= 10 ? '#c0392b' : (fr > 0 ? '#f39c12' : '#27508f')}">${fr}%</span>
                    </div>
                    <span class="muted">(${s.fail}/${s.total})</span>
                </td>
                <td>${s.lastCheck}</td>
            </tr>
        </c:forEach>
        <c:if test="${empty svcList}"><tr><td colspan="7" class="muted">SVC 점검 데이터가 없습니다.</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
