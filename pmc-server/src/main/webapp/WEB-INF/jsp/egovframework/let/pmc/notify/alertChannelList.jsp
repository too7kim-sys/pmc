<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">알림 채널 관리</h2>

<div class="card">
    <a class="btn gray" href="<c:url value='/pmc/alert/list.do'/>">알림이력</a>
    <span class="muted">마스터 스위치는 globals.properties 의 Globals.AlertEnabled 입니다. 채널이 없으면 Globals.AlertWebhookUrl 로 폴백합니다.</span>
</div>

<div class="card">
    <h3>채널 추가</h3>
    <form method="post" action="<c:url value='/pmc/admin/alertChannel/regist.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        이름 <input type="text" name="name" required>
        URL <input type="text" name="url" placeholder="https://hooks.slack.com/..." style="width:300px;" required>
        임계
        <select name="minSeverity">
            <option value="CRITICAL">위험만(CRITICAL)</option>
            <option value="WARN">주의 이상(WARN)</option>
        </select>
        유형(CSV, 비우면 전체)
        <input type="text" name="alertTypes" placeholder="RUN_CRITICAL,SVC_FAIL,HB_STALE">
        <button class="btn" type="submit">추가</button>
    </form>
</div>

<div class="card">
    <table class="tbl">
        <tr><th>ID</th><th>이름</th><th>URL</th><th>임계</th><th>유형</th><th>사용</th><th>관리</th></tr>
        <c:forEach var="c" items="${channels}">
            <tr>
                <td>${c.channelId}</td>
                <td><c:out value="${c.name}"/></td>
                <td class="muted"><c:out value="${c.url}"/></td>
                <td><span class="st ${c.minSeverity}">${c.minSeverity}</span></td>
                <td><c:out value="${empty c.alertTypes ? '전체' : c.alertTypes}"/></td>
                <td><span class="st ${c.enabled == 'Y' ? 'NORMAL' : 'NA'}">${c.enabled == 'Y' ? '사용' : '중지'}</span></td>
                <td>
                    <form class="inline" method="post" action="<c:url value='/pmc/admin/alertChannel/toggle.do'/>">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <input type="hidden" name="channelId" value="${c.channelId}"/>
                        <input type="hidden" name="enabled" value="${c.enabled == 'Y' ? 'N' : 'Y'}"/>
                        <button class="btn gray">${c.enabled == 'Y' ? '중지' : '사용'}</button>
                    </form>
                    <form class="inline" method="post" action="<c:url value='/pmc/admin/alertChannel/delete.do'/>">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <input type="hidden" name="channelId" value="${c.channelId}"/>
                        <button class="btn red">삭제</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty channels}"><tr><td colspan="7" class="muted">등록된 채널이 없습니다(globals 폴백 사용).</td></tr></c:if>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
