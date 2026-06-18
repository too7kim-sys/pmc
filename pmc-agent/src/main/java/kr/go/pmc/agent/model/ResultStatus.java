package kr.go.pmc.agent.model;

/**
 * 점검 결과 상태.
 * 심각도 순위(낮음→높음): NORMAL &lt; NA &lt; WARN &lt; CRITICAL &lt; ERROR
 * (ERROR/CRITICAL 가 가장 나쁨 — ERROR 는 수집 자체 실패이므로 최우선 경보 대상으로 둠)
 */
public enum ResultStatus {
    NORMAL(0),   // 정상
    NA(1),       // 해당 없음(제품 미설치 등)
    WARN(2),     // 주의
    CRITICAL(3), // 위험
    ERROR(4);    // 수집 오류

    private final int severity;

    ResultStatus(int severity) {
        this.severity = severity;
    }

    public int severity() {
        return severity;
    }

    /**
     * 두 상태 중 더 심각한(나쁜) 상태를 반환한다.
     */
    public static ResultStatus worst(ResultStatus a, ResultStatus b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.severity >= b.severity ? a : b;
    }
}
