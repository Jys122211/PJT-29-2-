/**
 * 보유 예금 화면 공통 포맷/검증 유틸.
 *
 * 날짜는 API 명세에 따라 yyyyMMdd 8자리 문자열로 주고받고,
 * 화면에는 YYYY-MM-DD 로 보여줍니다.
 */

/** "20251016" -> "2025-10-16" */
export function toDisplayDate(compact) {
  if (!compact || compact.length !== 8) return compact ?? '';
  return `${compact.slice(0, 4)}-${compact.slice(4, 6)}-${compact.slice(6, 8)}`;
}

/** "2025-10-16" 또는 "20251016" -> "20251016" */
export function toCompactDate(input) {
  return String(input ?? '').replace(/[^0-9]/g, '');
}

/** "20261016" -> "2026.10.16" (목록 카드 표기) */
export function toDotDate(compact) {
  if (!compact || compact.length !== 8) return compact ?? '';
  return `${compact.slice(0, 4)}.${compact.slice(4, 6)}.${compact.slice(6, 8)}`;
}

/** 1234567 -> "1,234,567" */
export function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '';
  return Number(value).toLocaleString('ko-KR');
}

/** "1,234,567원" -> 1234567 */
export function parseNumber(text) {
  const digits = String(text ?? '').replace(/[^0-9]/g, '');
  return digits === '' ? null : Number(digits);
}

/** 금리 입력 정리. "2.15%" -> "2.15" (소수점 3자리까지) */
export function sanitizeRate(text) {
  let cleaned = String(text ?? '').replace(/[^0-9.]/g, '');

  const parts = cleaned.split('.');
  if (parts.length > 2) {
    cleaned = `${parts[0]}.${parts.slice(1).join('')}`;
  }

  const [whole, decimal] = cleaned.split('.');
  const w = whole.slice(0, 2);

  // 20% 초과 입력 차단
  if (Number(w) > 20) return '20';

  if (decimal === undefined) return w;
  return `${w}.${decimal.slice(0, 3)}`;
}

/**
 * yyyyMMdd 문자열이 실제 존재하는 날짜인지 확인.
 * 20250230 같은 값을 걸러냅니다.
 */
export function isValidCompactDate(compact) {
  if (!/^\d{8}$/.test(compact ?? '')) return false;

  const year = Number(compact.slice(0, 4));
  const month = Number(compact.slice(4, 6));
  const day = Number(compact.slice(6, 8));

  if (month < 1 || month > 12) return false;

  const date = new Date(year, month - 1, day);
  return (
    date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day
  );
}

/**
 * 입력 시점에 커서 앞에 있던 숫자 개수.
 *
 * 포맷된 INPUT은 값을 다시 써넣을 때 커서가 맨 뒤로 초기화되므로,
 * 하이픈/콤마를 제외한 "숫자 몇 번째"를 기준으로 위치를 기억했다가 복원합니다.
 */
export function countDigitsBeforeCaret(input) {
  const cursorPosition = input.selectionStart ?? input.value.length;
  return input.value.slice(0, cursorPosition).replace(/[^0-9]/g, '').length;
}

/** countDigitsBeforeCaret 로 센 숫자 개수가 포맷 후 문자열에서 위치하는 지점 */
export function caretPositionAfterFormat(formattedValue, digitsBeforeCaret) {
  let position = 0;
  let digitCount = 0;

  while (position < formattedValue.length && digitCount < digitsBeforeCaret) {
    if (/[0-9]/.test(formattedValue[position])) {
      digitCount += 1;
    }
    position += 1;
  }

  return position;
}

/** 만기·가입일 판정은 기기 시간대와 무관하게 항상 한국 날짜를 기준으로 한다. */
const KST_DATE = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

/**
 * 오늘(한국 기준) 날짜를 yyyyMMdd 로 반환. 날짜 비교는 문자열 그대로 대소 비교하면 됩니다.
 *
 * en-CA 로케일이 YYYY-MM-DD 를 내주므로 하이픈만 걷어내면 된다.
 */
export function todayCompact() {
  return KST_DATE.format(new Date()).replace(/-/g, '');
}

/** yyyyMMdd 를 시간대 영향이 없는 UTC 기준 밀리초로. 날짜 간 일수 차이 계산용. */
function compactToUtcMillis(compact) {
  return Date.UTC(
    Number(compact.slice(0, 4)),
    Number(compact.slice(4, 6)) - 1,
    Number(compact.slice(6, 8)),
  );
}

/**
 * 만기일까지 남은 일수(한국 날짜 기준). 서버 dDay가 없을 때 폴백으로만 사용.
 *
 * "20261016" 과 "2026-10-16" 을 모두 받는다. HomePage가 쓰는
 * /api/deposits/list(UserDepositDTO.LocalDate)는 하이픈 형식으로,
 * 보유예금 목록이 쓰는 /api/deposits(DepositDTO.String)는 8자리로 내려오기 때문.
 *
 * new Date('2026-10-16') 은 명세상 UTC 자정으로 파싱되어 로컬 자정과 9시간
 * 어긋나므로 쓰지 않는다. 양쪽 모두 UTC 기준으로 맞춘 뒤 빼면 시간대가 상쇄된다.
 */
export function calcDDay(maturity) {
  const compact = toCompactDate(maturity);
  if (!isValidCompactDate(compact)) return null;

  const diff = compactToUtcMillis(compact) - compactToUtcMillis(todayCompact());
  return Math.round(diff / 86400000);
}

/** 만기일 배지 문구. 남았으면 D-30, 당일이면 D-Day, 지났으면 D+5. */
export function dDayText(maturity, fallback = '만기일') {
  const days = calcDDay(maturity);
  if (days === null) return fallback;

  if (days > 0) return `D-${days}`;
  if (days === 0) return 'D-Day';
  return `D+${Math.abs(days)}`;
}

/** 에러 응답에서 errorCode / field / message 추출 */
/** 서버 메시지 중 사용자용으로 정의된 것만 통과시킨다. */
export function extractApiError(error) {
  const data = error?.response?.data;

  if (data && typeof data === 'object' && data.errorCode && data.message) {
    return {
      errorCode: data.errorCode,
      field: data.field ?? null,
      message: data.message,
    };
  }

  // 문자열 본문, HTML 오류 페이지, 스택트레이스 등은 그대로 노출하지 않는다
  return {
    errorCode: null,
    field: null,
    message: '요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.',
  };
}
/** 숫자만 남긴다. '123-456-789012' → '123456789012' */
export function toCompactAccount(value) {
  return String(value ?? '').replace(/\D/g, '');
}

/** 화면 표시용 하이픈을 넣는다. '123456789012' → '123-456-789012' */
export function toDisplayAccount(value) {
  const d = toCompactAccount(value);
  if (d.length <= 3) return d;
  if (d.length <= 6) return `${d.slice(0, 3)}-${d.slice(3)}`;
  return `${d.slice(0, 3)}-${d.slice(3, 6)}-${d.slice(6)}`;
}

/** KB국민은행 일반 계좌 표시: 14자리 숫자 → 6자리-2자리-6자리 */
export function toDisplayKbAccount(value) {
  if (value === null || value === undefined || value === '') return '-';

  const digits = toCompactAccount(value).slice(0, 14);

  if (digits.length <= 6) return digits;
  if (digits.length <= 8) {
    return `${digits.slice(0, 6)}-${digits.slice(6)}`;
  }

  return `${digits.slice(0, 6)}-${digits.slice(6, 8)}-${digits.slice(8)}`;
}

