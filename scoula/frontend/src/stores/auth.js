import { ref, computed, reactive } from 'vue';
import { defineStore } from 'pinia';
import axios from 'axios';

const initState = {
  token: '', // 접근 토큰(JWT)
  user: {
    email: '', // Email
    name: '', // 이름
    creditScore: 0,
    maxMonthlyPayment: 0,
    roles: [], // 권한 목록
  },
};

export const useAuthStore = defineStore('auth', () => {
  const state = ref({ ...initState });

  const isLogin = computed(() => !!state.value.user.email); // 로그인 여부

  const email = computed(() => state.value.user.email); // 로그인 사용자 email
  const name = computed(() => state.value.user.name);
  const creditScore = computed(() => state.value.user.creditScore);
  const maxMonthlyPayment = computed(() => state.value.user.maxMonthlyPayment);

  const login = async (member) => {
    const { data } = await axios.post('/api/auth/login', member);
    state.value = { ...data };
    localStorage.setItem('auth', JSON.stringify(state.value));
  };

  const logout = () => {
    localStorage.clear();
    state.value = { ...initState };
  };

  const getToken = () => state.value.token;

  const changeProfile = (member) => {
    state.value.user.email = member.email;
    state.value.user.name = member.name;
    state.value.user.creditScore = member.creditScore;
    state.value.user.maxMonthlyPayment = member.maxMonthlyPayment;
    localStorage.setItem('auth', JSON.stringify(state.value));
  };

  const fetchProfile = async () => {
    try {
      const { default: api } = await import('@/api');
      // 백엔드 API 구조 변경에 맞춰 /api/users/me 호출
      const { data } = await api.get('/api/users/me');
      // data는 MemberDTO 객체
      state.value.user = {
          ...state.value.user,
          email: data.email,
          name: data.name,
          creditScore: data.creditScore,
          maxMonthlyPayment: data.maxMonthlyPayment
      };
      // [TODO: 로그인 구현 후 삭제] 강제로 로그인 상태로 만듦
      // 나중에 실제 로그인 API가 완성되면 아래 가짜 토큰 발급 로직을 완전히 삭제해야 합니다.
      if(!state.value.token) {
         state.value.token = 'demo-token';
      }
      localStorage.setItem('auth', JSON.stringify(state.value));
    } catch (e) {
      console.error("Failed to fetch profile", e);
    }
  };

  const load = () => {
    const auth = localStorage.getItem('auth');
    if (auth != null) {
      state.value = JSON.parse(auth);
      console.log(state.value);
    }
  };

  load();

  return {
    state,
    email,
    name,
    creditScore,
    maxMonthlyPayment,
    isLogin,
    changeProfile,
    login,
    logout,
    getToken,
    fetchProfile,
  };
});
