package egovframework.let.pmc.vuln.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 취약점 추적/예외/조치 매퍼.
 */
@Mapper
public interface VulnMapper {

    // ---- finding 추적(상태머신) ----
    VulnFindingVO selectFinding(@Param("serverId") Long serverId,
                                @Param("checkCode") String checkCode,
                                @Param("source") String source);

    int insertFinding(VulnFindingVO vo);

    /** 재탐지 갱신: occurrence_count+1, last_detected, status, severity, last_run_id, (재발 시 recur_count+1). */
    int updateFindingDetected(@Param("findingId") Long findingId,
                              @Param("status") String status,
                              @Param("severity") String severity,
                              @Param("lastRunId") String lastRunId,
                              @Param("recurInc") int recurInc,
                              @Param("updUser") String updUser);

    /** 현재 스캔에 없는(부재) finding 자동 FIXED. presentCodes 비면 해당 server+source 전부 FIXED. */
    int markFixedForServerExcept(@Param("serverId") Long serverId,
                                 @Param("source") String source,
                                 @Param("presentCodes") List<String> presentCodes,
                                 @Param("updUser") String updUser);

    /** 수동 상태 변경(조치 화면). */
    int updateFindingStatus(@Param("findingId") Long findingId,
                            @Param("status") String status,
                            @Param("updUser") String updUser);

    // ---- 조회(화면/보고서) ----
    List<VulnFindingVO> selectFindings(@Param("serverId") Long serverId,
                                       @Param("severity") String severity,
                                       @Param("status") String status);

    /** 검색(키워드=점검코드/취약점명) + 페이징 목록. */
    List<VulnFindingVO> selectFindingsPaged(@Param("serverId") Long serverId,
                                            @Param("severity") String severity,
                                            @Param("status") String status,
                                            @Param("keyword") String keyword,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);

    int countFindings(@Param("serverId") Long serverId,
                      @Param("severity") String severity,
                      @Param("status") String status,
                      @Param("keyword") String keyword);

    VulnFindingVO selectFindingById(@Param("findingId") Long findingId);

    List<Map<String, Object>> selectRecurrenceSummary();

    // ---- 예외(수용/면제) ----
    VulnExceptionVO selectActiveException(@Param("findingId") Long findingId);

    int insertException(VulnExceptionVO vo);

    List<VulnExceptionVO> selectExpiredExceptions();

    int expireException(@Param("exceptionId") Long exceptionId);

    // ---- 조치이력 ----
    int insertAction(VulnActionVO vo);

    List<VulnActionVO> selectActions(@Param("findingId") Long findingId);
}
