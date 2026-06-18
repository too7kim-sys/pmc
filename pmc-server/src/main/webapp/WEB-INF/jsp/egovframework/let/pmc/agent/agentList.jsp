<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">대상 서버 / Agent 관리</h2>

<c:if test="${not empty issuedToken}">
    <div class="card" style="border-color:#27508f;">
        <b>등록 토큰이 발급되었습니다.</b> 대상 서버의 agent.yml <code>enrollToken</code>에 입력하세요:<br>
        <code style="font-size:15px;">${issuedToken}</code>
    </div>
</c:if>

<div class="card">
    <h3>대상 서버 등록</h3>
    <form method="post" action="<c:url value='/pmc/agent/registServer.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="text" name="hostname" placeholder="호스트명" required>
        <input type="text" name="ipAddr" placeholder="IP">
        <select name="osType">
            <option value="LINUX">LINUX</option><option value="WINDOWS">WINDOWS</option>
            <option value="AIX">AIX</option><option value="HPUX">HPUX</option><option value="SOLARIS">SOLARIS</option>
        </select>
        <input type="text" name="osVersion" placeholder="OS 버전">
        <input type="text" name="deptCode" placeholder="부서">
        <input type="text" name="serviceName" placeholder="서비스명">
        <button type="submit" class="btn">등록</button>
    </form>
</div>

<div class="card">
    <h3>대상 서버 목록</h3>
    <table class="tbl">
        <tr><th>ID</th><th>호스트</th><th>IP</th><th>OS</th><th>서비스</th><th>부서</th><th>Agent 토큰발급</th></tr>
        <c:forEach var="s" items="${servers}">
            <tr>
                <td>${s.serverId}</td><td>${s.hostname}</td><td>${s.ipAddr}</td>
                <td>${s.osType}</td><td>${s.serviceName}</td><td>${s.deptCode}</td>
                <td>
                    <form class="inline" method="post" action="<c:url value='/pmc/agent/issueToken.do'/>">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <input type="hidden" name="serverId" value="${s.serverId}"/>
                        <input type="number" name="policyId" placeholder="정책ID" value="1" style="width:80px;">
                        <button type="submit" class="btn gray">토큰발급</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty servers}"><tr><td colspan="7" class="muted">등록된 서버 없음</td></tr></c:if>
    </table>
</div>

<div class="card">
    <h3>Agent 목록</h3>
    <table class="tbl">
        <tr><th>호스트</th><th>상태</th><th>버전</th><th>정책</th><th>실행주기</th><th>마지막 Heartbeat</th><th>제어</th></tr>
        <c:forEach var="a" items="${agents}">
            <tr>
                <td>${a.hostname}</td>
                <td><span class="st ${a.status=='ACTIVE'?'NORMAL':'NA'}">${a.status}</span></td>
                <td>${a.agentVersion}</td><td>${a.policyId}</td><td>${a.scheduleCron}</td>
                <td>${a.lastHeartbeat}</td>
                <td><a class="btn" href="<c:url value='/pmc/agent/control.do'/>?agentId=${a.agentId}">원격제어</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty agents}"><tr><td colspan="7" class="muted">등록된 Agent 없음</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
