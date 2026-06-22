<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">문제 가능성 점검 (규칙 기반)</h2>

<div class="card">
    <form class="inline" method="get" action="<c:url value='/pmc/risk/list.do'/>">
        분석기간:
        <select name="days" onchange="this.form.submit()">
            <option value="7"  ${days==7 ?'selected':''}>최근 7일</option>
            <option value="14" ${days==14?'selected':''}>최근 14일</option>
            <option value="30" ${days==30?'selected':''}>최근 30일</option>
        </select>
    </form>
    <form class="inline" method="post" action="<c:url value='/pmc/risk/report.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="days" value="${days}"/>
        <select name="type"><option>PDF</option><option>XLSX</option><option>CSV</option></select>
        <button class="btn">위험분석 보고서 생성</button>
    </form>
    <p class="muted">점수 = 최신판정 + 기간 위험/주의 항목 + heartbeat 누락 + 미점검 + 웹서비스 실패 가중합(최대 100).
       HIGH≥70 · MEDIUM≥40 · LOW≥1</p>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>호스트</th><th>서비스</th><th>위험점수</th><th>위험도</th><th>주요 사유</th></tr>
        <c:forEach var="r" items="${scores}">
            <tr>
                <td><c:out value="${r.hostname}"/></td>
                <td><c:out value="${r.serviceName}"/></td>
                <td>
                    <div class="bar" style="width:140px; display:inline-block; vertical-align:middle;">
                        <span style="width:${r.score}%; background:${r.score >= 70 ? '#c0392b' : (r.score >= 40 ? '#f39c12' : '#27508f')}">${r.score}</span>
                    </div>
                </td>
                <td><span class="st ${r.cssClass}">${r.level}</span></td>
                <td>
                    <c:choose>
                        <c:when test="${empty r.reasons}"><span class="muted">이상 징후 없음</span></c:when>
                        <c:otherwise>
                            <c:forEach var="rs" items="${r.reasons}" varStatus="st"><c:if test="${not st.first}"> · </c:if><c:out value="${rs}"/></c:forEach>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty scores}"><tr><td colspan="5" class="muted">분석할 서버가 없습니다.</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
