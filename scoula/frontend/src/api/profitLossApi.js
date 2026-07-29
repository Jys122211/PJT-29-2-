import api from '@/api';

const BASE_URL = '/api/comparisons';

export default {
  async createComparison(payload) {
    const { data } = await api.post(BASE_URL, payload);
    console.log('COMPARISON POST: ', data);
    return data;
  },

  async getComparison(id) {
    const { data } = await api.get(`${BASE_URL}/${id}`);
    console.log('COMPARISON GET: ', data);
    return data;
  },
};
