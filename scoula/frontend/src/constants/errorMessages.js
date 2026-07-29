// 득실계산기_인터페이스_계약서.md 4장 에러 code → 사용자 문구.
// 백엔드가 아직 안 만든 코드도 계약서 기준으로 미리 채워둔다 (문서에 있는 만큼 그대로).
export const ERROR_MESSAGES = {
  PAYMENT_TOO_LOW: '월 상환 가능 금액이 너무 적습니다. 금액을 조정해 주세요.',
  EXCEED_LOAN_LIMIT: '필요 금액이 대출 상품의 한도를 초과했습니다. 금액을 조정해 주세요.',
  INVALID_INPUT: '입력값을 다시 확인해 주세요.',
  DEPOSIT_NOT_FOUND: '예금 정보를 찾을 수 없습니다.',
  LOAN_PRODUCT_NOT_FOUND: '선택한 대출 상품을 찾을 수 없습니다.',
  COMPARISON_NOT_FOUND: '비교 이력을 찾을 수 없습니다.',
  GRADE_RATE_UNAVAILABLE: '금리 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.',
};

export const DEFAULT_ERROR_MESSAGE = '알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';

// axios 에러에서 백엔드가 내려준 {code, message}를 읽어 문구를 고른다.
// code가 없거나 모르는 값이면 기본 문구로 대체한다.
export function resolveErrorMessage(error) {
  const code = error?.response?.data?.code;
  return ERROR_MESSAGES[code] ?? DEFAULT_ERROR_MESSAGE;
}
