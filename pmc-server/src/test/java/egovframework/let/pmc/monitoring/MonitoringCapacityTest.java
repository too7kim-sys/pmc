package egovframework.let.pmc.monitoring;

import egovframework.let.pmc.monitoring.service.CapacityMetricVO;
import egovframework.let.pmc.monitoring.service.CapacitySegmentVO;
import egovframework.let.pmc.monitoring.service.MonitoringMapper;
import egovframework.let.pmc.monitoring.service.impl.MonitoringServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 용량 추이 통계·고사용 이상구간 분리 단위테스트(스텁 매퍼, DB 무).
 */
class MonitoringCapacityTest {

    private Map<String, Object> pt(String value, String status) {
        Map<String, Object> m = new HashMap<>();
        m.put("itemCode", "OS_CPU_USAGE");
        m.put("value", value);
        m.put("unit", "%");
        m.put("status", status);
        m.put("ts", "06-01 10:0" + value.charAt(0));
        return m;
    }

    private MonitoringMapper stubWithCpuSeries(List<Map<String, Object>> series) {
        return new MonitoringMapper() {
            public List<Map<String, Object>> selectSvcLatest(int days) { return Collections.emptyList(); }
            public List<Map<String, Object>> selectSvcFailCount(int days) { return Collections.emptyList(); }
            public List<Map<String, Object>> selectSvcUrlMap() { return Collections.emptyList(); }
            public List<Map<String, Object>> selectDailySummary(int days) { return Collections.emptyList(); }
            public List<Map<String, Object>> selectRunsByDate(String day) { return Collections.emptyList(); }
            public List<Map<String, Object>> selectRealtimeServers() { return Collections.emptyList(); }
            public Map<String, Object> selectCountersToday() { return new HashMap<>(); }
            public List<Map<String, Object>> selectCapacityServers(int days) { return Collections.emptyList(); }
            public List<Map<String, Object>> selectCapacitySeries(Long serverId, int days) { return series; }
        };
    }

    @Test
    void capacityStatsAndHighSegments() {
        List<Map<String, Object>> series = new ArrayList<>();
        series.add(pt("50", "NORMAL"));
        series.add(pt("85", "WARN"));
        series.add(pt("90", "CRITICAL"));
        series.add(pt("40", "NORMAL"));
        MonitoringServiceImpl svc = new MonitoringServiceImpl(stubWithCpuSeries(series));

        List<CapacityMetricVO> metrics = svc.getCapacity(1L, 7, 80.0);
        assertEquals(5, metrics.size()); // CPU/MEM/DISK/SWAP/INODE

        CapacityMetricVO cpu = null;
        for (CapacityMetricVO m : metrics) {
            if ("OS_CPU_USAGE".equals(m.getCode())) cpu = m;
        }
        assertNotNull(cpu);
        assertEquals(4, cpu.getCount());
        assertEquals(40.0, cpu.getMin());
        assertEquals(90.0, cpu.getMax());
        assertEquals(2, cpu.getHighCount());            // 85, 90 (>=80)
        assertEquals(1, cpu.getSegments().size());      // 연속 고사용 1구간
        CapacitySegmentVO seg = cpu.getSegments().get(0);
        assertEquals(2, seg.getCount());
        assertEquals(90.0, seg.getPeak());
        assertEquals("CRITICAL", seg.getLevel());       // 구간 내 최고 판정

        // 빈 지표는 NA/0
        CapacityMetricVO mem = null;
        for (CapacityMetricVO m : metrics) if ("OS_MEM_USAGE".equals(m.getCode())) mem = m;
        assertNotNull(mem);
        assertEquals(0, mem.getCount());
    }
}
