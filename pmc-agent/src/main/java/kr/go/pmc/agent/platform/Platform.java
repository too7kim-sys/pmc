package kr.go.pmc.agent.platform;

import java.util.Locale;

/**
 * 운영체제 플랫폼 식별.
 */
public enum Platform {
    LINUX,
    WINDOWS,
    AIX,
    HPUX,
    SOLARIS,
    UNKNOWN;

    /**
     * os.name 시스템 프로퍼티 형태의 문자열로부터 Platform 을 추정한다.
     */
    public static Platform fromOsName(String osName) {
        if (osName == null) return UNKNOWN;
        String n = osName.toLowerCase(Locale.ROOT);
        if (n.contains("win")) return WINDOWS;
        if (n.contains("linux")) return LINUX;
        if (n.contains("aix")) return AIX;
        if (n.contains("hp-ux") || n.contains("hpux")) return HPUX;
        if (n.contains("sunos") || n.contains("solaris")) return SOLARIS;
        // macOS/Darwin 은 운영 배포 대상이 아니며, 개발자 로컬 검증 편의를 위해서만 LINUX 로 취급한다.
        // (실제 macOS 에서는 /proc 부재 등으로 일부 수집 항목이 ERROR/NA 로 보고될 수 있음 — 정상 동작)
        if (n.contains("mac") || n.contains("darwin")) return LINUX;
        return UNKNOWN;
    }
}
