<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">Agent 원격제어 — <c:out value="${agent.hostname}"/></h2>

<div class="card">
    <p>상태: <span class="st ${agent.status=='ACTIVE'?'NORMAL':'NA'}">${agent.status}</span>
       · 정책: ${agent.policyId} · 현재 실행주기: <b>${agent.scheduleCron}</b>
       · 마지막 Heartbeat: ${agent.lastHeartbeat}</p>
    <p class="muted">명령은 큐에 적재되어 Agent가 heartbeat 시 수령·실행 후 결과를 회신합니다.</p>
</div>

<div class="card">
    <h3>명령 발행</h3>
    <table class="tbl">
        <tr>
            <td>
                <form method="post" action="<c:url value='/pmc/agent/command.do'/>">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input type="hidden" name="agentId" value="${agent.agentId}"/>
                    <b>추가 점검(RUN_NOW)</b><br>
                    분류(JSON): <input type="text" name="params" value='{"categories":["OS","NW","SVC"]}' style="width:280px;">
                    <input type="hidden" name="commandType" value="RUN_NOW"/>
                    <button class="btn">즉시 실행</button>
                </form>
            </td>
            <td>
                <form method="post" action="<c:url value='/pmc/agent/command.do'/>">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input type="hidden" name="agentId" value="${agent.agentId}"/>
                    <b>실행주기 설정(SET_SCHEDULE)</b><br>
                    cron/interval: <input type="text" name="params" value='{"cron":"0 0 6 * * *"}' style="width:220px;">
                    <input type="hidden" name="commandType" value="SET_SCHEDULE"/>
                    <button class="btn">주기 적용</button>
                </form>
            </td>
        </tr>
        <tr>
            <td>
                <form class="inline" method="post" action="<c:url value='/pmc/agent/command.do'/>">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input type="hidden" name="agentId" value="${agent.agentId}"/>
                    <input type="hidden" name="commandType" value="START"/>
                    <b>실행(START)</b> 스케줄러 가동 <button class="btn">실행</button>
                </form>
            </td>
            <td>
                <form class="inline" method="post" action="<c:url value='/pmc/agent/command.do'/>">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input type="hidden" name="agentId" value="${agent.agentId}"/>
                    <input type="hidden" name="commandType" value="STOP"/>
                    <b>종료(STOP)</b> 스케줄러 중지 <button class="btn red">종료</button>
                </form>
            </td>
        </tr>
        <tr>
            <td colspan="2">
                <form class="inline" method="post" action="<c:url value='/pmc/agent/command.do'/>">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input type="hidden" name="agentId" value="${agent.agentId}"/>
                    <input type="hidden" name="commandType" value="UPDATE_POLICY"/>
                    <b>정책 갱신(UPDATE_POLICY)</b> <button class="btn gray">정책 강제 반영</button>
                </form>
            </td>
        </tr>
    </table>
</div>

<div class="card">
    <h3>명령 이력</h3>
    <table class="tbl">
        <tr><th>ID</th><th>유형</th><th>파라미터</th><th>상태</th><th>결과</th><th>요청</th><th>요청시각</th><th>완료시각</th></tr>
        <c:forEach var="c" items="${commands}">
            <tr>
                <td>${c.commandId}</td><td><c:out value="${c.commandType}"/></td><td><code><c:out value="${c.params}"/></code></td>
                <td><span class="st ${c.status=='DONE'?'NORMAL':(c.status=='FAILED'?'CRITICAL':'NA')}"><c:out value="${c.status}"/></span></td>
                <td><c:out value="${c.resultMsg}"/></td><td><c:out value="${c.requestedBy}"/></td><td>${c.requestedAt}</td><td>${c.completedAt}</td>
            </tr>
        </c:forEach>
        <c:if test="${empty commands}"><tr><td colspan="8" class="muted">명령 없음</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
