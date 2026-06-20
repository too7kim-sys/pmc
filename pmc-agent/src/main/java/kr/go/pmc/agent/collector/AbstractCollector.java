package kr.go.pmc.agent.collector;

import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;
import kr.go.pmc.agent.platform.CommandRunner;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 수집기 공통 헬퍼. ResultItem 빌드, 안전한 숫자 파싱, 명령 1행 추출 등.
 */
public abstract class AbstractCollector implements Collector {

    /** 기본 ResultItem 생성(상태/값 지정). */
    protected ResultItem item(Category category, String itemCode, String name) {
        ResultItem it = new ResultItem();
        it.setCategory(category);
        it.setItemCode(itemCode);
        it.setName(name);
        it.setCollectedAt(Instant.now());
        return it;
    }

    /** 정상/측정값 항목. status 는 엔진의 ThresholdEvaluator 가 채울 수 있음. */
    protected ResultItem value(Category category, String itemCode, String name, String value, String unit) {
        // 값이 비어있으면(명령 출력 없음/미지원) NORMAL 오분류 대신 NA 로 보고
        if (value == null || value.trim().isEmpty()) {
            return na(category, itemCode, name, "값 없음(수집 실패 또는 미지원)");
        }
        ResultItem it = item(category, itemCode, name);
        it.setValue(value);
        it.setUnit(unit);
        it.setStatus(ResultStatus.NORMAL);
        return it;
    }

    /** 수집 오류 항목. */
    protected ResultItem error(Category category, String itemCode, String name, String errorMsg) {
        ResultItem it = item(category, itemCode, name);
        it.setStatus(ResultStatus.ERROR);
        it.setError(errorMsg);
        return it;
    }

    /** 해당 없음(제품 미설치 등) 항목. */
    protected ResultItem na(Category category, String itemCode, String name, String reason) {
        ResultItem it = item(category, itemCode, name);
        it.setStatus(ResultStatus.NA);
        it.setError(reason);
        return it;
    }

    /** 안전한 정수 파싱(실패시 def). */
    protected long parseLong(String s, long def) {
        if (s == null) return def;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 안전한 실수 파싱(실패시 def). */
    protected double parseDouble(String s, double def) {
        if (s == null) return def;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 소수점 자리수 포맷. */
    protected String fmt(double v, int decimals) {
        return String.format("%." + decimals + "f", v);
    }

    /** argv 명령 실행 후 첫 줄 반환(실패시 빈 문자열). */
    protected String firstLine(CommandRunner runner, long timeoutMs, String... argv) {
        CommandRunner.Result r = runner.run(timeoutMs, argv);
        return r.isSuccess() ? r.firstLine() : "";
    }

    /** 정규식 첫 그룹 추출(없으면 null). */
    protected String regexGroup(String text, String regex, int group) {
        if (text == null) return null;
        Matcher m = Pattern.compile(regex).matcher(text);
        if (m.find()) {
            return m.group(group);
        }
        return null;
    }

    /** 지정 문자열을 명령행에 포함하는 프로세스가 실행 중인지(pgrep -f). */
    protected boolean procContains(CollectContext ctx, String needle) {
        CommandRunner.Result r = ctx.runner().run(ctx.timeoutMs(), "pgrep", "-f", needle);
        return r.isSuccess() && !r.getStdout().trim().isEmpty();
    }
}
