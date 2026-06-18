<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">사용자 관리</h2>
<div class="card">
    <p class="muted">사용자정보 연계(LDAP/SSO/조직도)는 com/integration 어댑터로 동기화됩니다(설정 시).</p>
    <table class="tbl">
        <tr><th>ID</th><th>성명</th><th>이메일</th><th>직위</th><th>부서</th><th>권한</th><th>상태</th><th>잠금</th><th>최근로그인</th></tr>
        <c:forEach var="u" items="${users}">
            <tr><td>${u.emplyrId}</td><td>${u.userNm}</td><td>${u.emailAdres}</td><td>${u.ofcpsNm}</td>
                <td>${u.deptCode}</td><td>${u.roles}</td><td>${u.emplyrSttus}</td><td>${u.lockAt}</td><td>${u.lastLoginDt}</td></tr>
        </c:forEach>
    </table>
</div>
<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
