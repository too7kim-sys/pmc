<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">외부 스캐너 결과 가져오기 (관리자)</h2>

<div class="card">
    <p class="muted">OpenSCAP / Trivy / Nessus 의 JSON export 본문을 붙여넣으면 동일 취약점 추적에 반영됩니다
       (source=SCANNER, 전체 스캔으로 처리되어 이번 결과에 없는 기존 항목은 자동 FIXED).</p>
    <form method="post" action="<c:url value='/pmc/admin/vulnImport.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <p>스캐너:
            <select name="scannerType">
                <option value="TRIVY">Trivy</option>
                <option value="OPENSCAP">OpenSCAP</option>
                <option value="NESSUS">Nessus</option>
            </select>
            대상서버:
            <select name="serverId" required>
                <c:forEach var="s" items="${servers}">
                    <option value="${s.serverId}"><c:out value="${s.hostname}"/></option>
                </c:forEach>
            </select>
        </p>
        <p><textarea name="json" rows="16" cols="100" placeholder='{ "Results": [ ... ] }' required></textarea></p>
        <p><button class="btn">가져오기</button>
           <a class="btn" href="<c:url value='/pmc/vuln/list.do'/>">취소</a></p>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
