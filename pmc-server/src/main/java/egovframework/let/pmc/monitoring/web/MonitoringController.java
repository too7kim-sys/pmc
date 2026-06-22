package egovframework.let.pmc.monitoring.web;

import egovframework.let.pmc.monitoring.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 모니터링 화면: 웹서비스(URL) 가용성 / 일자별 점검 / 실시간 시스템 상태.
 */
@Controller
@RequestMapping("/pmc/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @Autowired
    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /** ① URL별 응답시간/접속 실패 확인 */
    @GetMapping("/svc.do")
    public String svc(@RequestParam(defaultValue = "7") int days, Model model) {
        model.addAttribute("days", days);
        model.addAttribute("svcList", monitoringService.getSvcMonitor(days));
        return "egovframework/let/pmc/monitoring/svcMonitor";
    }

    /** ② 일자별 점검 모니터링 */
    @GetMapping("/daily.do")
    public String daily(@RequestParam(defaultValue = "30") int days, Model model) {
        model.addAttribute("days", days);
        model.addAttribute("daily", monitoringService.getDailySummary(days));
        return "egovframework/let/pmc/monitoring/dailyMonitor";
    }

    /** ② 특정 일자 상세(드릴다운) */
    @GetMapping("/dailyDetail.do")
    public String dailyDetail(@RequestParam String day, Model model) {
        model.addAttribute("day", day);
        model.addAttribute("runs", monitoringService.getRunsByDate(day));
        return "egovframework/let/pmc/monitoring/dailyDetail";
    }

    /** ③ 실시간 시스템 상태(SSE 화면) */
    @GetMapping("/realtime.do")
    public String realtime(Model model) {
        model.addAttribute("snapshot", monitoringService.getRealtimeSnapshot());
        return "egovframework/let/pmc/monitoring/realtimeMonitor";
    }

    /** ④ 용량점검(CPU/메모리/디스크 추이 + 고사용 이상구간) */
    @GetMapping("/capacity.do")
    public String capacity(@RequestParam(required = false) Long serverId,
                           @RequestParam(defaultValue = "7") int days,
                           @RequestParam(defaultValue = "80") double high,
                           Model model) {
        model.addAttribute("days", days);
        model.addAttribute("high", high);
        model.addAttribute("servers", monitoringService.getCapacityServers(days));
        model.addAttribute("serverId", serverId);
        if (serverId != null) {
            model.addAttribute("metrics", monitoringService.getCapacity(serverId, days, high));
        }
        return "egovframework/let/pmc/monitoring/capacityMonitor";
    }
}
