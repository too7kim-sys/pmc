package egovframework.let.pmc.monitoring.service;

import java.util.List;
import java.util.Map;

/**
 * 모니터링 화면용 서비스. SVC URL 가용성, 일자별 집계, 실시간 스냅샷 제공.
 */
public interface MonitoringService {

    /** ① 서비스(URL)별 최신 응답코드/응답시간/SSL/실패율 목록 */
    List<Map<String, Object>> getSvcMonitor(int days);

    /** ② 일자별 점검 집계 */
    List<Map<String, Object>> getDailySummary(int days);

    /** ② 특정 일자 run 목록 */
    List<Map<String, Object>> getRunsByDate(String day);

    /** ③ 실시간 스냅샷: { servers:[...], counters:{...}, ts:"..." } */
    Map<String, Object> getRealtimeSnapshot();

    /** ④ 용량점검 대상 서버 목록(OS 용량 지표 보유) */
    List<Map<String, Object>> getCapacityServers(int days);

    /** ④ 특정 서버의 CPU/메모리/디스크 추이 + 통계 + 고사용 이상구간. highThreshold(%) 이상 또는 WARN/CRITICAL 을 이상으로 판정 */
    List<CapacityMetricVO> getCapacity(Long serverId, int days, double highThreshold);
}
