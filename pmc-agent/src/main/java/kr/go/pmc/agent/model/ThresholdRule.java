package kr.go.pmc.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 단일 임계치 규칙. level(WARN/CRITICAL), operator, 비교값으로 구성.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThresholdRule {

    private String level;        // WARN | CRITICAL
    private String operator;     // GT, GTE, LT, LTE, EQ, RANGE, REGEX
    private String compareValue; // 단일 비교 대상 (EQ/GT 등)
    private String rangeLow;     // RANGE 하한
    private String rangeHigh;    // RANGE 상한

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getCompareValue() {
        return compareValue;
    }

    public void setCompareValue(String compareValue) {
        this.compareValue = compareValue;
    }

    public String getRangeLow() {
        return rangeLow;
    }

    public void setRangeLow(String rangeLow) {
        this.rangeLow = rangeLow;
    }

    public String getRangeHigh() {
        return rangeHigh;
    }

    public void setRangeHigh(String rangeHigh) {
        this.rangeHigh = rangeHigh;
    }
}
