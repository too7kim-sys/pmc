package egovframework.let.pmc.monitoring.service;

import java.io.Serializable;

/** 용량 추이의 단일 측정점. */
public class CapacityPointVO implements Serializable {
    private String ts;        // 표시용 시각(MM-DD HH:mm)
    private double value;     // 사용률(%)
    private String status;    // NORMAL/WARN/CRITICAL/ERROR/NA
    private boolean high;     // 고사용(이상) 여부

    public CapacityPointVO(String ts, double value, String status, boolean high) {
        this.ts = ts; this.value = value; this.status = status; this.high = high;
    }
    public String getTs() { return ts; }
    public double getValue() { return value; }
    public String getStatus() { return status; }
    public boolean isHigh() { return high; }
}
