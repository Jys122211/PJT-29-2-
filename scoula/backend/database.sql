USE scoula_db;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS comparisons;

DROP TABLE IF EXISTS credit_loan_qualification_condition_history;
DROP TABLE IF EXISTS credit_loan_preferential_rate_history;
DROP TABLE IF EXISTS credit_loan_grade_rate_history;
DROP TABLE IF EXISTS credit_loan_product_history;

DROP TABLE IF EXISTS credit_loan_qualification_condition;
DROP TABLE IF EXISTS credit_loan_qualification_condition_question;
DROP TABLE IF EXISTS credit_loan_preferential_rate;
DROP TABLE IF EXISTS credit_loan_preferential_condition_question;
DROP TABLE IF EXISTS credit_loan_grade_rate;
DROP TABLE IF EXISTS credit_loan_product;

DROP TABLE IF EXISTS jeonse_eligibility_condition_history;
DROP TABLE IF EXISTS jeonse_preferential_condition_history;
DROP TABLE IF EXISTS jeonse_rate_option_history;
DROP TABLE IF EXISTS jeonse_product_history;

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
                       CONSTRAINT chk_users_is_deleted CHECK (is_deleted IN ('Y', 'N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자';


-- -------------------------------------------------------------------------
-- 1-2. user_deposits : 보유 예금
-- -------------------------------------------------------------------------
CREATE TABLE user_deposits (
                               user_deposit_id  BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '보유 예금 고유번호',
                               global_id        CHAR(36)     NOT NULL                          COMMENT '글로벌 ID (UUID)',
                               user_id          BIGINT       NOT NULL                          COMMENT '예금을 보유한 사용자',
                               bank_name        VARCHAR(50)  NOT NULL                          COMMENT '은행명',
                               product_name      VARCHAR(50)  NOT NULL                          COMMENT '상품명',
                               join_date        DATE         NOT NULL                          COMMENT '가입일',
                               maturity_date    DATE         NOT NULL                          COMMENT '만기일',
                               applied_rate     DECIMAL(5,3) NOT NULL                          COMMENT '적용금리(우대 포함, %)',
                               base_rate        DECIMAL(5,3) NOT NULL                          COMMENT '기본금리(중도해지 계산용, %)',
                               principal_amount BIGINT       NOT NULL                          COMMENT '가입금액(원)',
                               is_deleted       CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',
                               created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
                               created_by       BIGINT       NOT NULL                          COMMENT '등록자 (user_id)',
                               updated_at       DATETIME         NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
                               updated_by       BIGINT           NULL                          COMMENT '수정자 (user_id)',

                               PRIMARY KEY (user_deposit_id),
                               UNIQUE KEY uk_user_deposits_global (global_id),
                               INDEX idx_user_deposits_user (user_id),
                               CONSTRAINT chk_user_deposits_is_deleted CHECK (is_deleted IN ('Y', 'N')),
                               CONSTRAINT fk_user_deposits_user
                                   FOREIGN KEY (user_id) REFERENCES users (user_id)
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
                                max_preferential_rate DECIMAL(5,3) NOT NULL                          COMMENT '최대 우대금리(%)',
                                collected_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수집일자',

                                PRIMARY KEY (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 상품';


-- -------------------------------------------------------------------------
-- 2-2. jeonse_rate_option : 전세대출 금리옵션
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_rate_option (
                                    rate_option_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '금리옵션 ID',
                                    product_id     BIGINT       NOT NULL                COMMENT '전세대출 상품 ID',
                                    rate_type      VARCHAR(20)  NOT NULL                COMMENT '금리유형',
                                    base_rate      DECIMAL(5,3) NOT NULL                COMMENT '기준금리(%)',
                                    spread_rate    DECIMAL(5,3) NOT NULL                COMMENT '가산금리(%)',

                                    PRIMARY KEY (rate_option_id),
                                    INDEX idx_jeonse_rate_option_product (product_id),
                                    CONSTRAINT fk_jeonse_rate_option_product
                                        FOREIGN KEY (product_id) REFERENCES jeonse_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 금리옵션';


-- -------------------------------------------------------------------------
-- 2-3. jeonse_preferential_item : 전세대출 우대항목
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_preferential_item (
                                          preferential_item_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '우대항목 ID',
                                          condition_name       VARCHAR(100) NOT NULL                COMMENT '우대조건 이름',
                                          condition_detail     VARCHAR(255) NOT NULL                COMMENT '우대조건 상세설명',
                                          preferential_rate    DECIMAL(5,3) NOT NULL                COMMENT '우대금리(%)',

                                          PRIMARY KEY (preferential_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 우대항목';


-- -------------------------------------------------------------------------
-- 2-4. jeonse_preferential_condition : 전세대출 우대조건
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_preferential_condition (
                                               condition_id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '우대조건 ID',
                                               product_id           BIGINT NOT NULL                COMMENT '전세대출 상품 ID',
                                               preferential_item_id BIGINT NOT NULL                COMMENT '우대항목 ID',

                                               PRIMARY KEY (condition_id),
                                               INDEX idx_jeonse_pref_cond_product (product_id),
                                               INDEX idx_jeonse_pref_cond_item (preferential_item_id),
                                               CONSTRAINT fk_jeonse_pref_cond_product
                                                   FOREIGN KEY (product_id)           REFERENCES jeonse_product (product_id),
                                               CONSTRAINT fk_jeonse_pref_cond_item
                                                   FOREIGN KEY (preferential_item_id) REFERENCES jeonse_preferential_item (preferential_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 우대조건';


-- -------------------------------------------------------------------------
-- 2-5. jeonse_eligibility_question : 전세대출 자격질문
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_eligibility_question (
                                             eligibility_question_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '자격질문 ID',
                                             question_text           VARCHAR(200) NOT NULL                COMMENT '질문 내용',

                                             PRIMARY KEY (eligibility_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 자격질문';


-- -------------------------------------------------------------------------
-- 2-6. jeonse_eligibility_condition : 전세대출 자격조건
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_eligibility_condition (
                                              eligibility_condition_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '자격조건 ID',
                                              product_id               BIGINT NOT NULL                COMMENT '전세대출 상품 ID',
                                              eligibility_question_id  BIGINT NOT NULL                COMMENT '자격질문 ID',

                                              PRIMARY KEY (eligibility_condition_id),
                                              INDEX idx_jeonse_elig_cond_product (product_id),
                                              INDEX idx_jeonse_elig_cond_question (eligibility_question_id),
                                              CONSTRAINT fk_jeonse_elig_cond_product
                                                  FOREIGN KEY (product_id)              REFERENCES jeonse_product (product_id),
                                              CONSTRAINT fk_jeonse_elig_cond_question
                                                  FOREIGN KEY (eligibility_question_id) REFERENCES jeonse_eligibility_question (eligibility_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 자격조건';


-- =========================================================================
-- 3. 신용대출 DB
-- =========================================================================

-- -------------------------------------------------------------------------
-- 3-1. credit_loan_product : 신용대출 상품
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_product (
                                     loan_product_id   BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '상품 ID',
                                     product_name      VARCHAR(200) NOT NULL                          COMMENT '상품이름',
                                     loan_limit        BIGINT       NOT NULL                          COMMENT '대출 최고금액(원)',
                                     max_discount_rate DECIMAL(5,3) NOT NULL DEFAULT 0.000            COMMENT '최고 우대금리(%)',
                                     updated_at        DATETIME     NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP COMMENT '업데이트 날짜',

                                     PRIMARY KEY (loan_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 상품';


-- -------------------------------------------------------------------------
-- 3-2. credit_loan_grade_rate : 신용대출 등급·주기별 금리
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_grade_rate (
                                        loan_credit_grade_rate_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '등급별 금리 ID',
                                        loan_product_id           BIGINT       NOT NULL                COMMENT '신용대출 상품 ID',
                                        rate_period_months        INT          NOT NULL                COMMENT '금리변동주기(개월) : 3 | 6 | 12',
                                        credit_grade              INT          NOT NULL                COMMENT '신용등급',
                                        base_rate                 DECIMAL(5,3) NOT NULL                COMMENT '기본금리(%)',
                                        spread_rate               DECIMAL(5,3) NOT NULL                COMMENT '가산금리(%)',

                                        PRIMARY KEY (loan_credit_grade_rate_id),
                                        INDEX idx_clgr_lookup (loan_product_id, credit_grade, rate_period_months),
                                        CONSTRAINT fk_clgr_product
                                            FOREIGN KEY (loan_product_id) REFERENCES credit_loan_product (loan_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 등급·주기별 금리';


-- -------------------------------------------------------------------------
-- 3-3. credit_loan_preferential_condition_question : 신용대출 우대조건 질문
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_preferential_condition_question (
                                                             credit_loan_preferential_condition_question_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '우대조건 질문 ID',
                                                             credit_loan_preferential_condition_question_name   VARCHAR(100) NOT NULL                COMMENT '우대조건 이름',
                                                             credit_loan_preferential_condition_question_detail VARCHAR(300) NOT NULL                COMMENT '우대조건 상세설명',

                                                             PRIMARY KEY (credit_loan_preferential_condition_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 우대조건 질문';


-- -------------------------------------------------------------------------
-- 3-4. credit_loan_preferential_rate : 신용대출 우대금리
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_preferential_rate (
                                               credit_loan_preferential_rate_id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '우대금리 ID',
                                               loan_product_id                                BIGINT       NOT NULL                COMMENT '신용대출 상품 ID',
                                               credit_loan_preferential_condition_question_id BIGINT       NOT NULL                COMMENT '우대조건 질문 ID',
                                               discount_rate                                  DECIMAL(5,3) NOT NULL                COMMENT '우대금리(%)',

                                               PRIMARY KEY (credit_loan_preferential_rate_id),
                                               INDEX idx_clpr_product (loan_product_id),
                                               INDEX idx_clpr_question (credit_loan_preferential_condition_question_id),
                                               CONSTRAINT fk_clpr_product
                                                   FOREIGN KEY (loan_product_id) REFERENCES credit_loan_product (loan_product_id),
                                               CONSTRAINT fk_clpr_question
                                                   FOREIGN KEY (credit_loan_preferential_condition_question_id)
                                                       REFERENCES credit_loan_preferential_condition_question (credit_loan_preferential_condition_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 우대금리';


-- -------------------------------------------------------------------------
-- 3-5. credit_loan_qualification_condition_question : 신용대출 자격조건 질문
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_qualification_condition_question (
                                                              qualification_condition_question_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '자격조건 질문 ID',
                                                              question_text                       VARCHAR(300) NOT NULL                COMMENT '신청 조건',

                                                              PRIMARY KEY (qualification_condition_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 자격조건 질문';


-- -------------------------------------------------------------------------
-- 3-6. credit_loan_qualification_condition : 신용대출 자격조건
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_qualification_condition (
                                                     qualification_condition_id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '자격조건 ID',
                                                     loan_product_id                     BIGINT NOT NULL                COMMENT '신용대출 상품 ID',
                                                     qualification_condition_question_id BIGINT NOT NULL                COMMENT '자격조건 질문 ID',

                                                     PRIMARY KEY (qualification_condition_id),
                                                     INDEX idx_clqc_product (loan_product_id),
                                                     INDEX idx_clqc_question (qualification_condition_question_id),
                                                     CONSTRAINT fk_clqc_product
                                                         FOREIGN KEY (loan_product_id) REFERENCES credit_loan_product (loan_product_id),
                                                     CONSTRAINT fk_clqc_question
                                                         FOREIGN KEY (qualification_condition_question_id)
                                                             REFERENCES credit_loan_qualification_condition_question (qualification_condition_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 자격조건';


-- =========================================================================
-- 4. 득실 비교 결과
-- =========================================================================

-- -------------------------------------------------------------------------
-- 4-1. comparisons : 득실 비교 결과 이력
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


-- =========================================================================
-- 5. 전세대출 이력 테이블
-- =========================================================================

-- -------------------------------------------------------------------------
-- 5-1. jeonse_product_history : 전세대출 상품 히스토리
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_product_history (
                                        jeonse_product_history_id BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '히스토리 ID',
                                        global_id                 CHAR(36)     NOT NULL                          COMMENT '글로벌 아이디',
                                        product_id                BIGINT       NOT NULL                          COMMENT '상품 ID',
                                        product_name              VARCHAR(100) NOT NULL                          COMMENT '상품명',
                                        bank_name                 VARCHAR(50)  NOT NULL                          COMMENT '은행명',
                                        max_loan_limit            BIGINT       NOT NULL                          COMMENT '최대 대출한도(원)',
                                        detail_url                VARCHAR(500) NOT NULL                          COMMENT '상세 URL',
                                        max_preferential_rate     DECIMAL(5,3) NOT NULL                          COMMENT '최대 우대금리(%)',
                                        created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초 등록일자',
                                        created_by                VARCHAR(100) NOT NULL                          COMMENT '최초 등록자',
                                        updated_at                DATETIME     NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
                                        updated_by                VARCHAR(100) NOT NULL                          COMMENT '수정자',
                                        is_deleted                CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',

                                        PRIMARY KEY (jeonse_product_history_id),
                                        INDEX idx_jph_product (product_id),
                                        CONSTRAINT chk_jph_is_deleted CHECK (is_deleted IN ('Y', 'N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 상품 히스토리';


-- -------------------------------------------------------------------------
-- 5-2. jeonse_rate_option_history : 전세대출 금리옵션 히스토리
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_rate_option_history (
                                            rate_history_id           BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '금리이력 ID',
                                            global_id                 CHAR(36)     NOT NULL                          COMMENT '글로벌 아이디',
                                            jeonse_product_history_id BIGINT       NOT NULL                          COMMENT '전세대출 상품 히스토리 ID',
                                            rate_option_id            BIGINT       NOT NULL                          COMMENT '금리옵션 ID',
                                            rate_type                 VARCHAR(20)  NOT NULL                          COMMENT '금리유형',
                                            base_rate                 DECIMAL(5,3) NOT NULL                          COMMENT '기준금리(%)',
                                            spread_rate               DECIMAL(5,3) NOT NULL                          COMMENT '가산금리(%)',
                                            created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초 등록일자',
                                            created_by                VARCHAR(100) NOT NULL                          COMMENT '최초 등록자',
                                            updated_at                DATETIME     NULL DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',
                                            updated_by                VARCHAR(100) NOT NULL                          COMMENT '수정자',
                                            is_deleted                CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',

                                            PRIMARY KEY (rate_history_id),
                                            INDEX idx_jroh_product_history (jeonse_product_history_id),
                                            CONSTRAINT chk_jroh_is_deleted CHECK (is_deleted IN ('Y', 'N')),
                                            CONSTRAINT fk_jroh_product_history
                                                FOREIGN KEY (jeonse_product_history_id) REFERENCES jeonse_product_history (jeonse_product_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 금리옵션 히스토리';


-- -------------------------------------------------------------------------
-- 5-3. jeonse_preferential_condition_history : 전세대출 우대조건 히스토리
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_preferential_condition_history (
                                                       condition_history_id      BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '우대조건 히스토리 ID',
                                                       global_id                 CHAR(36)     NOT NULL                          COMMENT '글로벌 아이디',
                                                       jeonse_product_history_id BIGINT       NOT NULL                          COMMENT '전세대출 상품 히스토리 ID',
                                                       condition_detail          VARCHAR(255) NOT NULL                          COMMENT '우대조건 상세설명',
                                                       preferential_rate         DECIMAL(5,3) NOT NULL                          COMMENT '우대금리(%)',
                                                       created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일자',
                                                       created_by                VARCHAR(100) NOT NULL                          COMMENT '생성자 이름',
                                                       updated_at                DATETIME     NULL DEFAULT CURRENT_TIMESTAMP
                                                           ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
                                                       updated_by                VARCHAR(100) NOT NULL                          COMMENT '수정자',
                                                       is_deleted                CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',

                                                       PRIMARY KEY (condition_history_id),
                                                       INDEX idx_jpch_product_history (jeonse_product_history_id),
                                                       CONSTRAINT chk_jpch_is_deleted CHECK (is_deleted IN ('Y', 'N')),
                                                       CONSTRAINT fk_jpch_product_history
                                                           FOREIGN KEY (jeonse_product_history_id) REFERENCES jeonse_product_history (jeonse_product_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 우대조건 히스토리';


-- -------------------------------------------------------------------------
-- 5-4. jeonse_eligibility_condition_history : 전세대출 자격조건 히스토리
-- -------------------------------------------------------------------------
CREATE TABLE jeonse_eligibility_condition_history (
                                                      eligibility_condition_history_id BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '자격조건 히스토리 ID',
                                                      global_id                        CHAR(36)     NOT NULL                          COMMENT '글로벌 아이디',
                                                      jeonse_product_history_id        BIGINT       NOT NULL                          COMMENT '전세대출 상품 히스토리 ID',
                                                      question_text                    VARCHAR(200) NOT NULL                          COMMENT '질문 내용',
                                                      created_at                       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일자',
                                                      created_by                       VARCHAR(100) NOT NULL                          COMMENT '생성자 이름',
                                                      updated_at                       DATETIME     NULL DEFAULT CURRENT_TIMESTAMP
                                                          ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
                                                      updated_by                       VARCHAR(100) NOT NULL                          COMMENT '수정자',
                                                      is_deleted                       CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',

                                                      PRIMARY KEY (eligibility_condition_history_id),
                                                      INDEX idx_jech_product_history (jeonse_product_history_id),
                                                      CONSTRAINT chk_jech_is_deleted CHECK (is_deleted IN ('Y', 'N')),
                                                      CONSTRAINT fk_jech_product_history
                                                          FOREIGN KEY (jeonse_product_history_id) REFERENCES jeonse_product_history (jeonse_product_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전세대출 자격조건 히스토리';


-- =========================================================================
-- 6. 신용대출 이력 테이블
-- =========================================================================

-- -------------------------------------------------------------------------
-- 6-1. credit_loan_product_history : 신용대출 상품 히스토리
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_product_history (
                                             credit_loan_product_history_id  BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '신용대출 히스토리 ID',
                                             global_id                       CHAR(36)     NOT NULL                          COMMENT '글로벌 아이디(UUID)',
                                             loan_product_id                 BIGINT       NOT NULL                          COMMENT '신용대출 상품 ID',
                                             product_name                    VARCHAR(200) NOT NULL                          COMMENT '상품이름',
                                             loan_limit                      BIGINT       NOT NULL                          COMMENT '대출 최고 금액(원)',
                                             max_discount_rate               DECIMAL(5,3) NOT NULL DEFAULT 0.000            COMMENT '최대 우대 금리(%)',
                                             created_at                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '상품 생성 일자',
                                             created_by                      VARCHAR(100) NOT NULL                          COMMENT '생성자 이름',
                                             updated_at                      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP
                                                 ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
                                             updated_by                      VARCHAR(100) NOT NULL                          COMMENT '수정자',
                                             is_deleted                      CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',

                                             PRIMARY KEY (credit_loan_product_history_id),
                                             INDEX idx_clph_product (loan_product_id),
                                             CONSTRAINT chk_clph_is_deleted CHECK (is_deleted IN ('Y', 'N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 상품 히스토리';


-- -------------------------------------------------------------------------
-- 6-2. credit_loan_grade_rate_history : 신용대출 등급·주기별 금리 히스토리
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_grade_rate_history (
                                                loan_credit_grade_rate_history_id BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '등급별 금리 히스토리 ID',
                                                credit_loan_product_history_id    BIGINT       NOT NULL                          COMMENT '신용대출 상품 히스토리 ID',
                                                rate_period_months                INT          NOT NULL                          COMMENT '금리변동주기(개월)',
                                                credit_grade                      INT          NOT NULL                          COMMENT '신용등급',
                                                base_rate                         DECIMAL(5,3) NOT NULL                          COMMENT '기본금리(%)',
                                                spread_rate                       DECIMAL(5,3) NOT NULL                          COMMENT '가산금리(%)',
                                                created_at                        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일자',
                                                created_by                        VARCHAR(100) NOT NULL                          COMMENT '생성자 이름',
                                                updated_at                        DATETIME     NULL DEFAULT CURRENT_TIMESTAMP
                                                    ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
                                                updated_by                        VARCHAR(100) NOT NULL                          COMMENT '수정자',
                                                is_deleted                        CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',

                                                PRIMARY KEY (loan_credit_grade_rate_history_id),
                                                INDEX idx_clgrh_product_history (credit_loan_product_history_id),
                                                CONSTRAINT chk_clgrh_is_deleted CHECK (is_deleted IN ('Y', 'N')),
                                                CONSTRAINT fk_clgrh_product_history
                                                    FOREIGN KEY (credit_loan_product_history_id)
                                                        REFERENCES credit_loan_product_history (credit_loan_product_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 등급·주기별 금리 히스토리';


-- -------------------------------------------------------------------------
-- 6-3. credit_loan_preferential_rate_history : 신용대출 우대금리 히스토리
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_preferential_rate_history (
                                                       credit_loan_preferential_rate_history_id            BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '우대금리 히스토리 ID',
                                                       credit_loan_product_history_id                      BIGINT       NOT NULL                          COMMENT '신용대출 상품 히스토리 ID',
                                                       credit_loan_preferential_condition_question_detail VARCHAR(300) NOT NULL                          COMMENT '우대조건 상세설명',
                                                       discount_rate                                       DECIMAL(5,3) NOT NULL                          COMMENT '우대금리(%)',
                                                       created_at                                          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일자',
                                                       created_by                                          VARCHAR(100) NOT NULL                          COMMENT '생성자 이름',
                                                       updated_at                                          DATETIME     NULL DEFAULT CURRENT_TIMESTAMP
                                                           ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
                                                       updated_by                                          VARCHAR(100) NOT NULL                          COMMENT '수정자',
                                                       is_deleted                                          CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',

                                                       PRIMARY KEY (credit_loan_preferential_rate_history_id),
                                                       INDEX idx_clprh_product_history (credit_loan_product_history_id),
                                                       CONSTRAINT chk_clprh_is_deleted CHECK (is_deleted IN ('Y', 'N')),
                                                       CONSTRAINT fk_clprh_product_history
                                                           FOREIGN KEY (credit_loan_product_history_id)
                                                               REFERENCES credit_loan_product_history (credit_loan_product_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 우대금리 히스토리';


-- -------------------------------------------------------------------------
-- 6-4. credit_loan_qualification_condition_history : 신용대출 자격조건 히스토리
-- -------------------------------------------------------------------------
CREATE TABLE credit_loan_qualification_condition_history (
                                                             credit_loan_qualification_condition_history_id BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '자격조건 히스토리 ID',
                                                             credit_loan_product_history_id                 BIGINT       NOT NULL                          COMMENT '신용대출 상품 히스토리 ID',
                                                             question_text                                   VARCHAR(300) NOT NULL                          COMMENT '신청 조건',
                                                             created_at                                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일자',
                                                             created_by                                      VARCHAR(100) NOT NULL                          COMMENT '생성자 이름',
                                                             updated_at                                      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP
                                                                 ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
                                                             updated_by                                      VARCHAR(100) NOT NULL                          COMMENT '수정자',
                                                             is_deleted                                      CHAR(1)      NOT NULL DEFAULT 'N'              COMMENT '삭제 여부 : Y | N',

                                                             PRIMARY KEY (credit_loan_qualification_condition_history_id),
                                                             INDEX idx_clqch_product_history (credit_loan_product_history_id),
                                                             CONSTRAINT chk_clqch_is_deleted CHECK (is_deleted IN ('Y', 'N')),
                                                             CONSTRAINT fk_clqch_product_history
                                                                 FOREIGN KEY (credit_loan_product_history_id)
                                                                     REFERENCES credit_loan_product_history (credit_loan_product_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신용대출 자격조건 히스토리';

-- =========================================================================
-- 더미 데이터 INSERT
-- (CREATE TABLE의 카테고리 및 번호 체계와 1:1로 완벽히 일치시켰습니다)
-- =========================================================================

USE scoula_db;

-- =========================================================================
-- 1. 사용자 DB
-- =========================================================================

-- 1-1. users : 사용자
INSERT INTO users (email, password_hash, name, credit_score, max_monthly_payment)
VALUES ('hong@kb.co.kr', '$2a$10$dummy...', '홍길동', 850, 1500000);

-- 1-2. user_deposits : 보유 예금
INSERT INTO user_deposits (global_id, user_id, bank_name, product_name, join_date, maturity_date, applied_rate, base_rate, principal_amount, created_by)
VALUES ('uuid-deposit-001', 1, 'KB국민은행', 'KB Star 정기예금', '2023-01-01', '2024-01-01', 3.500, 3.000, 50000000, 1);


-- =========================================================================
-- 2. 전세대출 DB
-- =========================================================================

-- 2-1. jeonse_product : 전세대출 상품
INSERT INTO jeonse_product (product_name, bank_name, max_loan_limit, detail_url, max_preferential_rate)
VALUES ('KB 청년 전세자금대출', 'KB국민은행', 200000000, 'https://kbstar.com/...', 1.500);

-- 2-2. jeonse_rate_option : 전세대출 금리옵션
INSERT INTO jeonse_rate_option (product_id, rate_type, base_rate, spread_rate)
VALUES (1, '신규 COFIX(6개월)', 3.800, 0.500);

-- 2-3. jeonse_preferential_item : 전세대출 우대항목
INSERT INTO jeonse_preferential_item (condition_name, condition_detail, preferential_rate)
VALUES ('급여이체 우대', 'KB국민은행 계좌로 매월 50만원 이상 급여 이체', 0.300);

-- 2-4. jeonse_preferential_condition : 전세대출 우대조건
INSERT INTO jeonse_preferential_condition (product_id, preferential_item_id)
VALUES (1, 1);

-- 2-5. jeonse_eligibility_question : 전세대출 자격질문
INSERT INTO jeonse_eligibility_question (question_text)
VALUES ('현재 무주택 세대주이십니까?');

-- 2-6. jeonse_eligibility_condition : 전세대출 자격조건
INSERT INTO jeonse_eligibility_condition (product_id, eligibility_question_id)
VALUES (1, 1);


-- =========================================================================
-- 3. 신용대출 DB
-- =========================================================================

-- 3-1. credit_loan_product : 신용대출 상품
INSERT INTO credit_loan_product (product_name, loan_limit, max_discount_rate)
VALUES ('KB 직장인 든든 신용대출', 100000000, 1.200);

-- 3-2. credit_loan_grade_rate : 신용대출 등급·주기별 금리
INSERT INTO credit_loan_grade_rate (loan_product_id, rate_period_months, credit_grade, base_rate, spread_rate)
VALUES (1, 6, 1, 4.500, 1.000);

-- 3-3. credit_loan_preferential_condition_question : 신용대출 우대조건 질문
INSERT INTO credit_loan_preferential_condition_question (credit_loan_preferential_condition_question_name, credit_loan_preferential_condition_question_detail)
VALUES ('신용카드 이용실적', 'KB국민카드 월 30만원 이상 결제 실적 보유');

-- 3-4. credit_loan_preferential_rate : 신용대출 우대금리
INSERT INTO credit_loan_preferential_rate (loan_product_id, credit_loan_preferential_condition_question_id, discount_rate)
VALUES (1, 1, 0.200);

-- 3-5. credit_loan_qualification_condition_question : 신용대출 자격조건 질문
INSERT INTO credit_loan_qualification_condition_question (question_text)
VALUES ('현재 직장에서 6개월 이상 재직 중이십니까?');

-- 3-6. credit_loan_qualification_condition : 신용대출 자격조건
INSERT INTO credit_loan_qualification_condition (loan_product_id, qualification_condition_question_id)
VALUES (1, 1);


-- =========================================================================
-- 4. 득실 비교 결과
-- =========================================================================

-- 4-1. comparisons : 득실 비교 결과 이력
INSERT INTO comparisons (user_id, user_deposit_id, urgent_amount, monthly_payment, is_partial_allowed, is_lump_sum,
                         loan_name, loan_type, loan_interest_rate, rate_period_months, loan_interest, loan_penalty,
                         deposit_name, deposit_maintain_interest, deposit_cancel_interest_rate, deposit_cancel_interest,
                         a_final_balance, b_final_balance, winner)
VALUES (1, 1, 20000000, 1000000, FALSE, TRUE,
        'KB 직장인 든든 신용대출', 'CREDIT', 5.300, 6, 1060000, 0,
        'KB Star 정기예금', 1750000, 1.000, 500000,
        48500000, 48940000, 'LOAN');


-- =========================================================================
-- 5. 전세대출 이력 테이블
-- =========================================================================

-- 5-1. jeonse_product_history : 전세대출 상품 히스토리
INSERT INTO jeonse_product_history (global_id, product_id, product_name, bank_name, max_loan_limit, detail_url, max_preferential_rate, created_by, updated_by)
VALUES ('uuid-jeonse-hist-001', 1, 'KB 청년 전세자금대출', 'KB국민은행', 200000000, 'https://kbstar.com/...', 1.500, 'system_admin', 'system_admin');

-- 5-2. jeonse_rate_option_history : 전세대출 금리옵션 히스토리
INSERT INTO jeonse_rate_option_history (global_id, jeonse_product_history_id, rate_option_id, rate_type, base_rate, spread_rate, created_by, updated_by)
VALUES ('uuid-jeonse-rate-hist-001', 1, 1, '신규 COFIX(6개월)', 3.800, 0.500, 'system_admin', 'system_admin');

-- 5-3. jeonse_preferential_condition_history : 전세대출 우대조건 히스토리
INSERT INTO jeonse_preferential_condition_history (global_id, jeonse_product_history_id, condition_detail, preferential_rate, created_by, updated_by)
VALUES ('uuid-jeonse-pref-hist-001', 1, 'KB국민은행 계좌로 매월 50만원 이상 급여 이체', 0.300, 'system_admin', 'system_admin');

-- 5-4. jeonse_eligibility_condition_history : 전세대출 자격조건 히스토리
INSERT INTO jeonse_eligibility_condition_history (global_id, jeonse_product_history_id, question_text, created_by, updated_by)
VALUES ('uuid-jeonse-elig-hist-001', 1, '현재 무주택 세대주이십니까?', 'system_admin', 'system_admin');


-- =========================================================================
-- 6. 신용대출 이력 테이블
-- =========================================================================

-- 6-1. credit_loan_product_history : 신용대출 상품 히스토리
INSERT INTO credit_loan_product_history (global_id, loan_product_id, product_name, loan_limit, max_discount_rate, created_by, updated_by)
VALUES ('uuid-credit-hist-001', 1, 'KB 직장인 든든 신용대출', 100000000, 1.200, 'system_admin', 'system_admin');

-- 6-2. credit_loan_grade_rate_history : 신용대출 등급·주기별 금리 히스토리
INSERT INTO credit_loan_grade_rate_history (credit_loan_product_history_id, rate_period_months, credit_grade, base_rate, spread_rate, created_by, updated_by)
VALUES (1, 6, 1, 4.500, 1.000, 'system_admin', 'system_admin');

-- 6-3. credit_loan_preferential_rate_history : 신용대출 우대금리 히스토리
INSERT INTO credit_loan_preferential_rate_history (credit_loan_product_history_id, credit_loan_preferential_condition_question_detail, discount_rate, created_by, updated_by)
VALUES (1, 'KB국민카드 월 30만원 이상 결제 실적 보유', 0.200, 'system_admin', 'system_admin');

-- 6-4. credit_loan_qualification_condition_history : 신용대출 자격조건 히스토리
INSERT INTO credit_loan_qualification_condition_history (credit_loan_product_history_id, question_text, created_by, updated_by)
VALUES (1, '현재 직장에서 6개월 이상 재직 중이십니까?', 'system_admin', 'system_admin');