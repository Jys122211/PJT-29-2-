package org.scoula.profitLoss.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.profitLoss.dto.ComparisonRequest;
import org.scoula.profitLoss.dto.ComparisonResponse;
import org.scoula.profitLoss.enums.LoanType;
import org.scoula.profitLoss.mapper.ProfitLossMapper;
import org.scoula.profitLoss.service.ProfitLossServiceImpl;
import org.scoula.profitLoss.service.calculator.ComparisonCalculator;
import org.scoula.profitLoss.vo.ComparisonVO;
import org.scoula.profitLoss.vo.LoanProductRateVO;
import org.scoula.profitLoss.vo.UserDepositVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

// 인터페이스 계약서 2장 요청 형태를 조건1·가입1개월차 값으로 채워, Controller.create(POST) →
// Mapper 저장 → Controller.get(GET)까지 실제 코드 경로를 그대로 통과시킨다.
// Mapper만 목이고 나머지는 전부 실제 객체라 "Tomcat+MySQL로 켠 것"과 계산 결과는 동일하다.
@ExtendWith(MockitoExtension.class)
class ProfitLossControllerIntegrationTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private ProfitLossMapper mapper;

    @Test
    void condition1_at1Month_postThenGet_matchesSpecValues() {
        Long userId = 1L;

        when(mapper.selectUserDeposit(10L, userId)).thenReturn(UserDepositVO.builder()
                .userDepositId(10L)
                .userId(userId)
                .productName("KB Star 정기예금")
                .principal(30_000_000L)
                .maturityRate(new BigDecimal("3.2"))
                .baseRate(new BigDecimal("2.4"))
                .contractMonths(12)
                .joinDate(LocalDate.now(SEOUL).minusMonths(1))
                .build());
        when(mapper.selectLoanProducts(List.of(100L), 3)).thenReturn(List.of(
                loanRate(3, "4.81"), loanRate(6, "5.19"), loanRate(12, "5.71")));

        // insertComparison은 useGeneratedKeys 흉내: 넘어온 VO에 PK를 채워주고, 그 VO를 붙잡아뒀다가
        // selectComparisonById가 "저장된 값"으로 그대로 돌려주게 한다 (POST 결과 = DB에 실제로 남는 값).
        ArgumentCaptor<ComparisonVO> savedVo = ArgumentCaptor.forClass(ComparisonVO.class);
        doAnswer(invocation -> {
            ComparisonVO vo = invocation.getArgument(0);
            vo.setComparisonId(1L);
            return null;
        }).when(mapper).insertComparison(savedVo.capture());
        when(mapper.selectComparisonById(1L, userId)).thenAnswer(invocation -> savedVo.getValue());

        ProfitLossServiceImpl service = new ProfitLossServiceImpl(mapper);
        ProfitLossController controller = new ProfitLossController(service);

        // 계약서 2장 요청 구조 그대로, 값만 "조건1 · 가입1개월차"로 채움.
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

        var postResult = controller.create(request);
        assertEquals(201, postResult.getStatusCodeValue());
        var getResult = controller.get(postResult.getBody().getComparisonId());
        assertEquals(200, getResult.getStatusCodeValue());

        // POST 직후 응답과 GET 재조회 응답이 완전히 같아야 한다 (새로고침해도 같은 화면).
        assertResponseMatchesCondition1(postResult.getBody());
        assertResponseMatchesCondition1(getResult.getBody());
    }

    private void assertResponseMatchesCondition1(ComparisonResponse response) {
        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, response.getWinner());
        assertTrue(Math.abs(299_797L - response.getSavingAmount()) <= 1);
        assertEquals(20_000_000L, response.getUrgentAmount());
        assertEquals(900_000L, response.getMonthlyPayment());

        assertEquals("KB STAR 신용대출", response.getLoan().getName());
        assertEquals(LoanType.CREDIT, response.getLoan().getType());
        assertEquals(0, new BigDecimal("5.71").compareTo(response.getLoan().getInterestRate()));
        assertEquals(12, response.getLoan().getRatePeriodMonths());
        assertEquals(833_166L, response.getLoan().getInterest());
        assertEquals(6_661L, response.getLoan().getPenalty());
        assertEquals(839_827L, response.getLoan().getCost());
        assertEquals(Boolean.TRUE, response.getLoan().getIsRateEstimated());
        assertEquals(29_972_333L, response.getLoan().getFinalBalance());
        assertEquals(-27_667L, response.getLoan().getNetProfit());

        assertEquals("KB Star 정기예금", response.getDeposit().getName());
        assertEquals(812_160L, response.getDeposit().getMaintainInterest());
        assertEquals(0, new BigDecimal("0.100").compareTo(response.getDeposit().getCancelInterestRate()));
        assertEquals(1_410L, response.getDeposit().getCancelInterest());
        assertEquals(272_130L, response.getDeposit().getWithdrawalProfit());
        assertEquals(30_272_130L, response.getDeposit().getFinalBalance());

        assertEquals("신용대출", response.getBadges().getRecommended());
        assertEquals(Boolean.TRUE, response.getBadges().getIsPartialAllowed());
        assertEquals(Boolean.TRUE, response.getBadges().getIsLumpSum());

        // 절약금액(299,797)이 최저임금 하루치(82,560)보다 훨씬 커서 경고가 뜨면 안 된다.
        assertEquals(Boolean.FALSE, response.getWarning().getIsBelowMinimumWage());
        assertNull(response.getWarning().getMessage());
    }

    private static LoanProductRateVO loanRate(int ratePeriodMonths, String baseRate) {
        return LoanProductRateVO.builder()
                .loanProductId(100L)
                .productName("KB STAR 신용대출")
                .ratePeriodMonths(ratePeriodMonths)
                .baseRate(new BigDecimal(baseRate))
                .spreadRate(BigDecimal.ZERO)
                .loanLimit(50_000_000L)
                .build();
    }
}
