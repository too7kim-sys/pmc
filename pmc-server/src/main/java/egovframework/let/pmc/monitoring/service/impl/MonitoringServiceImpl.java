package egovframework.let.pmc.monitoring.service.impl;

import egovframework.let.pmc.monitoring.service.MonitoringMapper;
import egovframework.let.pmc.monitoring.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private long num(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }
}
