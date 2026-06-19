package kr.go.pmc.agent.collector.was;

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
 * WAS(Tomcat/JBoss/WebLogic/JEUS) 점검 수집기(리눅스). 프로세스 탐지 + 리슨포트.
 * 어떤 WAS 도 없으면 NA.
 */
public class WasCollector extends AbstractCollector {

    private static final String[][] WAS_SIGS = {
            {"Tomcat", "catalina"},
            {"JBoss", "jboss"},
            {"WildFly", "wildfly"},
            {"WebLogic", "weblogic"},
            {"JEUS", "jeus"},
    };

    @Override
    public Category category() {
        return Category.WAS;
    }

    @Override
    public boolean supports(Platform p) {
        return p == Platform.LINUX;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = new ArrayList<>();
        String detected = null;
        for (String[] sig : WAS_SIGS) {
            if (procContains(ctx, sig[1])) {
                detected = sig[0];
                break;
            }
        }
        if (detected == null) {
            out.add(na(Category.WAS, "WAS_PROC_ALIVE", "WAS 프로세스", "탐지된 WAS 없음"));
            return out;
        }
        ResultItem alive = value(Category.WAS, "WAS_PROC_ALIVE", "WAS 프로세스", "UP", "");
        alive.setRaw("was=" + detected);
        out.add(alive);

        // 리슨 포트 수
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "ss", "-ltn");
        if (!r.isSuccess()) {
            r = ctx.runner().run(ctx.timeoutMs(), "netstat", "-ltn");
        }
        int count = 0;
        if (r.isSuccess()) {
            for (String line : r.getStdout().split("\\R")) {
                if (line.contains("LISTEN")) count++;
            }
        }
        ResultItem port = value(Category.WAS, "WAS_PORT_LISTEN", "WAS 리슨포트 수", String.valueOf(count), "ea");
        port.setStatus(count > 0 ? ResultStatus.NORMAL : ResultStatus.WARN);
        out.add(port);
        return out;
    }
}
