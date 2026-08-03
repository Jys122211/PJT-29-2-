import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { useRouter } from 'vue-router';
import axios from 'axios';

const STORAGE_KEY = 'auth';

const createInitialState = () => ({
  token: '', // 접근 토큰(JWT)
  user: {
    userId: null, // users.user_id
    username: '', // 로그인 ID
    email: '', // Email
    name: '',
    creditScore: null,
    maxMonthlyPayment: null,
    roles: [], // 권한 목록
  },
});

// 새로고침할 때 저장된 JWT가 아직 유효한지 만료시간(exp)을 확인한다.
const isTokenExpired = (token) => {
  try {
    const payload = JSON.parse(
      decodeURIComponent(
        atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))
          .split('')
          .map(
            (character) =>
              `%${character.charCodeAt(0).toString(16).padStart(2, '0')}`,
          )
          .join(''),
      ),
    );

    return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
};

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter();
  const state = ref(createInitialState());

  const isLogin = computed(() => !!state.value.token); // 로그인 여부

  const username = computed(() => state.value.user.username);
  const userId = computed(() => state.value.user.userId);
  const email = computed(() => state.value.user.email); // 로그인 사용자 email
  const name = computed(() => state.value.user.name);

  const login = async (member) => {
    // 기존 임시 화면의 username 값도 이메일로 받을 수 있도록 호환한다.
    const loginEmail = member.email ?? member.username;
    const { data } = await axios.post('/api/auth/login', {
      email: loginEmail,
      password: member.password,
    });

    state.value = {
      ...createInitialState(),
      ...data,
      user: {
        ...createInitialState().user,
        ...data.user,
      },
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state.value));
  };

  // 인증 정보만 초기화하여 다른 기능의 로컬 저장값은 보존한다.
  const clearSession = () => {
    localStorage.removeItem(STORAGE_KEY);
    state.value = createInitialState();
  };

  // 메뉴에서 로그아웃하면 인증 정보를 지운 뒤 로그인 화면으로 이동한다.
  const logout = () => {
    clearSession();

    if (router.currentRoute.value.name !== 'login') {
      return router.push({ name: 'login' });
    }

    return Promise.resolve();
  };

  const getToken = () => state.value.token;

  const hasValidSession = () => {
    const token = getToken();

    if (!token || isTokenExpired(token)) {
      clearSession();
      return false;
    }

    return true;
  };

  const changeProfile = (member) => {
    state.value.user = {
      ...state.value.user,
      ...member,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state.value));
  };

  const load = () => {
    const savedAuth = localStorage.getItem(STORAGE_KEY);

    if (savedAuth == null) {
      return;
    }

    try {
      const parsedAuth = JSON.parse(savedAuth);
      state.value = {
        ...createInitialState(),
        ...parsedAuth,
        user: {
          ...createInitialState().user,
          ...parsedAuth.user,
        },
      };

      hasValidSession();
    } catch {
      clearSession();
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
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state.value));
    } catch (e) {
      console.error('Failed to fetch profile', e);
    }
  };

  return {
    state,
    userId,
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
    hasValidSession,
    fetchProfile,
  };
});
