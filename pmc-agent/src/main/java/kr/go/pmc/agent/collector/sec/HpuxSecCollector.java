package kr.go.pmc.agent.collector.sec;

import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.platform.Platform;

import java.util.List;

/**
 * HP-UX 보안 구성 진단. /etc/default/security 등 NIO 읽기 위주, 미해당 시 NA.
 * collect() 는 절대 예외를 던지지 않는다.
 */
public class HpuxSecCollector extends AbstractSecCollector {

    @Override
    public boolean supports(Platform p) {
        return p == Platform.HPUX;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = newList();
        addSafe(out, "SEC_H01_PW_MINLEN", this::pwMinLen);
        addSafe(out, "SEC_H02_PW_MAXDAYS", this::pwMaxDays);
        addSafe(out, "SEC_H03_PASSWD_PERM", this::passwdPerm);
        return out;
    }

    /** H-01 최소 암호 길이(상): /etc/default/security MIN_PASSWORD_LENGTH. */
    private ResultItem pwMinLen() {
        String code = "SEC_H01_PW_MINLEN", name = "최소 암호 길이";
        List<String> lines = readLines("/etc/default/security");
        if (lines == null) return notApplicable(code, name, "/etc/default/security 없음");
        String line = findActiveLine(lines, "(?i)^min_password_length\\s*=.*");
        if (line == null) return vuln(code, name, "상", "MIN_PASSWORD_LENGTH 미설정");
        int v = (int) parseLong(line.replaceAll(".*=", "").trim(), 0);
        if (v >= 8) return good(code, name, "상", "MIN_PASSWORD_LENGTH=" + v);
        return vuln(code, name, "상", "MIN_PASSWORD_LENGTH=" + v + " (<8)");
    }

    /** H-02 최대 사용기간(중): /etc/default/security PASSWORD_MAXDAYS. */
    private ResultItem pwMaxDays() {
        String code = "SEC_H02_PW_MAXDAYS", name = "패스워드 최대 사용기간";
        List<String> lines = readLines("/etc/default/security");
        if (lines == null) return notApplicable(code, name, "/etc/default/security 없음");
        String line = findActiveLine(lines, "(?i)^password_maxdays\\s*=.*");
        if (line == null) return vuln(code, name, "중", "PASSWORD_MAXDAYS 미설정");
        int v = (int) parseLong(line.replaceAll(".*=", "").trim(), -1);
        if (v > 0 && v <= 90) return good(code, name, "중", "PASSWORD_MAXDAYS=" + v);
        return vuln(code, name, "중", "PASSWORD_MAXDAYS=" + v + " (권고 ≤90)");
    }

    /** H-03 /etc/passwd 권한(상): 444/644 이하. */
    private ResultItem passwdPerm() {
        String code = "SEC_H03_PASSWD_PERM", name = "/etc/passwd 파일 권한";
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
