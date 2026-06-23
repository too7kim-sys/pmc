<%@ include file="/WEB-INF/jsp/inc/header.jspf" %>
<h2 class="page">실시간 시스템 상태 모니터링</h2>

<div class="card">
    <a class="btn" href="<c:url value='/pmc/monitoring/realtime.do'/>">실시간 상태</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/svc.do'/>">웹서비스(URL)</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/daily.do'/>">일자별 점검</a>
    <a class="btn gray" href="<c:url value='/pmc/monitoring/capacity.do'/>">용량점검</a>
    <a class="btn gray" href="<c:url value='/pmc/alert/list.do'/>">알림이력</a>
</div>

<div class="card">
    <span id="rt-conn" class="st NA">연결 중…</span>
    <span class="muted">5초 주기 자동 갱신(SSE) · 최종 수신: <b id="rt-ts">-</b></span>
</div>

<div class="grid">
    <div class="kpi"><div class="num" id="kpi-run">${snapshot.counters.runToday}</div><div class="lbl">오늘 점검 실행</div></div>
    <div class="kpi"><div class="num" id="kpi-crit" style="color:#c0392b">${snapshot.counters.criticalToday}</div><div class="lbl">위험 항목(24h)</div></div>
    <div class="kpi"><div class="num" id="kpi-warn" style="color:#f39c12">${snapshot.counters.warnToday}</div><div class="lbl">주의 항목(24h)</div></div>
</div>

<div class="card">
    <h3>서버별 최신 상태</h3>
    <table class="tbl">
        <thead><tr><th>호스트</th><th>서비스</th><th>최신 판정</th><th>최근 점검</th><th>Agent</th><th>Heartbeat</th><th>경과(분)</th></tr></thead>
        <tbody id="rt-body">
        <c:forEach var="s" items="${snapshot.servers}">
            <tr>
                <td><c:out value="${s.hostname}"/></td>
                <td><c:out value="${s.serviceName}"/></td>
                <td><span class="st ${s.overallStatus}">${s.overallStatus}</span></td>
                <td>${s.lastRun}</td>
                <td>${s.agentStatus}</td>
                <td>${s.lastHeartbeat}</td>
                <td>${s.hbAgeMin}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

<script>
(function () {
    var STREAM = '<c:url value="/pmc/monitoring/stream"/>';
    var conn = document.getElementById('rt-conn');

    function esc(v) {
        if (v === null || v === undefined) return '';
        return String(v).replace(/[&<>"']/g, function (c) {
            return { '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;' }[c];
        });
    }
    function badge(st) {
        var ok = ['NORMAL','WARN','CRITICAL','ERROR','NA'];
        var cls = ok.indexOf(st) >= 0 ? st : 'NA';
        return '<span class="st ' + cls + '">' + esc(st || 'NA') + '</span>';
    }
    function render(snap) {
        var c = snap.counters || {};
        document.getElementById('kpi-run').textContent  = c.runToday || 0;
        document.getElementById('kpi-crit').textContent = c.criticalToday || 0;
        document.getElementById('kpi-warn').textContent = c.warnToday || 0;
        document.getElementById('rt-ts').textContent = snap.ts || '-';
        var rows = (snap.servers || []).map(function (s) {
            var stale = (s.hbAgeMin !== null && s.hbAgeMin !== undefined && s.hbAgeMin >= 1440);
            return '<tr>' +
                '<td>' + esc(s.hostname) + '</td>' +
                '<td>' + esc(s.serviceName) + '</td>' +
                '<td>' + badge(s.overallStatus) + '</td>' +
                '<td>' + esc(s.lastRun) + '</td>' +
                '<td>' + esc(s.agentStatus) + '</td>' +
                '<td>' + esc(s.lastHeartbeat) + '</td>' +
                '<td' + (stale ? ' style="color:#c0392b;font-weight:bold"' : '') + '>' + esc(s.hbAgeMin) + '</td>' +
                '</tr>';
        }).join('');
        document.getElementById('rt-body').innerHTML = rows ||
            '<tr><td colspan="7" class="muted">데이터 없음</td></tr>';
    }

    if (!window.EventSource) {
        conn.className = 'st WARN'; conn.textContent = 'SSE 미지원 브라우저';
        return;
    }
    var es = new EventSource(STREAM);
    es.addEventListener('open', function () { conn.className = 'st NORMAL'; conn.textContent = '실시간 연결됨'; });
    es.addEventListener('snapshot', function (e) {
        try { render(JSON.parse(e.data)); } catch (x) {}
    });
    es.addEventListener('error', function () { conn.className = 'st CRITICAL'; conn.textContent = '연결 끊김(재시도 중)'; });
})();
</script>

<%@ include file="/WEB-INF/jsp/inc/footer.jspf" %>
