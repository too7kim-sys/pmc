package kr.go.pmc.agent.collector;

import kr.go.pmc.agent.collector.db.DbCollector;
import kr.go.pmc.agent.collector.nw.NwCollector;
import kr.go.pmc.agent.collector.os.AixOsCollector;
import kr.go.pmc.agent.collector.os.HpuxOsCollector;
import kr.go.pmc.agent.collector.os.LinuxOsCollector;
import kr.go.pmc.agent.collector.os.WindowsOsCollector;
import kr.go.pmc.agent.collector.svc.HttpServiceCollector;
import kr.go.pmc.agent.collector.sw.SwCollector;
import kr.go.pmc.agent.collector.was.WasCollector;
import kr.go.pmc.agent.collector.web.ApacheNginxCollector;
import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.platform.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 플랫폼/정책 활성 카테고리에 맞는 수집기를 선별해 제공한다.
 */
public class CollectorRegistry {

    private final List<Collector> all = new ArrayList<>();

    public CollectorRegistry() {
        // OS
        all.add(new LinuxOsCollector());
        all.add(new WindowsOsCollector());
        all.add(new AixOsCollector());
        all.add(new HpuxOsCollector());
        // WEB / WAS / DB / SW / NW (리눅스)
        all.add(new ApacheNginxCollector());
        all.add(new WasCollector());
        all.add(new DbCollector());
        all.add(new SwCollector());
        all.add(new NwCollector());
        // SVC (모든 플랫폼)
        all.add(new HttpServiceCollector());
    }

    /**
     * 현재 플랫폼이 지원하고, 요청된 카테고리에 포함되는 수집기 목록.
     *
     * @param platform           현재 플랫폼
     * @param enabledCategories  수집할 카테고리 문자열 목록(null/빈값이면 전체)
     */
    public List<Collector> active(Platform platform, List<String> enabledCategories) {
        Set<Category> wanted = new HashSet<>();
        if (enabledCategories == null || enabledCategories.isEmpty()) {
            wanted.addAll(Arrays.asList(Category.values()));
        } else {
            for (String s : enabledCategories) {
                if (s == null) continue;
                try {
                    wanted.add(Category.valueOf(s.trim().toUpperCase()));
                } catch (IllegalArgumentException ignore) {
                    // 알 수 없는 카테고리는 무시
                }
            }
        }
        List<Collector> result = new ArrayList<>();
        for (Collector c : all) {
            if (c.supports(platform) && wanted.contains(c.category())) {
                result.add(c);
            }
        }
        return result;
    }

    public List<Collector> allCollectors() {
        return new ArrayList<>(all);
    }
}
