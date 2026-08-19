-- =========================================================================
-- 보유예금 변경 이력 테이블 추가
--
-- 목적 : user_deposits 는 UPDATE 로 덮어써서 "무엇이 어떤 값에서 어떤 값으로
--        바뀌었는지"가 남지 않는다. 등록·수정·삭제 시점의 스냅샷을 쌓아 둔다.
--
-- 실행 : scoula_db 에 순서대로 실행. 애플리케이션 코드(DepositHistoryMapper)는
--        이미 이 테이블을 사용하도록 작성되어 있어 별도 수정이 필요 없다.
-- =========================================================================

USE scoula_db;


-- -------------------------------------------------------------------------
-- 1. 이력 테이블 생성
--
-- 설계 메모
--  · applied_rate / base_rate 는 DECIMAL(4,2). 금리 입력이 sanitizeRate 에서
--    소수점 2자리로 제한되므로 실제 저장되는 값의 정밀도에 맞춘다.
--
--  · user_deposit_id 에 FK 를 걸지 않는다. 이력은 원본이 사라져도 남아야 하고,
--    이 프로젝트의 상품 이력 테이블(jeonse_product_history 등)도 원본 PK 를
--    FK 없이 인덱스만 걸어 두는 방식으로 통일되어 있다.
--
--  · account_number 는 NULL 을 허용한다. 암호화 도입 이전에 등록된 예금은
--    계좌번호가 비어 있을 수 있고, 이력은 그 상태 그대로를 기록해야 한다.
-- -------------------------------------------------------------------------
CREATE TABLE user_deposit_history (
    history_id        BIGINT       NOT NULL AUTO_INCREMENT,
    deposit_global_id CHAR(36)     NOT NULL     COMMENT '보유예금 글로벌 ID',
    user_deposit_id   BIGINT       NOT NULL     COMMENT '원본 예금 ID (FK 아님)',
    user_id           BIGINT       NOT NULL     COMMENT '예금을 보유한 사용자',
    bank_name         VARCHAR(50)  NOT NULL     COMMENT '은행명',
    product_name      VARCHAR(50)  NOT NULL     COMMENT '상품명',
    account_number    VARCHAR(255)     NULL     COMMENT 'AES-256-GCM 암호문 그대로 복사',
    join_date         DATE         NOT NULL     COMMENT '가입일',
    maturity_date     DATE         NOT NULL     COMMENT '만기일',
    applied_rate      DECIMAL(4,2) NOT NULL     COMMENT '적용금리(우대 포함, %)',
    base_rate         DECIMAL(4,2) NOT NULL     COMMENT '기본금리(중도해지 계산용, %)',
    principal_amount  BIGINT       NOT NULL     COMMENT '가입금액(원)',
    is_deleted        CHAR(1)      NOT NULL     COMMENT '변경 시점의 삭제 상태 : Y | N',
    created_at        DATETIME     NOT NULL     COMMENT '원본 등록일자',
    created_by        BIGINT           NULL     COMMENT '원본 등록자 (user_id)',
    updated_at        DATETIME         NULL     COMMENT '원본 수정일자',
    updated_by        BIGINT           NULL     COMMENT '원본 수정자 (user_id)',
    change_type       VARCHAR(10)  NOT NULL     COMMENT 'I 등록 | U 수정 | D 삭제',
    changed_at        DATETIME(3)  NOT NULL     COMMENT '이력이 기록된 시각',
    changed_by        BIGINT           NULL     COMMENT '변경을 일으킨 사용자 (user_id)',

    PRIMARY KEY (history_id),
    KEY idx_udh_deposit (user_deposit_id, history_id),
    KEY idx_udh_user (user_id, changed_at),
    CONSTRAINT fk_udh_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_udh_change_type
        CHECK (change_type IN ('I', 'U', 'D'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '보유예금 변경 이력 (append-only)';


-- -------------------------------------------------------------------------
-- 2. 기존 예금의 최초 이력 적재
--
-- 이미 등록되어 있는 예금들을 'I'(등록)로 한 건씩 넣어 시작점을 만든다.
-- created_at 은 원본 등록일자를 그대로 옮기고, changed_at 은 이 마이그레이션을
-- 실행한 시각이 된다.
-- -------------------------------------------------------------------------
INSERT INTO user_deposit_history (
    deposit_global_id, user_deposit_id, user_id, bank_name, product_name,
    account_number, join_date, maturity_date, applied_rate, base_rate,
    principal_amount, is_deleted, created_at, created_by,
    updated_at, updated_by, change_type, changed_at, changed_by
)
SELECT global_id,
       user_deposit_id,
       user_id,
       bank_name,
       product_name,
       account_number,
       join_date,
       maturity_date,
       applied_rate,
       base_rate,
       principal_amount,
       is_deleted,
       created_at,
       created_by,
       updated_at,
       updated_by,
       'I',
       NOW(3),
       created_by
  FROM user_deposits;


-- -------------------------------------------------------------------------
-- 3. 검증
-- -------------------------------------------------------------------------

-- 3-1. 외래키 : fk_udh_user 1행만 나오면 정상
SELECT constraint_name, column_name, referenced_table_name
  FROM information_schema.key_column_usage
 WHERE table_schema        = 'scoula_db'
   AND table_name          = 'user_deposit_history'
   AND referenced_table_name IS NOT NULL;

-- 3-2. 건수 : 예금 수 = 이력 수 이면 정상
SELECT (SELECT COUNT(*) FROM user_deposits)        AS 예금,
       (SELECT COUNT(*) FROM user_deposit_history) AS 이력;

-- 3-3. 적재 결과 확인
SELECT history_id, user_deposit_id, product_name, applied_rate,
       is_deleted, change_type, changed_at, changed_by
  FROM user_deposit_history
 ORDER BY history_id DESC
 LIMIT 10;


-- -------------------------------------------------------------------------
-- 4. 동작 확인 (화면에서)
--
-- 예금 등록 → 금리 수정 → 삭제를 각각 1회 수행한 뒤 3-3 쿼리를 다시 실행하면
-- change_type 이 I / U / D 로 쌓여 있어야 한다.
-- 쌓이지 않으면 DepositServiceImpl 이 insertSnapshot 을 호출하는지 확인한다.
-- -------------------------------------------------------------------------
