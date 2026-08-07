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

  // 소수점 중복 제거
  const parts = cleaned.split('.');
  if (parts.length > 2) {
    cleaned = `${parts[0]}.${parts.slice(1).join('')}`;
  }

  const [whole, decimal] = cleaned.split('.');
  if (decimal === undefined) return whole.slice(0, 2);

  return `${whole.slice(0, 2)}.${decimal.slice(0, 3)}`;
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

/** 오늘 날짜를 yyyyMMdd 로 반환. 날짜 비교는 문자열 그대로 대소 비교하면 됩니다. */
export function todayCompact() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}${month}${day}`;
}

/** 만기일까지 남은 일수. 서버 dDay가 없을 때 폴백으로만 사용 */
export function calcDDay(maturityCompact) {
  if (!isValidCompactDate(maturityCompact)) return null;

  const target = new Date(
    Number(maturityCompact.slice(0, 4)),
    Number(maturityCompact.slice(4, 6)) - 1,
    Number(maturityCompact.slice(6, 8)),
  );
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  return Math.round((target - today) / 86400000);
}

/** 에러 응답에서 errorCode / field / message 추출 */
export function extractApiError(error) {
  const data = error?.response?.data ?? {};
  return {
    errorCode: typeof data === 'object' ? (data.errorCode ?? null) : null,
    field: typeof data === 'object' ? (data.field ?? null) : null,
    message:
      typeof data === 'string'
        ? data
        : (data.message ?? '요청을 처리하지 못했어요. 다시 시도해주세요.'),
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
  const digits = toCompactAccount(value).slice(0, 14);

  if (digits.length <= 6) return digits;
  if (digits.length <= 8) {
    return `${digits.slice(0, 6)}-${digits.slice(6)}`;
  }

  return `${digits.slice(0, 6)}-${digits.slice(6, 8)}-${digits.slice(8)}`;
}

