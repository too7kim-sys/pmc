package kr.go.pmc.agent.collector.os;

import kr.go.pmc.agent.collector.AbstractCollector;
import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.platform.CommandRunner;
import kr.go.pmc.agent.platform.Platform;

import java.util.ArrayList;
import java.util.List;

/**
 * AIX OS 점검 수집기(best-effort). vmstat/df 기반. 일부 항목은 NA 가능.
 */
public class AixOsCollector extends AbstractCollector {

    @Override
    public Category category() {
        return Category.OS;
    }

    @Override
    public boolean supports(Platform p) {
        return p == Platform.AIX;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = new ArrayList<>();
        out.add(cpu(ctx));
        out.add(disk(ctx));
        return out;
    }

    private ResultItem cpu(CollectContext ctx) {
        // vmstat 마지막 줄: ... us sy id wa  → idle 로 사용률 산출
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "vmstat", "1", "2");
        if (!r.isSuccess()) {
            return na(Category.OS, "OS_CPU_USAGE", "CPU 사용률", "vmstat 미지원");
        }
        String[] lines = r.getStdout().trim().split("\\R");
        if (lines.length == 0) {
            return na(Category.OS, "OS_CPU_USAGE", "CPU 사용률", "vmstat 출력 없음");
        }
        String[] c = lines[lines.length - 1].trim().split("\\s+");
        // AIX vmstat: 마지막 4컬럼이 us sy id wa
        if (c.length >= 4) {
            double idle = parseDouble(c[c.length - 2], -1);
            if (idle >= 0) {
                return value(Category.OS, "OS_CPU_USAGE", "CPU 사용률", fmt(100 - idle, 1), "%");
            }
        }
        return na(Category.OS, "OS_CPU_USAGE", "CPU 사용률", "vmstat 파싱 실패");
    }

    private ResultItem disk(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "df", "-P");
        if (!r.isSuccess()) {
            return na(Category.OS, "OS_DISK_USAGE", "디스크 사용률", "df 미지원");
        }
        int max = -1;
        for (String line : r.getStdout().split("\\R")) {
            String[] c = line.trim().split("\\s+");
            if (c.length >= 5 && c[4].endsWith("%")) {
                int pct = (int) parseLong(c[4].replace("%", ""), -1);
                if (pct > max) max = pct;
            }
        }
        if (max < 0) return na(Category.OS, "OS_DISK_USAGE", "디스크 사용률", "df 파싱 실패");
        return value(Category.OS, "OS_DISK_USAGE", "디스크 사용률(최대)", String.valueOf(max), "%");
    }
}
