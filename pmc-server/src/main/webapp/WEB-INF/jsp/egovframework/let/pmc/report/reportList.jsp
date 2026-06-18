<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">보고서</h2>
<div class="card">
    <table class="tbl">
        <tr><th>ID</th><th>유형</th><th>범위</th><th>파일명</th><th>크기</th><th>생성시각</th><th></th></tr>
        <c:forEach var="r" items="${reports}">
            <tr>
                <td>${r.reportId}</td><td>${r.reportType}</td><td>${r.scopeType}</td>
                <td>${r.fileName}</td><td>${r.fileSize} B</td><td>${r.regDt}</td>
                <td><a class="btn" href="<c:url value='/pmc/report/download.do'/>?reportId=${r.reportId}">다운로드</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty reports}"><tr><td colspan="7" class="muted">생성된 보고서 없음 (점검상세/계획상세에서 생성)</td></tr></c:if>
    </table>
</div>
<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
