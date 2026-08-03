package org.scoula.deposit.exception;

/**
 * 예금이 없거나 다른 사용자의 예금인 경우.
 *
 * <p>존재 여부와 소유 여부를 구분하지 않습니다. "남의 예금이다"라고 알려주면
 * ID를 바꿔가며 타인의 데이터 존재를 탐색할 수 있기 때문입니다.
 */
public class DepositNotFoundException extends RuntimeException {

    public DepositNotFoundException() {
        super("해당 예금을 찾을 수 없습니다");
    }
}
