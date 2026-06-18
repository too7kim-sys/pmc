package egovframework.let.pmc.plan.service;

import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class PlanVO implements Serializable {
    private Long planId;
    private String planName;
    private String cycle;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodTo;
    private Long policyId;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedDate;
    private String inspectorId;
    private String approverId;
    private String status;
    private String approveStatus;
    private String approveOpinion;
    private OffsetDateTime approveDt;

    // 표시/집계
    private int targetCnt;
    private int doneCnt;
    private List<PlanTargetVO> targets;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getCycle() { return cycle; }
    public void setCycle(String cycle) { this.cycle = cycle; }
    public LocalDate getPeriodFrom() { return periodFrom; }
    public void setPeriodFrom(LocalDate periodFrom) { this.periodFrom = periodFrom; }
    public LocalDate getPeriodTo() { return periodTo; }
    public void setPeriodTo(LocalDate periodTo) { this.periodTo = periodTo; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
    public String getInspectorId() { return inspectorId; }
    public void setInspectorId(String inspectorId) { this.inspectorId = inspectorId; }
    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApproveStatus() { return approveStatus; }
    public void setApproveStatus(String approveStatus) { this.approveStatus = approveStatus; }
    public String getApproveOpinion() { return approveOpinion; }
    public void setApproveOpinion(String approveOpinion) { this.approveOpinion = approveOpinion; }
    public OffsetDateTime getApproveDt() { return approveDt; }
    public void setApproveDt(OffsetDateTime approveDt) { this.approveDt = approveDt; }
    public int getTargetCnt() { return targetCnt; }
    public void setTargetCnt(int targetCnt) { this.targetCnt = targetCnt; }
    public int getDoneCnt() { return doneCnt; }
    public void setDoneCnt(int doneCnt) { this.doneCnt = doneCnt; }
    public List<PlanTargetVO> getTargets() { return targets; }
    public void setTargets(List<PlanTargetVO> targets) { this.targets = targets; }

    /** 이행률(%) */
    public int getCompliance() {
        return targetCnt == 0 ? 0 : (int) Math.round(doneCnt * 100.0 / targetCnt);
    }
}
