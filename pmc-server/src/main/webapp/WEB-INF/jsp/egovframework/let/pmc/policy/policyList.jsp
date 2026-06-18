<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">점검 정책</h2>

<div class="card">
    <h3>정책 등록</h3>
    <form method="post" action="<c:url value='/pmc/policy/regist.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="text" name="policyName" placeholder="정책명" required>
        <input type="text" name="scheduleCron" placeholder="실행주기(cron) 예: 0 0 6 * * *" style="width:240px;">
        <input type="text" name="description" placeholder="설명" style="width:300px;">
        <button class="btn">등록</button>
    </form>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>ID</th><th>정책명</th><th>버전</th><th>실행주기</th><th>설명</th><th></th></tr>
        <c:forEach var="p" items="${policies}">
            <tr>
                <td>${p.policyId}</td><td>${p.policyName}</td><td>${p.version}</td>
                <td>${p.scheduleCron}</td><td>${p.description}</td>
                <td><a class="btn" href="<c:url value='/pmc/policy/detail.do'/>?policyId=${p.policyId}">상세</a></td>
            </tr>
        </c:forEach>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
