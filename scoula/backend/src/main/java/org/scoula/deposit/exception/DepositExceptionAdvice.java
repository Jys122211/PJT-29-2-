package org.scoula.deposit.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * deposit 도메인 전용 예외 처리.
 *
 * <p>
 * org.scoula.exception 패키지의 공용 Advice(@Order(2))보다 먼저 잡히도록
 * 
 * @Order(1)로 설정합니다. 공용 파일을 수정하지 않기 위해 자체 패키지에 둡니다.
 */
@RestControllerAdvice(basePackages = "org.scoula.deposit")
@Log4j2
@Order(1)
public class DepositExceptionAdvice {

    /** 400 - 화면 07-02, 07-03 */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(ValidationException e) {
        log.info("검증 실패 [{}] {} : {}", e.getErrorCode(), e.getField(), e.getMessage());

        Map<String, String> body = new LinkedHashMap<>();
        body.put("errorCode", e.getErrorCode());
        body.put("field", e.getField());
        body.put("message", e.getMessage());

        return ResponseEntity.badRequest().body(body);
    }

    /** 404 - 화면 07-10 편집 대상 없음 */
    @ExceptionHandler(DepositNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(DepositNotFoundException e) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("errorCode", "DEPOSIT_NOT_FOUND");
        body.put("message", e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * 요청 본문을 DTO로 바꾸지 못한 경우.
     * 금액이 Long 범위를 넘거나, 숫자 자리에 문자가 온 경우가 여기서 잡힌다.
     * validate()는 변환 이후에 실행되므로 이 단계를 통과하지 못한 요청은 닿지 않는다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException e) {
        log.info("요청 본문 변환 실패 : {}", e.getMostSpecificCause().getMessage());

        Map<String, String> body = new LinkedHashMap<>();
        body.put("errorCode", "INVALID_FORMAT");
        body.put("message", "입력값이 올바르지 않습니다. 금액과 금리를 다시 확인해주세요.");
        return ResponseEntity.badRequest().body(body);
    }

    /** 위에서 잡히지 않은 모든 예외. 원인은 로그에만 남기고 사용자에겐 일반 문구만 보낸다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        log.error("예금 도메인 처리 중 예상하지 못한 오류", e);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("errorCode", "INTERNAL_ERROR");
        body.put("message", "요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

}
