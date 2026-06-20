package egovframework.let.pmc.common.web;

import egovframework.let.pmc.common.ApiException;
import egovframework.let.pmc.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Agent API(/api/v1) 전용 예외 처리 — @RestController 한정, 항상 ApiResponse JSON 엔벨로프로 응답.
 */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException e) {
        return ResponseEntity.status(statusFor(e.getCode()))
                .body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    /** 업무 예외 코드 → HTTP 상태 매핑(기본 400). */
    private HttpStatus statusFor(String code) {
        if ("FORBIDDEN".equals(code)) return HttpStatus.FORBIDDEN;          // 403
        if ("UNAUTHORIZED".equals(code) || "INVALID_TOKEN".equals(code)) return HttpStatus.UNAUTHORIZED; // 401
        return HttpStatus.BAD_REQUEST;                                       // 400
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleEtc(Exception e) {
        log.error("API 처리 오류", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("ERROR", e.getMessage()));
    }
}
