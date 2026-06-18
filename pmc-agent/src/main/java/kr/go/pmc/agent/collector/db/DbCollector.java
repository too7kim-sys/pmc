package kr.go.pmc.agent.collector.db;

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
 * DB(PostgreSQL/Oracle/MySQL) 점검 수집기(리눅스). 프로세스 탐지 + 리스너 포트.
 * 어떤 DB 도 없으면 NA.
 */
public class DbCollector extends AbstractCollector {

    private static final String[][] DB_SIGS = {
            {"PostgreSQL", "postgres", "5432"},
            {"Oracle", "ora_pmon", "1521"},
            {"MySQL", "mysqld", "3306"},
            {"MariaDB", "mariadbd", "3306"},
    };

    @Override
    public Category category() {
        return Category.DB;
    }

    @Override
    public boolean supports(Platform p) {
        return p == Platform.LINUX;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = new ArrayList<>();
        String detected = null;
        String port = null;
        for (String[] sig : DB_SIGS) {
            if (procContains(ctx, sig[1])) {
                detected = sig[0];
                port = sig[2];
                break;
            }
        }
        if (detected == null) {
            out.add(na(Category.DB, "DB_PROC_ALIVE", "DB 프로세스", "탐지된 DB 없음"));
            return out;
        }
        ResultItem alive = value(Category.DB, "DB_PROC_ALIVE", "DB 프로세스", "UP", "");
        alive.setRaw("db=" + detected);
        out.add(alive);

        // 리스너 포트 확인
        boolean listening = portListening(ctx, port);
        ResultItem li = value(Category.DB, "DB_PORT_LISTEN", "DB 리스너 포트",
                listening ? port : "DOWN", "");
        li.setStatus(listening ? ResultStatus.NORMAL : ResultStatus.CRITICAL);
        if (!listening) li.setError(port + " 포트 미리슨");
        out.add(li);
        return out;
    }

    private boolean procContains(CollectContext ctx, String needle) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "pgrep", "-f", needle);
        return r.isSuccess() && !r.getStdout().trim().isEmpty();
    }

    private boolean portListening(CollectContext ctx, String port) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "ss", "-ltn");
        if (!r.isSuccess()) {
            r = ctx.runner().run(ctx.timeoutMs(), "netstat", "-ltn");
        }
        if (!r.isSuccess()) return false;
        for (String line : r.getStdout().split("\\R")) {
            if (line.matches(".*[:.]" + port + "\\b.*")) return true;
        }
        return false;
    }
}
