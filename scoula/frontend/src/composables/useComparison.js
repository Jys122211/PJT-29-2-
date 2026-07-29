import { ref } from 'vue';

// TODO(5단계): 여기 목업을 실제 API 응답으로 교체한다. 이 파일만 바꾸면 되도록
// 컴포넌트는 useComparison()이 돌려주는 반응형 값만 읽는다 (mock을 직접 import하지 않는다).
// 득실계산기_인터페이스_계약서.md 3장 응답 JSON 예시를 그대로 옮긴 값이다.
const MOCK_COMPARISON = {
  comparisonId: 1,
  winner: 'LOAN',
  savingAmount: 133931,
  urgentAmount: 5000000,
  monthlyPayment: 1500000,
  createdAt: '2026-07-28T14:30:00',

  badges: {
    recommended: '신용대출',
    isPartialAllowed: true,
    isLumpSum: true,
  },

  warning: {
    isBelowMinimumWage: true,
    minimumWageDaily: 82560,
    message:
      '이 차액은 최저임금 하루치보다 적어요. 인지세, 보증료, 서류발급비, 교통비 등 부대비용까지 따져보면 실질적인 이득은 적을 수 있어요.',
  },

  loan: {
    name: 'KB STAR 신용대출',
    type: 'CREDIT',
    interestRate: 4.81,
    ratePeriodMonths: 3,
    interest: 80167,
    penalty: 20245,
    cost: 100411,
    isRateEstimated: true,
    finalBalance: 15711749,
    netProfit: 711749,
  },

  deposit: {
    name: 'KB Star 정기예금',
    maintainInterest: 812160,
    cancelInterestRate: 1.5,
    cancelInterest: 307098,
    withdrawalProfit: 577818,
    finalBalance: 15577818,
  },
};

export function useComparison() {
  const comparison = ref(null);
  const loading = ref(false);
  const error = ref(null);

  const fetchComparison = async (comparisonId) => {
    loading.value = true;
    error.value = null;
    try {
      // TODO(5단계): await api.get(`/api/comparisons/${comparisonId}`) 로 교체
      comparison.value = MOCK_COMPARISON;
    } catch (e) {
      error.value = e;
    } finally {
      loading.value = false;
    }
  };

  return { comparison, loading, error, fetchComparison };
}
