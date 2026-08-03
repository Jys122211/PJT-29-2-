import axios from 'axios';
import { useAuthStore } from '@/stores/auth';

// 모든 일반 API 요청에서 공통으로 사용할 Axios 인스턴스이다.
const instance = axios.create({
  timeout: 10000,
});

// 요청 인터셉터: 로그인 상태라면 모든 API 요청 헤더에 JWT를 자동으로 붙인다.
instance.interceptors.request.use(
  (config) => {
    // JWT 추출
    const { getToken } = useAuthStore();
    const token = getToken();
    if (token) {
      // 백엔드는 "Bearer {JWT}" 형식으로 토큰을 받는다.
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    // 요청 중 에러가 난 경우
    return Promise.reject(error);
  },
);

// 응답 인터셉터: JWT 만료 또는 인증 실패 응답을 공통 처리한다.
instance.interceptors.response.use(
  (response) => {
    // 정상 응답은 호출한 화면으로 그대로 반환한다.
    return response;
  },
  async (error) => {
    // 에러 응답인 경우(401, 403, 305, 500 등)
    if (error.response?.status === 401) {
      const authStore = useAuthStore();
      authStore.logout();
      return Promise.reject({ error: '로그인이 필요한 서비스입니다.' });
    }

    return Promise.reject(error);
  },
);

export default instance; // 인터셉터가 적용된 axios 인스턴스
