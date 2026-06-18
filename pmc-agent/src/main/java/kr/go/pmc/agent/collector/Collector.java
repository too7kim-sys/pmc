package kr.go.pmc.agent.collector;

import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.platform.Platform;

import java.util.List;

/**
 * 점검 수집기 인터페이스.
 * <p>{@link #collect(CollectContext)} 는 절대 예외를 던지지 않아야 한다.
 * 실패 시 status=ERROR + error 텍스트 항목을, 제품 미설치 시 status=NA 항목을 반환한다.</p>
 */
public interface Collector {

    Category category();

    boolean supports(Platform p);

    List<ResultItem> collect(CollectContext ctx);
}
