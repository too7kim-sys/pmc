package kr.go.pmc.agent.collector.sec;

import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.platform.CommandRunner;
import kr.go.pmc.agent.platform.Platform;

import java.util.List;

/**
 * 윈도우 보안 구성 진단. 하드코딩 argv 명령(net/auditpol) 사용, 미지원 시 NA.
 * collect() 는 절대 예외를 던지지 않는다.
 */
public class WindowsSecCollector extends AbstractSecCollector {

    @Override
    public boolean supports(Platform p) {
        return p == Platform.WINDOWS;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = newList();
        addSafe(out, "SEC_W01_PW_MINLEN",  () -> pwMinLength(ctx));
        addSafe(out, "SEC_W02_LOCKOUT",    () -> lockoutThreshold(ctx));
        addSafe(out, "SEC_W03_GUEST",      () -> guestAccount(ctx));
        addSafe(out, "SEC_W04_AUDIT",      () -> auditPolicy(ctx));
        return out;
    }

    /** W-01 최소 암호 길이(상): net accounts. */
    private ResultItem pwMinLength(CollectContext ctx) {
        String code = "SEC_W01_PW_MINLEN", name = "최소 암호 길이";
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "net", "accounts");
        if (!r.isSuccess() || r.getStdout().isEmpty()) {
            return notApplicable(code, name, "net accounts 미지원");
        }
        String line = firstMatch(r.getStdout(), "(?im)^.*(Minimum password length|최소 암호 길이).*$");
        if (line == null) return notApplicable(code, name, "최소 암호 길이 항목 없음");
        String num = regexGroup(line, "(\\d+)", 1);
        int v = (int) parseLong(num, -1);
        if (v >= 8) return good(code, name, "상", "minlen=" + v);
        return vuln(code, name, "상", "minlen=" + v + " (<8)");
    }

    /** W-02 계정 잠금 임계값(중). */
    private ResultItem lockoutThreshold(CollectContext ctx) {
        String code = "SEC_W02_LOCKOUT", name = "계정 잠금 임계값";
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "net", "accounts");
        if (!r.isSuccess() || r.getStdout().isEmpty()) {
            return notApplicable(code, name, "net accounts 미지원");
        }
        String line = firstMatch(r.getStdout(), "(?im)^.*(Lockout threshold|잠금 임계값).*$");
        if (line == null) return notApplicable(code, name, "잠금 임계값 항목 없음");
        if (line.toLowerCase().contains("never") || line.contains("안 함") || line.contains("없음")) {
            return vuln(code, name, "중", "계정 잠금 미설정");
        }
        String num = regexGroup(line, "(\\d+)", 1);
        int v = (int) parseLong(num, 0);
        if (v >= 1 && v <= 5) return good(code, name, "중", "lockout=" + v);
        return vuln(code, name, "중", "lockout=" + (num == null ? line : num) + " (권고 1~5)");
    }

    /** W-03 Guest 계정 비활성(중). */
    private ResultItem guestAccount(CollectContext ctx) {
        String code = "SEC_W03_GUEST", name = "Guest 계정 비활성";
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "net", "user", "guest");
        if (!r.isSuccess() || r.getStdout().isEmpty()) {
            return notApplicable(code, name, "net user guest 미지원");
        }
        String line = firstMatch(r.getStdout(), "(?im)^.*(Account active|계정 활성).*$");
        if (line == null) return notApplicable(code, name, "계정 활성 항목 없음");
        if (line.toLowerCase().contains("no") || line.contains("아니")) {
            return good(code, name, "중", "Guest 비활성");
        }
        return vuln(code, name, "중", "Guest 계정 활성화됨");
    }

    /** W-04 감사 정책 설정 여부(중). */
    private ResultItem auditPolicy(CollectContext ctx) {
        String code = "SEC_W04_AUDIT", name = "감사 정책 설정";
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "auditpol", "/get", "/category:*");
        if (!r.isSuccess() || r.getStdout().isEmpty()) {
            return notApplicable(code, name, "auditpol 미지원");
        }
        String out = r.getStdout();
        boolean anyAudit = out.matches("(?is).*(Success|Failure|성공|실패).*");
        boolean allNo = out.matches("(?is).*No Auditing.*") && !anyAudit;
        if (anyAudit && !allNo) return good(code, name, "중", "감사 정책 일부 활성");
        return vuln(code, name, "중", "감사 정책 미설정");
    }

    private String firstMatch(String text, String lineRegex) {
        if (text == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(lineRegex).matcher(text);
        return m.find() ? m.group().trim() : null;
    }
}
