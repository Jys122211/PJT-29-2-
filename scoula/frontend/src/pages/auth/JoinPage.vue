<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import authApi from '@/api/authApi';
import { useAuthStore } from '@/stores/auth';
import deuksilLogo from '@/assets/images/deuksil-logo.png';
import { EMAIL_DOMAINS } from '@/constants/emailDomains';
import EmailDomainSelect from '@/components/auth/EmailDomainSelect.vue';

const router = useRouter();
const auth = useAuthStore();

// 회원가입 화면의 입력값을 Vue가 반응형으로 관리한다.
const form = reactive({
  name: '',
  emailLocal: '',
  emailDomain: 'naver.com',
  customEmailDomain: '',
  password: '',
});

// 중복 이메일 외의 서버 오류를 표시할 때 사용한다.
const errorMessage = ref('');

// 회원가입과 자동 로그인 요청이 진행 중인지 나타낸다.
const isSubmitting = ref(false);

// 이름·이메일·비밀번호 입력창 아래에 각각 표시할 오류이다.
const fieldErrors = reactive({
  name: '',
  email: '',
  password: '',
});

const emailLocalPattern = /^[^\s@]+$/;

// 비밀번호 최대 자릿수. 백엔드 SignupServiceImpl의 PASSWORD_MAX_LENGTH와 같은 값이어야 한다.
const PASSWORD_MAX_LENGTH = 16;

// 사용자가 선택한 이메일 아이디와 도메인을 하나의 이메일 주소로 합친다.
const email = computed(() => {
  const domain = form.emailDomain === '직접입력' ? form.customEmailDomain.trim() : form.emailDomain;
  return `${form.emailLocal.trim()}@${domain}`;
});

// 필수값이 비어 있을 때 "다음" 버튼을 비활성 색상으로 표시한다.
const isFormIncomplete = computed(() => {
  return (
    !form.name.trim() ||
    !form.emailLocal.trim() ||
    !form.password
  );
});

// 백엔드 호출 전에 필수값과 이메일 아이디 형식을 검사한다.
const validateForm = () => {
  fieldErrors.name = '';
  fieldErrors.email = '';
  fieldErrors.password = '';

  if (!form.name.trim()) {
    fieldErrors.name = '이름을 입력해 주세요.';
  }

  if (!form.emailLocal.trim()) {
    fieldErrors.email = '이메일을 입력해 주세요.';
  } else if (!emailLocalPattern.test(form.emailLocal.trim())) {
    fieldErrors.email = '올바른 이메일 형식으로 입력해 주세요.';
  } else if (form.emailDomain === '직접입력' && !form.customEmailDomain.trim()) {
    fieldErrors.email = '이메일 도메인을 입력해 주세요.';
  }

  if (!form.password) {
    fieldErrors.password = '비밀번호를 입력해 주세요.';
  } else if (form.password.length > PASSWORD_MAX_LENGTH) {
    fieldErrors.password = `비밀번호는 최대 ${PASSWORD_MAX_LENGTH}자까지 입력할 수 있습니다.`;
  }

  return !fieldErrors.name && !fieldErrors.email && !fieldErrors.password;
};

// 사용자가 값을 다시 입력하면 해당 입력창의 이전 오류를 지운다.
const clearFieldError = (field) => {
  fieldErrors[field] = '';
  errorMessage.value = '';
};

/**
 * 회원가입 버튼 클릭 흐름
 * 1. 입력값 검증 → 2. users 저장 → 3. 자동 로그인 → 4. 가입 완료 화면 이동
 */
