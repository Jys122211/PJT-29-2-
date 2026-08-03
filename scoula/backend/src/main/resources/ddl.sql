CREATE DATABASE IF NOT EXISTS scoula_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE scoula_db;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS comparisons;


DROP TABLE IF EXISTS credit_loan_qualification_condition;
DROP TABLE IF EXISTS credit_loan_qualification_condition_question;
DROP TABLE IF EXISTS credit_loan_preferential_rate;
DROP TABLE IF EXISTS credit_loan_preferential_condition_question;
DROP TABLE IF EXISTS credit_loan_grade_rate;
DROP TABLE IF EXISTS credit_loan_products;


DROP TABLE IF EXISTS jeonse_eligibility_condition;
DROP TABLE IF EXISTS jeonse_eligibility_question;
DROP TABLE IF EXISTS jeonse_preferential_condition;
DROP TABLE IF EXISTS jeonse_preferential_item;
DROP TABLE IF EXISTS jeonse_rate_option;
DROP TABLE IF EXISTS jeonse_product;

DROP TABLE IF EXISTS user_deposits;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;


-- =========================================================================
-- 1. 사용자 DB
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1-1. users : 사용자
-- -------------------------------------------------------------------------
CREATE TABLE users (
    user_id             BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '사용자 고유번호',
    email               VARCHAR(100) NOT NULL                          COMMENT '이메일 (로그인 ID)',
    password_hash       VARCHAR(255) NOT NULL                          COMMENT '비밀번호 해시',
    name                VARCHAR(50)  NOT NULL                          COMMENT '사용자 이름',
    credit_score        INT              NULL                          COMMENT '신용점수',
    max_monthly_payment BIGINT           NULL                          COMMENT '월 최대 상환 금액(원)',
    is_deleted          CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
    created_by          BIGINT           NULL                          COMMENT '등록자 (user_id)',
    updated_at          DATETIME         NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    updated_by          BIGINT           NULL                          COMMENT '수정자 (user_id)',

    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_created_by (created_by),
    KEY idx_users_updated_by (updated_by),
    CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users (user_id),
    CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자';


-- -------------------------------------------------------------------------
-- 1-2. user_deposits : 보유 예금
--      · base_rate  = 기본금리 (중도해지이율 계산의 기준)
--      · applied_rate = 우대 포함 실제 적용금리 (만기 수령액 계산용)
-- -------------------------------------------------------------------------
CREATE TABLE user_deposits (
    user_deposit_id  BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '보유 예금 고유번호',
    global_id        CHAR(36)     NOT NULL                          COMMENT '글로벌 ID (UUID)',
    user_id          BIGINT       NOT NULL                          COMMENT '예금을 보유한 사용자',
    bank_name        VARCHAR(50)  NOT NULL                          COMMENT '은행명',
    product_name     VARCHAR(50)  NOT NULL                          COMMENT '상품명',
    join_date        DATE         NOT NULL                          COMMENT '가입일',
    maturity_date    DATE         NOT NULL                          COMMENT '만기일',
    applied_rate     DECIMAL(4,2) NOT NULL                          COMMENT '적용금리(우대 포함, %)',
    base_rate        DECIMAL(4,2) NOT NULL                          COMMENT '기본금리(중도해지 계산용, %)',
    principal_amount BIGINT       NOT NULL                          COMMENT '가입금액(원)',
    is_deleted       CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
    created_by       BIGINT       NOT NULL                          COMMENT '등록자 (user_id)',
    updated_at       DATETIME         NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
    updated_by       BIGINT           NULL                          COMMENT '수정자 (user_id)',

    PRIMARY KEY (user_deposit_id),
    UNIQUE KEY uk_user_deposits_global_id (global_id),
    KEY idx_user_deposits_user (user_id),
    KEY idx_user_deposits_created_by (created_by),
    KEY idx_user_deposits_updated_by (updated_by),
    CONSTRAINT fk_user_deposits_user
        FOREIGN KEY (user_id)    REFERENCES users (user_id),
    CONSTRAINT fk_user_deposits_created_by
        FOREIGN KEY (created_by) REFERENCES users (user_id),
    CONSTRAINT fk_user_deposits_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='보유 예금';


-- =========================================================================
-- 2. 전세대출 DB
-- =========================================================================

-- -------------------------------------------------------------------------
-- 2-1. jeonse_product : 전세대출 상품
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_product (
    product_id            BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '상품 ID',
    product_name          VARCHAR(100) NOT NULL                          COMMENT '상품명',
    bank_name             VARCHAR(50)  NOT NULL                          COMMENT '은행명',
    max_loan_limit        BIGINT       NOT NULL                          COMMENT '최대 대출한도(원)',
    detail_url            VARCHAR(500) NOT NULL                          COMMENT '상세 URL',
    collected_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수집일자',
    max_preferential_rate DECIMAL(4,2) NOT NULL                          COMMENT '최대 우대금리(%) — 우대 합계의 상한',

    PRIMARY KEY (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 상품';


-- -------------------------------------------------------------------------
-- 2-2. jeonse_rate_option : 전세대출 금리옵션
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_rate_option (
    rate_option_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '금리옵션 ID',
    product_id     BIGINT       NOT NULL                COMMENT '전세대출 상품 ID',
    rate_type      VARCHAR(20)  NOT NULL                COMMENT '금리유형',
    base_rate      DECIMAL(4,2) NOT NULL                COMMENT '기준금리(%)',
    spread_rate    DECIMAL(4,2) NOT NULL                COMMENT '가산금리(%)',

    PRIMARY KEY (rate_option_id),
    CONSTRAINT fk_rate_option_product
        FOREIGN KEY (product_id) REFERENCES jeonse_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 금리옵션';


-- -------------------------------------------------------------------------
-- 2-3. jeonse_preferential_item : 전세대출 우대항목 (마스터)
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_preferential_item (
    preferential_item_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '우대항목 ID',
    condition_name       VARCHAR(100) NOT NULL                COMMENT '우대조건 이름',
    condition_detail     VARCHAR(255) NOT NULL                COMMENT '우대조건 상세설명',
    preferential_rate    DECIMAL(4,2) NOT NULL                COMMENT '우대금리(%)',

    PRIMARY KEY (preferential_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 우대항목';


-- -------------------------------------------------------------------------
-- 2-4. jeonse_preferential_condition : 상품 ↔ 우대항목 매핑
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_preferential_condition (
    condition_id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '우대조건 ID',
    product_id           BIGINT NOT NULL                COMMENT '전세대출 상품 ID',
    preferential_item_id BIGINT NOT NULL                COMMENT '우대항목 ID',

    PRIMARY KEY (condition_id),
    CONSTRAINT fk_preferential_condition_product
        FOREIGN KEY (product_id)           REFERENCES jeonse_product (product_id),
    CONSTRAINT fk_preferential_condition_item
        FOREIGN KEY (preferential_item_id) REFERENCES jeonse_preferential_item (preferential_item_id),
    CONSTRAINT uq_preferential_condition_product_item
        UNIQUE (product_id, preferential_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 우대조건';


-- -------------------------------------------------------------------------
-- 2-5. jeonse_eligibility_question : 전세대출 자격질문 (마스터)
--      ※ '득실 계산 조건' 2개(분할 인출, 만기 일시상환)는 프론트엔드에 하드코딩됨
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_eligibility_question (
    eligibility_question_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '자격질문 ID',
    question_text           VARCHAR(200) NOT NULL                COMMENT '질문 내용',

    PRIMARY KEY (eligibility_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 자격질문';


-- -------------------------------------------------------------------------
-- 2-6. jeonse_eligibility_condition : 상품 ↔ 자격질문 매핑
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_eligibility_condition (
    eligibility_condition_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '자격조건 ID',
    product_id               BIGINT NOT NULL                COMMENT '전세대출 상품 ID',
    eligibility_question_id  BIGINT NOT NULL                COMMENT '자격질문 ID',

    PRIMARY KEY (eligibility_condition_id),
    CONSTRAINT fk_eligibility_condition_product
        FOREIGN KEY (product_id)              REFERENCES jeonse_product (product_id),
    CONSTRAINT fk_eligibility_condition_question
        FOREIGN KEY (eligibility_question_id) REFERENCES jeonse_eligibility_question (eligibility_question_id),
    CONSTRAINT uq_eligibility_condition_product_question
        UNIQUE (product_id, eligibility_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 자격조건';


-- =========================================================================
-- 3. 신용대출 DB
-- =========================================================================

-- -------------------------------------------------------------------------
-- 3-1. credit_loan_products : 신용대출 상품
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_products (
    loan_product_id   BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '상품 ID',
    product_name      VARCHAR(200) NOT NULL                          COMMENT '상품이름',
    loan_limit        BIGINT       NOT NULL                          COMMENT '대출 최고금액(원)',
    max_discount_rate DECIMAL(4,2) NOT NULL DEFAULT 0.00             COMMENT '최고 우대금리(%)',
    updated_at        DATETIME         NULL DEFAULT CURRENT_TIMESTAMP COMMENT '업데이트 날짜',

    PRIMARY KEY (loan_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 상품';


-- -------------------------------------------------------------------------
-- 3-2. credit_loan_grade_rate
--      신용등급 × 금리변동주기 별 기본금리 / 가산금리
--      · 최종금리 = base_rate + spread_rate - (우대금리 합계)
--      · 득실 계산기는 (loan_product_id, credit_grade) 로 조회 후
--        rate_period_months 3 / 6 / 12 를 순회하며 비교한다
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_grade_rate (
    loan_credit_grade_rate_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '등급별 금리 ID',
    loan_product_id           BIGINT       NOT NULL                COMMENT '신용대출 상품 ID',
    rate_period_months        INT          NOT NULL                COMMENT '금리변동주기(개월) : 3 | 6 | 12',
    credit_grade              INT          NOT NULL                COMMENT '신용등급',
    base_rate                 DECIMAL(4,2) NOT NULL                COMMENT '기본금리(%)',
    spread_rate               DECIMAL(4,2) NOT NULL                COMMENT '가산금리(%)',

    PRIMARY KEY (loan_credit_grade_rate_id),
    INDEX idx_credit_loan_grade_rate_period (rate_period_months),
    INDEX idx_credit_loan_grade_rate_grade (credit_grade),
    CONSTRAINT fk_credit_loan_grade_rate_product
        FOREIGN KEY (loan_product_id) REFERENCES credit_loan_products (loan_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 등급·주기별 금리';


-- -------------------------------------------------------------------------
-- 3-3. credit_loan_preferential_condition_question : 우대조건 질문 (마스터)
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_preferential_condition_question (
    credit_loan_preferential_condition_question_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '우대조건 질문 ID',
    credit_loan_preferential_condition_question_name   VARCHAR(100) NOT NULL                COMMENT '우대조건 이름',
    credit_loan_preferential_condition_question_detail VARCHAR(300) NOT NULL                COMMENT '우대조건 상세설명',

    PRIMARY KEY (credit_loan_preferential_condition_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 우대조건 질문';


-- -------------------------------------------------------------------------
-- 3-4. credit_loan_preferential_rate : 상품 ↔ 우대조건 + 우대금리
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_preferential_rate (
    credit_loan_preferential_rate_id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '우대금리 ID',
    loan_product_id                                BIGINT       NOT NULL                COMMENT '신용대출 상품 ID',
    credit_loan_preferential_condition_question_id BIGINT       NOT NULL                COMMENT '우대조건 질문 ID',
    discount_rate                                  DECIMAL(4,2) NOT NULL                COMMENT '우대금리(%)',

    PRIMARY KEY (credit_loan_preferential_rate_id),
    CONSTRAINT fk_preferential_rate_product
        FOREIGN KEY (loan_product_id) REFERENCES credit_loan_products (loan_product_id),
    CONSTRAINT fk_preferential_rate_question
        FOREIGN KEY (credit_loan_preferential_condition_question_id)
        REFERENCES credit_loan_preferential_condition_question (credit_loan_preferential_condition_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 우대금리';


-- -------------------------------------------------------------------------
-- 3-5. credit_loan_qualification_condition_question : 자격조건 질문 (마스터)
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_qualification_condition_question (
    qualification_condition_question_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '자격조건 질문 ID',
    question_text                       VARCHAR(300) NOT NULL                COMMENT '신청 조건',

    PRIMARY KEY (qualification_condition_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 자격조건 질문';


-- -------------------------------------------------------------------------
-- 3-6. credit_loan_qualification_condition : 상품 ↔ 자격조건 매핑
--      ※ ERD에는 loan_product_id 가 두 번 적혀 있었다.
--        담당자 원본 DDL 기준으로 두 번째를
--        credit_loan_qualification_condition_question_id 로 확정.
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_qualification_condition (
    qualification_condition_id                      BIGINT NOT NULL AUTO_INCREMENT COMMENT '신용대출 자격조건 ID',
    loan_product_id                                 BIGINT NOT NULL                COMMENT '신용대출 상품 ID',
    credit_loan_qualification_condition_question_id BIGINT NOT NULL                COMMENT '신용대출 자격조건 질문 ID',

    PRIMARY KEY (qualification_condition_id),
    CONSTRAINT fk_qualification_condition_product
        FOREIGN KEY (loan_product_id) REFERENCES credit_loan_products (loan_product_id),
    CONSTRAINT fk_qualification_condition_question
        FOREIGN KEY (credit_loan_qualification_condition_question_id)
        REFERENCES credit_loan_qualification_condition_question (qualification_condition_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 자격조건';


-- =========================================================================
-- 4. 득실 비교 결과
-- =========================================================================

-- -------------------------------------------------------------------------
-- 4-1. comparisons : 득실 비교 결과 이력
--      한 번 생성되면 수정하지 않는 이력 테이블이라
--      updated_at / updated_by 를 두지 않는다.
-- -------------------------------------------------------------------------
CREATE TABLE comparisons (
    comparison_id                BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '비교 이력 ID',
    user_id                      BIGINT       NOT NULL                          COMMENT '사용자 ID',
    user_deposit_id              BIGINT       NOT NULL                          COMMENT '비교 대상 보유예금 ID',

    urgent_amount                BIGINT       NOT NULL                          COMMENT '급전 · 필요 금액(원)',
    monthly_payment              BIGINT       NOT NULL                          COMMENT '월 상환 가능 금액(원)',
    is_partial_allowed           BOOLEAN      NOT NULL                          COMMENT '부분해지(분할 인출) 가능 여부',
    is_lump_sum                  BOOLEAN      NOT NULL                          COMMENT '만기 예금으로 목돈 상환 여부',

    loan_name                    VARCHAR(200) NOT NULL                          COMMENT '대출 상품명 (비교 시점 스냅샷)',
    loan_type                    VARCHAR(10)  NOT NULL                          COMMENT '대출 종류 : CREDIT | JEONSE',
    loan_interest_rate           DECIMAL(5,3) NOT NULL                          COMMENT '적용 대출금리(%)',
    rate_period_months           INT          NOT NULL                          COMMENT '금리변동주기(개월) : 3 | 6 | 12',
    loan_interest                BIGINT       NOT NULL                          COMMENT '대출 총이자(원)',
    loan_penalty                 BIGINT       NOT NULL DEFAULT 0                COMMENT '중도상환수수료(원)',

    deposit_name                 VARCHAR(50)  NOT NULL                          COMMENT '예금 상품명 (비교 시점 스냅샷)',
    deposit_maintain_interest    BIGINT       NOT NULL                          COMMENT '예금 만기이자 (세후, 원)',
    deposit_cancel_interest_rate DECIMAL(5,3) NOT NULL                          COMMENT '중도해지이율(%)',
    deposit_cancel_interest      BIGINT       NOT NULL                          COMMENT '중도해지이자 (세후, 원)',

    a_final_balance              BIGINT       NOT NULL                          COMMENT 'A안 총자산 - 예금 깨기(원)',
    b_final_balance              BIGINT       NOT NULL                          COMMENT 'B안 총자산 - 대출(원)',
    winner                       VARCHAR(12)  NOT NULL                          COMMENT '판정 결과 : WITHDRAWAL | LOAN | TIE',

    created_at                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '비교 실행 일시',

    PRIMARY KEY (comparison_id),
    INDEX idx_comparisons_user_created (user_id, created_at DESC),
    CONSTRAINT chk_comparisons_loan_type CHECK (loan_type IN ('CREDIT', 'JEONSE')),
    CONSTRAINT chk_comparisons_winner    CHECK (winner IN ('WITHDRAWAL', 'LOAN', 'TIE')),
    CONSTRAINT fk_comparisons_user
        FOREIGN KEY (user_id)         REFERENCES users (user_id),
    CONSTRAINT fk_comparisons_deposit
        FOREIGN KEY (user_deposit_id) REFERENCES user_deposits (user_deposit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='득실 비교 결과 이력';

select * from users;