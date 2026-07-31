package org.scoula.profitLoss.service;

// 인터페이스 계약서 4장 EXCEED_LOAN_LIMIT에 대응. 필요금액이 조회된 대출 상품들의 최고한도를 모두 넘을 때 던진다.
public class ExceedLoanLimitException extends RuntimeException {

    public ExceedLoanLimitException(String message) {
        super(message);
    }
}
