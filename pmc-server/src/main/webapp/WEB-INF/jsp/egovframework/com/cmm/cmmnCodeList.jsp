<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">공통코드 관리</h2>
<div class="card">
    <table class="tbl">
        <tr><th>그룹코드</th><th>그룹명</th><th>코드</th><th>코드명</th><th>순서</th><th>사용</th></tr>
        <c:forEach var="c" items="${codes}">
            <tr><td>${c.clCode}</td><td>${c.clCodeNm}</td><td><code>${c.code}</code></td>
                <td>${c.codeNm}</td><td>${c.sortOrdr}</td><td>${c.useYn}</td></tr>
        </c:forEach>
    </table>
</div>
<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
