-- =========================================================================
--  득실 (쪼개미 2팀) — 시드 데이터
--
--  ※ 반드시 득실_통합_DDL.sql 을 먼저 실행한 뒤에 이 파일을 실행한다.
--
--  [출처]  각 담당자가 슬랙에 올린 더미를 FK 순서대로 합친 것
--    users / user_deposits          : 정성윤  (2026-07-25)
--    jeonse_*                       : 신윤지  (2026-07-24)
--    credit_loan_*                  : 조윤상  (2026-07-24)
--    user_deposits 6번(검산용)       : 안상우  — 계산 검증 목적으로 추가
--
--  [원본에서 보완한 것]
--    · user_deposits.global_id  : NOT NULL 인데 원본 INSERT 에 없어서 UUID() 로 채움
--    · user_deposits.created_by : NOT NULL 인데 원본 INSERT 에 없어서 1 로 채움
--    → is_deleted / created_at 은 DDL 에 DEFAULT 가 있어 생략해도 된다.
--
--  [comparisons 제외]
--    앱이 계산할 때마다 스스로 INSERT 하는 이력 테이블이라 시드가 필요 없다.
--
--  [재실행]
--    통합 DDL 을 다시 실행(테이블 초기화)한 뒤에 이 파일을 돌려야 한다.
--    DDL 없이 이 파일만 두 번 돌리면 데이터가 두 배로 쌓인다.
-- =========================================================================

USE scoula_db;


-- =========================================================================
-- 1. 사용자
-- =========================================================================

-- 1-1. users : 사용자 3명
INSERT INTO users (email, password_hash, name, credit_score, max_monthly_payment) VALUES
    ('seoyeon@kb.co.kr', '$2a$10$dummyhash1', '김서연', 942, 2500000),
    ('junho@kb.co.kr',   '$2a$10$dummyhash2', '박준호', 810, 1500000),
    ('hong@kb.co.kr',    '$2a$10$dummyhash3', '홍길동', 720, 1000000);


-- 1-2. user_deposits : 보유 예금 5건
--      applied_rate = 우대 포함 적용금리 / base_rate = 기본금리(중도해지 계산 기준)
INSERT INTO user_deposits
    (global_id, user_id, bank_name, product_name, join_date, maturity_date,
     applied_rate, base_rate, principal_amount, created_by)
VALUES
    (UUID(), 1, 'KB은행', 'KB Star 정기예금',   '2025-10-16', '2026-10-16', 3.50, 2.40, 20000000, 1),
    (UUID(), 1, 'KB은행', '쪼개미 예금',        '2026-01-10', '2027-01-10', 3.20, 2.20,  3600000, 1),
    (UUID(), 2, 'KB은행', '쪼개미 자유예금',     '2026-07-01', '2026-08-15', 4.00, 3.00, 12000000, 2),
    (UUID(), 3, 'KB은행', 'KB 세이프박스',       '2026-06-01', '2027-06-01', 2.00, 1.50,  5000000, 3),
    (UUID(), 3, 'KB은행', 'KB 먼저 이자 예금',   '2026-05-20', '2027-05-20', 3.80, 2.80,  8000000, 3);


-- 1-3. user_deposits : 계산 검산용 (user_deposit_id = 6)
--      득실 계산 로직 명세서 「조건1」 재현용
--        원금 3,000만 / 적용금리 3.2% / 기본금리 2.4% / 계약 12개월
--        가입일을 "오늘 - 1개월" 로 넣어 경과월수가 1개월이 되게 한다
--      → 필요금액 2,000만 · 월상환 90만 으로 계산하면
--        명세서의 조건1 · 1개월차 결과와 맞아야 한다
INSERT INTO user_deposits
    (global_id, user_id, bank_name, product_name, join_date, maturity_date,
     applied_rate, base_rate, principal_amount, created_by)
VALUES
    (UUID(), 1, 'KB은행', 'KB Star 정기예금(검산용)',
     DATE_SUB(CURDATE(), INTERVAL 1 MONTH),
     DATE_ADD(CURDATE(), INTERVAL 11 MONTH),
     3.20, 2.40, 30000000, 1);


-- =========================================================================
-- 2. 전세대출
--    크롤링 원본 : LN20000026 (KB 주택전세자금대출, 은행재원 협약보증)
-- =========================================================================

-- 2-1. jeonse_product : 상품 1건
INSERT INTO jeonse_product
    (product_name, bank_name, max_loan_limit, detail_url, max_preferential_rate)
