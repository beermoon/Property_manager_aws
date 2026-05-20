package kr.co.choi.property_manager.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Map;

/**
 * /api/** 엔드포인트의 예외를 일관된 JSON 응답으로 변환한다.
 *
 * <p>{@link RestControllerAdvice} 는 {@code @ControllerAdvice + @ResponseBody}
 * 의 합성이라, JSON 바디로 응답이 자동 직렬화된다.
 *
 * <p>응답 포맷은 다음과 같이 표준화한다:
 * <pre>
 * {
 *   "timestamp": "2026-05-19T08:00:00Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Property not found : 999"
 * }
 * </pre>
 */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * 잘못된 요청 — 입력값 오류, 비즈니스 규칙 위반.
     * <p>현재 코드베이스에서 "Property not found", "잘못된 요청입니다",
     * "지번 주소는 필수입니다" 등이 모두 {@link IllegalArgumentException}으로 던져진다.
     * 그중 "not found"는 별도로 분기해서 404로 응답한다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        boolean notFound = msg.toLowerCase().contains("not found");

        HttpStatus status = notFound ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        log.warn("API {} : {}", status, msg);
        return ResponseEntity.status(status).body(body(status, msg));
    }

    /**
     * 파일 입출력 실패 (사진 저장 중 IOException 등) — 서버 측 문제로 본다.
     */
    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<Map<String, Object>> handleIO(UncheckedIOException e) {
        log.error("API IO error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다."));
    }

    /**
     * 그 외 잡지 못한 예외 — 5xx로 응답하되 스택트레이스는 노출하지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("API unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."));
    }

    private Map<String, Object> body(HttpStatus status, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
    }
}
