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
 * 윈도우 OS 점검 수집기. PowerShell Get-CimInstance 로 CPU/메모리/디스크 수집.
 */
public class WindowsOsCollector extends AbstractCollector {

    @Override
    public Category category() {
        return Category.OS;
    }

    @Override
    public boolean supports(Platform p) {
        return p == Platform.WINDOWS;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = new ArrayList<>();
        out.add(cpu(ctx));
        out.add(mem(ctx));
        out.add(disk(ctx));
        return out;
    }

    private ResultItem cpu(CollectContext ctx) {
        // 내부 하드코딩 PowerShell — 외부 입력 미사용
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(),
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average");
        if (!r.isSuccess()) {
            return error(Category.OS, "OS_CPU_USAGE", "CPU 사용률", "PowerShell 실패: " + r.getStderr());
        }
        return value(Category.OS, "OS_CPU_USAGE", "CPU 사용률", r.firstLine(), "%");
    }

    private ResultItem mem(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(),
                "powershell", "-NoProfile", "-Command",
                "$o=Get-CimInstance Win32_OperatingSystem; " +
                        "[math]::Round((($o.TotalVisibleMemorySize-$o.FreePhysicalMemory)/$o.TotalVisibleMemorySize)*100,1)");
        if (!r.isSuccess()) {
            return error(Category.OS, "OS_MEM_USAGE", "메모리 사용률", "PowerShell 실패: " + r.getStderr());
        }
        return value(Category.OS, "OS_MEM_USAGE", "메모리 사용률", r.firstLine(), "%");
    }

    private ResultItem disk(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(),
                "powershell", "-NoProfile", "-Command",
                "Get-CimInstance Win32_LogicalDisk -Filter \"DriveType=3\" | " +
                        "ForEach-Object { [math]::Round((($_.Size-$_.FreeSpace)/$_.Size)*100,0) } | " +
                        "Sort-Object -Descending | Select-Object -First 1");
        if (!r.isSuccess()) {
            return error(Category.OS, "OS_DISK_USAGE", "디스크 사용률", "PowerShell 실패: " + r.getStderr());
        }
        return value(Category.OS, "OS_DISK_USAGE", "디스크 사용률(최대)", r.firstLine(), "%");
    }
}
