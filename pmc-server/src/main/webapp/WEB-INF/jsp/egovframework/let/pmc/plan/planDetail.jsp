<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">정기점검 계획 상세 — <c:out value="${plan.planName}"/></h2>

<div class="card">
    <p>주기: ${plan.cycle} · 예정일: ${plan.plannedDate} · 점검자: ${plan.inspectorId} · 결재자: ${plan.approverId}</p>
    <p>상태: <span class="st ${plan.status=='DONE'?'NORMAL':(plan.status=='OVERDUE'?'CRITICAL':'NA')}">${plan.status}</span>
       · 결재: <b>${plan.approveStatus}</b>
       <c:if test="${not empty plan.approveOpinion}"> (${plan.approveOpinion})</c:if></p>
    <p>이행률:
        <span class="bar" style="display:inline-block; width:200px; vertical-align:middle;"><span style="width:${plan.compliance}%">${plan.compliance}%</span></span>
        ${plan.doneCnt}/${plan.targetCnt}</p>

    <form class="inline" method="post" action="<c:url value='/pmc/plan/runAuto.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="planId" value="${plan.planId}"/>
        <button class="btn">자동 점검 일괄 실행(RUN_NOW)</button>
    </form>
    <form class="inline" method="post" action="<c:url value='/pmc/plan/requestApproval.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="planId" value="${plan.planId}"/>
        <button class="btn gray">결재 요청</button>
    </form>
    <form class="inline" method="post" action="<c:url value='/pmc/plan/approve.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="planId" value="${plan.planId}"/>
        <input type="text" name="opinion" placeholder="결재의견">
        <button class="btn">승인</button>
    </form>
    <form class="inline" method="post" action="<c:url value='/pmc/report/generatePlan.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="planId" value="${plan.planId}"/>
        <select name="type"><option>PDF</option><option>XLSX</option><option>CSV</option></select>
        <button class="btn">결과보고서 생성</button>
    </form>
</div>

<div class="card">
    <h3>대상 서버별 실적</h3>
    <table class="tbl">
        <tr><th>호스트</th><th>OS</th><th>서비스</th><th>실적</th><th>종합판정</th><th>완료일</th><th>연계/수동</th></tr>
        <c:forEach var="t" items="${plan.targets}">
            <tr>
                <td><c:out value="${t.hostname}"/></td><td><c:out value="${t.osType}"/></td><td><c:out value="${t.serviceName}"/></td>
                <td><span class="st ${t.resultStatus=='DONE'?'NORMAL':'NA'}">${t.resultStatus}</span></td>
                <td><c:if test="${not empty t.overallStatus}"><span class="st ${t.overallStatus}">${t.overallStatus}</span></c:if></td>
                <td>${t.doneDt}</td>
                <td>
                    <form class="inline" method="post" action="<c:url value='/pmc/plan/link.do'/>">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <input type="hidden" name="planId" value="${plan.planId}"/>
                        <input type="hidden" name="serverId" value="${t.serverId}"/>
                        <button class="btn gray" title="해당 서버의 최근 점검을 실적으로 연계">최근점검 연계</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty plan.targets}"><tr><td colspan="7" class="muted">대상 없음</td></tr></c:if>
    </table>
</div>

<div class="card">
    <h3>수동 점검 결과 입력 (MANUAL 항목)</h3>
    <form method="post" action="<c:url value='/pmc/plan/manualInput.do'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="planId" value="${plan.planId}"/>
        대상 서버:
        <select name="serverId">
            <c:forEach var="t" items="${plan.targets}"><option value="${t.serverId}">${t.hostname}</option></c:forEach>
        </select>
        <table class="tbl" style="margin-top:8px;">
            <tr><th>분류</th><th>항목코드</th><th>항목명</th><th>값</th><th>판정</th></tr>
            <c:forEach var="n" begin="0" end="2">
                <tr>
                    <td><input type="text" name="category" value="DB"></td>
                    <td><input type="text" name="itemCode" placeholder="DB_BACKUP_STATUS"></td>
                    <td><input type="text" name="itemName" placeholder="백업 정상 여부"></td>
                    <td><input type="text" name="value" placeholder="정상"></td>
                    <td><select name="status"><option>NORMAL</option><option>WARN</option><option>CRITICAL</option><option>NA</option></select></td>
                </tr>
            </c:forEach>
        </table>
        <button class="btn" type="submit">수동 결과 저장</button>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
