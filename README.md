# 득실 &nbsp;&nbsp;&nbsp;<img src="docs/assets/deukshil-logo.png" width="40"/> 

예금 해지 vs 대출, 숫자로 비교하는 손익 비교 계산기 서비스

## 소개

갑자기 목돈이 필요해진 순간, 예금을 깨야 할지 대출을 받아야 할지 정확하게 계산해주는 서비스가 없었습니다.
득실은 예금 중도해지 손실과 대출 이자·수수료를 같은 시점, 실제 손에 남는/나가는 금액 기준으로 비교해,
사용자가 스스로 답을 찾을 수 있도록 기획했습니다.

## 주요 기능

- **OCR 자산 등록** — 은행 앱 캡처 업로드만으로 7개 항목 자동 인식
- **신용대출·전세대출 손익 비교** — 부분해지·만기목돈상환 4가지 분기 지원
- **득실 히스토리 기록** — 비교 시점의 상품명·금리와 함께 저장되어 재확인 가능

## 기술 스택

| 구분 | 내용 |
|---|---|
| 프론트엔드 | Vue 3, Vite, Pinia, Vue Router |
| 백엔드 | Spring Legacy 5.3, MyBatis, Spring Security, JWT |
| DB / 외부연동 | MySQL 8.4, 금융감독원 API, Gemini API(OCR), Playwright/Jsoup |
| 협업 | Git & GitHub, Figma, Slack, draw.io, Notion, Google Docs |

## 팀 소개

- **팀명**: 쪼개미
- **개발기간**: 2026.07 ~ 2026.08
- **개발인원**: Full-stack 5명, Backend 1명 (총 6명) · DB는 전원 협업

## 실행 방법

### 프론트엔드

```bash
cd scoula/frontend
npm install
npm run dev
```

### 백엔드

Spring Legacy 기반 WAR 프로젝트입니다. IntelliJ 등 IDE에 Tomcat 서버를 설정해 실행하거나,
아래 명령으로 WAR를 빌드할 수 있습니다.

```bash
cd scoula/backend
./gradlew build
```

## 프로젝트 구조

```
kb-final/
├── scoula/
│   ├── frontend/   # Vue 3 + Vite
│   └── backend/    # Spring Legacy (WAR)
└── docs/           # 문서
```