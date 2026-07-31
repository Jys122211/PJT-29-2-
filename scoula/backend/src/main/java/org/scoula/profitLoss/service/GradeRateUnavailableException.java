package org.scoula.profitLoss.service;

// 인터페이스 계약서 4장 GRADE_RATE_UNAVAILABLE, 로직 명세서 STEP 3-3 필수 방어에 대응.
// API_가산(3등급)이 0이거나 없으면 등급배율(분모)이 정의되지 않는다.
public class GradeRateUnavailableException extends RuntimeException {

    public GradeRateUnavailableException(String message) {
        super(message);
    }
}