const join = async () => {
  errorMessage.value = '';

  if (!validateForm()) {
    return;
  }

  isSubmitting.value = true;

  try {
    // 이름·이메일·비밀번호를 회원가입 API에 전달한다.
    await authApi.signup({
      name: form.name.trim(),
      email: email.value,
      password: form.password,
    });

    // 가입 완료 화면과 다음 기능에서 JWT를 사용할 수 있도록 바로 로그인한다.
    await auth.login({
      email: email.value,
      password: form.password,
    });

    router.push('/signup/complete');
  } catch (error) {
    // 백엔드가 중복 이메일을 409로 응답하면 이메일 입력창 아래에 안내한다.
    if (error.response?.status === 409) {
      fieldErrors.email =
        '중복된 이메일이 존재합니다. 다시 입력해주세요.';
      return;
    }

    errorMessage.value =
      error.response?.data || '회원가입 처리 중 오류가 발생했습니다.';
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <main class="signup-page">
    <section class="signup-card">
      <!-- 회원가입 로고와 현재 단계 -->
      <div class="brand-mark">
        <img class="brand-logo" :src="deuksilLogo" alt="득실 로고" />
        <div>
          <p class="brand-title">득실 회원가입</p>
          <p class="brand-subtitle">기본 정보</p>
        </div>
      </div>

      <!-- 회원가입 진행 상태 표시 -->
      <div class="progress-bar">
        <span></span>
      </div>

      <!-- 이름·이메일·비밀번호 입력 폼 -->
      <form class="signup-form" @submit.prevent="join">
        <label class="field">
          <span>이름</span>
          <input
            v-model="form.name"
            type="text"
            autocomplete="name"
            placeholder="이름 입력"
            :class="{ 'input-error': fieldErrors.name }"
            @input="clearFieldError('name')"
          />
          <p v-if="fieldErrors.name" class="field-error">
            {{ fieldErrors.name }}
          </p>
        </label>

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
              autocomplete="off"
              placeholder="아이디 입력"
              @input="clearFieldError('email')"
            />
            <span class="email-at">@</span>
            <input
              v-if="form.emailDomain === '직접입력'"
              v-model="form.customEmailDomain"
              type="text"
              placeholder="직접입력"
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
          <input
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            :maxlength="PASSWORD_MAX_LENGTH"
            :placeholder="`비밀번호 입력 (최대 ${PASSWORD_MAX_LENGTH}자)`"
            :class="{ 'input-error': fieldErrors.password }"
            @input="clearFieldError('password')"
          />
          <p v-if="fieldErrors.password" class="field-error">
            {{ fieldErrors.password }}
          </p>
        </label>

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

        <button
          class="submit-button"
          :class="{ 'is-incomplete': isFormIncomplete }"
          type="submit"
          :disabled="isSubmitting"
        >
          {{ isSubmitting ? '처리 중...' : '다음' }}
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.signup-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: #f7f6f2;
}

.signup-card {
  width: 100%;
  max-width: 390px;
  min-height: 620px;
  padding: 42px var(--kb-space-xl) 28px;
  background: #fffdfa;
  border: 1px solid #eee9df;
  border-radius: 28px;
  box-shadow: 0 18px 40px rgba(36, 30, 18, 0.12);
}

.brand-mark {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.brand-logo {
  width: 76px;
  height: auto;
  display: block;
  object-fit: contain;
  filter: brightness(1.03);
}

.brand-title {
  margin: 0;
  color: #222;
  font-size: var(--kb-font-xl);
  font-weight: 800;
}

.brand-subtitle {
  margin: 4px 0 0;
  color: #9a948a;
  font-size: var(--kb-font-sm);
}

.progress-bar {
  height: 4px;
  margin: 30px 0 60px;
  overflow: hidden;
  border-radius: 999px;
  background: #ece8df;
}

.progress-bar span {
  display: block;
  width: 50%;
  height: 100%;
  background: #ffbd00;
}

.signup-form {
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

.error-message,
.field-error {
  margin: 0;
  color: #e54848;
  font-size: 12px;
  font-weight: 700;
}

.field-error {
  margin-top: -1px;
}

.submit-button {
  width: 100%;
  height: var(--kb-btn-height);
  margin-top: 26px;
  border: 0;
  border-radius: 10px;
  background: #ffbd00;
  color: #1f1f1f;
  font-size: var(--kb-font-lg);
  font-weight: 800;
}

.submit-button.is-incomplete {
  background: #e2cf96;
  color: #796f58;
}

.submit-button:disabled {
  cursor: not-allowed;
}
</style>
