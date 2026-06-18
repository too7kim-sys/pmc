package egovframework.let.pmc.plan.service;

import egovframework.let.pmc.ingest.service.ResultItemVO;

import java.util.List;

public interface PlanService {
    List<PlanVO> getPlanList();
    PlanVO getPlanDetail(Long planId);
    Long createPlan(PlanVO vo, List<Long> serverIds);

    /** 계획 대상에 대해 RUN_NOW 원격명령 일괄 발행(자동 점검). 반환=발행 건수 */
    int runAuto(Long planId, String requestedBy);

    /** 대상 서버의 최근 점검 실행을 계획 실적으로 연계 */
    void linkLatestRun(Long planId, Long serverId);

    /** 수동(MANUAL) 점검 결과 입력 */
    void saveManual(Long planId, Long serverId, List<ResultItemVO> items, String inputUser);

    void requestApproval(Long planId);
    void approve(Long planId, String opinion);
    void reject(Long planId, String opinion);
    void markOverdue();
}
