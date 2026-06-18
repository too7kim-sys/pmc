package kr.go.pmc.agent.collector;

import kr.go.pmc.agent.model.PolicyDoc;
import kr.go.pmc.agent.platform.CommandRunner;
import kr.go.pmc.agent.platform.Platform;

/**
 * 수집기 실행 컨텍스트. 플랫폼/명령실행기/정책을 보유한다.
 */
public class CollectContext {

    private final Platform platform;
    private final CommandRunner runner;
    private final PolicyDoc policy;
    private final long timeoutMs;

    public CollectContext(Platform platform, CommandRunner runner, PolicyDoc policy, long timeoutMs) {
        this.platform = platform;
        this.runner = runner;
        this.policy = policy;
        this.timeoutMs = timeoutMs <= 0 ? 10000 : timeoutMs;
    }

    public Platform platform() {
        return platform;
    }

    public CommandRunner runner() {
        return runner;
    }

    public PolicyDoc policy() {
        return policy;
    }

    public long timeoutMs() {
        return timeoutMs;
    }

    /** 해당 itemCode 가 정책상 수집 대상인지(정책 없으면 기본 true). */
    public boolean isItemEnabled(String itemCode) {
        return policy == null || policy.isItemEnabled(itemCode);
    }
}
