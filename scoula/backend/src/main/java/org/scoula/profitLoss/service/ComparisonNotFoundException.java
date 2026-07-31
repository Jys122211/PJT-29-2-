package org.scoula.profitLoss.service;

// 인터페이스 계약서 4장 COMPARISON_NOT_FOUND에 대응. 이력이 없거나 본인 소유가 아닐 때 던진다.
public class ComparisonNotFoundException extends RuntimeException {

    public ComparisonNotFoundException(String message) {
        super(message);
    }
}
