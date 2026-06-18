package kr.go.pmc.agent.engine;

import kr.go.pmc.agent.collector.CollectContext;
import kr.go.pmc.agent.collector.Collector;
import kr.go.pmc.agent.collector.CollectorRegistry;
import kr.go.pmc.agent.config.AgentConfig;
import kr.go.pmc.agent.eval.ThresholdEvaluator;
import kr.go.pmc.agent.model.InspectionResult;
import kr.go.pmc.agent.model.PolicyDoc;
import kr.go.pmc.agent.model.PolicyItem;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;
import kr.go.pmc.agent.platform.CommandRunner;
import kr.go.pmc.agent.platform.Platform;
import kr.go.pmc.agent.platform.PlatformDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 활성 수집기를 실행해 {@link InspectionResult} 를 조립한다.
 * 수집기가 상태를 채우지 않은 항목은 정책 임계치로 평가한다.
 * (서버도 재평가하므로 상태/임계치 필드는 보조 정보로 채운다.)
 */
public class InspectionEngine {

    private static final Logger log = LoggerFactory.getLogger(InspectionEngine.class);

    private final AgentConfig cfg;
    private final CollectorRegistry registry;
    private final ThresholdEvaluator evaluator;
    private final CommandRunner runner;

    public InspectionEngine(AgentConfig cfg) {
        this.cfg = cfg;
        this.registry = new CollectorRegistry();
        this.evaluator = new ThresholdEvaluator();
        this.runner = new CommandRunner();
    }

    /**
     * 점검 실행.
     *
     * @param policy     현재 정책(null 가능)
     * @param categories 수집 카테고리(null 이면 설정값 사용)
     * @param runType    AUTO/MANUAL
     */
    public InspectionResult run(PolicyDoc policy, List<String> categories, String runType) {
        Platform platform = PlatformDetector.detect();
        Instant started = Instant.now();

        InspectionResult result = new InspectionResult();
        result.setRunId(UUID.randomUUID().toString());
        result.setAgentId(cfg.getAgentId());
        result.setHostname(cfg.getHostname());
        result.setPlatform(platform.name());
        result.setRunType(runType == null ? "AUTO" : runType);
        result.setStartedAt(started);
        if (policy != null) {
            result.setPolicyId(policy.getPolicyId());
            result.setPolicyVersion(policy.getVersion());
        }

        List<String> cats = (categories == null || categories.isEmpty())
                ? cfg.getCategories() : categories;
        long timeout = cfg.getTimeoutMs();
        CollectContext ctx = new CollectContext(platform, runner, policy, timeout);

        List<Collector> active = registry.active(platform, cats);
        log.info("점검 시작 platform={} categories={} collectors={}", platform, cats, active.size());

        for (Collector c : active) {
            List<ResultItem> items;
            try {
                items = c.collect(ctx);
            } catch (RuntimeException e) {
                // 인터페이스 계약상 예외를 던지지 않아야 하나, 방어적으로 처리
                ResultItem err = new ResultItem();
                err.setCategory(c.category());
                err.setItemCode(c.category().name() + "_COLLECT");
                err.setName(c.category().name() + " 수집");
                err.setStatus(ResultStatus.ERROR);
                err.setError("수집기 예외: " + e.getMessage());
                err.setCollectedAt(Instant.now());
                result.getItems().add(err);
                continue;
            }
            if (items == null) continue;
            for (ResultItem it : items) {
                applyThreshold(policy, it);
                result.getItems().add(it);
            }
        }

        result.setFinishedAt(Instant.now());
        log.info("점검 종료 runId={} items={}", result.getRunId(), result.getItems().size());
        return result;
    }

    /**
     * 수집기가 NORMAL 로만 둔(즉 자체 판정 안 한) 측정값 항목에 대해 정책 임계치를 적용한다.
     * ERROR/NA, 또는 수집기가 이미 WARN/CRITICAL 로 판정한 항목은 그대로 둔다.
     */
    private void applyThreshold(PolicyDoc policy, ResultItem it) {
        if (policy == null || it == null) return;
        if (it.getStatus() == ResultStatus.ERROR || it.getStatus() == ResultStatus.NA) return;
        if (it.getValue() == null) return;

        PolicyItem pi = policy.findItem(it.getItemCode());
        if (pi == null || pi.getThresholds() == null || pi.getThresholds().isEmpty()) {
            return;
        }
        ResultStatus evaluated = evaluator.evaluate(it.getValue(), pi);
        // 수집기 판정과 임계치 판정 중 더 심각한 쪽 채택
        it.setStatus(ResultStatus.worst(it.getStatus(), evaluated));

        // 보조 임계치 표기 채우기
        for (kr.go.pmc.agent.model.ThresholdRule rule : pi.getThresholds()) {
            String desc = describe(rule);
            if ("WARN".equalsIgnoreCase(rule.getLevel()) && it.getThresholdWarn() == null) {
                it.setThresholdWarn(desc);
            } else if ("CRITICAL".equalsIgnoreCase(rule.getLevel()) && it.getThresholdCritical() == null) {
                it.setThresholdCritical(desc);
            }
        }
    }

    private String describe(kr.go.pmc.agent.model.ThresholdRule r) {
        if ("RANGE".equalsIgnoreCase(r.getOperator())) {
            return "RANGE[" + r.getRangeLow() + "," + r.getRangeHigh() + "]";
        }
        return r.getOperator() + " " + r.getCompareValue();
    }
}
