<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">취약점 상세</h2>

<c:if test="${not empty msg}"><div class="card"><span class="st NORMAL"><c:out value="${msg}"/></span></div></c:if>

<c:choose>
<c:when test="${empty finding}">
    <div class="card"><p class="muted">존재하지 않는 취약점입니다.</p></div>
</c:when>
<c:otherwise>
<div class="card">
    <table class="tbl">
        <tr><th>호스트</th><td><c:out value="${finding.hostname}"/></td>
            <th>점검코드</th><td><c:out value="${finding.checkCode}"/></td></tr>
        <tr><th>취약점</th><td colspan="3"><c:out value="${finding.title}"/></td></tr>
        <tr><th>등급</th><td><span class="st ${finding.cssClass}"><c:out value="${finding.severity}"/></span></td>
            <th>상태</th><td><span class="st ${finding.statusCssClass}"><c:out value="${finding.status}"/></span></td></tr>
        <tr><th>출처</th><td><c:out value="${finding.source}"/></td>
            <th>발생/재발</th><td>${finding.occurrenceCount} / ${finding.recurCount}</td></tr>
        <tr><th>최초탐지</th><td>${finding.firstDetectedAt}</td>
            <th>최근탐지</th><td>${finding.lastDetectedAt}</td></tr>
    </table>
    <c:if test="${not empty exception}">
        <p class="muted">현재 예외(수용/면제) 적용 중 — 만료: ${exception.expiresAt} (사유: <c:out value="${exception.reason}"/>)</p>
    </c:if>
</div>

<div class="card">
    <h3>조치 등록</h3>
    <form method="post" action="<c:url value='/pmc/vuln/action.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="findingId" value="${finding.findingId}"/>
        <p>조치내용: <input type="text" name="actionDesc" size="60" placeholder="조치 내역을 입력"/></p>
        <p>처리상태:
            <select name="resultingStatus">
                <option value="">(상태 변경 없음)</option>
                <option value="FIXED">FIXED(조치완료)</option>
                <option value="OPEN">OPEN(재오픈)</option>
            </select>
            <button class="btn">조치 등록</button>
        </p>
    </form>
</div>

<sec:authorize access="hasRole('ADMIN')">
<div class="card">
    <h3>예외(수용/면제) 등록 — 관리자</h3>
    <form method="post" action="<c:url value='/pmc/admin/vulnException.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="findingId" value="${finding.findingId}"/>
        <p>사유: <input type="text" name="reason" size="50" required/></p>
        <p>결재자: <input type="text" name="approver" size="20"/>
           만료일: <input type="date" name="expiresOn" required/>
           <button class="btn">예외 등록</button></p>
        <p class="muted">만료일 도래 시 자동으로 예외 해제 → 재탐지되면 재발(RECURRED)로 알림됩니다.</p>
    </form>
</div>
</sec:authorize>

<div class="card">
    <h3>조치 이력</h3>
    <table class="tbl">
        <tr><th>일시</th><th>담당자</th><th>조치내용</th><th>결과상태</th></tr>
        <c:forEach var="a" items="${actions}">
            <tr>
                <td>${a.actionDt}</td>
                <td><c:out value="${a.actionUser}"/></td>
                <td><c:out value="${a.actionDesc}"/></td>
                <td><c:out value="${a.resultingStatus}"/></td>
            </tr>
        </c:forEach>
        <c:if test="${empty actions}"><tr><td colspan="4" class="muted">조치 이력이 없습니다.</td></tr></c:if>
    </table>
    <p><a class="btn" href="<c:url value='/pmc/vuln/list.do'/>">목록</a></p>
</div>
</c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
