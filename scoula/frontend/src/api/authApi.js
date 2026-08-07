import api from '@/api';

export default {
  // 피그마 회원가입 화면에서 입력한 이름·이메일·비밀번호를 JSON으로 전송한다.
  async signup(payload) {
    const { data } = await api.post('/api/auth/signup', payload);
    return data;
  },
};
