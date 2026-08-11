<script setup>
import { computed, reactive, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import deuksilLogo from '@/assets/images/deuksil-logo.png';
import { EMAIL_DOMAINS } from '@/constants/emailDomains';
import EmailDomainSelect from '@/components/auth/EmailDomainSelect.vue';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

// 화면 입력값을 Vue가 반응형으로 관리한다.
const form = reactive({
  emailLocal: '',
  emailDomain: 'naver.com',
  customEmailDomain: '',
  password: '',
});

// 로그인 API 실패처럼 화면 위쪽 모달에 표시할 오류이다.
const loginError = ref(null);

// 비밀번호 표시 여부를 관리한다.
const showPassword = ref(false);
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value;
};

// API 요청 중 중복 클릭을 막고 버튼 문구를 바꿀 때 사용한다.
const isSubmitting = ref(false);

// 로그인 성공 후 대시보드로 이동하기 전에 안내 문구를 표시한다.
const showLoginSuccess = ref(false);

// 각 입력창 바로 아래에 표시할 검증 오류이다.
const fieldErrors = reactive({
  email: '',
  password: '',
});

// 라우터가 전달한 오류 코드에 따라 로그인 필요·만료 안내 내용을 만든다.
const routeError = computed(() => {
  if (route.query.error === 'session_expired') {
    return {
      title: '로그인 시간이 만료되었습니다.',
      description: '보안을 위해 다시 로그인해 주세요.',
    };
  }

  if (route.query.error === 'login_required') {
    return {
      title: '로그인이 필요한 서비스입니다.',
      description: '로그인 후 다시 이용해 주세요.',
    };
  }

  return null;
});

// API 로그인 오류가 있으면 우선 표시하고, 없으면 라우터 오류를 표시한다.
const displayedError = computed(() => loginError.value || routeError.value);

const emailLocalPattern = /^[^\s@]+$/;
const domainPattern = /^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

// 분리된 이메일 아이디와 도메인을 실제 로그인 이메일 주소로 합친다.
const email = computed(() => {
  const domain = form.emailDomain === '직접입력' ? form.customEmailDomain.trim() : form.emailDomain;
  return `${form.emailLocal.trim()}@${domain}`;
});

// 필수값이 비어 있으면 버튼을 비활성 색상으로 보여주기 위한 값이다.
const isFormIncomplete = computed(() => {
  return !form.emailLocal.trim() || !form.password;
});

// 로그인 API를 호출하기 전에 이메일과 비밀번호 입력 여부를 검사한다.
const validateForm = () => {
  fieldErrors.email = '';
  fieldErrors.password = '';

  if (!emailLocalPattern.test(form.emailLocal.trim())) {
    fieldErrors.email = '올바른 아이디 형식을 입력해주세요.';
  } else if (form.emailDomain === '직접입력' && !form.customEmailDomain.trim()) {
    fieldErrors.email = '이메일 도메인을 입력해주세요.';
  } else if (form.emailDomain === '직접입력' && !domainPattern.test(form.customEmailDomain.trim())) {
    fieldErrors.email = '올바른 도메인 형식을 입력해 주세요. (예: example.com)';
  } else if (email.value.length > 30) {
    fieldErrors.email = '이메일 주소는 최대 30자까지 입력할 수 있습니다.';
  }

  if (!form.password) {
    fieldErrors.password = '비밀번호를 입력해주세요.';
  }

  return !fieldErrors.email && !fieldErrors.password;
};

// 사용자가 다시 입력하기 시작하면 해당 입력창의 이전 오류를 지운다.
const clearFieldError = (field) => {
  fieldErrors[field] = '';
  loginError.value = null;
};

// 성공 안내를 사용자가 읽을 수 있도록 지정한 시간만큼 기다린다.
const wait = (milliseconds) =>
  new Promise((resolve) => window.setTimeout(resolve, milliseconds));

/**
 * 로그인 버튼 클릭 흐름
 * 1. 입력값 검증 → 2. 로그인 API 호출 → 3. JWT 저장
 * 4. 성공 안내 표시 → 5. 1초 후 대시보드 이동
 */
