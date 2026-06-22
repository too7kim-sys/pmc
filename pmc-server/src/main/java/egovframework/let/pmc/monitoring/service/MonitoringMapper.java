package egovframework.let.pmc.monitoring.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 모니터링(웹서비스 가용성/일자별/실시간) 조회 매퍼. 모두 기존 적재 데이터 집계(신규 테이블 없음).
 */
@Mapper
public interface MonitoringMapper {

    // ① 웹서비스 URL 모니터링
    List<Map<String, Object>> selectSvcLatest(@Param("days") int days);
    List<Map<String, Object>> selectSvcFailCount(@Param("days") int days);
    List<Map<String, Object>> selectSvcUrlMap();

    // ② 일자별 점검 모니터링
    List<Map<String, Object>> selectDailySummary(@Param("days") int days);
    List<Map<String, Object>> selectRunsByDate(@Param("day") String day);

    // ③ 실시간 시스템 상태
    List<Map<String, Object>> selectRealtimeServers();
    Map<String, Object> selectCountersToday();

    // ④ 용량점검(CPU/메모리/디스크 추이)
    List<Map<String, Object>> selectCapacityServers(@Param("days") int days);
    List<Map<String, Object>> selectCapacitySeries(@Param("serverId") Long serverId, @Param("days") int days);
}
