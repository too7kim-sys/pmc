<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">점검 결과 상세 — ${run.hostname}</h2>

<div class="card">
    <p>종합판정: <span class="st ${run.overallStatus}">${run.overallStatus}</span>
       · 유형: ${run.runType} · 항목 ${run.itemCount} (주의 ${run.warnCount} / 위험 ${run.criticalCount} / 오류 ${run.errorCount})
       · 점검시작: ${run.startedAt}</p>
    <form class="inline" method="post" action="<c:url value='/pmc/report/generateRun.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="runId" value="${run.runId}"/>
        보고서:
        <select name="type"><option>PDF</option><option>XLSX</option><option>CSV</option></select>
        <button class="btn">점검표 생성</button>
    </form>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>분류</th><th>항목코드</th><th>항목명</th><th>수집값</th><th>단위</th><th>기준(주의/위험)</th><th>판정</th><th>소스</th></tr>
        <c:forEach var="i" items="${run.items}">
            <tr>
                <td>${i.category}</td><td><code>${i.itemCode}</code></td><td>${i.itemName}</td>
                <td>${i.value}</td><td>${i.unit}</td>
                <td>${i.thresholdWarn} / ${i.thresholdCritical}</td>
                <td><span class="st ${i.status}">${i.status}</span></td>
                <td>${i.source}</td>
            </tr>
        </c:forEach>
        <c:if test="${empty run.items}"><tr><td colspan="8" class="muted">항목 없음</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