const login = async () => {
  loginError.value = null;
  showLoginSuccess.value = false;

  if (!validateForm()) {
    return;
  }

  isSubmitting.value = true;

  try {
    // Pinia Store가 로그인 API 호출과 JWT 저장을 담당한다.
    await auth.login({
      email: email.value,
      password: form.password,
    });

    showLoginSuccess.value = true;
    await router.replace({ name: 'home' });
  } catch (error) {
    if (error.response?.status === 401) {
      // 이메일 또는 비밀번호가 틀린 경우 피그마 형태의 오류 모달을 표시한다.
      loginError.value = {
        title: '입력한 정보가 일치하지 않습니다.',
        description: '이메일 또는 비밀번호를 확인해 주세요.',
      };
    } else {
      let title = '로그인 처리 중 오류가 발생했습니다.';
      const responseData = error.response?.data;
      
      if (typeof responseData === 'string') {
        title = responseData.trim().startsWith('<') ? '서버와 연결할 수 없습니다.' : responseData;
      } else if (responseData?.message) {
        title = responseData.message;
      }

      loginError.value = {
        title,
        description: '잠시 후 다시 시도해 주세요.',
      };
    }
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <!-- 서비스 로고와 소개 문구 -->
      <div class="brand-mark">
        <img class="brand-logo" :src="deuksilLogo" alt="득실 로고" />
        <div>
          <h1 class="brand-title">득실</h1>
          <p class="brand-subtitle">기울여 보면 답이 보인다</p>
        </div>
      </div>

      <p class="service-description">
        예금 해지와 대출, 어느 쪽이 이득인지<br />
        만기 기준으로 정확히 비교해 드립니다.
      </p>

      <!-- 로그인 실패·JWT 만료·로그인 필요 안내 모달 -->
      <div
        v-if="displayedError"
        class="error-alert"
        role="alert"
        aria-live="polite"
      >
        <span class="error-icon" aria-hidden="true">!</span>
        <span class="error-copy">
          <strong>{{ displayedError.title }}</strong>
          <span>{{ displayedError.description }}</span>
        </span>
      </div>

      <!-- 이메일·비밀번호 입력 폼 -->
      <form class="login-form" @submit.prevent="login">
        <label class="field">
          <span>이메일</span>
          <div
            class="email-input-row"
            :class="{ 'has-error': fieldErrors.email, 'has-custom': form.emailDomain === '직접입력' }"
          >
            <input
              v-model="form.emailLocal"
              type="text"
              inputmode="email"
              autocomplete="username"
              placeholder="아이디 입력"
              maxlength="15"
              @input="clearFieldError('email')"
            />
            <span class="email-at">@</span>
            <input
              v-if="form.emailDomain === '직접입력'"
              v-model="form.customEmailDomain"
              type="text"
              placeholder="직접입력"
              maxlength="14"
              @input="clearFieldError('email')"
            />
            <EmailDomainSelect
              v-model="form.emailDomain"
              :options="EMAIL_DOMAINS"
              :invalid="Boolean(fieldErrors.email)"
              @change="clearFieldError('email')"
            />
          </div>
          <p v-if="fieldErrors.email" class="field-error">
            {{ fieldErrors.email }}
          </p>
        </label>

        <label class="field">
          <span>비밀번호</span>
          <div class="password-wrapper">
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="비밀번호 입력"
              :class="{ 'input-error': fieldErrors.password }"
              @input="clearFieldError('password')"
            />
            <button
              type="button"
              class="toggle-password"
              @click="togglePasswordVisibility"
              :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 표시'"
            >
              <svg v-if="!showPassword" viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
            </button>
          </div>
          <p v-if="fieldErrors.password" class="field-error">
            {{ fieldErrors.password }}
          </p>
        </label>

        <button
          class="login-button"
          :class="{ 'is-incomplete': isFormIncomplete }"
          type="submit"
          :disabled="isSubmitting"
        >
          {{ isSubmitting ? '로그인 중...' : '로그인' }}
        </button>
      </form>

      <div class="login-links">
        <RouterLink to="/signup">회원가입</RouterLink>
        <span class="link-divider" aria-hidden="true"></span>
        <RouterLink to="/password/find">비밀번호 찾기</RouterLink>
      </div>

      <Transition name="success-toast">
        <div
          v-if="showLoginSuccess"
          class="success-toast"
          role="status"
          aria-live="polite"
        >
          로그인 성공 <span aria-hidden="true">·</span> 홈 대시보드로
          이동합니다.
        </div>
      </Transition>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: #f7f6f2;
}

.login-card {
  width: 100%;
  max-width: 390px;
  min-height: 620px;
  padding: 64px var(--kb-space-xl) 36px;
  background: #fffdfa;
  border: 1px solid #eee9df;
  border-radius: 28px;
  box-shadow: 0 18px 40px rgba(36, 30, 18, 0.12);
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: left;
}

.brand-logo {
  width: 86px;
  height: auto;
  display: block;
  flex-shrink: 0;
  object-fit: contain;
  filter: brightness(1.03);
}

.brand-title {
  margin: 0;
  color: #222;
  font-size: var(--kb-font-xl);
  font-weight: 900;
  line-height: 1;
}

.brand-subtitle {
  margin: 7px 0 0;
  color: #aaa399;
  font-size: 10px;
}

.service-description {
  margin: 48px 0 24px;
  color: #918a80;
  text-align: center;
  font-size: var(--kb-font-md);
  line-height: 1.7;
}

.error-alert {
  margin: 0 0 14px;
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr);
  align-items: start;
  gap: 9px;
  padding: 11px 13px;
  border: 1px solid #ef9a97;
  border-radius: 9px;
  background: #fff0ef;
  color: #dc4540;
}