VALUES
    ('KB 주택전세자금대출 (은행재원 협약보증)',
     'KB국민은행',
     444000000,
     'https://obank.kbstar.com/quics?page=C103507&cc=b104363:b104516&isNew=N&prcode=LN20000026&QSL=F',
     1.40);

SET @jeonse_product_id = LAST_INSERT_ID();


-- 2-2. jeonse_rate_option : 금리유형 6개
--      최종금리 = base_rate + spread_rate − 우대금리
INSERT INTO jeonse_rate_option (product_id, rate_type, base_rate, spread_rate) VALUES
    (@jeonse_product_id, '신규COFIX6개월',   3.05, 2.16),
    (@jeonse_product_id, '신규COFIX12개월',  3.05, 2.27),
    (@jeonse_product_id, '잔액COFIX6개월',   2.94, 2.34),
    (@jeonse_product_id, '잔액COFIX12개월',  2.94, 2.34),
    (@jeonse_product_id, '신잔액COFIX6개월', 2.54, 2.99),
    (@jeonse_product_id, '신잔액COFIX12개월',2.54, 2.96);


-- 2-3. jeonse_preferential_item : 우대항목 9개
--      ⚠ 1~3번은 KB카드 이용실적 30/60/90만원 구간으로 서로 배타적이다.
--        90만원 사용자는 3개 모두 충족되지만 실제로는 0.3 하나만 적용된다.
--        단순 SUM 하면 0.6 이 되어 상품 상한(1.40)을 넘는다.
--        → 합계를 만드는 쪽에서 condition_name 이 같은 그룹은 MAX 하나만 잡아야 한다.
INSERT INTO jeonse_preferential_item (condition_name, condition_detail, preferential_rate)
VALUES ('KB국민카드(신용) 이용실적 우대', '결제계좌를 KB국민은행으로 지정하고 최근 3개월간 30만원 이상 이용실적이 있는 경우', 0.1);
SET @jpi1 = LAST_INSERT_ID();

INSERT INTO jeonse_preferential_item (condition_name, condition_detail, preferential_rate)
VALUES ('KB국민카드(신용) 이용실적 우대', '결제계좌를 KB국민은행으로 지정하고 최근 3개월간 60만원 이상 이용실적이 있는 경우', 0.2);
SET @jpi2 = LAST_INSERT_ID();

INSERT INTO jeonse_preferential_item (condition_name, condition_detail, preferential_rate)
VALUES ('KB국민카드(신용) 이용실적 우대', '결제계좌를 KB국민은행으로 지정하고 최근 3개월간 90만원 이상 이용실적이 있는 경우', 0.3);
SET @jpi3 = LAST_INSERT_ID();

INSERT INTO jeonse_preferential_item (condition_name, condition_detail, preferential_rate)
VALUES ('급여(연금)이체 실적 우대', '연 0.1%p ~ 연 0.3%p', 0.3);
SET @jpi4 = LAST_INSERT_ID();

INSERT INTO jeonse_preferential_item (condition_name, condition_detail, preferential_rate)
VALUES ('자동이체 거래실적 우대(3건 이상)', '아파트관리비/지로/금융결제원 CMS/펌뱅킹', 0.1);
SET @jpi5 = LAST_INSERT_ID();

INSERT INTO jeonse_preferential_item (condition_name, condition_detail, preferential_rate)
VALUES ('적립식예금 30만원 이상 계좌 보유 우대', '연 0.1%p', 0.1);
SET @jpi6 = LAST_INSERT_ID();

INSERT INTO jeonse_preferential_item (condition_name, condition_detail, preferential_rate)
VALUES ('KB스타뱅킹 이용실적 우대', 'KB스타뱅킹을 통한 이체실적이 있는 경우', 0.1);
SET @jpi7 = LAST_INSERT_ID();

INSERT INTO jeonse_preferential_item (condition_name, condition_detail, preferential_rate)
VALUES ('부동산 전자계약 우대', '연 0.2%p', 0.2);
SET @jpi8 = LAST_INSERT_ID();

INSERT INTO jeonse_preferential_item (condition_name, condition_detail, preferential_rate)
VALUES ('주택자금대출에 대한 취약차주 우대', '연 0.3%p', 0.3);
SET @jpi9 = LAST_INSERT_ID();


