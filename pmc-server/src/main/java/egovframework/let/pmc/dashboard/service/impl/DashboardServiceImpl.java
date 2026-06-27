package egovframework.let.pmc.dashboard.service.impl;

import egovframework.let.pmc.dashboard.service.DashboardMapper;
import egovframework.let.pmc.dashboard.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;

    @Autowired
    public DashboardServiceImpl(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    @Override
    public Map<String, Object> getDashboard() {
        Map<String, Object> m = new HashMap<>();
        m.put("counters", dashboardMapper.selectCounters());
        m.put("statusByCategory", dashboardMapper.selectStatusByCategory());
        m.put("riskTop", dashboardMapper.selectRiskTopServers());
        m.put("recentRuns", dashboardMapper.selectRecentRuns());
        m.put("svcAvailability", dashboardMapper.selectSvcAvailability());
        m.put("planCompliance", dashboardMapper.selectPlanCompliance());
        m.put("dqSummary", dashboardMapper.selectDqSummary());
        m.put("dailyTrend", dashboardMapper.selectDailyTrend());
        m.put("vulnSummary", dashboardMapper.selectVulnSummary());
        m.put("vulnTop", dashboardMapper.selectVulnTopServers());
        return m;
    }
}
