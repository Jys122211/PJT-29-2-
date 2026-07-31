-- =========================================================
-- comparisons: 득실 비교 결과 이력
-- 담당: 안상우 (org.scoula.profitLoss)
-- 기준 문서: 득실 계산 로직 명세서.md / 득실계산기_인터페이스_계약서.md
-- =========================================================
CREATE TABLE comparisons (
    comparison_id                  BIGINT          NOT NULL AUTO_INCREMENT             COMMENT '비교 이력 ID',
    user_id                        BIGINT          NOT NULL                             COMMENT '사용자 ID (FK)',
    user_deposit_id                BIGINT          NOT NULL                             COMMENT '비교 대상 보유예금 (FK)',
    urgent_amount                  BIGINT          NOT NULL                             COMMENT '급전·필요 금액(원)',
    monthly_payment                BIGINT          NOT NULL                             COMMENT '월 상환 가능 금액(원)',
    is_partial_allowed             BOOLEAN         NOT NULL                             COMMENT '분할 인출(부분해지) 가능 여부',
    is_lump_sum                    BOOLEAN         NOT NULL                             COMMENT '만기 예금으로 상환 여부',
    loan_name                      VARCHAR(200)    NOT NULL                             COMMENT '대출 상품명 (스냅샷)',
    loan_type                      VARCHAR(10)     NOT NULL                             COMMENT '대출 종류: CREDIT | JEONSE',
    loan_interest_rate             DECIMAL(5,3)    NOT NULL                             COMMENT '적용 대출금리(%)',
    rate_period_months             INT             NOT NULL                             COMMENT '금리변동주기(개월): 3 | 6 | 12',
    loan_interest                  BIGINT          NOT NULL                             COMMENT '대출 총이자(원)',
    loan_penalty                   BIGINT          NOT NULL DEFAULT 0                   COMMENT '중도상환수수료(원)',
    deposit_name                   VARCHAR(50)     NOT NULL                             COMMENT '예금 상품명 (스냅샷)',
    deposit_maintain_interest      BIGINT          NOT NULL                             COMMENT '예금 만기이자(세후, 원)',
    deposit_cancel_interest_rate   DECIMAL(5,3)    NOT NULL                             COMMENT '중도해지이율(%)',
    deposit_cancel_interest        BIGINT          NOT NULL                             COMMENT '중도해지이자(세후, 원)',
    a_final_balance                BIGINT          NOT NULL                             COMMENT 'A안 총자산 - 예금 깨기(원)',
    b_final_balance                BIGINT          NOT NULL                             COMMENT 'B안 총자산 - 대출(원)',
    winner                         VARCHAR(12)     NOT NULL                             COMMENT '판정 결과: WITHDRAWAL | LOAN | TIE',
    created_at                     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '이력 생성 일시',

    PRIMARY KEY (comparison_id),

    INDEX idx_comparisons_user_created (user_id, created_at DESC),

    CONSTRAINT chk_comparisons_loan_type CHECK (loan_type IN ('CREDIT', 'JEONSE')),
    CONSTRAINT chk_comparisons_winner CHECK (winner IN ('WITHDRAWAL', 'LOAN', 'TIE'))

    -- FK 제약: users, user_deposit 테이블이 아직 없어 컬럼만 두고 제약은 보류.
    -- 테이블 생성 후 아래 주석 해제 (user_deposit 테이블명은 미결 사항 — ERD user_deposit vs SQL 메모 user_deposits 확인 필요)
    -- , CONSTRAINT fk_comparisons_user FOREIGN KEY (user_id) REFERENCES users (user_id)
    -- , CONSTRAINT fk_comparisons_user_deposit FOREIGN KEY (user_deposit_id) REFERENCES user_deposit (user_deposit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='득실 비교 결과 이력';
