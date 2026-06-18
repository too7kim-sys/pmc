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
        if (n.contains("mac") || n.contains("darwin")) return LINUX; // 개발 환경 편의상 unix 계열로 취급
        return UNKNOWN;
    }
}
