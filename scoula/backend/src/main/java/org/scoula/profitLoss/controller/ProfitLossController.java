package org.scoula.profitLoss.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.profitLoss.dto.ComparisonRequest;
import org.scoula.profitLoss.dto.ComparisonResponse;
import org.scoula.profitLoss.dto.CreditLoanPreferentialRateRequestDTO;
import org.scoula.profitLoss.dto.CreditLoanQualificationRequestDTO;
import org.scoula.profitLoss.dto.UserDepositDTO;
import org.scoula.profitLoss.service.ComparisonNotFoundException;
import org.scoula.profitLoss.service.DepositNotFoundException;
import org.scoula.profitLoss.service.ExceedLoanLimitException;
import org.scoula.profitLoss.service.GradeRateUnavailableException;
import org.scoula.profitLoss.service.ProfitLossService;
import org.scoula.profitLoss.service.calculator.PaymentTooLowException;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 클래스 레벨 @RequestMapping을 두지 않는다 — 득실 비교(/api/comparisons)와 입력 화면
// (/deposits, /credit-loans)이 서로 다른 베이스 경로를 쓰기 때문에 메서드마다 전체 경로를 지정한다.
@RestController
@RequiredArgsConstructor
@Log4j2
public class ProfitLossController {

    private final ProfitLossService service;

    // TODO: member 시스템이 username(String)만 PK로 쓰고 있어 숫자 user_id가 아직 없다
    // (comparisons.user_id는 BIGINT). 보안/회원 파트에서 숫자 ID가 생기면 이 메서드를
    // @AuthenticationPrincipal CustomUser에서 실제 값을 뽑도록 교체한다.
    private Long currentUserId() {
        return 1L;
    }

    // ── 입력 화면 (조윤상)

    @GetMapping("/deposits/list")
    public ResponseEntity<List<UserDepositDTO>> getDeposits(Authentication authentication) {
        CustomUser authenticatedUser = (CustomUser) authentication.getPrincipal();
        Long userId = authenticatedUser.getMember().getUserId();
        return ResponseEntity.ok(service.getDeposits(userId));
    }

    @PostMapping("/credit-loans/qualified")
    public ResponseEntity<List<Long>> getQualifiedLoanProductIds(
            @RequestBody CreditLoanQualificationRequestDTO request
    ) {
        return ResponseEntity.ok(
                service.getQualifiedLoanProductIds(
                        request.getQualificationQuestionIds()
                )
        );
    }

    @PostMapping("/credit-loans/preferential-rate")
    public ResponseEntity<BigDecimal> getFinalDiscountRate(
            @RequestBody CreditLoanPreferentialRateRequestDTO request
    ) {
        return ResponseEntity.ok(
                service.getFinalDiscountRate(
                        request.getLoanProductId(),
                        request.getPreferentialQuestionIds()
                )
        );
    }

    // ── 득실 비교 (안상우)

    @PostMapping("/api/comparisons")
    public ResponseEntity<ComparisonResponse> create(@RequestBody ComparisonRequest request) {
        ComparisonResponse response = service.compare(currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/comparisons/{comparisonId}")
    public ResponseEntity<ComparisonResponse> get(@PathVariable Long comparisonId) {
        return ResponseEntity.ok(service.getComparison(currentUserId(), comparisonId));
    }

    @ExceptionHandler(PaymentTooLowException.class)
    public ResponseEntity<Map<String, String>> handlePaymentTooLow(PaymentTooLowException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody("PAYMENT_TOO_LOW", e.getMessage()));
    }

    @ExceptionHandler(ExceedLoanLimitException.class)
    public ResponseEntity<Map<String, String>> handleExceedLoanLimit(ExceedLoanLimitException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody("EXCEED_LOAN_LIMIT", e.getMessage()));
    }

    @ExceptionHandler(GradeRateUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleGradeRateUnavailable(GradeRateUnavailableException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody("GRADE_RATE_UNAVAILABLE", e.getMessage()));
    }

    @ExceptionHandler(DepositNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDepositNotFound(DepositNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("DEPOSIT_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(ComparisonNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleComparisonNotFound(ComparisonNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("COMPARISON_NOT_FOUND", e.getMessage()));
    }

    private Map<String, String> errorBody(String code, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        return body;
    }
}
