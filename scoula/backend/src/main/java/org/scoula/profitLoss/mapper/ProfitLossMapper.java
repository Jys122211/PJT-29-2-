package org.scoula.profitLoss.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.profitLoss.domain.UserDepositVO;
import org.scoula.profitLoss.vo.ComparisonVO;
import org.scoula.profitLoss.vo.JeonseLoanProductVO;
import org.scoula.profitLoss.vo.LoanProductRateVO;

import java.math.BigDecimal;
import java.util.List;

public interface ProfitLossMapper {

    // ── 입력 화면 (조윤상)
    List<UserDepositVO> getDepositsByUserId(@Param("userId") Long userId);

    List<Long> selectQualifiedLoanProductIds(
            @Param("qualificationQuestionIds") List<Long> qualificationQuestionIds,
            @Param("qualificationQuestionIdCount") int qualificationQuestionIdCount
    );

    BigDecimal selectFinalDiscountRate(
            @Param("loanProductId") Long loanProductId,
            @Param("preferentialQuestionIds") List<Long> preferentialQuestionIds
    );

    // ── 득실 비교 (안상우)

    // useGeneratedKeys로 comparison_id를 comparison.comparisonId에 채워 넣는다 (XML 참고)
    void insertComparison(ComparisonVO comparison);

    // 본인 이력만 조회 (WHERE comparison_id = ? AND user_id = ?)
    ComparisonVO selectComparisonById(@Param("comparisonId") Long comparisonId, @Param("userId") Long userId);

    // 본인 소유 예금만 조회 (WHERE user_deposit_id = ? AND user_id = ?)
    UserDepositVO selectUserDeposit(@Param("userDepositId") Long userDepositId, @Param("userId") Long userId);

    // 대출 상품별 rate_period(3/6/12)당 base/spread/우대금리 + 등급별 평균 가산금리(등급배율 산출용).
    // credit_loan_products / credit_loan_grade_rate 테이블: XML TODO 참고
    List<LoanProductRateVO> selectLoanProducts(@Param("productIds") List<Long> productIds, @Param("creditGrade") Integer creditGrade);

    // 전세대출 상품별 COFIX 금리옵션(rate_type) 6종. jeonse_rate_option × jeonse_product 조인.
    List<JeonseLoanProductVO> selectJeonseLoanProducts(@Param("productIds") List<Long> productIds);
}
