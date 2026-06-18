package kr.go.pmc.agent.eval;

import kr.go.pmc.agent.model.PolicyItem;
import kr.go.pmc.agent.model.ResultStatus;
import kr.go.pmc.agent.model.ThresholdRule;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 수집값과 정책 임계치 규칙을 비교해 {@link ResultStatus} 를 산출한다.
 * <p>지원 연산자: GT, GTE, LT, LTE, EQ, RANGE, REGEX.
 * CRITICAL 이 WARN 보다 우선한다(둘 다 만족하면 CRITICAL).</p>
 */
public class ThresholdEvaluator {

    /**
     * 정책 항목의 모든 임계치 규칙을 평가한다. 일치하는 규칙이 없으면 NORMAL.
     *
     * @param value 수집값(문자열). 숫자 비교 연산자는 숫자 파싱이 필요.
     * @param item  정책 항목(임계치 규칙 보유). null 이거나 규칙 없으면 NORMAL.
     */
    public ResultStatus evaluate(String value, PolicyItem item) {
        if (item == null || item.getThresholds() == null) {
            return ResultStatus.NORMAL;
        }
        return evaluate(value, item.getThresholds());
    }

    /**
     * 임계치 규칙 리스트를 직접 평가한다. CRITICAL 우선.
     */
    public ResultStatus evaluate(String value, List<ThresholdRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return ResultStatus.NORMAL;
        }
        boolean warnHit = false;
        boolean criticalHit = false;
        for (ThresholdRule rule : rules) {
            if (rule == null || rule.getOperator() == null) continue;
            if (matches(value, rule)) {
                if ("CRITICAL".equalsIgnoreCase(rule.getLevel())) {
                    criticalHit = true;
                } else if ("WARN".equalsIgnoreCase(rule.getLevel())) {
                    warnHit = true;
                }
            }
        }
        if (criticalHit) return ResultStatus.CRITICAL;
        if (warnHit) return ResultStatus.WARN;
        return ResultStatus.NORMAL;
    }

    /**
     * 단일 규칙이 값에 매칭되는지.
     */
    public boolean matches(String value, ThresholdRule rule) {
        String op = rule.getOperator().trim().toUpperCase();
        switch (op) {
            case "REGEX":
                return matchRegex(value, rule.getCompareValue());
            case "EQ":
                return matchEq(value, rule.getCompareValue());
            case "RANGE":
                return matchRange(value, rule.getRangeLow(), rule.getRangeHigh());
            case "GT":
            case "GTE":
            case "LT":
            case "LTE":
                return matchNumeric(value, op, rule.getCompareValue());
            default:
                return false;
        }
    }

    private boolean matchRegex(String value, String regex) {
        if (value == null || regex == null) return false;
        try {
            return Pattern.compile(regex).matcher(value).find();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private boolean matchEq(String value, String compare) {
        if (value == null || compare == null) return false;
        // 숫자로 파싱 가능하면 수치 동등성, 아니면 문자열 동등성
        Double v = toDouble(value);
        Double c = toDouble(compare);
        if (v != null && c != null) {
            return v.doubleValue() == c.doubleValue();
        }
        return value.trim().equals(compare.trim());
    }

    private boolean matchRange(String value, String low, String high) {
        Double v = toDouble(value);
        Double lo = toDouble(low);
        Double hi = toDouble(high);
        if (v == null || lo == null || hi == null) return false;
        // [low, high] 범위 안에 들어오면 매칭(정상 범위를 벗어남 판정에 쓰는 경우는
        // 정책측에서 범위를 비정상 구간으로 정의하여 사용)
        return v >= lo && v <= hi;
    }

    private boolean matchNumeric(String value, String op, String compare) {
        Double v = toDouble(value);
        Double c = toDouble(compare);
        if (v == null || c == null) return false;
        switch (op) {
            case "GT":
                return v > c;
            case "GTE":
                return v >= c;
            case "LT":
                return v < c;
            case "LTE":
                return v <= c;
            default:
                return false;
        }
    }

    private Double toDouble(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        // 후행 단위(%, ms 등) 제거 시도
        StringBuilder sb = new StringBuilder();
        boolean seenDot = false;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == '-' && i == 0) {
                sb.append(ch);
            } else if (ch == '.' && !seenDot) {
                seenDot = true;
                sb.append(ch);
            } else if (Character.isDigit(ch)) {
                sb.append(ch);
            } else {
                break;
            }
        }
        if (sb.length() == 0 || "-".contentEquals(sb)) return null;
        try {
            return Double.parseDouble(sb.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
