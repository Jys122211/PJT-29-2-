package org.scoula.profitLoss.service;

import org.scoula.profitLoss.dto.ComparisonRequest;
import org.scoula.profitLoss.dto.ComparisonResponse;

public interface ProfitLossService {

    ComparisonResponse compare(Long userId, ComparisonRequest request);
}
