<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">데이터 품질 점검</h2>

<div class="card">
    <form method="post" action="<c:url value='/pmc/admin/dqRun.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <button class="btn">전체 품질룰 실행</button>
    </form>
</div>

<div class="card">
    <h3>품질 룰</h3>
    <table class="tbl">
        <tr><th>ID</th><th>룰명</th><th>대상테이블</th><th>유형</th><th>심각도</th><th>사용</th></tr>
        <c:forEach var="r" items="${rules}">
            <tr><td>${r.ruleId}</td><td>${r.ruleName}</td><td>${r.targetTable}</td>
                <td>${r.ruleType}</td><td>${r.severity}</td><td>${r.useYn}</td></tr>
        </c:forEach>
    </table>
</div>

<div class="card">
    <h3>점검 결과(최근)</h3>
    <table class="tbl">
        <tr><th>ID</th><th>룰명</th><th>심각도</th><th>위반건수</th><th>상태</th><th>점검시각</th></tr>
        <c:forEach var="r" items="${results}">
            <tr><td>${r.resultId}</td><td>${r.ruleName}</td><td>${r.severity}</td>
                <td>${r.violationCount}</td>
                <td><span class="st ${r.status=='PASS'?'NORMAL':(r.status=='FAIL'?'CRITICAL':'ERROR')}">${r.status}</span></td>
                <td>${r.checkedAt}</td></tr>
        </c:forEach>
        <c:if test="${empty results}"><tr><td colspan="6" class="muted">결과 없음</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
