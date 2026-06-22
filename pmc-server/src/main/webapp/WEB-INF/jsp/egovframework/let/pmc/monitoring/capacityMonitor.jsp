<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">용량 점검 — CPU · 메모리 · 디스크 추이</h2>

<div class="card">
    <a class="btn gray" href="<c:url value='/pmc/monitoring/realtime.do'/>">실시간 상태</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/svc.do'/>">웹서비스(URL)</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/daily.do'/>">일자별 점검</a>
    <a class="btn" href="<c:url value='/pmc/monitoring/capacity.do'/>">용량점검</a>
</div>

<div class="card">
    <form class="inline" method="get" action="<c:url value='/pmc/monitoring/capacity.do'/>">
        대상 서버:
        <select name="serverId" onchange="this.form.submit()">
            <option value="">-- 선택 --</option>
            <c:forEach var="s" items="${servers}">
                <option value="${s.serverId}" ${serverId == s.serverId ? 'selected' : ''}><c:out value="${s.hostname}"/></option>
            </c:forEach>
        </select>
        기간:
        <select name="days" onchange="this.form.submit()">
            <option value="1"  ${days==1 ?'selected':''}>1일</option>
            <option value="7"  ${days==7 ?'selected':''}>7일</option>
            <option value="30" ${days==30?'selected':''}>30일</option>
        </select>
        고사용 기준:
        <select name="high" onchange="this.form.submit()">
            <option value="70" ${high==70?'selected':''}>70%</option>
            <option value="80" ${high==80?'selected':''}>80%</option>
            <option value="90" ${high==90?'selected':''}>90%</option>
        </select>
        <span class="muted">사용률 ≥ 기준 또는 판정 WARN/CRITICAL 구간을 이상으로 표시</span>
    </form>
</div>

<c:if test="${empty serverId}">
    <div class="card muted">대상 서버를 선택하면 CPU·메모리·디스크 사용률 추이와 고사용 이상구간을 확인할 수 있습니다.</div>
</c:if>

<c:forEach var="m" items="${metrics}">
    <div class="card">
        <h3><c:out value="${m.label}"/>
            <c:if test="${m.count > 0}">
                <span class="st ${empty m.latestStatus ? 'NA' : m.latestStatus}" style="margin-left:8px;">현재 ${m.latest}%</span>
                <span class="muted">최소 ${m.min}% · 평균 ${m.avg}% · 최대 ${m.max}% · 측정 ${m.count}회 · 이상 ${m.highCount}회</span>
            </c:if>
        </h3>

        <c:choose>
            <c:when test="${m.count == 0}">
                <p class="muted">해당 기간 ${m.label} 데이터가 없습니다.</p>
            </c:when>
            <c:otherwise>
                <%-- 추이 스파크라인: 600x120, 사용률 0~100% (y = 120 - v*1.1) --%>
                <svg width="100%" viewBox="0 0 600 120" preserveAspectRatio="none"
                     style="background:#fafbfd; border:1px solid #e0e4ea; border-radius:4px;">
                    <%-- 0/50/100% 기준선 --%>
                    <line x1="0" y1="120" x2="600" y2="120" stroke="#dde2e8"/>
                    <line x1="0" y1="65"  x2="600" y2="65"  stroke="#eef2f7"/>
                    <line x1="0" y1="10"  x2="600" y2="10"  stroke="#eef2f7"/>
                    <%-- 고사용 기준선(빨강 점선) --%>
                    <c:set var="hy" value="${120 - high*1.1}"/>
                    <line x1="0" y1="${hy}" x2="600" y2="${hy}" stroke="#c0392b" stroke-dasharray="4,3"/>
                    <text x="2" y="${hy - 2}" font-size="9" fill="#c0392b">고사용 ${high}%</text>
                    <%-- 추이 선 --%>
                    <polyline fill="none" stroke="#27508f" stroke-width="1.5" points="<c:forEach var="pt" items="${m.points}" varStatus="st"><c:set var="x" value="${m.count > 1 ? st.index * 600 / (m.count - 1) : 300}"/><c:set var="y" value="${120 - pt.value * 1.1}"/>${x},${y} </c:forEach>"/>
                    <%-- 이상(고사용) 측정점 강조 --%>
                    <c:forEach var="pt" items="${m.points}" varStatus="st">
                        <c:if test="${pt.high}">
                            <c:set var="cx" value="${m.count > 1 ? st.index * 600 / (m.count - 1) : 300}"/>
                            <c:set var="cy" value="${120 - pt.value * 1.1}"/>
                            <circle cx="${cx}" cy="${cy}" r="2.5" fill="${pt.status == 'CRITICAL' ? '#c0392b' : '#f39c12'}"/>
                        </c:if>
                    </c:forEach>
                </svg>
                <p class="muted">기간: <c:out value="${m.points[0].ts}"/> ~ <c:out value="${m.points[m.count-1].ts}"/></p>

                <%-- 고사용 이상구간 --%>
                <c:choose>
                    <c:when test="${empty m.segments}">
                        <p><span class="st NORMAL">이상 없음</span> <span class="muted">고사용 구간이 발견되지 않았습니다.</span></p>
                    </c:when>
                    <c:otherwise>
                        <table class="tbl" style="margin-top:8px;">
                            <tr><th>이상구간 시작</th><th>종료</th><th>측정점</th><th>최고 사용률</th><th>판정</th></tr>
                            <c:forEach var="seg" items="${m.segments}">
                                <tr>
                                    <td>${seg.startTs}</td><td>${seg.endTs}</td><td>${seg.count}회</td>
                                    <td>${seg.peak}%</td>
                                    <td><span class="st ${seg.level}">${seg.level}</span></td>
                                </tr>
                            </c:forEach>
                        </table>
                    </c:otherwise>
                </c:choose>
            </c:otherwise>
        </c:choose>
    </div>
</c:forEach>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
