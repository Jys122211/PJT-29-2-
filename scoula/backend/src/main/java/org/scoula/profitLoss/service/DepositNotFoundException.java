package org.scoula.profitLoss.service;

// 인터페이스 계약서 4장 DEPOSIT_NOT_FOUND에 대응. userDepositId가 없거나 타인 소유일 때 던진다.
public class DepositNotFoundException extends RuntimeException {

    public DepositNotFoundException(String message) {
        super(message);
    }
}
