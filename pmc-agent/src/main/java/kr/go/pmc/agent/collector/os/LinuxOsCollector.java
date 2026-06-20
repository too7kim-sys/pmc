package kr.go.pmc.agent.collector.os;

import kr.go.pmc.agent.collector.AbstractCollector;
import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;
import kr.go.pmc.agent.platform.CommandRunner;
import kr.go.pmc.agent.platform.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 리눅스 OS 점검 수집기. /proc 우선, 명령(df/who/ps/uname 등) 보조.
 */
public class LinuxOsCollector extends AbstractCollector {

    @Override
    public Category category() {
        return Category.OS;
    }

    @Override
    public boolean supports(Platform p) {
        return p == Platform.LINUX;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = new ArrayList<>();
        addSafe(out, () -> cpuUsage(ctx));
        addSafe(out, () -> memory(ctx));
        addSafe(out, () -> swap(ctx));
        addSafe(out, () -> disk(ctx));
        addSafe(out, () -> inode(ctx));
        addSafe(out, () -> loadAvg(ctx));
        addSafe(out, () -> uptime(ctx));
        addSafe(out, () -> zombies(ctx));
        addSafe(out, () -> ntp(ctx));
        addSafe(out, () -> kernel(ctx));
        addSafe(out, () -> loginUsers(ctx));
        addSafe(out, () -> syslogErrors(ctx));
        return out;
    }

    private interface Producer {
        List<ResultItem> get() throws Exception;
    }

    /** 단일 점검 실패가 전체 run 을 깨지 않도록 보호. */
    private void addSafe(List<ResultItem> out, Producer p) {
        try {
            List<ResultItem> items = p.get();
            if (items != null) out.addAll(items);
        } catch (Exception e) {
            out.add(error(Category.OS, "OS_COLLECT", "OS 수집", "수집 오류: " + e.getMessage()));
        }
    }

    private List<ResultItem> one(ResultItem i) {
        List<ResultItem> l = new ArrayList<>(1);
        l.add(i);
        return l;
    }

    // CPU 사용률 — /proc/stat 두 스냅샷 차분
    private List<ResultItem> cpuUsage(CollectContext ctx) throws IOException, InterruptedException {
        long[] s1 = readCpuStat();
        Thread.sleep(200);
        long[] s2 = readCpuStat();
        if (s1 == null || s2 == null) {
            return one(error(Category.OS, "OS_CPU_USAGE", "CPU 사용률", "/proc/stat 읽기 실패"));
        }
        long idle1 = s1[3] + s1[4];
        long idle2 = s2[3] + s2[4];
        long total1 = sum(s1);
        long total2 = sum(s2);
        long totalDiff = total2 - total1;
        long idleDiff = idle2 - idle1;
        double usage = totalDiff <= 0 ? 0.0 : (100.0 * (totalDiff - idleDiff) / totalDiff);
        return one(value(Category.OS, "OS_CPU_USAGE", "CPU 사용률", fmt(usage, 1), "%"));
    }

    private long[] readCpuStat() throws IOException {
        for (String line : Files.readAllLines(Paths.get("/proc/stat"), StandardCharsets.UTF_8)) {
            if (line.startsWith("cpu ")) {
                String[] p = line.trim().split("\\s+");
                // user nice system idle iowait irq softirq steal ...
                long[] v = new long[Math.max(5, p.length - 1)];
                for (int i = 1; i < p.length; i++) {
                    v[i - 1] = parseLong(p[i], 0);
                }
                return v;
            }
        }
        return null;
    }

    private long sum(long[] a) {
        long s = 0;
        for (long v : a) s += v;
        return s;
    }

    // 메모리 — /proc/meminfo
    private List<ResultItem> memory(CollectContext ctx) throws IOException {
        long total = meminfo("MemTotal");
        long avail = meminfo("MemAvailable");
        if (total <= 0) {
            return one(error(Category.OS, "OS_MEM_USAGE", "메모리 사용률", "/proc/meminfo 읽기 실패"));
        }
        long used = total - (avail > 0 ? avail : meminfo("MemFree"));
        double pct = 100.0 * used / total;
        ResultItem it = value(Category.OS, "OS_MEM_USAGE", "메모리 사용률", fmt(pct, 1), "%");
        it.setRaw("totalKB=" + total + ",usedKB=" + used);
        return one(it);
    }

