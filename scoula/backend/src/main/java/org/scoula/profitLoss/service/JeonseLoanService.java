package org.scoula.profitLoss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.profitLoss.dto.JeonseEligibilityQuestionDTO;
import org.scoula.profitLoss.dto.JeonsePreferentialItemDTO;
import org.scoula.profitLoss.mapper.JeonseLoanMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class JeonseLoanService {

    private final JeonseLoanMapper jeonseLoanMapper;

    public List<JeonseEligibilityQuestionDTO> getEligibilityQuestions() {
        return jeonseLoanMapper.findAllEligibilityQuestions();
    }

    public List<JeonsePreferentialItemDTO> getPreferentialItems() {
        return jeonseLoanMapper.findAllPreferentialItems();
    }

    public List<Long> getQualifiedLoanProductIds(List<Long> userQuestionIds) {
        // 1. 등록된 모든 대출 상품의 ID를 먼저 조회한다.
        List<Long> allProductIds = jeonseLoanMapper.findAllProductIds();
        List<Long> qualifiedProductIds = new ArrayList<>();

        // 2. 각 상품별로 반복하면서 요구하는 자격 조건을 조회하고, 사용자의 선택과 비교한다.
        for (Long productId : allProductIds) {
            List<Long> productRequiredIds = jeonseLoanMapper.findEligibilityQuestionIdsByProductId(productId);
            // 사용자가 선택한 질문 ID 리스트(userQuestionIds)가 상품의 요구 질문 ID를 모두 포함하는지 확인
            if (userQuestionIds.containsAll(productRequiredIds)) {
                qualifiedProductIds.add(productId);
            }
        }
        return qualifiedProductIds;
    }

    public BigDecimal getFinalDiscountRate(Long productId, List<Long> preferentialQuestionIds) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        
        // 1. 사용자가 선택한 우대항목들을 순회하며 해당 상품에 적용되는 우대금리를 각각 조회 및 합산한다.
        for (Long itemId : preferentialQuestionIds) {
            BigDecimal rate = jeonseLoanMapper.findPreferentialRateByItemAndProduct(productId, itemId);
            if (rate != null) {
                totalDiscount = totalDiscount.add(rate);
            }
        }
        
        // 2. 최대 한도를 초과하는지 검사
        BigDecimal maxRate = jeonseLoanMapper.findMaxPreferentialRate(productId);
        if (maxRate != null && totalDiscount.compareTo(maxRate) > 0) {
            return maxRate;
        }
        
        return totalDiscount;
    }
}
