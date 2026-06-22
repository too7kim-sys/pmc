package egovframework.let.pmc.monitoring.service.impl;

import egovframework.let.pmc.monitoring.service.CapacityMetricVO;
import egovframework.let.pmc.monitoring.service.CapacityPointVO;
import egovframework.let.pmc.monitoring.service.CapacitySegmentVO;
import egovframework.let.pmc.monitoring.service.MonitoringMapper;
import egovframework.let.pmc.monitoring.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonitoringServiceImpl implements MonitoringService {

    private final MonitoringMapper monitoringMapper;

    @Autowired
    public MonitoringServiceImpl(MonitoringMapper monitoringMapper) {
        this.monitoringMapper = monitoringMapper;
    }

    @Override
    public List<Map<String, Object>> getSvcMonitor(int days) {
        // 서비스(svcKey)별로 항목코드별 최신값을 한 행으로 피벗
        Map<String, Map<String, Object>> bySvc = new LinkedHashMap<>();
        for (Map<String, Object> row : monitoringMapper.selectSvcLatest(days)) {
            String svcKey = str(row.get("svcKey"));
            Map<String, Object> svc = bySvc.computeIfAbsent(svcKey, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("svcKey", k);
                m.put("hostname", row.get("hostname"));
                return m;
            });
            String code = str(row.get("itemCode"));
            switch (code) {
                case "SVC_URL_STATUS":
                    svc.put("httpStatus", row.get("value"));
                    svc.put("statusJudge", row.get("status"));
                    svc.put("expected", row.get("rawText"));
                    svc.put("error", row.get("errorText"));
                    svc.put("lastCheck", row.get("receivedAt"));
                    break;
                case "SVC_RESPONSE_TIME":
                    svc.put("responseMs", row.get("value"));
                    svc.put("responseJudge", row.get("status"));
                    break;
                case "SVC_SSL_EXPIRY":
                    svc.put("sslDays", row.get("value"));
                    svc.put("sslJudge", row.get("status"));
                    break;
                case "SVC_CONTENT_MATCH":
                    svc.put("contentMatch", row.get("value"));
                    break;
                default:
                    break;
            }
        }
        // URL 매핑(policy svc target) 부착
        Map<String, String> urlMap = new HashMap<>();
        for (Map<String, Object> u : monitoringMapper.selectSvcUrlMap()) {
            urlMap.put(str(u.get("svcName")), str(u.get("url")));
        }
        // 실패율 부착
        for (Map<String, Object> f : monitoringMapper.selectSvcFailCount(days)) {
            Map<String, Object> svc = bySvc.get(str(f.get("svcKey")));
            if (svc != null) {
                long total = num(f.get("total"));
                long fail = num(f.get("fail"));
                svc.put("total", total);
                svc.put("fail", fail);
                svc.put("failRate", total > 0 ? Math.round(fail * 1000.0 / total) / 10.0 : 0.0);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> svc : bySvc.values()) {
            svc.put("url", urlMap.getOrDefault(str(svc.get("svcKey")), ""));
            svc.putIfAbsent("failRate", 0.0);
            out.add(svc);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> getDailySummary(int days) {
        return monitoringMapper.selectDailySummary(days);
    }

    @Override
    public List<Map<String, Object>> getRunsByDate(String day) {
        return monitoringMapper.selectRunsByDate(day);
    }

    @Override
    public Map<String, Object> getRealtimeSnapshot() {
        Map<String, Object> snap = new HashMap<>();
        snap.put("servers", monitoringMapper.selectRealtimeServers());
        snap.put("counters", monitoringMapper.selectCountersToday());
        snap.put("ts", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return snap;
    }

    // ===== ④ 용량점검 =====

    private static final Pattern NUMBER_PREFIX =
            Pattern.compile("^[+-]?\\d+(?:\\.\\d+)?");

    /** 용량 지표 코드 → 표시 라벨(추이 순서대로). */
    private static final String[][] CAP_METRICS = {
            {"OS_CPU_USAGE", "CPU 사용률"},
            {"OS_MEM_USAGE", "메모리 사용률"},
            {"OS_DISK_USAGE", "디스크 사용률"},
    };

    @Override
    public List<Map<String, Object>> getCapacityServers(int days) {
        return monitoringMapper.selectCapacityServers(days);
    }

    @Override
    public List<CapacityMetricVO> getCapacity(Long serverId, int days, double highThreshold) {
        // item_code 별로 측정점 분리(쿼리는 item_code, 시각 오름차순)
        Map<String, List<Map<String, Object>>> byCode = new LinkedHashMap<>();
        for (Map<String, Object> row : monitoringMapper.selectCapacitySeries(serverId, days)) {
            byCode.computeIfAbsent(str(row.get("itemCode")), k -> new ArrayList<>()).add(row);
        }
        List<CapacityMetricVO> metrics = new ArrayList<>();
        for (String[] def : CAP_METRICS) {
            metrics.add(buildMetric(def[0], def[1], byCode.get(def[0]), highThreshold));
        }
        return metrics;
    }

    private CapacityMetricVO buildMetric(String code, String label,
                                         List<Map<String, Object>> rows, double highThreshold) {
        CapacityMetricVO m = new CapacityMetricVO();
        m.setCode(code);
        m.setLabel(label);
        m.setUnit("%");
        List<CapacityPointVO> points = new ArrayList<>();
        List<CapacitySegmentVO> segments = new ArrayList<>();
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, sum = 0;
        int highCount = 0;

        // 진행 중 이상구간 상태
        String segStart = null, segEnd = null, segLevel = null;
        int segCount = 0;
        double segPeak = 0;

        if (rows != null) {
            for (Map<String, Object> r : rows) {
                Double v = toDouble(str(r.get("value")));
                if (v == null) continue; // 수치화 불가(예: NA) 점은 추이에서 제외
                String status = str(r.get("status"));
                String ts = str(r.get("ts"));
                boolean high = v >= highThreshold
                        || "WARN".equals(status) || "CRITICAL".equals(status);
                points.add(new CapacityPointVO(ts, round1(v), status, high));
                min = Math.min(min, v);
                max = Math.max(max, v);
                sum += v;
                if (high) {
                    highCount++;
                    String lvl = "CRITICAL".equals(status) ? "CRITICAL" : "WARN";
                    if (segStart == null) {
                        segStart = ts; segPeak = v; segLevel = lvl; segCount = 1;
                    } else {
                        segPeak = Math.max(segPeak, v);
                        if ("CRITICAL".equals(lvl)) segLevel = "CRITICAL";
                        segCount++;
                    }
                    segEnd = ts;
                } else if (segStart != null) {
                    segments.add(new CapacitySegmentVO(segStart, segEnd, segCount, round1(segPeak), segLevel));
                    segStart = null; segCount = 0; segPeak = 0; segLevel = null;
                }
            }
        }
        if (segStart != null) {
            segments.add(new CapacitySegmentVO(segStart, segEnd, segCount, round1(segPeak), segLevel));
        }

        m.setPoints(points);
        m.setSegments(segments);
        m.setCount(points.size());
        m.setHighCount(highCount);
        if (!points.isEmpty()) {
            m.setMin(round1(min));
            m.setMax(round1(max));
            m.setAvg(round1(sum / points.size()));
            CapacityPointVO last = points.get(points.size() - 1);
            m.setLatest(last.getValue());
            m.setLatestStatus(last.getStatus());
        } else {
            m.setLatestStatus("NA");
        }
        return m;
    }

    private Double toDouble(String s) {
        if (s == null) return null;
        Matcher mt = NUMBER_PREFIX.matcher(s.trim());
        if (!mt.find()) return null;
        try {
            return Double.parseDouble(mt.group());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private long num(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }
}
