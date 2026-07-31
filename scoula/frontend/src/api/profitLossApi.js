import api from '@/api';

const DEPOSITS_URL = '/api/deposits/list';
const QUALIFIED_LOANS_URL = '/api/credit-loans/qualified';
const PREFERENTIAL_RATE_URL = '/api/credit-loans/preferential-rate';
const COMPARISONS_URL = '/api/comparisons';

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
    console.log('COMPARISON GET: ', data);
    return data;
  },
};
