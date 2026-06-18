package kr.go.pmc.agent.platform;

/**
 * 현재 실행 중인 플랫폼을 식별한다.
 */
public final class PlatformDetector {

    private PlatformDetector() {
    }

    public static Platform detect() {
        return Platform.fromOsName(System.getProperty("os.name"));
    }
}