-- 2-4. jeonse_preferential_condition : 상품 ↔ 우대항목 매핑 9건
INSERT INTO jeonse_preferential_condition (product_id, preferential_item_id) VALUES
    (@jeonse_product_id, @jpi1),
    (@jeonse_product_id, @jpi2),
    (@jeonse_product_id, @jpi3),
    (@jeonse_product_id, @jpi4),
    (@jeonse_product_id, @jpi5),
    (@jeonse_product_id, @jpi6),
    (@jeonse_product_id, @jpi7),
    (@jeonse_product_id, @jpi8),
    (@jeonse_product_id, @jpi9);


-- 2-5. jeonse_eligibility_question : 자격질문 5개 (Figma 화면 문구)
INSERT INTO jeonse_eligibility_question (question_text)
VALUES ('현재 민법상 성년인 세대주 또는 세대주 이신가요?');
SET @jeq1 = LAST_INSERT_ID();

INSERT INTO jeonse_eligibility_question (question_text)
VALUES ('주택금융신용보증서 발급이 가능하신가요?');
SET @jeq2 = LAST_INSERT_ID();

INSERT INTO jeonse_eligibility_question (question_text)
VALUES ('전세보증금이 수도권 7억원(그 외 지역 5억원) 이하인 임대차계약을 체결하여 보증금의 5% 이상을 지급하셨나요?');
SET @jeq3 = LAST_INSERT_ID();

INSERT INTO jeonse_eligibility_question (question_text)
VALUES ('본인과 배우자 합산 주택 수가 1주택 이내이신가요?');
SET @jeq4 = LAST_INSERT_ID();


-- 2-6. jeonse_eligibility_condition : 상품 ↔ 자격질문 매핑 4건
INSERT INTO jeonse_eligibility_condition (product_id, eligibility_question_id) VALUES
    (@jeonse_product_id, @jeq1),
    (@jeonse_product_id, @jeq2),
    (@jeonse_product_id, @jeq3),
    (@jeonse_product_id, @jeq4);


-- =========================================================================
-- 3. 신용대출
-- =========================================================================

-- 3-1. credit_loan_products : 상품 1건
INSERT INTO credit_loan_products (product_name, loan_limit, max_discount_rate)
VALUES ('KB스타 신용대출Ⅱ(신규)', 350000000, 0.90);

SET @loan_product_id = LAST_INSERT_ID();


-- 3-2. credit_loan_grade_rate : 금리 12행 (3주기 × 4등급)
--      ⚠ 득실 계산기는 3 / 6 / 12개월을 전부 순회한다. 12행이 다 있어야 동작한다.
--      ⚠ 4등급 가산금리가 3등급보다 낮다 (1.90→1.50 / 1.91→1.51 / 1.94→1.53).
--        크롤링 원본 그대로 넣었다. 팀 확인 필요.
INSERT INTO credit_loan_grade_rate
    (loan_product_id, rate_period_months, credit_grade, base_rate, spread_rate)
VALUES
    -- 3개월물
    (@loan_product_id,  3, 1, 2.89, 1.33),
    (@loan_product_id,  3, 2, 2.90, 1.59),
    (@loan_product_id,  3, 3, 2.91, 1.90),
    (@loan_product_id,  3, 4, 2.96, 1.50),
    -- 6개월물
    (@loan_product_id,  6, 1, 3.26, 1.34),
    (@loan_product_id,  6, 2, 3.27, 1.60),
    (@loan_product_id,  6, 3, 3.28, 1.91),
    (@loan_product_id,  6, 4, 3.34, 1.51),
    -- 12개월물
    (@loan_product_id, 12, 1, 3.74, 1.36),
    (@loan_product_id, 12, 2, 3.76, 1.63),
    (@loan_product_id, 12, 3, 3.77, 1.94),
    (@loan_product_id, 12, 4, 3.84, 1.53);


-- 3-3. credit_loan_preferential_condition_question : 우대조건 질문 7개
--      ⚠ 1~3번이 KB카드 30/60/90만원 구간 (전세대출과 동일한 배타 구조)
INSERT INTO credit_loan_preferential_condition_question
    (credit_loan_preferential_condition_question_name,
     credit_loan_preferential_condition_question_detail)
VALUES
    ('KB신용카드 이용실적 우대',        '최근 3개월 30만원 이상 이용'),
    ('KB신용카드 이용실적 우대',        '최근 3개월 60만원 이상 이용'),
    ('KB신용카드 이용실적 우대',        '최근 3개월 90만원 이상 이용'),
    ('급여(연금)이체 관련 실적 우대',   '최근 3개월 동안 본인 명의의 KB국민은행 계좌로 건별 50만원 이상의 급여 또는 연금을 2회 이상 받은 경우'),
    ('적립식예금(30만원 이상) 보유 우대','KB국민은행 적립식예금 계좌에 30만원 이상의 잔액을 보유한 경우'),
    ('자동이체 실적 우대',              'KB국민은행 계좌에서 아파트 관리비, 공과금, 통신비 또는 보험료 등의 자동이체 실적이 3건 이상인 경우'),
    ('KB스타뱅킹 이용 우대',            'KB스타뱅킹을 통해 월 1건 이상 이체한 실적이 있는 경우');


