import api from '@/api';

const BASE_URL = '/depositList';

export default {
  async getDeposits() {
    const { data } = await api.get(BASE_URL);
    return data;
  },
};
