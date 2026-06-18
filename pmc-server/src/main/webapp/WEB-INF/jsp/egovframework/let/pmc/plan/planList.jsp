<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">정기점검 계획 및 결과 관리</h2>

<div class="card">
    <a class="btn" href="<c:url value='/pmc/plan/regist.do'/>">+ 계획 수립</a>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>ID</th><th>계획명</th><th>주기</th><th>예정일</th><th>점검자</th><th>상태</th><th>결재</th><th>이행률</th><th></th></tr>
        <c:forEach var="p" items="${plans}">
            <tr>
                <td>${p.planId}</td><td>${p.planName}</td><td>${p.cycle}</td><td>${p.plannedDate}</td>
                <td>${p.inspectorId}</td>
                <td><span class="st ${p.status=='DONE'?'NORMAL':(p.status=='OVERDUE'?'CRITICAL':'NA')}">${p.status}</span></td>
                <td>${p.approveStatus}</td>
                <td>
                    <div class="bar" style="width:120px;"><span style="width:${p.compliance}%">${p.compliance}%</span></div>
                    <span class="muted">${p.doneCnt}/${p.targetCnt}</span>
                </td>
                <td><a class="btn" href="<c:url value='/pmc/plan/detail.do'/>?planId=${p.planId}">상세</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty plans}"><tr><td colspan="9" class="muted">계획 없음</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
