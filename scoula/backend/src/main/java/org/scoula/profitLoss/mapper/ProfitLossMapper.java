package org.scoula.profitLoss.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.profitLoss.vo.ComparisonVO;
import org.scoula.profitLoss.vo.LoanProductRateVO;
import org.scoula.profitLoss.vo.UserDepositVO;

import java.util.List;

public interface ProfitLossMapper {

    // useGeneratedKeys로 comparison_id를 comparison.comparisonId에 채워 넣는다 (XML 참고)
    void insertComparison(ComparisonVO comparison);

    // 본인 이력만 조회 (WHERE comparison_id = ? AND user_id = ?)
    ComparisonVO selectComparisonById(@Param("comparisonId") Long comparisonId, @Param("userId") Long userId);

    // 본인 소유 예금만 조회 (WHERE user_deposit_id = ? AND user_id = ?). user_deposit 테이블: XML TODO 참고
    UserDepositVO selectUserDeposit(@Param("userDepositId") Long userDepositId, @Param("userId") Long userId);

    // 대출 상품별 rate_period(3/6/12)당 base/spread/우대금리 + 등급별 평균 가산금리(등급배율 산출용).
    // credit_loan_products / credit_loan_grade_rate 테이블: XML TODO 참고
    List<LoanProductRateVO> selectLoanProducts(@Param("productIds") List<Long> productIds, @Param("creditGrade") Integer creditGrade);
}
