<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">일자별 점검 모니터링</h2>

<div class="card">
    <a class="btn gray" href="<c:url value='/pmc/monitoring/realtime.do'/>">실시간 상태</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/svc.do'/>">웹서비스(URL)</a>
    <a class="btn" href="<c:url value='/pmc/monitoring/daily.do'/>">일자별 점검</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/capacity.do'/>">용량점검</a>
    <a class="btn gray" href="<c:url value='/pmc/alert/list.do'/>">알림이력</a>
</div>

<div class="card">
    <form class="inline" method="get" action="<c:url value='/pmc/monitoring/daily.do'/>">
        조회기간:
        <select name="days" onchange="this.form.submit()">
            <option value="7"  ${days==7 ?'selected':''}>최근 7일</option>
            <option value="30" ${days==30?'selected':''}>최근 30일</option>
            <option value="90" ${days==90?'selected':''}>최근 90일</option>
        </select>
    </form>
</div>

<div class="card">
    <table class="tbl">
        <tr>
            <th>일자</th><th>점검 실행</th><th>대상 서버</th>
            <th>정상</th><th>주의</th><th>위험</th><th>오류</th>
            <th>판정 분포</th><th></th>
        </tr>
        <c:forEach var="d" items="${daily}">
            <c:set var="tot" value="${d.runCnt > 0 ? d.runCnt : 1}"/>
            <tr>
                <td>${d.day}</td>
                <td>${d.runCnt}</td>
                <td>${d.serverCnt}</td>
                <td>${d.normalCnt}</td>
                <td style="color:#f39c12">${d.warnCnt}</td>
                <td style="color:#c0392b">${d.criticalCnt}</td>
                <td>${d.errorCnt}</td>
                <td>
                    <div class="bar" style="display:flex; width:200px;">
                        <span style="width:${d.normalCnt*100/tot}%; background:#2e7d32"></span>
                        <span style="width:${d.warnCnt*100/tot}%; background:#f39c12"></span>
                        <span style="width:${d.criticalCnt*100/tot}%; background:#c0392b"></span>
                        <span style="width:${d.errorCnt*100/tot}%; background:#6b2737"></span>
                    </div>
                </td>
                <td><a class="btn" href="<c:url value='/pmc/monitoring/dailyDetail.do'/>?day=${d.day}">상세</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty daily}"><tr><td colspan="9" class="muted">데이터 없음</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
