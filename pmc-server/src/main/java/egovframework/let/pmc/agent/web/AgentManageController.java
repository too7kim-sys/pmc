package egovframework.let.pmc.agent.web;

import egovframework.let.pmc.agent.service.AgentService;
import egovframework.let.pmc.agent.service.AgentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 대상 서버/Agent 관리 + 원격제어 화면.
 */
@Controller
@RequestMapping("/pmc/agent")
public class AgentManageController {

    private final AgentService agentService;

    @Autowired
    public AgentManageController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/list.do")
    public String list(Model model) {
        model.addAttribute("servers", agentService.getServerList());
        model.addAttribute("agents", agentService.getAgentList());
        return "egovframework/let/pmc/agent/agentList";
    }

    /** 대상 서버 등록 */
    @PostMapping("/registServer.do")
    public String registServer(@ModelAttribute AgentVO vo) {
        agentService.registerServer(vo);
        return "redirect:/pmc/agent/list.do";
    }

    /** Agent 등록 토큰 발급 */
    @PostMapping("/issueToken.do")
    public String issueToken(@RequestParam Long serverId, @RequestParam(required = false) Long policyId,
                             Model model) {
        String token = agentService.issueEnrollToken(serverId, policyId);
        model.addAttribute("servers", agentService.getServerList());
        model.addAttribute("agents", agentService.getAgentList());
        model.addAttribute("issuedToken", token);
        return "egovframework/let/pmc/agent/agentList";
    }

    /** Agent 원격제어 화면 */
    @GetMapping("/control.do")
    public String control(@RequestParam String agentId, Model model) {
        model.addAttribute("agent", agentService.getAgent(agentId));
        model.addAttribute("commands", agentService.getCommandHistory(agentId));
        return "egovframework/let/pmc/agent/agentControl";
    }

    /** 원격 명령 발행 */
    @PostMapping("/command.do")
    public String command(@RequestParam String agentId, @RequestParam String commandType,
                          @RequestParam(required = false) String params,
                          java.security.Principal principal) {
        String who = principal != null ? principal.getName() : "admin";
        agentService.issueCommand(agentId, commandType, params, who);
        return "redirect:/pmc/agent/control.do?agentId=" + agentId;
    }
}
