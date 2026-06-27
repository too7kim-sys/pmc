<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">취약점 진단 현황 (동일 취약점 관리·재발방지)</h2>

<c:if test="${not empty msg}"><div class="card"><span class="st NORMAL"><c:out value="${msg}"/></span></div></c:if>

<div class="card">
    <form class="inline" method="get" action="<c:url value='/pmc/vuln/list.do'/>">
        대상서버:
        <select name="serverId" onchange="this.form.submit()">
            <option value="">전체</option>
            <c:forEach var="s" items="${servers}">
                <option value="${s.serverId}" ${fServerId == s.serverId ? 'selected':''}><c:out value="${s.hostname}"/></option>
            </c:forEach>
        </select>
        등급:
        <select name="severity" onchange="this.form.submit()">
            <option value="">전체</option>
            <option value="상" ${fSeverity=='상'?'selected':''}>상</option>
            <option value="중" ${fSeverity=='중'?'selected':''}>중</option>
            <option value="하" ${fSeverity=='하'?'selected':''}>하</option>
        </select>
        상태:
        <select name="status" onchange="this.form.submit()">
            <option value="">전체</option>
            <option value="OPEN"     ${fStatus=='OPEN'?'selected':''}>OPEN</option>
            <option value="RECURRED" ${fStatus=='RECURRED'?'selected':''}>RECURRED(재발)</option>
            <option value="FIXED"    ${fStatus=='FIXED'?'selected':''}>FIXED</option>
            <option value="EXEMPTED" ${fStatus=='EXEMPTED'?'selected':''}>EXEMPTED(예외)</option>
        </select>
        검색: <input name="keyword" value="<c:out value='${fKeyword}'/>" placeholder="점검코드/취약점명" style="width:160px"/>
        <button class="btn">검색</button>
    </form>
    <span class="inline">
        <a class="btn" href="<c:url value='/pmc/vuln/recurrence.do'/>">재발 현황</a>
        <sec:authorize access="hasRole('ADMIN')">
            <a class="btn" href="<c:url value='/pmc/admin/vulnImport.do'/>">외부 스캐너 가져오기</a>
        </sec:authorize>
    </span>
    <form class="inline" method="post" action="<c:url value='/pmc/vuln/report.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <select name="type"><option>PDF</option><option>XLSX</option><option>CSV</option></select>
        <button class="btn">취약점 보고서 생성</button>
    </form>
    <p class="muted">등급: 상=CRITICAL · 중=WARN · 하=NA. 상태: OPEN(미조치) · RECURRED(조치 후 재발) · FIXED(해소) · EXEMPTED(수용/면제).</p>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>호스트</th><th>점검코드</th><th>취약점</th><th>등급</th><th>상태</th><th>출처</th><th>발생/재발</th><th>최근탐지</th></tr>
        <c:forEach var="f" items="${findings}">
            <tr>
                <td><c:out value="${f.hostname}"/></td>
                <td><c:out value="${f.checkCode}"/></td>
                <td><a href="<c:url value='/pmc/vuln/detail.do'/>?findingId=${f.findingId}"><c:out value="${f.title}"/></a></td>
                <td><span class="st ${f.cssClass}"><c:out value="${f.severity}"/></span></td>
                <td><span class="st ${f.statusCssClass}"><c:out value="${f.status}"/></span></td>
                <td><c:out value="${f.source}"/></td>
                <td>${f.occurrenceCount} / ${f.recurCount}</td>
                <td>${f.lastDetectedAt}</td>
            </tr>
        </c:forEach>
        <c:if test="${empty findings}"><tr><td colspan="8" class="muted">취약점이 없습니다.</td></tr></c:if>
    </table>
    <p class="inline" style="margin-top:8px">
        <span class="muted">총 ${totalCount}건 · ${page}/${totalPages} 페이지</span>
        <c:if test="${page > 1}">
            <c:url var="prevUrl" value="/pmc/vuln/list.do">
                <c:if test="${not empty fServerId}"><c:param name="serverId" value="${fServerId}"/></c:if>
                <c:if test="${not empty fSeverity}"><c:param name="severity" value="${fSeverity}"/></c:if>
                <c:if test="${not empty fStatus}"><c:param name="status" value="${fStatus}"/></c:if>
                <c:if test="${not empty fKeyword}"><c:param name="keyword" value="${fKeyword}"/></c:if>
                <c:param name="page" value="${page-1}"/>
            </c:url>
            <a class="btn" href="${prevUrl}">이전</a>
        </c:if>
        <c:if test="${page < totalPages}">
            <c:url var="nextUrl" value="/pmc/vuln/list.do">
                <c:if test="${not empty fServerId}"><c:param name="serverId" value="${fServerId}"/></c:if>
                <c:if test="${not empty fSeverity}"><c:param name="severity" value="${fSeverity}"/></c:if>
                <c:if test="${not empty fStatus}"><c:param name="status" value="${fStatus}"/></c:if>
                <c:if test="${not empty fKeyword}"><c:param name="keyword" value="${fKeyword}"/></c:if>
                <c:param name="page" value="${page+1}"/>
            </c:url>
            <a class="btn" href="${nextUrl}">다음</a>
        </c:if>
    </p>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
