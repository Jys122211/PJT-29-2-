package org.scoula.profitLoss.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.profitLoss.dto.ComparisonRequest;
import org.scoula.profitLoss.dto.ComparisonResponse;
import org.scoula.profitLoss.enums.LoanType;
import org.scoula.profitLoss.mapper.ProfitLossMapper;
import org.scoula.profitLoss.service.calculator.ComparisonCalculator;
import org.scoula.profitLoss.vo.LoanProductRateVO;
import org.scoula.profitLoss.vo.UserDepositVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfitLossServiceImplTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private ProfitLossMapper mapper;

    @InjectMocks
    private ProfitLossServiceImpl service;

    // 명세서 저자의 반올림 시점을 완벽히 재현하는 건 불가능할 수 있어 ±1원까지 허용한다 (ComparisonCalculatorTest와 동일 정책).
    private static void assertSavingAmount(long expected, long actual) {
        assertTrue(Math.abs(expected - actual) <= 1,
                () -> "savingAmount expected≈" + expected + " but was " + actual);
    }

    // 조건1 · 가입1개월차: 예금 3천만·만기3.2%·기본2.4%·계약12개월(1개월 전 가입) / 급전 2천만 / 부분해지O / 만기목돈상환O / 월납입90만
    @Test
    void compare_condition1_at1Month_returnsWithdrawal() {
        Long userId = 1L;

        UserDepositVO deposit = UserDepositVO.builder()
                .userDepositId(10L)
                .userId(userId)
                .productName("KB Star 정기예금")
                .principal(30_000_000L)
                .maturityRate(new BigDecimal("3.2"))
                .baseRate(new BigDecimal("2.4"))
                .contractMonths(12)
                .joinDate(LocalDate.now(SEOUL).minusMonths(1))
                .build();
        when(mapper.selectUserDeposit(10L, userId)).thenReturn(deposit);

        // totalDiscountRate=0, 등급배율=1(gradeAverageSpreadRate/baseGradeAverageSpreadRate 동일)이 되도록 목만들어서
        // baseRate 자체가 최종 대출금리가 되게 한다 — LoanRateResolverTest 등에서 이미 검증된 4.81/5.19/5.71을 그대로 재사용.
        List<LoanProductRateVO> loanRates = List.of(
                loanRate(3, "4.81"), loanRate(6, "5.19"), loanRate(12, "5.71")
        );
        when(mapper.selectLoanProducts(List.of(100L), 3)).thenReturn(loanRates);

        ComparisonRequest request = ComparisonRequest.builder()
                .userFinancialInfo(ComparisonRequest.UserFinancialInfo.builder()
                        .monthlyPayment(900_000L)
                        .creditGrade(3)
                        .build())
                .deposit(ComparisonRequest.DepositCondition.builder()
                        .userDepositId(10L)
                        .isPartialAllowed(true)
                        .build())
                .loan(ComparisonRequest.LoanCondition.builder()
                        .loanProductId(List.of(100L))
                        .loanType(LoanType.CREDIT)
                        .totalDiscountRate(BigDecimal.ZERO)
                        .build())
                .comparisonCondition(ComparisonRequest.ComparisonCondition.builder()
                        .urgentAmount(20_000_000L)
                        .isLumpSum(true)
                        .build())
                .build();

        ComparisonResponse response = service.compare(userId, request);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, response.getWinner());
        assertSavingAmount(299_798L, response.getSavingAmount());
    }

    private static LoanProductRateVO loanRate(int ratePeriodMonths, String baseRate) {
        return LoanProductRateVO.builder()
                .loanProductId(100L)
                .productName("KB STAR 신용대출")
                .loanType(LoanType.CREDIT)
                .ratePeriodMonths(ratePeriodMonths)
                .baseRate(new BigDecimal(baseRate))
                .spreadRate(BigDecimal.ZERO)
                .preferentialRate(BigDecimal.ZERO)
                .gradeAverageSpreadRate(BigDecimal.ONE)
                .baseGradeAverageSpreadRate(BigDecimal.ONE)
                .build();
    }
}