-- 3-4. credit_loan_preferential_rate : 우대금리 7행
--      단순합 1.20 / 상품 상한(max_discount_rate) 0.90
--      → 카드 하위 2개(0.10+0.20) 제외하면 정확히 0.90 이 된다
INSERT INTO credit_loan_preferential_rate
    (loan_product_id, credit_loan_preferential_condition_question_id, discount_rate)
VALUES
    (@loan_product_id, 1, 0.10),
    (@loan_product_id, 2, 0.20),
    (@loan_product_id, 3, 0.30),
    (@loan_product_id, 4, 0.30),
    (@loan_product_id, 5, 0.10),
    (@loan_product_id, 6, 0.10),
    (@loan_product_id, 7, 0.10);


-- 3-5. credit_loan_qualification_condition_question : 자격조건 질문 2개
INSERT INTO credit_loan_qualification_condition_question (question_text) VALUES
    ('건강보험 자격득실확인서로 재직 확인이 가능한 근로소득자이신가요?'),
    ('현재 직장의 재직기간이 상품에서 요구하는 기준을 충족하시나요?');


-- 3-6. credit_loan_qualification_condition : 상품 ↔ 자격조건 매핑 2건
INSERT INTO credit_loan_qualification_condition
    (loan_product_id, credit_loan_qualification_condition_question_id)
VALUES
    (@loan_product_id, 1),
    (@loan_product_id, 2);


-- =========================================================================
-- 4. 입력 확인
-- =========================================================================
SELECT 'users'                       AS 테이블, COUNT(*) AS 건수, 3  AS 기대값 FROM users
UNION ALL SELECT 'user_deposits',                COUNT(*), 6  FROM user_deposits
UNION ALL SELECT 'jeonse_product',               COUNT(*), 1  FROM jeonse_product
UNION ALL SELECT 'jeonse_rate_option',           COUNT(*), 6  FROM jeonse_rate_option
UNION ALL SELECT 'jeonse_preferential_item',     COUNT(*), 9  FROM jeonse_preferential_item
UNION ALL SELECT 'jeonse_preferential_condition',COUNT(*), 9  FROM jeonse_preferential_condition
UNION ALL SELECT 'jeonse_eligibility_question',  COUNT(*), 5  FROM jeonse_eligibility_question
UNION ALL SELECT 'jeonse_eligibility_condition', COUNT(*), 5  FROM jeonse_eligibility_condition
UNION ALL SELECT 'credit_loan_products',         COUNT(*), 1  FROM credit_loan_products
UNION ALL SELECT 'credit_loan_grade_rate',       COUNT(*), 12 FROM credit_loan_grade_rate
UNION ALL SELECT 'credit_loan_pref_question',    COUNT(*), 7  FROM credit_loan_preferential_condition_question
UNION ALL SELECT 'credit_loan_preferential_rate',COUNT(*), 7  FROM credit_loan_preferential_rate
UNION ALL SELECT 'credit_loan_qual_question',    COUNT(*), 2  FROM credit_loan_qualification_condition_question
UNION ALL SELECT 'credit_loan_qual_condition',   COUNT(*), 2  FROM credit_loan_qualification_condition;


-- 검산용 예금(6번)이 제대로 들어갔는지 — 경과월수가 1 이어야 한다
SELECT user_deposit_id,
       product_name,
       principal_amount                                        AS 원금,
       applied_rate                                            AS 적용금리,
       base_rate                                               AS 기본금리,
       TIMESTAMPDIFF(MONTH, join_date, maturity_date)           AS 계약월수,
       TIMESTAMPDIFF(MONTH, join_date, CURDATE())               AS 경과월수
FROM user_deposits
WHERE user_deposit_id = 6;


-- 신용대출 3등급 금리 확인 — 4.81 / 5.19 / 5.71 이 나와야 한다
SELECT rate_period_months                AS 금리주기,
       base_rate                         AS 기본금리,
       spread_rate                       AS 가산금리,
       (base_rate + spread_rate)         AS 우대전_금리
FROM credit_loan_grade_rate
WHERE credit_grade = 3
ORDER BY rate_period_months;