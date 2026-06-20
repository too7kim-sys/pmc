package egovframework.let.pmc.plan;

import egovframework.let.pmc.plan.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기점검 계획 지연(OVERDUE) 전환 배치.
 * 예정일 경과 & 미완료 계획을 주기적으로 OVERDUE 로 표시한다.
 * (이전에는 plan/list.do GET 마다 UPDATE 가 실행되어 조회에 쓰기 경합이 발생 → 배치로 분리)
 */
@Component
public class PlanOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlanOverdueScheduler.class);

    private final PlanService planService;

    @Autowired
    public PlanOverdueScheduler(PlanService planService) {
        this.planService = planService;
    }

    /** 매시 정각에 지연 계획 전환. */
    @Scheduled(cron = "0 0 * * * *")
    public void markOverdue() {
        try {
            planService.markOverdue();
        } catch (Exception e) {
            log.warn("정기점검 OVERDUE 배치 실패: {}", e.getMessage());
        }
    }
}
