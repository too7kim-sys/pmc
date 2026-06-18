package kr.go.pmc.agent.collector.web;

import kr.go.pmc.agent.collector.AbstractCollector;
import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;
import kr.go.pmc.agent.platform.CommandRunner;
import kr.go.pmc.agent.platform.Platform;

import java.util.ArrayList;
import java.util.List;

/**
 * 웹서버(Apache httpd / Nginx) 점검 수집기(리눅스). 프로세스 생존/버전/리슨포트.
 * 미설치 시 NA.
 */
public class ApacheNginxCollector extends AbstractCollector {

    @Override
    public Category category() {
        return Category.WEB;
    }

    @Override
    public boolean supports(Platform p) {
        return p == Platform.LINUX;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = new ArrayList<>();
        boolean apache = procAlive(ctx, "httpd") || procAlive(ctx, "apache2");
        boolean nginx = procAlive(ctx, "nginx");

        if (!apache && !nginx) {
            out.add(na(Category.WEB, "WEB_PROC_ALIVE", "웹서버 프로세스", "Apache/Nginx 미설치 또는 미기동"));
            return out;
        }

        String server = apache ? "Apache" : "Nginx";
        ResultItem alive = value(Category.WEB, "WEB_PROC_ALIVE", "웹서버 프로세스", "UP", "");
        alive.setRaw("server=" + server);
        out.add(alive);

        // 버전
        String ver;
        if (apache) {
            ver = firstLine(ctx.runner(), ctx.timeoutMs(), "sh", "-c",
                    "(httpd -v 2>/dev/null || apache2 -v 2>/dev/null)");
        } else {
            CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "nginx", "-v");
            ver = r.getStderr().trim().isEmpty() ? r.firstLine() : r.getStderr().trim();
        }
        if (ver != null && !ver.isEmpty()) {
            out.add(value(Category.WEB, "WEB_VERSION", "웹서버 버전", ver, ""));
        }

        // 리슨 포트(80/443) — ss 우선
        out.add(listenPorts(ctx, server));

        // 에러로그 스캔(best-effort)
        out.add(errorLog(ctx, apache));
        return out;
    }

    private boolean procAlive(CollectContext ctx, String name) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "pgrep", "-x", name);
        return r.isSuccess() && !r.getStdout().trim().isEmpty();
    }

    private ResultItem listenPorts(CollectContext ctx, String server) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "ss", "-ltn");
        if (!r.isSuccess()) {
            r = ctx.runner().run(ctx.timeoutMs(), "netstat", "-ltn");
        }
        boolean has80 = false, has443 = false;
        if (r.isSuccess()) {
            for (String line : r.getStdout().split("\\R")) {
                if (line.matches(".*[:.]80\\b.*")) has80 = true;
                if (line.matches(".*[:.]443\\b.*")) has443 = true;
            }
        }
        ResultItem it = value(Category.WEB, "WEB_PORT_LISTEN", "웹서버 리슨포트",
                (has80 ? "80 " : "") + (has443 ? "443" : ""), "");
        it.setStatus(has80 || has443 ? ResultStatus.NORMAL : ResultStatus.WARN);
        if (!has80 && !has443) it.setError("80/443 리슨 포트 없음");
        return it;
    }

    private ResultItem errorLog(CollectContext ctx, boolean apache) {
        String path = apache ? "/var/log/apache2/error.log" : "/var/log/nginx/error.log";
        // 내부 하드코딩 경로만 사용(외부 입력 미사용)
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "sh", "-c",
                "test -r " + path + " && tail -n 200 " + path + " | grep -c -i error || echo 0");
        if (!r.isSuccess()) {
            return na(Category.WEB, "WEB_ERROR_LOG", "웹 에러로그", "에러로그 접근 불가");
        }
        int n = (int) parseLong(r.firstLine(), 0);
        ResultItem it = value(Category.WEB, "WEB_ERROR_LOG", "웹 에러로그(최근200줄 error)", String.valueOf(n), "ea");
        it.setStatus(n > 0 ? ResultStatus.WARN : ResultStatus.NORMAL);
        return it;
    }
}
