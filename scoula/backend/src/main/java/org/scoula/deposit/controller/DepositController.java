package org.scoula.deposit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.deposit.dto.DepositDTO;
import org.scoula.deposit.dto.DepositListDTO;
import org.scoula.deposit.dto.DepositRequestDTO;
import org.scoula.deposit.service.DepositService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 보유 예금 API (자산 등록 - 화면 07-01 ~ 07-11).
 *
 * <p>요청/응답은 JSON입니다. board의 등록 API는 multipart라 @RequestBody가 없지만
 * 예금 등록/수정은 JSON이므로 @RequestBody가 필수입니다. (OCR만 multipart)
 *
 * <p>ProfitLossController의 GET /deposits/list 와 역할이 다릅니다.
 * 저쪽은 득실 계산기용 조회이고, 이쪽은 자산 관리 CRUD입니다.
 */
@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
@Log4j2
public class DepositController {

    private final DepositService service;

    /** 1. 목록 조회 - 화면 02-03, 07-09 */
    @GetMapping("")
    public ResponseEntity<DepositListDTO> getList() {
        return ResponseEntity.ok(service.getList());
    }

    /** 2. 개수 - 화면 07-01 우상단 뱃지 */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCount() {
        Map<String, Integer> body = new LinkedHashMap<>();
        body.put("count", service.getCount());

        return ResponseEntity.ok(body);
    }

    /** 3. 상세 - 화면 07-10 진입 */
    @GetMapping("/{userDepositId}")
    public ResponseEntity<DepositDTO> get(@PathVariable Long userDepositId) {
        return ResponseEntity.ok(service.get(userDepositId));
    }

    /** 4. 등록 - 화면 07-01, 07-04, 07-07 */
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody DepositRequestDTO request) {

        Long userDepositId = service.create(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userDepositId", userDepositId);
        body.put("message", "등록 완료");

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** 5. 수정 - 화면 07-10 */
    @PutMapping("/{userDepositId}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long userDepositId,
            @RequestBody DepositRequestDTO request) {

        service.update(userDepositId, request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userDepositId", userDepositId);
        body.put("message", "수정 완료");

        return ResponseEntity.ok(body);
    }

    /** 6. 삭제 - 화면 07-11 */
    @DeleteMapping("/{userDepositId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long userDepositId) {
        service.delete(userDepositId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userDepositId", userDepositId);
        body.put("message", "삭제 완료");

        return ResponseEntity.ok(body);
    }
}
