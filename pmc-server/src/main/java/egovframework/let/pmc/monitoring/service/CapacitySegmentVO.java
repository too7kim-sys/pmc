package egovframework.let.pmc.monitoring.service;

import java.io.Serializable;

/** 고사용(이상) 연속 구간. */
public class CapacitySegmentVO implements Serializable {
    private String startTs;
    private String endTs;
    private int count;     // 구간 내 측정점 수
    private double peak;    // 구간 최고 사용률
    private String level;   // WARN/CRITICAL(구간 내 최고 판정)

    public CapacitySegmentVO(String startTs, String endTs, int count, double peak, String level) {
        this.startTs = startTs; this.endTs = endTs; this.count = count; this.peak = peak; this.level = level;
    }
    public String getStartTs() { return startTs; }
    public String getEndTs() { return endTs; }
    public int getCount() { return count; }
    public double getPeak() { return peak; }
    public String getLevel() { return level; }
}
