import api from '@/api';

const DEPOSITS_URL = '/api/deposits/list';
const QUALIFIED_LOANS_URL = '/api/credit-loans/qualified';
const PREFERENTIAL_RATE_URL = '/api/credit-loans/preferential-rate';
const COMPARISONS_URL = '/api/comparisons';

// 전세대출
const JEONSE_ELIGIBILITY_URL = '/jeonse-loans/eligibility-questions';
const JEONSE_PREFERENTIAL_ITEMS_URL = '/jeonse-loans/preferential-items';
const JEONSE_QUALIFIED_URL = '/jeonse-loans/qualified';
const JEONSE_PREFERENTIAL_RATE_URL = '/jeonse-loans/preferential-rate';

export default {
  async getDeposits() {
    const { data } = await api.get(DEPOSITS_URL);
    return data;
  },

  async getUserFinancialInfo() {
    const { data } = await api.get('/api/users/me');
    return data;
  },

  async getQualifiedLoanProductIds(qualificationQuestionIds) {
    const { data } = await api.post(QUALIFIED_LOANS_URL, {
      qualificationQuestionIds,
    });

    return data;
  },

  async getFinalDiscountRate(loanProductId, preferentialQuestionIds) {
    const { data } = await api.post(PREFERENTIAL_RATE_URL, {
      loanProductId,
      preferentialQuestionIds,
    });

    return data;
  },

  async createComparison(requestPayload) {
    const { data } = await api.post(COMPARISONS_URL, requestPayload, {
      timeout: 30_000,
    });

    return data;
  },

  async getComparison(id) {
    const { data } = await api.get(`${COMPARISONS_URL}/${id}`);
    return data;
  },
  async getComparisons() {
    const { data } = await api.get(COMPARISONS_URL);
    return data;
  },

  // 전세대출 메서드 추가
  async getJeonseEligibilityQuestions() {
    const { data } = await api.get(JEONSE_ELIGIBILITY_URL);
    return data;
  },

  async getJeonsePreferentialItems() {
    const { data } = await api.get(JEONSE_PREFERENTIAL_ITEMS_URL);
    return data;
  },

  async getJeonseQualifiedLoanProductIds(qualificationQuestionIds) {
    const { data } = await api.post(JEONSE_QUALIFIED_URL, {
      qualificationQuestionIds,
    });
    return data;
  },

  async getJeonseFinalDiscountRate(loanProductId, preferentialQuestionIds) {
    const { data } = await api.post(JEONSE_PREFERENTIAL_RATE_URL, {
      loanProductId,
      preferentialQuestionIds,
    });
    return data;
  },
};
