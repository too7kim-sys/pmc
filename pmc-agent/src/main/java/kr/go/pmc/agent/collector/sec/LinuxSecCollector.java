package kr.go.pmc.agent.collector.sec;

import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.platform.CommandRunner;
import kr.go.pmc.agent.platform.Platform;

import java.util.List;

/**
 * 리눅스 보안 취약점/구성 진단(KISA 「주요정보통신기반시설 기술적 취약점 분석·평가 기준」 U-코드 준거).
 * <p>
 * 가능한 한 NIO 파일 읽기/권한 조회로 점검(외부 명령 비경유). 일부 항목만 하드코딩 argv 명령 사용.
 * collect() 는 절대 예외를 던지지 않는다(항목별 addSafe 보호).
 */
public class LinuxSecCollector extends AbstractSecCollector {

    @Override
    public boolean supports(Platform p) {
        return p == Platform.LINUX;
    }

    @Override
    public List<ResultItem> collect(CollectContext ctx) {
        List<ResultItem> out = newList();
        addSafe(out, "SEC_U01_ROOT_REMOTE",  this::rootRemoteLogin);
        addSafe(out, "SEC_U02_PW_COMPLEX",   this::passwordComplexity);
        addSafe(out, "SEC_U04_PW_MAX_DAYS",  this::passwordMaxDays);
        addSafe(out, "SEC_U05_PASSWD_PERM",  this::passwdFilePerm);
        addSafe(out, "SEC_U06_SHADOW_PERM",  this::shadowFilePerm);
        addSafe(out, "SEC_U07_EMPTY_PW",     this::emptyPassword);
        addSafe(out, "SEC_U08_UMASK",        this::umask);
        addSafe(out, "SEC_U09_UNNEEDED_SVC", () -> unneededServices(ctx));
        addSafe(out, "SEC_U10_CRON_PERM",    this::cronPerm);
        addSafe(out, "SEC_U11_LOG_PERM",     this::logPerm);
        return out;
    }

    /** U-01 root 원격 SSH 로그인 제한(상). */
    private ResultItem rootRemoteLogin() {
        String code = "SEC_U01_ROOT_REMOTE", name = "root 계정 원격 접속 제한";
        List<String> lines = readLines("/etc/ssh/sshd_config");
        if (lines == null) return notApplicable(code, name, "/etc/ssh/sshd_config 없음(SSH 미사용)");
        String line = findActiveLine(lines, "(?i)^permitrootlogin\\s+.*");
        if (line == null) {
            // 기본값(배포판별 상이) — 명시 설정 없음은 취약으로 간주(상)
            return vuln(code, name, "상", "PermitRootLogin 미설정(기본 허용 위험)");
        }
        String val = line.replaceAll("(?i)^permitrootlogin\\s+", "").trim().toLowerCase();
        if (val.startsWith("no")) {
            return good(code, name, "상", "PermitRootLogin " + val);
        }
        return vuln(code, name, "상", "PermitRootLogin " + val);
    }

    /** U-02 패스워드 복잡도 설정(상). */
    private ResultItem passwordComplexity() {
        String code = "SEC_U02_PW_COMPLEX", name = "패스워드 복잡도 설정";
        List<String> pw = readLines("/etc/security/pwquality.conf");
        if (pw != null) {
            String minlen = findActiveLine(pw, "(?i)^minlen\\s*=.*");
            if (minlen != null) {
                int v = (int) parseLong(minlen.replaceAll(".*=", "").trim(), 0);
                if (v >= 8) return good(code, name, "상", "minlen=" + v);
                return vuln(code, name, "상", "minlen=" + v + " (<8)");
            }
        }
        List<String> pam = readLines("/etc/pam.d/system-auth");
        if (pam == null) pam = readLines("/etc/pam.d/common-password");
        if (pam != null && findActiveLine(pam, "(?i).*pam_pwquality\\.so.*|.*pam_cracklib\\.so.*") != null) {
            return good(code, name, "상", "pam pwquality/cracklib 적용");
        }
        return vuln(code, name, "상", "패스워드 복잡도 정책 미확인");
    }

    /** U-04 패스워드 최대 사용기간(중). */
    private ResultItem passwordMaxDays() {
        String code = "SEC_U04_PW_MAX_DAYS", name = "패스워드 최대 사용기간";
        List<String> lines = readLines("/etc/login.defs");
        if (lines == null) return notApplicable(code, name, "/etc/login.defs 없음");
        String line = findActiveLine(lines, "(?i)^pass_max_days\\s+.*");
        if (line == null) return vuln(code, name, "중", "PASS_MAX_DAYS 미설정");
        int v = (int) parseLong(line.replaceAll("(?i)^pass_max_days\\s+", "").trim(), -1);
        if (v > 0 && v <= 90) return good(code, name, "중", "PASS_MAX_DAYS=" + v);
        return vuln(code, name, "중", "PASS_MAX_DAYS=" + v + " (권고 ≤90)");
    }

    /** U-05 /etc/passwd 권한(상): 644 이하. */
    private ResultItem passwdFilePerm() {
        return permCheck("SEC_U05_PASSWD_PERM", "/etc/passwd 파일 권한", "/etc/passwd", "644", "상");
    }

    /** U-06 /etc/shadow 권한(상): 400/000 등 그룹·기타 권한 없음. */
    private ResultItem shadowFilePerm() {
        String code = "SEC_U06_SHADOW_PERM", name = "/etc/shadow 파일 권한";
        String perm = octalPerm("/etc/shadow");
        if (perm == null) return notApplicable(code, name, "/etc/shadow 없음 또는 조회 불가");
        int mode = (int) parseLong(normalizeOctal(perm), 0, 8);
        // 그룹/기타에 권한이 있으면 취약(소유자 r/w 만 허용)
        if ((mode & 0077) == 0) return good(code, name, "상", "shadow=" + perm);
        return vuln(code, name, "상", "shadow=" + perm + " (그룹/기타 권한 존재)");
    }

