package egovframework.let.pmc.plan.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlanMapper {
    List<PlanVO> selectPlanList();
    PlanVO selectPlan(@Param("planId") Long planId);
    void insertPlan(PlanVO vo);
    void insertTarget(PlanTargetVO vo);
    List<PlanTargetVO> selectTargets(@Param("planId") Long planId);
    void updateStatus(@Param("planId") Long planId, @Param("status") String status);
    void updateApprove(@Param("planId") Long planId, @Param("approveStatus") String approveStatus,
                       @Param("opinion") String opinion);
    void markOverdue();
    String selectLatestRunIdByServer(@Param("serverId") Long serverId);
}
