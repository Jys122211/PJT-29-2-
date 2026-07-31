package org.scoula.profitLoss.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.profitLoss.dto.JeonseEligibilityQuestionDTO;
import org.scoula.profitLoss.dto.JeonsePreferentialItemDTO;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface JeonseLoanMapper {
    List<JeonseEligibilityQuestionDTO> findAllEligibilityQuestions();
    List<JeonsePreferentialItemDTO> findAllPreferentialItems();
    
    // 전체 대출 상품 ID 목록 조회
    List<Long> findAllProductIds();
    
    // 특정 상품이 요구하는 자격조건 질문 ID 목록 조회
    List<Long> findEligibilityQuestionIdsByProductId(@Param("productId") Long productId);
    
    // 특정 상품과 우대조건 항목에 해당하는 우대금리 조회
    BigDecimal findPreferentialRateByItemAndProduct(@Param("productId") Long productId, @Param("preferentialItemId") Long preferentialItemId);
    
    // 특정 상품의 최대 우대 금리 조회
    BigDecimal findMaxPreferentialRate(@Param("productId") Long productId);
}
