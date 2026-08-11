package org.scoula.profitLoss.constant;

import java.time.ZoneId;

// 예금 만기·경과월수 판정은 전부 이 타임존 기준이어야 한다. 클라이언트 로컬 타임존으로 판단하면
// 자정 부근에서 하루가 밀려 만기 당일 판정이 어긋난다.
public final class DepositTimeConstants {

    public static final ZoneId DEPOSIT_TIMEZONE = ZoneId.of("Asia/Seoul");

    private DepositTimeConstants() {
    }
}
