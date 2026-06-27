package egovframework.let.pmc.vuln.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 취약점 추적(동일 취약점 관리)·재발방지 서비스.
 */
public interface VulnService {

    /**
     * 진단 결과 처리(상태머신 단일 진입점). 내장 점검(ingest)·외부 스캐너 import 양쪽에서 호출.
     *
     * @param serverId 대상 서버(null 이면 무시)
     * @param runId    내장 점검 run_id(스캐너 import 는 null)
     * @param source   BUILTIN / SCANNER / MANUAL
     * @param detected 이번 스캔에서 탐지된 취약 항목(없으면 전부 부재→FIXED 처리)
     * @param fullScan true 면 부재 항목 자동 FIXED(전체 스캔). 부분 추가(MANUAL)면 false.
     */
    void processFindings(Long serverId, String runId, String source,
                         List<VulnFinding> detected, boolean fullScan);

    List<VulnFindingVO> getFindings(Long serverId, String severity, String status);

    /** 검색+페이징 목록. */
    List<VulnFindingVO> getFindingsPaged(Long serverId, String severity, String status,
                                         String keyword, int limit, int offset);

    int countFindings(Long serverId, String severity, String status, String keyword);

    VulnFindingVO getFinding(Long findingId);

    List<VulnActionVO> getActions(Long findingId);

    VulnExceptionVO getActiveException(Long findingId);

    List<Map<String, Object>> getRecurrenceSummary();

    /** 조치이력 등록. resultingStatus 지정 시 finding 상태도 변경(예: FIXED). */
    void registerAction(Long findingId, String actionUser, String actionDesc,
                        String resultingStatus, String regUser);

    /** 예외(수용/면제) 등록 → finding EXEMPTED 전환. */
    void registerException(Long findingId, String reason, String approver,
                           OffsetDateTime expiresAt, String regUser);

    /** 만료 예외 정리(스케줄러): ACTIVE→EXPIRED + 대상 finding EXEMPTED→OPEN 재활성. 처리 건수 반환. */
    int expireExceptions();
}
