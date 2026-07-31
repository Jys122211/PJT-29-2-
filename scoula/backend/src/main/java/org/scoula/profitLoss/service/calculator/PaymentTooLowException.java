package org.scoula.profitLoss.service.calculator;

// 인터페이스 계약서 4장 PAYMENT_TOO_LOW에 대응. 월납입이 첫 달 이자보다 적어 영구 미상환일 때 던진다.
public class PaymentTooLowException extends RuntimeException {

    public PaymentTooLowException(String message) {
        super(message);
    }
}
