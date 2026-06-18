package kr.go.pmc.agent.collector.nw;

import kr.go.pmc.agent.collector.AbstractCollector;
import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;
import kr.go.pmc.agent.platform.CommandRunner;
import kr.go.pmc.agent.platform.Platform;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 네트워크 점검 수집기(리눅스). 인터페이스/기본경로/리슨포트수/방화벽/DNS/연결성/본딩.
 */
public class NwCollector extends AbstractCollector {

    @Override
    public Category category() {
        return Category.NW;
    }

    @Override
    public boolean supports(Platform p) {
        return p == Platform.LINUX;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = new ArrayList<>();
        out.add(ifaceStatus(ctx));
        out.add(defaultRoute(ctx));
        out.add(listenPortCount(ctx));
        out.add(firewall(ctx));
        out.add(dnsResolve(ctx));
        out.add(connectivity(ctx));
        out.add(bonding(ctx));
        return out;
    }

    private ResultItem ifaceStatus(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "ip", "-br", "link");
        if (!r.isSuccess()) {
            r = ctx.runner().run(ctx.timeoutMs(), "ifconfig", "-a");
        }
        if (!r.isSuccess()) {
            return na(Category.NW, "NW_IFACE_STATUS", "인터페이스 상태", "ip/ifconfig 미지원");
        }
        int up = 0, total = 0;
        for (String line : r.getStdout().split("\\R")) {
            String l = line.trim();
            if (l.isEmpty() || l.startsWith("lo")) continue;
            total++;
            if (l.toUpperCase().contains("UP")) up++;
        }
        ResultItem it = value(Category.NW, "NW_IFACE_STATUS", "인터페이스 상태(UP/전체)",
                up + "/" + total, "");
        it.setStatus(total == 0 || up > 0 ? ResultStatus.NORMAL : ResultStatus.CRITICAL);
        return it;
    }

    private ResultItem defaultRoute(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "ip", "route", "show", "default");
        if (!r.isSuccess()) {
            r = ctx.runner().run(ctx.timeoutMs(), "netstat", "-rn");
        }
        boolean has = r.isSuccess() && r.getStdout().toLowerCase().contains("default")
                || (r.isSuccess() && r.getStdout().contains("0.0.0.0"));
        ResultItem it = value(Category.NW, "NW_DEFAULT_ROUTE", "기본 게이트웨이",
                has ? "PRESENT" : "MISSING", "");
        it.setStatus(has ? ResultStatus.NORMAL : ResultStatus.CRITICAL);
        if (!has) it.setError("기본 라우트 없음");
        return it;
    }

    private ResultItem listenPortCount(CollectContext ctx) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "ss", "-ltn");
        if (!r.isSuccess()) {
            r = ctx.runner().run(ctx.timeoutMs(), "netstat", "-ltn");
        }
        if (!r.isSuccess()) {
            return na(Category.NW, "NW_LISTEN_PORTS", "리슨 포트 수", "ss/netstat 미지원");
        }
        int n = 0;
        for (String line : r.getStdout().split("\\R")) {
            if (line.contains("LISTEN")) n++;
        }
        return value(Category.NW, "NW_LISTEN_PORTS", "리슨 포트 수", String.valueOf(n), "ea");
    }

    private ResultItem firewall(CollectContext ctx) {
        // firewalld → ufw → iptables 순으로 확인 (내부 하드코딩 명령)
        CommandRunner.Result fw = ctx.runner().run(ctx.timeoutMs(), "systemctl", "is-active", "firewalld");
        if (fw.isSuccess() && fw.firstLine().trim().equals("active")) {
            return value(Category.NW, "NW_FIREWALL", "방화벽 상태", "firewalld:active", "");
        }
        CommandRunner.Result ufw = ctx.runner().run(ctx.timeoutMs(), "sh", "-c", "ufw status 2>/dev/null | head -1");
        if (ufw.isSuccess() && ufw.firstLine().toLowerCase().contains("active")) {
            return value(Category.NW, "NW_FIREWALL", "방화벽 상태", "ufw:active", "");
        }
        CommandRunner.Result ipt = ctx.runner().run(ctx.timeoutMs(), "sh", "-c",
                "iptables -L -n 2>/dev/null | grep -c -E '^(ACCEPT|DROP|REJECT)'");
        if (ipt.isSuccess()) {
            int rules = (int) parseLong(ipt.firstLine(), 0);
            ResultItem it = value(Category.NW, "NW_FIREWALL", "방화벽 상태",
                    rules > 0 ? "iptables:" + rules + "rules" : "none", "");
            it.setStatus(rules > 0 ? ResultStatus.NORMAL : ResultStatus.WARN);
            return it;
        }
        return na(Category.NW, "NW_FIREWALL", "방화벽 상태", "방화벽 도구 미탐지");
    }

    private ResultItem dnsResolve(CollectContext ctx) {
        try {
            long start = System.nanoTime();
            InetAddress addr = InetAddress.getByName("localhost");
            long ms = (System.nanoTime() - start) / 1_000_000L;
            ResultItem it = value(Category.NW, "NW_DNS_RESOLVE", "DNS 해석(localhost)",
                    addr.getHostAddress(), "");
            it.setRaw("ms=" + ms);
            return it;
        } catch (Exception e) {
            return error(Category.NW, "NW_DNS_RESOLVE", "DNS 해석", "해석 실패: " + e.getMessage());
        }
    }

    private ResultItem connectivity(CollectContext ctx) {
        // 게이트웨이 ping (내부 하드코딩 — 루프백 대상)
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(),
                "ping", "-c", "1", "-W", "2", "127.0.0.1");
        boolean ok = r.isSuccess();
        ResultItem it = value(Category.NW, "NW_CONNECTIVITY", "연결성(ping 127.0.0.1)",
                ok ? "OK" : "FAIL", "");
        it.setStatus(ok ? ResultStatus.NORMAL : ResultStatus.WARN);
        return it;
    }

    private ResultItem bonding(CollectContext ctx) {
        File bondDir = new File("/proc/net/bonding");
        if (!bondDir.isDirectory()) {
            return na(Category.NW, "NW_NIC_BONDING", "NIC 본딩", "본딩 미구성");
        }
        String[] bonds = bondDir.list();
        int n = bonds == null ? 0 : bonds.length;
        if (n == 0) {
            return na(Category.NW, "NW_NIC_BONDING", "NIC 본딩", "본딩 인터페이스 없음");
        }
        return value(Category.NW, "NW_NIC_BONDING", "NIC 본딩 수", String.valueOf(n), "ea");
    }
}
