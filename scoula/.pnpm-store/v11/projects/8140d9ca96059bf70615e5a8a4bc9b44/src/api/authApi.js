import api from '@/api';

const BASE_URL = '/api/users';
const headers = { 'Content-Type': 'multipart/form-data' };

export default {
  // email 중복 체크, true: 중복(사용불가), false: 사용 가능
  async checkEmail(email) {
    const { data } = await api.get(`${BASE_URL}/checkemail/${email}`);
    console.log('AUTH GET CHECKEMAIL', data);
    return data;
  },

  async getMe() {
    const { data } = await api.get(`${BASE_URL}/me`);
    return data;
  },

  async create(member) {
    // 아바타 파일 업로드 – multipart 인코딩 필요  FormData 객체 사용

    const formData = new FormData();
    formData.append('email', member.email);
    formData.append('password', member.password);
    formData.append('name', member.name ?? '');
    if (member.creditScore != null) {
      formData.append('creditScore', member.creditScore);
    }
    if (member.maxMonthlyPayment != null) {
      formData.append('maxMonthlyPayment', member.maxMonthlyPayment);
    }

    if (member.avatar) {
      formData.append('avatar', member.avatar);
    }

    const { data } = await api.post(BASE_URL, formData, headers);

    console.log('AUTH POST: ', data);
    return data;
  },

  async update(member) {
    const formData = new FormData();
    formData.append('email', member.email);
    formData.append('name', member.name ?? '');
    if (member.creditScore != null) {
      formData.append('creditScore', member.creditScore);
    }
    if (member.maxMonthlyPayment != null) {
      formData.append('maxMonthlyPayment', member.maxMonthlyPayment);
    }

    if (member.avatar) {
      formData.append('avatar', member.avatar);
    }

    const { data } = await api.put(
      `${BASE_URL}/${member.email}`,
      formData,
      headers,
    );
    console.log('AUTH PUT: ', data);
    return data;
  },

  async changePassword(formData) {
    const { data } = await api.put(
      `${BASE_URL}/${formData.email}/changepassword`,
      formData,
    );
    console.log('AUTH PUT: ', data);

    return data;
  },
};