    /** U-07 빈 패스워드 계정(상). */
    private ResultItem emptyPassword() {
        String code = "SEC_U07_EMPTY_PW", name = "빈 패스워드 계정";
        List<String> lines = readLines("/etc/shadow");
        if (lines == null) return notApplicable(code, name, "/etc/shadow 조회 불가(권한)");
        StringBuilder bad = new StringBuilder();
        for (String l : lines) {
            String[] f = l.split(":", -1);
            if (f.length >= 2 && f[1].isEmpty()) {
                if (bad.length() > 0) bad.append(',');
                bad.append(f[0]);
            }
        }
        if (bad.length() == 0) return good(code, name, "상", "빈 패스워드 계정 없음");
        return vuln(code, name, "상", "빈 패스워드 계정: " + bad);
    }

    /** U-08 umask 설정(하): 022 이상 제한. */
    private ResultItem umask() {
        String code = "SEC_U08_UMASK", name = "umask 설정";
        List<String> def = readLines("/etc/login.defs");
        String line = def != null ? findActiveLine(def, "(?i)^umask\\s+.*") : null;
        if (line == null) {
            List<String> prof = readLines("/etc/profile");
            line = prof != null ? findActiveLine(prof, "(?i).*umask\\s+[0-7]{3,4}.*") : null;
        }
        if (line == null) return vuln(code, name, "하", "umask 설정 미확인");
        String val = regexGroup(line, "([0-7]{3,4})", 1);
        if (val == null) return vuln(code, name, "하", "umask 값 파싱 실패: " + line);
        int last = val.charAt(val.length() - 1) - '0';
        int prev = val.charAt(val.length() - 2) - '0';
        if ((prev & 2) != 0 && (last & 2) != 0) return good(code, name, "하", "umask=" + val);
        return vuln(code, name, "하", "umask=" + val + " (권고 022/027)");
    }

    /** U-09 불필요 서비스 활성화(중): telnet/rsh/ftp 등. */
    private ResultItem unneededServices(CollectContext ctx) {
        String code = "SEC_U09_UNNEEDED_SVC", name = "불필요 서비스 활성화";
        String[] svc = {"telnet.socket", "telnet", "rsh.socket", "rlogin.socket", "vsftpd", "tftp"};
        StringBuilder enabled = new StringBuilder();
        boolean systemctl = false;
        for (String s : svc) {
            CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "systemctl", "is-enabled", s);
            // exitcode 무관: stdout 첫 줄이 enabled/active 면 활성
            if (!r.getStdout().isEmpty() || !r.getStderr().isEmpty()) systemctl = true;
            String st = r.firstLine().trim().toLowerCase();
            if ("enabled".equals(st) || "active".equals(st) || "static".equals(st)) {
                if (enabled.length() > 0) enabled.append(',');
                enabled.append(s);
            }
        }
        if (!systemctl) return notApplicable(code, name, "systemctl 미지원(수동 점검 필요)");
        if (enabled.length() == 0) return good(code, name, "중", "telnet/rsh/ftp 등 비활성");
        return vuln(code, name, "중", "활성 서비스: " + enabled);
    }

    /** U-10 cron 설정파일 권한(중): /etc/crontab 640 이하. */
    private ResultItem cronPerm() {
        return permCheck("SEC_U10_CRON_PERM", "cron 설정파일 권한", "/etc/crontab", "640", "중");
    }

    /** U-11 로그 디렉터리/파일 권한(하). */
    private ResultItem logPerm() {
        String code = "SEC_U11_LOG_PERM", name = "주요 로그파일 권한";
        String[] candidates = {"/var/log/secure", "/var/log/auth.log", "/var/log/messages", "/var/log/syslog"};
        for (String c : candidates) {
            String perm = octalPerm(c);
            if (perm == null) continue;
            int mode = (int) parseLong(normalizeOctal(perm), 0, 8);
            if ((mode & 0007) != 0) return vuln(code, name, "하", c + "=" + perm + " (기타 권한 존재)");
            return good(code, name, "하", c + "=" + perm);
        }
        return notApplicable(code, name, "주요 로그파일 없음");
    }

    // ---- 공통 권한 비교 헬퍼 ----

    /** 권한이 maxOctal 이하(각 자리 비트가 maxOctal 자리 비트의 부분집합)인지 검사. */
    private ResultItem permCheck(String code, String name, String path, String maxOctal, String severity) {
        String perm = octalPerm(path);
        if (perm == null) return notApplicable(code, name, path + " 없음 또는 조회 불가");
        int mode = (int) parseLong(normalizeOctal(perm), 0, 8);
        int max = (int) parseLong(maxOctal, 0, 8);
        // mode 의 어떤 비트도 max 에 없는 비트를 켜고 있으면 취약
        if ((mode & ~max) == 0) return good(code, name, severity, path + "=" + perm);
        return vuln(code, name, severity, path + "=" + perm + " (권고 " + maxOctal + " 이하)");
    }

    /** 8진수 문자열 정규화(앞의 setuid 비트 등 4자리는 뒤 3자리만 사용). */
    private String normalizeOctal(String octal) {
        if (octal == null) return "0";
        String s = octal.trim();
        if (s.length() > 3) s = s.substring(s.length() - 3);
        return s;
    }

    /** 진법 지정 long 파싱(실패시 def). */
    private long parseLong(String s, long def, int radix) {
        if (s == null) return def;
        try {
            return Long.parseLong(s.trim(), radix);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
