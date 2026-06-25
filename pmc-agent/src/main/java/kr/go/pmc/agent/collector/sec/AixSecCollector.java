package kr.go.pmc.agent.collector.sec;

import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.platform.Platform;

import java.util.List;

/**
 * AIX 보안 구성 진단. /etc/security/* 설정파일 NIO 읽기 위주, 미해당 시 NA.
 * collect() 는 절대 예외를 던지지 않는다.
 */
public class AixSecCollector extends AbstractSecCollector {

    @Override
    public boolean supports(Platform p) {
        return p == Platform.AIX;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = newList();
        addSafe(out, "SEC_A01_PW_MINLEN", this::pwMinLen);
        addSafe(out, "SEC_A02_PW_MAXAGE", this::pwMaxAge);
        addSafe(out, "SEC_A03_LOGIN_RETRIES", this::loginRetries);
        addSafe(out, "SEC_A04_PASSWD_PERM", this::passwdPerm);
        return out;
    }

    /** A-01 최소 암호 길이(상): /etc/security/user minlen. */
    private ResultItem pwMinLen() {
        String code = "SEC_A01_PW_MINLEN", name = "최소 암호 길이";
        List<String> lines = readLines("/etc/security/user");
        if (lines == null) return notApplicable(code, name, "/etc/security/user 없음");
        String line = findActiveLine(lines, "(?i)^minlen\\s*=.*");
        if (line == null) return vuln(code, name, "상", "minlen 미설정");
        int v = (int) parseLong(line.replaceAll(".*=", "").trim(), 0);
        if (v >= 8) return good(code, name, "상", "minlen=" + v);
        return vuln(code, name, "상", "minlen=" + v + " (<8)");
    }

    /** A-02 최대 사용기간(중): maxage(주). */
    private ResultItem pwMaxAge() {
        String code = "SEC_A02_PW_MAXAGE", name = "패스워드 최대 사용기간";
        List<String> lines = readLines("/etc/security/user");
        if (lines == null) return notApplicable(code, name, "/etc/security/user 없음");
        String line = findActiveLine(lines, "(?i)^maxage\\s*=.*");
        if (line == null) return vuln(code, name, "중", "maxage 미설정");
        int weeks = (int) parseLong(line.replaceAll(".*=", "").trim(), -1);
        if (weeks > 0 && weeks <= 13) return good(code, name, "중", "maxage=" + weeks + "주");
        return vuln(code, name, "중", "maxage=" + weeks + "주 (권고 ≤13)");
    }

    /** A-03 로그인 실패 임계(중): /etc/security/login.cfg loginretries. */
    private ResultItem loginRetries() {
        String code = "SEC_A03_LOGIN_RETRIES", name = "로그인 실패 임계";
        List<String> lines = readLines("/etc/security/user");
        if (lines == null) return notApplicable(code, name, "/etc/security/user 없음");
        String line = findActiveLine(lines, "(?i)^loginretries\\s*=.*");
        if (line == null) return vuln(code, name, "중", "loginretries 미설정");
        int v = (int) parseLong(line.replaceAll(".*=", "").trim(), 0);
        if (v >= 1 && v <= 5) return good(code, name, "중", "loginretries=" + v);
        return vuln(code, name, "중", "loginretries=" + v + " (권고 1~5)");
    }

    /** A-04 /etc/passwd 권한(상): 644 이하. */
    private ResultItem passwdPerm() {
        String code = "SEC_A04_PASSWD_PERM", name = "/etc/passwd 파일 권한";
        String perm = octalPerm("/etc/passwd");
        if (perm == null) return notApplicable(code, name, "/etc/passwd 조회 불가");
        String last3 = perm.length() > 3 ? perm.substring(perm.length() - 3) : perm;
        int mode = (int) safeOctal(last3);
        if ((mode & ~0644) == 0) return good(code, name, "상", "passwd=" + perm);
        return vuln(code, name, "상", "passwd=" + perm + " (권고 644 이하)");
    }

    private long safeOctal(String s) {
        try { return Long.parseLong(s, 8); } catch (NumberFormatException e) { return 0; }
    }
}
