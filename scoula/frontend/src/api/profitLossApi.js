import api from '@/api';
const BASE_URL = '/deposits/list';

export default {
  async getDeposits() {
    const { data } = await api.get(BASE_URL);
    return data;
  },
};