    // 스왑 — /proc/meminfo
    private List<ResultItem> swap(CollectContext ctx) throws IOException {
        long total = meminfo("SwapTotal");
        long free = meminfo("SwapFree");
        if (total <= 0) {
            ResultItem it = value(Category.OS, "OS_SWAP_USAGE", "스왑 사용률", "0", "%");
            it.setRaw("swap 미구성");
            return one(it);
        }
        double pct = 100.0 * (total - free) / total;
        ResultItem it = value(Category.OS, "OS_SWAP_USAGE", "스왑 사용률", fmt(pct, 1), "%");
        it.setRaw("totalKB=" + total);
        return one(it);
    }

    private long meminfo(String key) throws IOException {
        Path p = Paths.get("/proc/meminfo");
        if (!Files.isReadable(p)) return -1;
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (line.startsWith(key + ":")) {
                String num = line.replaceAll("[^0-9]", "");
                return parseLong(num, -1);
            }
        }
        return -1;
    }

    // 디스크 사용률 — df -P (최대 사용률 파티션)
    private List<ResultItem> disk(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "df", "-P");
        if (!r.isSuccess()) {
            return one(error(Category.OS, "OS_DISK_USAGE", "디스크 사용률", "df 실행 실패: " + r.getStderr()));
        }
        Usage u = parseMaxPercent(r.getStdout());
        if (u == null) {
            return one(error(Category.OS, "OS_DISK_USAGE", "디스크 사용률", "df 파싱 실패"));
        }
        ResultItem it = value(Category.OS, "OS_DISK_USAGE", "디스크 사용률", String.valueOf(u.pct), "%");
        it.setRaw("mount=" + u.mount);
        return one(it);
    }

    /** 사용률(%)/마운트 보관. */
    private static final class Usage {
        final int pct; final String mount;
        Usage(int pct, String mount) { this.pct = pct; this.mount = mount; }
    }

    /**
     * df 류 출력에서 최대 사용률(%)과 해당 마운트를 추출.
     * 고정 컬럼 인덱스 대신 'NN%' 토큰을 직접 찾고 마운트는 마지막 토큰으로 사용 →
     * 긴 디바이스명이 줄바꿈(wrap)되어 컬럼 수가 달라져도 견고하게 동작.
     */
    private Usage parseMaxPercent(String stdout) {
        int maxPct = -1;
        String maxMount = "";
        String[] lines = stdout.split("\\R");
        for (int i = 1; i < lines.length; i++) {
            String[] c = lines[i].trim().split("\\s+");
            if (c.length < 2) continue;            // 디바이스명만 있는 wrap 라인은 건너뜀
            int pct = -1;
            for (String tok : c) {
                if (tok.endsWith("%")) {
                    pct = (int) parseLong(tok.substring(0, tok.length() - 1), -1);
                    break;
                }
            }
            if (pct > maxPct) {
                maxPct = pct;
                maxMount = c[c.length - 1];        // 마운트 지점은 마지막 토큰
            }
        }
        return maxPct < 0 ? null : new Usage(maxPct, maxMount);
    }

    // inode 사용률 — df -i
    private List<ResultItem> inode(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "df", "-i");
        if (!r.isSuccess()) {
            return one(na(Category.OS, "OS_INODE_USAGE", "inode 사용률", "df -i 미지원"));
        }
        Usage u = parseMaxPercent(r.getStdout());
        if (u == null) {
            return one(na(Category.OS, "OS_INODE_USAGE", "inode 사용률", "inode 정보 없음"));
        }
        ResultItem it = value(Category.OS, "OS_INODE_USAGE", "inode 사용률", String.valueOf(u.pct), "%");
        it.setRaw("mount=" + u.mount);
        return one(it);
    }

    // 로드 애버리지 — /proc/loadavg
    private List<ResultItem> loadAvg(CollectContext ctx) throws IOException {
        Path p = Paths.get("/proc/loadavg");
        if (!Files.isReadable(p)) {
            return one(error(Category.OS, "OS_LOAD_AVG", "Load Average", "/proc/loadavg 없음"));
        }
        String line = new String(Files.readAllBytes(p), StandardCharsets.UTF_8).trim();
        String[] c = line.split("\\s+");
        ResultItem it = value(Category.OS, "OS_LOAD_AVG", "Load Average(1m)", c.length > 0 ? c[0] : "0", "");
        if (c.length >= 3) it.setRaw("5m=" + c[1] + ",15m=" + c[2]);
        return one(it);
    }

    // 업타임 — /proc/uptime
    private List<ResultItem> uptime(CollectContext ctx) throws IOException {
        Path p = Paths.get("/proc/uptime");
        if (!Files.isReadable(p)) {
            return one(na(Category.OS, "OS_UPTIME", "업타임", "/proc/uptime 없음"));
        }
        String line = new String(Files.readAllBytes(p), StandardCharsets.UTF_8).trim();
        double secs = parseDouble(line.split("\\s+")[0], 0);
        long days = (long) (secs / 86400);
        ResultItem it = value(Category.OS, "OS_UPTIME", "업타임", String.valueOf(days), "days");
        it.setStatus(ResultStatus.NORMAL);
        return one(it);
    }

    // 좀비 프로세스 — ps
    private List<ResultItem> zombies(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "ps", "-eo", "stat");
        if (!r.isSuccess()) {
            return one(na(Category.OS, "OS_ZOMBIE_PROC", "좀비 프로세스", "ps 실행 실패"));
        }
        int z = 0;
        for (String line : r.getStdout().split("\\R")) {
            String s = line.trim();
            if (s.startsWith("Z")) z++;
        }
        return one(value(Category.OS, "OS_ZOMBIE_PROC", "좀비 프로세스 수", String.valueOf(z), "ea"));
    }

    // NTP 동기화 — timedatectl
    private List<ResultItem> ntp(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "timedatectl", "show",
                "-p", "NTPSynchronized", "--value");
        if (!r.isSuccess()) {
            return one(na(Category.OS, "OS_NTP_SYNC", "NTP 동기화", "timedatectl 미지원"));
        }
        String v = r.firstLine().trim();
        boolean sync = "yes".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v);
        ResultItem it = value(Category.OS, "OS_NTP_SYNC", "NTP 동기화", sync ? "SYNCED" : "UNSYNCED", "");
        it.setStatus(sync ? ResultStatus.NORMAL : ResultStatus.WARN);
        return one(it);
    }

    // 커널/패치 버전 — uname -r
    private List<ResultItem> kernel(CollectContext ctx) {
        String v = firstLine(ctx.runner(), ctx.timeoutMs(), "uname", "-r");
        if (v.isEmpty()) {
            return one(na(Category.OS, "OS_KERNEL_VER", "커널 버전", "uname 실행 실패"));
        }
        return one(value(Category.OS, "OS_KERNEL_VER", "커널 버전", v, ""));
    }

    // 로그인 사용자 수 — who
    private List<ResultItem> loginUsers(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "who");
        if (!r.isSuccess()) {
            return one(na(Category.OS, "OS_LOGIN_USERS", "로그인 사용자", "who 실행 실패"));
        }
        int n = 0;
        for (String line : r.getStdout().split("\\R")) {
            if (!line.trim().isEmpty()) n++;
        }
        return one(value(Category.OS, "OS_LOGIN_USERS", "로그인 사용자 수", String.valueOf(n), "ea"));
    }

    // syslog 에러(best-effort) — journalctl 우선
    private List<ResultItem> syslogErrors(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(),
                "journalctl", "-p", "err", "--since", "-1h", "--no-pager", "-q");
        if (!r.isSuccess()) {
            return one(na(Category.OS, "OS_SYSLOG_ERR", "시스템 로그 에러", "journalctl 미지원"));
        }
        int n = 0;
        for (String line : r.getStdout().split("\\R")) {
            if (!line.trim().isEmpty()) n++;
        }
        ResultItem it = value(Category.OS, "OS_SYSLOG_ERR", "최근1시간 에러로그 수", String.valueOf(n), "ea");
        it.setStatus(n > 0 ? ResultStatus.WARN : ResultStatus.NORMAL);
        return one(it);
    }
}
