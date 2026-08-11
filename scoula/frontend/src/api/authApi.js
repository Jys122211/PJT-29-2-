import api from '@/api';

export default {
  // 피그마 회원가입 화면에서 입력한 이름·이메일·비밀번호를 JSON으로 전송한다.
  async signup(payload) {
    const { data } = await api.post('/api/auth/signup', payload);
    return data;
  },

  // 비밀번호 찾기 1단계 - 가입된 이메일이면 6자리 인증번호를 메일로 보낸다.
  // 재발송(인증번호 다시 보내기)도 같은 함수를 쓰며, 이전 인증번호는 무효가 된다.
  // 가입되지 않은 이메일이면 404로 응답한다.
  async sendPasswordResetCode(email) {
    const { data } = await api.post('/api/auth/password/code', { email });
    return data;
  },

  // 비밀번호 찾기 2단계 - 인증번호를 검증하고 3단계에서 쓸 1회용 토큰을 받는다.
  async verifyPasswordResetCode(email, code) {
    const { data } = await api.post('/api/auth/password/verify', {
      email,
      code,
    });
    return data;
  },

  // 비밀번호 찾기 3단계 - 1회용 토큰과 새 비밀번호로 실제 변경을 요청한다.
  async resetPassword(resetToken, newPassword) {
    const { data } = await api.post('/api/auth/password/reset', {
      resetToken,
      newPassword,
    });
    return data;
  },
};
