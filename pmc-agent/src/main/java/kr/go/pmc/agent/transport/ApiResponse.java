package kr.go.pmc.agent.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 서버 공통 응답 엔벨로프 {@code { success, code, message, data }}.
 * data 는 엔드포인트마다 다르므로 JsonNode 로 보관 후 호출측에서 매핑한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse {

    private boolean success;
    private String code;
    private String message;
    private JsonNode data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }
}
