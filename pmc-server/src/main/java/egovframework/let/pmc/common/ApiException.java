package egovframework.let.pmc.common;

/**
 * Agent API 처리 중 발생하는 업무 예외.
 */
public class ApiException extends RuntimeException {

    private final String code;

    public ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
