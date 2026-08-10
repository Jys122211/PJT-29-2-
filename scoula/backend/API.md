# scoula API 명세서

예금 중도해지와 대출 중 어느 쪽이 유리한지 비교하는 서비스의 REST API 명세입니다.

- **Base URL** : `http://localhost:8080`
- **형식** : 요청·응답 모두 `application/json` (OCR 업로드만 `multipart/form-data`)
- **인증** : JWT Bearer 토큰
  ```
  Authorization: Bearer {token}
  ```
- **날짜 형식**
  - 예금 CRUD API (`/api/deposits`) : `yyyyMMdd` 8자리 문자열 (예: `"20250901"`)
  - 득실 비교 API (`/api/comparisons`, `/api/deposits/list`) : ISO-8601 (예: `"2025-09-01"`, `"2025-09-01T14:30:00"`)
- **금액 단위** : 원 (정수)
- **금리 단위** : % (소수점 3자리, 예: `3.200`)

---

## 목차

1. [인증 (Auth)](#1-인증-auth)
2. [회원 (Users)](#2-회원-users)
3. [보유 예금 (Deposits)](#3-보유-예금-deposits)
4. [OCR](#4-ocr)
5. [신용대출 (Credit Loans)](#5-신용대출-credit-loans)
6. [전세대출 (Jeonse Loans)](#6-전세대출-jeonse-loans)
7. [득실 비교 (Comparisons)](#7-득실-비교-comparisons)
8. [공통 에러 코드](#8-공통-에러-코드)

---

## 1. 인증 (Auth)

### 1-1. 로그인

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/auth/login` |
| 인증 | 불필요 |

Spring Security 필터가 처리합니다.

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response `200 OK`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "userId": 1,
    "username": "user@example.com",
    "email": "user@example.com",
    "name": "김서연",
    "creditScore": 800,
    "maxMonthlyPayment": 700000,
    "roles": ["ROLE_USER"]
  }
}
```

**에러**

| 상태 | 설명 |
|---|---|
| `401` | 이메일 또는 비밀번호 불일치 |

---

### 1-2. 회원가입

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/auth/signup` |
| 인증 | 불필요 |

**Request**
```json
{
  "name": "김서연",
  "email": "user@example.com",
  "password": "password123"
}
```

**Response `201 Created`**
```json
{
  "userId": 1,
  "name": "김서연",
  "email": "user@example.com"
}
```

비밀번호는 해시로만 저장되며 응답에 포함되지 않습니다.

**에러**

| 상태 | 설명 |
|---|---|
| `400` | 필수값 누락 / 형식 오류 |
| `409` | 이미 가입된 이메일 |

---

## 2. 회원 (Users)

### 2-1. 내 프로필 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/users/me` |
| 인증 | **필요** |

**Response `200 OK`**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "name": "김서연",
  "creditScore": 800,
  "maxMonthlyPayment": 700000,
  "createdAt": "2025-07-01T10:00:00",
  "updatedAt": "2025-08-01T09:12:00",
  "authList": ["ROLE_USER"]
}
```

---

### 2-2. 신용점수 수정

| 항목 | 내용 |
|---|---|
| Method | `PATCH` |
| URL | `/api/users/me/credit-score` |
| 인증 | **필요** |

**Request**
```json
{ "creditScore": 820 }
```

**Response `200 OK`** — 수정된 프로필 (2-1과 동일 구조)

---

### 2-3. 월 상환 가능 금액 수정

| 항목 | 내용 |
|---|---|
| Method | `PATCH` |
| URL | `/api/users/me/max-monthly-payment` |
| 인증 | **필요** |

**Request**
```json
{ "maxMonthlyPayment": 700000 }
```

**Response `200 OK`** — 수정된 프로필

---

### 2-4. 이메일 중복 확인

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/users/checkemail/{email}` |
| 인증 | 불필요 |

**Response `200 OK`**
```json
true
```
`true` = 사용 가능, `false` = 이미 사용 중

---

### 2-5. 프로필 전체 수정

| 항목 | 내용 |
|---|---|
| Method | `PUT` |
| URL | `/api/users/me` |
| Content-Type | `multipart/form-data` (아바타 업로드 지원) |
| 인증 | **필요** |

**Request (form fields)**

| 필드 | 타입 | 설명 |
|---|---|---|
| `email` | string | 이메일 |
| `name` | string | 이름 |
| `creditScore` | int | 신용점수 |
| `maxMonthlyPayment` | long | 월 상환 가능 금액 |
| `avatar` | file | 프로필 이미지 (선택) |

**Response `200 OK`** — 수정된 프로필

---

### 2-6. 비밀번호 변경

| 항목 | 내용 |
|---|---|
| Method | `PUT` |
| URL | `/api/users/{email}/changepassword` |
| 인증 | **필요** |

**Request**
```json
{
  "email": "user@example.com",
  "oldPassword": "current123",
  "newPassword": "new12345"
}
```

**Response `200 OK`** — 본문 없음

**에러**

| 상태 | 설명 |
|---|---|
| `400` | 기존 비밀번호 불일치 |

---

### 2-7. 아바타 이미지 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/users/{email}/avatar` |
| 인증 | 불필요 |

**Response `200 OK`** — 이미지 바이너리 (`image/png`). 등록된 이미지가 없으면 기본 이미지를 반환합니다.

---

## 3. 보유 예금 (Deposits)

모든 엔드포인트에 인증이 필요하며, 본인 소유 예금만 접근할 수 있습니다.

### 3-1. 목록 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/deposits` |

**Response `200 OK`**
```json
{
  "count": 2,
  "totalPrincipal": 60000000,
  "deposits": [
    {
      "userDepositId": 1,
      "bankName": "KB국민은행",
      "productName": "KB Star 정기예금",
      "accountNumber": "11223310678928",
      "joinDate": "20250901",
      "maturityDate": "20260901",
      "principalAmount": 30000000,
      "baseRate": 2.400,
      "appliedRate": 3.200,
      "dDay": 26
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `count` | int | 보유 예금 건수 |
| `totalPrincipal` | long | 총 가입금액 합계(원) |
| `accountNumber` | string | 숫자만 저장 (하이픈 없음) |
| `baseRate` | decimal | 기본금리(%) — 중도해지 계산용 |
| `appliedRate` | decimal | 적용금리(%) — 우대 포함 |
| `dDay` | int | 만기까지 남은 일수 |

---

### 3-2. 보유 건수 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/deposits/count` |

**Response `200 OK`**
```json
{ "count": 2 }
```

---

### 3-3. 상세 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/deposits/{userDepositId}` |

**Response `200 OK`** — 3-1의 `deposits` 배열 요소와 동일 구조

**에러**

| 상태 | errorCode | 설명 |
|---|---|---|
| `404` | `DEPOSIT_NOT_FOUND` | 예금이 없거나 타인 소유 |

> 보안상 "존재하지 않음"과 "타인 소유"를 구분하지 않고 동일하게 404를 반환합니다.

---

### 3-4. 등록

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/deposits` |

**Request**
```json
{
  "bankName": "KB국민은행",
  "productName": "KB Star 정기예금",
  "accountNumber": "11223310678928",
  "joinDate": "20250901",
  "maturityDate": "20260901",
  "principalAmount": 30000000,
  "baseRate": 2.4,
  "appliedRate": 3.2
}
```

**검증 규칙**

| 필드 | 규칙 | errorCode |
|---|---|---|
| `bankName` | 필수 | `REQUIRED_FIELD` |
| `productName` | 필수 | `REQUIRED_FIELD` |
| `accountNumber` | 필수, 숫자 10~16자리 | `REQUIRED_FIELD` / `INVALID_ACCOUNT` |
| `joinDate` | 필수, `yyyyMMdd` | `REQUIRED_FIELD` / `INVALID_DATE` |
| `maturityDate` | 필수, `yyyyMMdd`, 가입일보다 이후 | `INVALID_DATE` |
| `principalAmount` | 필수, 0보다 큼 | `REQUIRED_FIELD` |
| `baseRate` | 필수 | `REQUIRED_FIELD` |
| `appliedRate` | 필수, 기본금리 이상 | `INVALID_RATE` |

**Response `201 Created`**
```json
{
  "userDepositId": 1,
  "message": "등록 완료"
}
```

**에러 `400`**
```json
{
  "errorCode": "INVALID_DATE",
  "field": "maturityDate",
  "message": "만기일이 가입일보다 빠릅니다"
}
```

`field`를 함께 내려주므로 프론트에서 해당 입력칸 아래에 메시지를 표시할 수 있습니다.

---

### 3-5. 수정

| 항목 | 내용 |
|---|---|
| Method | `PUT` |
| URL | `/api/deposits/{userDepositId}` |

**Request** — 3-4와 동일

**Response `200 OK`**
```json
{
  "userDepositId": 1,
  "message": "수정 완료"
}
```

---

### 3-6. 삭제

| 항목 | 내용 |
|---|---|
| Method | `DELETE` |
| URL | `/api/deposits/{userDepositId}` |

논리 삭제(`is_deleted = 'Y'`)로 처리됩니다.

**Response `200 OK`**
```json
{
  "userDepositId": 1,
  "message": "삭제 완료"
}
```

---

### 3-7. 비교용 예금 목록 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/deposits/list` |

득실 비교 입력 화면 전용 목록입니다. 3-1(자산 관리용)과 응답 구조가 다릅니다.

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "bankName": "KB국민은행",
    "productName": "KB Star 정기예금",
    "accountNumber": "11223310678928",
    "joinDate": "2025-09-01",
    "maturityDate": "2026-09-01",
    "interestRate": 3.200,
    "baseRate": 2.400,
    "balance": 30000000,
    "maturityText": "가입 11개월 경과"
  }
]
```

| 필드 | 설명 |
|---|---|
| `id` | 예금 고유번호 (`userDepositId`) |
| `interestRate` | 적용금리(%) |
| `balance` | 가입금액(원) |
| `maturityText` | "가입 N개월 경과" 표시 문구 |

---

## 4. OCR

### 4-1. 예금 정보 자동 추출

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/ocr/extract` |
| Content-Type | `multipart/form-data` |

은행 앱 캡쳐 이미지에서 예금 정보를 추출합니다. **추출만 하고 저장하지는 않습니다** — 사용자가 확인 후 3-4로 등록합니다.

**Request**

| 필드 | 타입 | 제약 |
|---|---|---|
| `file` | file | PNG / JPEG, 10MB 이하 |

**Response `200 OK`**
```json
{
  "bankName": "KB국민은행",
  "productName": "KB Star 정기예금",
  "accountNumber": "11223310678928",
  "principalAmount": 30000000,
  "joinDate": "20250901",
  "maturityDate": "20260901",
  "baseRate": 2.400,
  "appliedRate": 3.200
}
```

추출하지 못한 항목은 `null`로 내려옵니다.

**에러**

| 상태 | errorCode | 설명 |
|---|---|---|
| `400` | `INVALID_OCR_FILE` | 지원하지 않는 형식이거나 용량 초과 |
| `502` | `OCR_PROVIDER_ERROR` | 외부 OCR 서비스 호출 실패 |
| `502` | `OCR_RESPONSE_ERROR` | OCR 응답 파싱 실패 |
| `504` | `OCR_TIMEOUT` | 분석 시간 초과 |
| `500` | `OCR_FAILED` | 그 외 처리 오류 |

**에러 응답 형식**
```json
{
  "status": "FAILED",
  "errorCode": "OCR_TIMEOUT",
  "message": "이미지 분석 시간이 초과되었습니다."
}
```

---

## 5. 신용대출 (Credit Loans)

### 5-1. 자격조건 통과 상품 조회

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/credit-loans/qualified` |
| 인증 | **필요** |

사용자가 "예"라고 답한 자격질문 ID들을 보내면, 그 조건을 모두 만족하는 대출 상품 ID 목록을 반환합니다.

**Request**
```json
{ "qualificationQuestionIds": [1, 2, 5] }
```

**Response `200 OK`**
```json
[101, 102]
```

빈 배열이면 조건을 만족하는 상품이 없다는 뜻입니다.

---

### 5-2. 최종 우대금리 계산

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/credit-loans/preferential-rate` |
| 인증 | **필요** |

**Request**
```json
{
  "loanProductId": 101,
  "preferentialQuestionIds": [3, 7]
}
```

**Response `200 OK`**
```json
0.700
```
합산된 우대금리(%)입니다. 상품별 최대 우대금리를 넘지 않습니다.

---

## 6. 전세대출 (Jeonse Loans)

### 6-1. 자격질문 목록 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/jeonse-loans/eligibility-questions` |

**Response `200 OK`**
```json
[
  { "id": 1, "text": "만 19세 이상 무주택 세대주인가요?" }
]
```

---

### 6-2. 우대항목 목록 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/jeonse-loans/preferential-items` |

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "conditionName": "급여이체",
    "conditionDetail": "당행 계좌로 3개월 이상 급여이체 시",
    "preferentialRate": 0.300
  }
]
```

---

### 6-3. 자격조건 통과 상품 조회

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/jeonse-loans/qualified` |

**Request**
```json
{ "qualificationQuestionIds": [1, 3] }
```

**Response `200 OK`**
```json
[201, 202]
```

---

### 6-4. 최종 우대금리 계산

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/jeonse-loans/preferential-rate` |

**Request**
```json
{
  "loanProductId": 201,
  "preferentialQuestionIds": [1, 2]
}
```

**Response `200 OK`**
```json
0.500
```

---

## 7. 득실 비교 (Comparisons)

### 7-1. 비교 실행

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/comparisons` |
| 인증 | **필요** |

예금 중도해지와 대출 실행을 비교해 어느 쪽이 유리한지 계산하고, 결과를 이력으로 저장합니다.

**Request**
```json
{
  "userFinancialInfo": {
    "monthlyPayment": 700000,
    "creditGrade": 3
  },
  "deposit": {
    "userDepositId": 1,
    "isPartialAllowed": false
  },
  "loan": {
    "loanProductId": [101],
    "loanType": "CREDIT",
    "totalDiscountRate": 0.7
  },
  "comparisonCondition": {
    "urgentAmount": 5000000,
    "isLumpSum": true
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `monthlyPayment` | long | 월 상환 가능 금액(원) |
| `creditGrade` | int | 신용등급 (신용점수에서 환산) |
| `userDepositId` | long | 비교 대상 보유 예금 ID |
| `isPartialAllowed` | boolean | 부분해지(분할 인출) 가능 여부 |
| `loanProductId` | long[] | 대출 상품 ID 목록 |
| `loanType` | enum | `CREDIT` \| `JEONSE` |
| `totalDiscountRate` | decimal | 5-2 / 6-4에서 계산한 우대금리(%) |
| `urgentAmount` | long | 급하게 필요한 금액(원) |
| `isLumpSum` | boolean | 만기 예금으로 목돈 상환 여부 |

**Response `201 Created`**
```json
{
  "comparisonId": 15,
  "winner": "LOAN",
  "savingAmount": 182000,
  "urgentAmount": 5000000,
  "monthlyPayment": 700000,
  "createdAt": "2026-08-07T14:30:00",
  "badges": {
    "recommended": "대출",
    "isPartialAllowed": false,
    "isLumpSum": true
  },
  "warning": {
    "isBelowMinimumWage": false,
    "minimumWageDaily": 80240,
    "message": null
  },
  "loan": {
    "name": "KB 직장인든든 신용대출",
    "type": "CREDIT",
    "interestRate": 5.230,
    "ratePeriodMonths": 12,
    "interest": 131000,
    "penalty": 0,
    "cost": 131000,
    "isRateEstimated": true,
    "finalBalance": 30450000,
    "netProfit": 319000
  },
  "deposit": {
    "name": "KB Star 정기예금",
    "accountNumber": "11223310678928",
    "maintainInterest": 960000,
    "cancelInterestRate": 0.800,
    "cancelInterest": 240000,
    "withdrawalProfit": 137000
  }
}
```

**주요 필드**

| 필드 | 타입 | 설명 |
|---|---|---|
| `winner` | enum | `WITHDRAWAL`(해지 유리) \| `LOAN`(대출 유리) \| `TIE`(동일) |
| `savingAmount` | long | 유리한 쪽 선택 시 절약되는 금액(원) |
| `loan.cost` | long | 대출 총비용 = 이자 + 중도상환수수료 |
| `loan.isRateEstimated` | boolean | 실제 심사금리가 아닌 추정금리 여부 |
| `deposit.maintainInterest` | long | 만기까지 유지 시 받는 이자(원) |
| `deposit.cancelInterest` | long | 중도해지 시 받는 이자(원) |
| `warning.isBelowMinimumWage` | boolean | 상환 후 가처분소득이 최저임금 미만인지 |

**에러**

| 상태 | code | 설명 |
|---|---|---|
| `400` | `PAYMENT_TOO_LOW` | 월 상환 가능 금액으로는 대출 상환이 불가능 |
| `400` | `EXCEED_LOAN_LIMIT` | 필요 금액이 대출 한도 초과 |
| `404` | `DEPOSIT_NOT_FOUND` | 예금이 없거나 타인 소유 |
| `500` | `GRADE_RATE_UNAVAILABLE` | 해당 신용등급의 금리 정보 없음 |

**에러 응답 형식**
```json
{
  "code": "PAYMENT_TOO_LOW",
  "message": "월 상환 가능 금액이 부족합니다"
}
```

> 예금 API(`errorCode`/`field`)와 키 이름이 다릅니다. 비교 API는 `code`/`message`를 사용합니다.

---

### 7-2. 비교 이력 목록 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/comparisons` |
| 인증 | **필요** |

**Response `200 OK`**
```json
[
  {
    "comparisonId": 15,
    "createdAt": "2026-08-07T14:30:00",
    "urgentAmount": 5000000,
    "recommended": "대출",
    "savingAmount": 182000,
    "depositName": "KB Star 정기예금",
    "loanTypeLabel": "신용대출",
    "accountNumber": "11223310678928"
  }
]
```

---

### 7-3. 비교 이력 상세 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/comparisons/{comparisonId}` |
| 인증 | **필요** |

**Response `200 OK`** — 7-1의 응답과 동일 구조

**에러**

| 상태 | code | 설명 |
|---|---|---|
| `404` | `COMPARISON_NOT_FOUND` | 이력이 없거나 타인 소유 |

---

## 8. 공통 에러 코드

### 인증 관련

| 상태 | 설명 |
|---|---|
| `401 Unauthorized` | 토큰이 없거나 만료됨 |
| `403 Forbidden` | 접근 권한 없음 |

### 도메인별 에러 응답 형식

프로젝트 내에 두 가지 형식이 공존합니다.

**예금 / OCR** — `errorCode` 사용
```json
{
  "errorCode": "REQUIRED_FIELD",
  "field": "bankName",
  "message": "은행명을 입력해주세요"
}
```

**득실 비교** — `code` 사용
```json
{
  "code": "EXCEED_LOAN_LIMIT",
  "message": "필요 금액이 대출 한도를 초과합니다"
}
```

### 에러 코드 일람

| errorCode / code | 상태 | 발생 API |
|---|---|---|
| `REQUIRED_FIELD` | 400 | 예금 등록·수정 |
| `INVALID_ACCOUNT` | 400 | 예금 등록·수정 |
| `INVALID_DATE` | 400 | 예금 등록·수정 |
| `INVALID_RATE` | 400 | 예금 등록·수정 |
| `DEPOSIT_NOT_FOUND` | 404 | 예금 상세·수정·삭제, 비교 실행 |
| `INVALID_OCR_FILE` | 400 | OCR |
| `OCR_TIMEOUT` | 504 | OCR |
| `OCR_PROVIDER_ERROR` | 502 | OCR |
| `OCR_RESPONSE_ERROR` | 502 | OCR |
| `OCR_FAILED` | 500 | OCR |
| `PAYMENT_TOO_LOW` | 400 | 비교 실행 |
| `EXCEED_LOAN_LIMIT` | 400 | 비교 실행 |
| `GRADE_RATE_UNAVAILABLE` | 500 | 비교 실행 |
| `COMPARISON_NOT_FOUND` | 404 | 비교 이력 상세 |

---

## 부록: 전체 엔드포인트 요약

| # | Method | URL | 인증 | 설명 |
|---|---|---|---|---|
| 1-1 | POST | `/api/auth/login` | — | 로그인 |
| 1-2 | POST | `/api/auth/signup` | — | 회원가입 |
| 2-1 | GET | `/api/users/me` | ✓ | 내 프로필 조회 |
| 2-2 | PATCH | `/api/users/me/credit-score` | ✓ | 신용점수 수정 |
| 2-3 | PATCH | `/api/users/me/max-monthly-payment` | ✓ | 월 상환 가능 금액 수정 |
| 2-4 | GET | `/api/users/checkemail/{email}` | — | 이메일 중복 확인 |
| 2-5 | PUT | `/api/users/me` | ✓ | 프로필 전체 수정 |
| 2-6 | PUT | `/api/users/{email}/changepassword` | ✓ | 비밀번호 변경 |
| 2-7 | GET | `/api/users/{email}/avatar` | — | 아바타 이미지 |
| 3-1 | GET | `/api/deposits` | ✓ | 예금 목록 |
| 3-2 | GET | `/api/deposits/count` | ✓ | 예금 건수 |
| 3-3 | GET | `/api/deposits/{userDepositId}` | ✓ | 예금 상세 |
| 3-4 | POST | `/api/deposits` | ✓ | 예금 등록 |
| 3-5 | PUT | `/api/deposits/{userDepositId}` | ✓ | 예금 수정 |
| 3-6 | DELETE | `/api/deposits/{userDepositId}` | ✓ | 예금 삭제 |
| 3-7 | GET | `/api/deposits/list` | ✓ | 비교용 예금 목록 |
| 4-1 | POST | `/api/ocr/extract` | — | OCR 정보 추출 |
| 5-1 | POST | `/api/credit-loans/qualified` | ✓ | 신용대출 자격 통과 상품 |
| 5-2 | POST | `/api/credit-loans/preferential-rate` | ✓ | 신용대출 우대금리 |
| 6-1 | GET | `/jeonse-loans/eligibility-questions` | — | 전세대출 자격질문 |
| 6-2 | GET | `/jeonse-loans/preferential-items` | — | 전세대출 우대항목 |
| 6-3 | POST | `/jeonse-loans/qualified` | — | 전세대출 자격 통과 상품 |
| 6-4 | POST | `/jeonse-loans/preferential-rate` | — | 전세대출 우대금리 |
| 7-1 | POST | `/api/comparisons` | ✓ | 득실 비교 실행 |
| 7-2 | GET | `/api/comparisons` | ✓ | 비교 이력 목록 |
| 7-3 | GET | `/api/comparisons/{comparisonId}` | ✓ | 비교 이력 상세 |