.error-icon {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #ffd9d7;
  color: #d83f3a;
  font-size: 13px;
  font-weight: 900;
  line-height: 1;
}

.error-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-top: 1px;
}

.error-copy strong,
.error-copy span {
  display: block;
  line-height: 1.4;
}

.error-copy strong {
  font-size: 12px;
  font-weight: 800;
}

.error-copy span {
  color: #e05b56;
  font-size: 11px;
  font-weight: 600;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--kb-space-xs);
  color: #2f2c28;
  font-size: var(--kb-font-sm);
  font-weight: 700;
}

.field input,
.field select {
  width: 100%;
  height: var(--kb-input-height);
  padding: 0 var(--kb-space-md);
  border: 1px solid #e4ded3;
  border-radius: 10px;
  background: #fff;
  color: #222;
  font-size: var(--kb-font-md);
  outline: none;
}

.field select {
  cursor: pointer;
}

.field input:focus,
.field select:focus {
  border-color: #ffbd00;
  box-shadow: 0 0 0 3px rgba(255, 189, 0, 0.15);
}

.field input.input-error,
.email-input-row.has-error input,
.email-input-row.has-error select {
  border-color: #ef7772;
}

.field-error {
  margin: -1px 0 0;
  color: #e54848;
  font-size: 12px;
  font-weight: 700;
}

.email-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1.15fr);
  align-items: center;
  gap: 8px;
}

.email-input-row.has-custom {
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1.2fr) minmax(0, 0.85fr);
}

.email-at {
  color: #9a948a;
  font-size: 14px;
  font-weight: 700;
}

.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-wrapper input {
  padding-right: 42px;
}

/* Edge 브라우저 기본 눈 모양 아이콘 숨기기 */
.password-wrapper input::-ms-reveal,
.password-wrapper input::-ms-clear {
  display: none;
}

.toggle-password {
  position: absolute;
  right: 12px;
  background: transparent;
  border: none;
  padding: 4px;
  color: #a49e95;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s ease;
}

.toggle-password:hover {
  color: #555;
}

.login-button {
  width: 100%;
  height: var(--kb-btn-height);
  margin-top: 12px;
  border: 0;
  border-radius: 10px;
  background: #ffbd00;
  color: #1f1f1f;
  font-size: var(--kb-font-lg);
  font-weight: 800;
}

.login-button.is-incomplete {
  background: #e2cf96;
  color: #796f58;
}

.login-button:disabled {
  cursor: not-allowed;
}

.login-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 13px;
  margin-top: 22px;
  color: #9d968c;
  font-size: 12px;
}

.login-links a {
  color: inherit;
  text-decoration: none;
}

.link-divider {
  width: 1px;
  height: 12px;
  background: #ddd7ce;
}

.success-toast {
  width: fit-content;
  max-width: 100%;
  margin: 22px auto 0;
  padding: 13px 20px;
  border-radius: 10px;
  background: #222;
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.2);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  text-align: center;
  white-space: nowrap;
}

.success-toast-enter-active,
.success-toast-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.success-toast-enter-from,
.success-toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
