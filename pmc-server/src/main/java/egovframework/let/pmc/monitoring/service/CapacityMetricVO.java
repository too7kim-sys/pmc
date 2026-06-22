package egovframework.let.pmc.monitoring.service;

import java.io.Serializable;
import java.util.List;

/** CPU/메모리/디스크 등 한 지표의 용량 추이 + 통계 + 이상구간. */
public class CapacityMetricVO implements Serializable {
    private String code;        // OS_CPU_USAGE 등
    private String label;       // CPU 사용률 등
    private String unit;        // %
    private int count;
    private double min;
    private double max;
    private double avg;
    private double latest;
    private String latestStatus;
    private int highCount;      // 고사용(이상) 측정점 수
    private List<CapacityPointVO> points;
    private List<CapacitySegmentVO> segments;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public double getMin() { return min; }
    public void setMin(double min) { this.min = min; }
    public double getMax() { return max; }
    public void setMax(double max) { this.max = max; }
    public double getAvg() { return avg; }
    public void setAvg(double avg) { this.avg = avg; }
    public double getLatest() { return latest; }
    public void setLatest(double latest) { this.latest = latest; }
    public String getLatestStatus() { return latestStatus; }
    public void setLatestStatus(String latestStatus) { this.latestStatus = latestStatus; }
    public int getHighCount() { return highCount; }
    public void setHighCount(int highCount) { this.highCount = highCount; }
    public List<CapacityPointVO> getPoints() { return points; }
    public void setPoints(List<CapacityPointVO> points) { this.points = points; }
    public List<CapacitySegmentVO> getSegments() { return segments; }
    public void setSegments(List<CapacitySegmentVO> segments) { this.segments = segments; }
}
