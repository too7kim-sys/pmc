package egovframework.let.pmc.dashboard.service;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {
    Map<String, Object> selectCounters();                 // 서버/agent/오늘 run 수 등
    List<Map<String, Object>> selectStatusByCategory();   // category,status,cnt (최근 7일)
    List<Map<String, Object>> selectRiskTopServers();     // 위험/주의 많은 서버
    List<Map<String, Object>> selectRecentRuns();         // 최근 점검
    List<Map<String, Object>> selectSvcAvailability();    // SVC 항목 상태
    Map<String, Object> selectPlanCompliance();           // 정기점검 이행률
    Map<String, Object> selectDqSummary();                // 데이터품질
    List<Map<String, Object>> selectDailyTrend();         // 최근 14일 일자별 판정 추이
    Map<String, Object> selectVulnSummary();              // 취약점 요약(미조치/등급/재발/예외)
    List<Map<String, Object>> selectVulnTopServers();     // 미조치 취약점 다발 서버 Top5
}
