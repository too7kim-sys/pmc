<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">정책 상세 — ${policy.policyName} (v${policy.version})</h2>

<div class="card">
    <h3>점검 항목 (행정안전부 매뉴얼 준거) — 총 ${fn:length(policy.items)}건</h3>
    <table class="tbl">
        <tr><th>분류</th><th>중분류</th><th>항목코드</th><th>항목명</th><th>단위</th><th>주기</th><th>유형</th><th>판정기준</th><th>임계치(주의/위험)</th></tr>
        <c:forEach var="i" items="${policy.items}">
            <tr>
                <td>${i.category}</td><td>${i.midCategory}</td><td><code>${i.itemCode}</code></td>
                <td>${i.itemName}</td><td>${i.unit}</td><td>${i.checkCycle}</td>
                <td><span class="st ${i.checkType=='AUTO'?'NORMAL':'NA'}">${i.checkType}</span></td>
                <td>${i.judgeCriteria}</td>
                <td>
                    <c:forEach var="t" items="${i.thresholds}">${t.level}:${t.operator} ${t.compareValue} </c:forEach>
                </td>
            </tr>
        </c:forEach>
    </table>
</div>

<div class="card">
    <h3>웹서비스(SVC) 점검 대상 URL</h3>
    <table class="tbl">
        <tr><th>서비스</th><th>URL</th><th>기대코드</th><th>본문검증</th><th>타임아웃</th><th>SSL</th><th></th></tr>
        <c:forEach var="s" items="${policy.svcTargets}">
            <tr>
                <td>${s.svcName}</td><td>${s.url}</td><td>${s.expectedStatus}</td>
                <td>${s.expectedContent}</td><td>${s.timeoutMs}ms</td><td>${s.sslCheckYn}</td>
                <td>
                    <form class="inline" method="post" action="<c:url value='/pmc/policy/svcTargetDelete.do'/>">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <input type="hidden" name="svcTargetId" value="${s.svcTargetId}"/>
                        <input type="hidden" name="policyId" value="${policy.policyId}"/>
                        <button class="btn red">삭제</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
    </table>
    <form method="post" action="<c:url value='/pmc/policy/svcTarget.do'/>" style="margin-top:10px;">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="policyId" value="${policy.policyId}"/>
        <input type="text" name="svcName" placeholder="서비스명" required>
        <input type="text" name="url" placeholder="http://..." style="width:280px;" required>
        <input type="number" name="expectedStatus" placeholder="200" value="200" style="width:80px;">
        <input type="text" name="expectedContent" placeholder="본문 키워드">
        <button class="btn">대상 추가</button>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
