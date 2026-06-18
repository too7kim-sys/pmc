<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">정기점검 계획 수립</h2>

<div class="card">
    <form method="post" action="<c:url value='/pmc/plan/regist.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <table class="tbl">
            <tr><th>계획명</th><td><input type="text" name="planName" style="width:300px;" required></td></tr>
            <tr><th>주기</th><td>
                <select name="cycle">
                    <option value="MONTHLY">월간</option><option value="QUARTERLY">분기</option>
                    <option value="HALF">반기</option><option value="YEARLY">연간</option>
                </select></td></tr>
            <tr><th>점검 기간</th><td>
                <input type="date" name="periodFrom"> ~ <input type="date" name="periodTo"></td></tr>
            <tr><th>점검 예정일</th><td><input type="date" name="plannedDate"></td></tr>
            <tr><th>적용 정책</th><td>
                <select name="policyId">
                    <c:forEach var="p" items="${policies}">
                        <option value="${p.policyId}">${p.policyName} (v${p.version})</option>
                    </c:forEach>
                </select></td></tr>
            <tr><th>점검자</th><td><input type="text" name="inspectorId" value="admin"></td></tr>
            <tr><th>결재자</th><td><input type="text" name="approverId" value="admin"></td></tr>
            <tr><th>대상 서버</th><td>
                <c:forEach var="s" items="${servers}">
                    <label><input type="checkbox" name="serverIds" value="${s.serverId}"> ${s.hostname} (${s.osType})</label><br>
                </c:forEach>
                <c:if test="${empty servers}"><span class="muted">등록된 서버가 없습니다.</span></c:if>
            </td></tr>
        </table>
        <button class="btn" type="submit">계획 저장</button>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
