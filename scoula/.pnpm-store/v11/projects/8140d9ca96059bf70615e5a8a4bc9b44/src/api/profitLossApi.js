import api from '@/api';

const DEPOSITS_URL = '/deposits/list';
const QUALIFIED_LOANS_URL = '/credit-loans/qualified';

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
};
