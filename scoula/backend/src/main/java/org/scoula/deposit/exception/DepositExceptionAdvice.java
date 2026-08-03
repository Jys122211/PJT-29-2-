package org.scoula.deposit.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * deposit 도메인 전용 예외 처리.
 *
 * <p>org.scoula.exception 패키지의 공용 Advice(@Order(2))보다 먼저 잡히도록
 * @Order(1)로 설정합니다. 공용 파일을 수정하지 않기 위해 자체 패키지에 둡니다.
 */
@RestControllerAdvice
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
}
