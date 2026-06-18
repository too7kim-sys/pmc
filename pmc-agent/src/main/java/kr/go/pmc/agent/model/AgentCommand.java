package kr.go.pmc.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 서버가 Agent 에게 내리는 명령. commandType: RUN_NOW, START, STOP, SET_SCHEDULE,
 * UPDATE_CONFIG, UPDATE_POLICY. params 는 JSON 문자열.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentCommand {

    private Long commandId;
    private String commandType;
    private String params;

    public AgentCommand() {
    }

    public AgentCommand(Long commandId, String commandType, String params) {
        this.commandId = commandId;
        this.commandType = commandType;
        this.params = params;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}
