import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { useRouter } from 'vue-router';
import axios from 'axios';

const initState = {
  token: '', // 접근 토큰(JWT)
  user: {
    username: '', // 로그인 ID
    email: '', // Email
    name: '',
    creditScore: null,
    maxMonthlyPayment: null,
    roles: [], // 권한 목록
  },
};

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter();
  const state = ref({ ...initState });

  const isLogin = computed(() => !!state.value.user.username); // 로그인 여부

  const username = computed(() => state.value.user.username);
  const email = computed(() => state.value.user.email); // 로그인 사용자 email
  const name = computed(() => state.value.user.name);
  const login = async (member) => {
    const { data } = await axios.post('/api/auth/login', member);
    state.value = { ...data };
    localStorage.setItem('auth', JSON.stringify(state.value));
  };

  const logout = () => {
    localStorage.clear();
    state.value = { ...initState };
    router.push({ name: 'login' });
  };

  const getToken = () => state.value.token;

  const changeProfile = (member) => {
    state.value.user.email = member.email;
    localStorage.setItem('auth', JSON.stringify(state.value));
  };

  const load = () => {
    const auth = localStorage.getItem('auth');
    if (auth != null) {
      state.value = JSON.parse(auth);
      console.log(state.value);
    }
  };

  load();
  const creditScore = computed(() => state.value.user.creditScore);
  const maxMonthlyPayment = computed(() => state.value.user.maxMonthlyPayment);

  const fetchProfile = async () => {
    try {
      const { data } = await axios.get('/api/users/me', {
        headers: {
          Authorization: `Bearer ${state.value.token}`,
        },
      });
      state.value.user = { ...state.value.user, ...data };
      localStorage.setItem('auth', JSON.stringify(state.value));
    } catch (e) {
      console.error('Failed to fetch profile', e);
    }
  };

  return {
    state,
    username,
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
