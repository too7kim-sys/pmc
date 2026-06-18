package kr.go.pmc.agent.collector.sw;

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
 * 소프트웨어/보안 점검 수집기(리눅스). 설치 패키지 수, 보안에이전트(백신) 프로세스.
 */
public class SwCollector extends AbstractCollector {

    private static final String[] SECURITY_PROCS = {
            "clamd", "freshclam", "ds_agent", "AhnLab", "v3", "TmEntity", "falcon-sensor", "ossec"
    };

    @Override
    public Category category() {
        return Category.SW;
    }

    @Override
    public boolean supports(Platform p) {
        return p == Platform.LINUX;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = new ArrayList<>();
        out.add(packageCount(ctx));
        out.add(securityAgent(ctx));
        return out;
    }

    private ResultItem packageCount(CollectContext ctx) {
        // dpkg 우선, 없으면 rpm — 내부 하드코딩 명령
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "sh", "-c",
                "dpkg -l 2>/dev/null | grep -c '^ii' || rpm -qa 2>/dev/null | wc -l");
        if (!r.isSuccess()) {
            return na(Category.SW, "SW_PKG_COUNT", "설치 패키지 수", "dpkg/rpm 미지원");
        }
        String n = r.firstLine();
        if (n.isEmpty() || "0".equals(n)) {
            return na(Category.SW, "SW_PKG_COUNT", "설치 패키지 수", "패키지 관리자 없음");
        }
        return value(Category.SW, "SW_PKG_COUNT", "설치 패키지 수", n, "ea");
    }

    private ResultItem securityAgent(CollectContext ctx) {
        for (String proc : SECURITY_PROCS) {
            CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "pgrep", "-f", proc);
            if (r.isSuccess() && !r.getStdout().trim().isEmpty()) {
                ResultItem it = value(Category.SW, "SW_SECURITY_AGENT", "보안 에이전트", "RUNNING", "");
                it.setRaw("proc=" + proc);
                return it;
            }
        }
        ResultItem it = value(Category.SW, "SW_SECURITY_AGENT", "보안 에이전트", "NOT_FOUND", "");
        it.setStatus(ResultStatus.WARN);
        it.setError("알려진 보안 에이전트 프로세스 미탐지");
        return it;
    }
}
