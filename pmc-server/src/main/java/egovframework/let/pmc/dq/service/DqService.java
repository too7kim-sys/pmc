package egovframework.let.pmc.dq.service;

import java.util.List;
import java.util.Map;

public interface DqService {
    List<Map<String, Object>> getRules();
    List<Map<String, Object>> getResults();
    /** 전체 활성 룰 실행 → 결과 적재. 반환=실행 건수 */
    int runAll();
}
