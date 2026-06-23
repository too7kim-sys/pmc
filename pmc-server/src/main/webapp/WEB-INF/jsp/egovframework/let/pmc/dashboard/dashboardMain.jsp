<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">종합 대시보드</h2>

<div class="grid">
    <div class="kpi"><div class="num">${counters.serverCnt}</div><div class="lbl">점검대상 서버</div></div>
    <div class="kpi"><div class="num">${counters.agentCnt}</div><div class="lbl">활성 Agent</div></div>
    <div class="kpi"><div class="num">${counters.runToday}</div><div class="lbl">오늘 점검 실행</div></div>
    <div class="kpi"><div class="num" style="color:#c0392b">${counters.criticalToday}</div><div class="lbl">위험 항목(24h)</div></div>
    <div class="kpi"><div class="num" style="color:#f39c12">${counters.warnToday}</div><div class="lbl">주의 항목(24h)</div></div>
</div>

<div class="card">
    <h3>최근 14일 점검 판정 추이</h3>
    <c:set var="maxRun" value="1"/>
    <c:forEach var="d" items="${dailyTrend}"><c:if test="${d.runCnt > maxRun}"><c:set var="maxRun" value="${d.runCnt}"/></c:if></c:forEach>
    <c:set var="n" value="${fn:length(dailyTrend)}"/>
    <c:choose>
        <c:when test="${n == 0}"><p class="muted">최근 점검 데이터가 없습니다.</p></c:when>
        <c:otherwise>
            <%-- 일자별 누적 막대(정상=녹/주의=주황/위험=빨강/오류=암적), 높이는 일 최대 run 기준 정규화 --%>
            <svg width="100%" viewBox="0 0 700 160" preserveAspectRatio="none"
                 style="background:#fafbfd; border:1px solid #e0e4ea; border-radius:4px;">
                <line x1="0" y1="140" x2="700" y2="140" stroke="#dde2e8"/>
                <c:set var="bw" value="${700 / n}"/>
                <c:forEach var="d" items="${dailyTrend}" varStatus="st">
                    <c:set var="x" value="${st.index * bw + bw*0.15}"/>
                    <c:set var="w" value="${bw*0.7}"/>
                    <c:set var="hCrit" value="${d.criticalCnt * 130 / maxRun}"/>
                    <c:set var="hErr"  value="${d.errorCnt * 130 / maxRun}"/>
                    <c:set var="hWarn" value="${d.warnCnt * 130 / maxRun}"/>
                    <c:set var="hNorm" value="${d.normalCnt * 130 / maxRun}"/>
                    <c:set var="y0" value="140"/>
                    <rect x="${x}" y="${y0 - hNorm}" width="${w}" height="${hNorm}" fill="#2e7d32"/>
                    <rect x="${x}" y="${y0 - hNorm - hWarn}" width="${w}" height="${hWarn}" fill="#f39c12"/>
                    <rect x="${x}" y="${y0 - hNorm - hWarn - hCrit}" width="${w}" height="${hCrit}" fill="#c0392b"/>
                    <rect x="${x}" y="${y0 - hNorm - hWarn - hCrit - hErr}" width="${w}" height="${hErr}" fill="#6b2737"/>
                    <text x="${x + w/2}" y="152" font-size="8" fill="#888" text-anchor="middle">${d.day}</text>
                </c:forEach>
            </svg>
            <p class="muted">
                <span class="st NORMAL">정상</span>
                <span class="st WARN">주의</span>
                <span class="st CRITICAL">위험</span>
                <span class="st ERROR">오류</span>
                · 막대 높이 = 일자별 점검 실행 수(최대 ${maxRun}건 기준)
            </p>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h3>정기점검 이행률</h3>
    <c:set var="rate" value="${planCompliance.targetCnt > 0 ? (planCompliance.doneCnt*100/planCompliance.targetCnt) : 0}"/>
    <div class="bar"><span style="width:${rate}%">${rate}%</span></div>
    <p class="muted">대상 ${planCompliance.targetCnt}건 중 완료 ${planCompliance.doneCnt}건 · 지연(OVERDUE) 계획 ${planCompliance.overdueCnt}건</p>
</div>

<div class="card">
    <h3>분류별 점검 판정 현황 (최근 7일)</h3>
    <table class="tbl">
        <tr><th>분류</th><th>판정</th><th>건수</th></tr>
        <c:forEach var="row" items="${statusByCategory}">
            <tr>
                <td>${row.category}</td>
                <td><span class="st ${row.status}">${row.status}</span></td>
                <td>${row.cnt}</td>
            </tr>
        </c:forEach>
        <c:if test="${empty statusByCategory}"><tr><td colspan="3" class="muted">데이터 없음</td></tr></c:if>
    </table>
</div>

<div class="card">
    <h3>위험/주의 다발 서버 Top</h3>
    <table class="tbl">
        <tr><th>호스트</th><th>서비스</th><th>위험</th><th>주의</th></tr>
        <c:forEach var="r" items="${riskTop}">
            <tr><td>${r.hostname}</td><td>${r.serviceName}</td><td>${r.criticalCnt}</td><td>${r.warnCnt}</td></tr>
        </c:forEach>
        <c:if test="${empty riskTop}"><tr><td colspan="4" class="muted">이상 없음</td></tr></c:if>
    </table>
</div>

<div class="card">
    <h3>웹서비스(SVC) 가용성 현황 (24h)</h3>
    <table class="tbl">
        <tr><th>서비스</th><th>호스트</th><th>HTTP</th><th>판정</th><th>시각</th></tr>
        <c:forEach var="s" items="${svcAvailability}">
            <tr><td>${s.itemName}</td><td>${s.hostname}</td><td>${s.value}</td>
                <td><span class="st ${s.status}">${s.status}</span></td><td>${s.receivedAt}</td></tr>
        </c:forEach>
        <c:if test="${empty svcAvailability}"><tr><td colspan="5" class="muted">데이터 없음</td></tr></c:if>
    </table>
</div>

<div class="card">
    <h3>최근 점검 실행</h3>
    <table class="tbl">
        <tr><th>호스트</th><th>종합판정</th><th>항목</th><th>주의</th><th>위험</th><th>시각</th><th></th></tr>
        <c:forEach var="r" items="${recentRuns}">
            <tr>
                <td>${r.hostname}</td>
                <td><span class="st ${r.overallStatus}">${r.overallStatus}</span></td>
                <td>${r.itemCount}</td><td>${r.warnCount}</td><td>${r.criticalCount}</td>
                <td>${r.receivedAt}</td>
                <td><a class="btn" href="<c:url value='/pmc/inspection/detail.do'/>?runId=${r.runId}">상세</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty recentRuns}"><tr><td colspan="7" class="muted">데이터 없음</td></tr></c:if>
    </table>
</div>

<div class="card">
    <h3>데이터 품질</h3>
    <p>활성 룰 ${dqSummary.ruleCnt}건 · 최근 24h 위반(FAIL) ${dqSummary.failCnt}건
       <a class="btn" href="<c:url value='/pmc/admin/dq.do'/>">품질점검 현황</a></p>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
