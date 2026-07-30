import api from '@/api';

const DEPOSITS_URL = '/deposits/list';
const QUALIFIED_LOANS_URL = '/credit-loans/qualified';
const PREFERENTIAL_RATE_URL = '/credit-loans/preferential-rate';
const COMPARISONS_URL = '/api/comparisons';

export default {
  async getDeposits() {
    const { data } = await api.get(DEPOSITS_URL);
    return data;
  },

  async getQualifiedLoanProductIds(qualificationQuestionIds) {
    const { data } = await api.post(QUALIFIED_LOANS_URL, {
      qualificationQuestionIds,
    });

    return data;
  },

  async getFinalDiscountRate(
    loanProductId,
    preferentialQuestionIds,
  ) {
    const { data } = await api.post(PREFERENTIAL_RATE_URL, {
      loanProductId,
      preferentialQuestionIds,
    });

    return data;
  },

  async createComparison(payload) {
    const { data } = await api.post(COMPARISONS_URL, payload);
    console.log('COMPARISON POST: ', data);
    return data;
  },

  async getComparison(id) {
    const { data } = await api.get(`${COMPARISONS_URL}/${id}`);
    console.log('COMPARISON GET: ', data);
    return data;
  },
};
