<script setup>
import { computed, onUnmounted, reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import authApi from '@/api/authApi';
import deuksilLogo from '@/assets/images/deuksil-logo.png';

const router = useRouter();

/**
 * 비밀번호 찾기 화면은 한 페이지에서 4단계로 진행된다.
 * 1 : 이메일 입력 → 2 : 인증번호 확인 → 3 : 새 비밀번호 설정 → 4 : 완료
 */
const step = ref(1);

// 백엔드 PasswordResetServiceImpl의 CODE_EXPIRE_MINUTES와 같은 값이어야 한다.
const CODE_EXPIRE_SECONDS = 180;

// 백엔드 PasswordPolicy, 프론트 JoinPage.vue와 같은 값이어야 한다.
const PASSWORD_MIN_LENGTH = 4;
const PASSWORD_MAX_LENGTH = 16;

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  newPasswordConfirm: '',
});

// 각 입력창 바로 아래에 표시할 오류이다.
const fieldErrors = reactive({
  email: '',
  code: '',
  newPassword: '',
  newPasswordConfirm: '',
});

// 카드 위쪽 배너에 표시할 오류. 피그마 05번 화면에 해당한다.
const alertMessage = ref('');

// API 요청 중 중복 클릭을 막는다.
const isSubmitting = ref(false);

// 2단계에서 검증에 성공하면 받는 1회용 토큰. 3단계 요청에만 쓰고 저장하지 않는다.
const resetToken = ref('');

// 인증번호 남은 시간(초)과 setInterval 핸들
const remainingSeconds = ref(0);
let timerId = null;

const stopTimer = () => {
  if (timerId !== null) {
    window.clearInterval(timerId);
    timerId = null;
  }
};

// 인증번호를 보낼 때마다 남은 시간을 처음부터 다시 센다.
const startTimer = () => {
  stopTimer();
  remainingSeconds.value = CODE_EXPIRE_SECONDS;

  timerId = window.setInterval(() => {
    remainingSeconds.value -= 1;

    if (remainingSeconds.value <= 0) {
      stopTimer();
      remainingSeconds.value = 0;
      fieldErrors.code = '인증번호가 만료되었습니다. 다시 요청해 주세요.';
    }
  }, 1000);
};

// 화면을 벗어날 때 타이머가 남아 있지 않도록 정리한다.
onUnmounted(stopTimer);

// 02:59 형태로 보여준다.
const remainingTimeLabel = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60);
  const seconds = remainingSeconds.value % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
});

const stepTitle = computed(() => {
  if (step.value === 2) return '인증번호 확인';
  if (step.value === 3) return '새 비밀번호 설정';
  return '비밀번호 찾기';
});

const stepDescription = computed(() => {
  if (step.value === 2) return '이메일로 전송된 인증번호를 입력해 주세요';
  if (step.value === 3) return '안전한 비밀번호로 변경해 주세요';
  return '가입한 이메일 주소를 입력해 주세요';
});

// 진행바 채움 비율
const progressWidth = computed(() => `${(step.value / 3) * 100}%`);

// 필수값이 비었으면 버튼을 비활성 색상으로 보여준다.
const isFormIncomplete = computed(() => {
  if (step.value === 1) return !form.email.trim();
  if (step.value === 2) return !form.code.trim();
  return !form.newPassword || !form.newPasswordConfirm;
});

// 1단계 버튼 문구는 오류가 있으면 "다시 확인하기"로 바뀐다.
const submitLabel = computed(() => {
  if (isSubmitting.value) return '처리 중...';
  if (step.value === 1) return alertMessage.value ? '다시 확인하기' : '인증번호 받기';
  if (step.value === 2) return '확인';
  return '비밀번호 변경';
});

// 사용자가 다시 입력하면 해당 입력창의 이전 오류를 지운다.
const clearFieldError = (field) => {
  fieldErrors[field] = '';
  alertMessage.value = '';
};

// 인증번호 입력칸은 숫자만 받는다.
const onCodeInput = () => {
  form.code = form.code.replace(/[^0-9]/g, '');
  clearFieldError('code');
};

// 서버가 text/plain으로 내려준 메시지를 그대로 쓰고, 없으면 기본 문구를 보여준다.
const messageOf = (error, fallback) => {
  const body = error.response?.data;
  return typeof body === 'string' && body.trim() ? body : fallback;
};

/** 1단계 - 인증번호 발송 요청 */
const requestCode = async () => {
  fieldErrors.email = '';
  alertMessage.value = '';

  const email = form.email.trim();

  if (!email) {
    fieldErrors.email = '이메일을 입력해 주세요.';
    return;
  }
  if (!emailPattern.test(email)) {
    fieldErrors.email = '올바른 이메일 형식으로 입력해 주세요.';
    return;
  }

  isSubmitting.value = true;

  try {
    await authApi.sendPasswordResetCode(email);

    form.code = '';
    fieldErrors.code = '';
    step.value = 2;
    startTimer();
  } catch (error) {
    // 가입되지 않은 이메일이면 피그마 05번 화면처럼 배너와 입력창 오류를 함께 보여준다.
    if (error.response?.status === 404) {
      alertMessage.value = messageOf(
        error,
        '입력한 이메일로 가입된 계정을 찾을 수 없습니다.',
      );
      fieldErrors.email = '이메일 주소를 다시 확인해 주세요.';
      return;
    }

    alertMessage.value = messageOf(
      error,
      '인증번호 발송 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
    );
  } finally {
    isSubmitting.value = false;
  }
};

/** 2단계 - 인증번호 확인 */
const verifyCode = async () => {
  fieldErrors.code = '';
  alertMessage.value = '';

  const code = form.code.trim();

  if (!code) {
    fieldErrors.code = '인증번호를 입력해 주세요.';
    return;
  }
  if (code.length !== 6) {
    fieldErrors.code = '인증번호 6자리를 입력해 주세요.';
    return;
  }
  if (remainingSeconds.value <= 0) {
    fieldErrors.code = '인증번호가 만료되었습니다. 다시 요청해 주세요.';
    return;
  }

  isSubmitting.value = true;

  try {
    const data = await authApi.verifyPasswordResetCode(form.email.trim(), code);

    resetToken.value = data.resetToken;
    stopTimer();

    form.newPassword = '';
    form.newPasswordConfirm = '';
    step.value = 3;
  } catch (error) {
    fieldErrors.code = messageOf(error, '인증번호가 올바르지 않습니다.');
  } finally {
    isSubmitting.value = false;
  }
};

/** 2단계 - 인증번호 다시 보내기 */
const resendCode = async () => {
  fieldErrors.code = '';
  alertMessage.value = '';
  isSubmitting.value = true;

  try {
    await authApi.sendPasswordResetCode(form.email.trim());

    form.code = '';
    startTimer();
  } catch (error) {
    alertMessage.value = messageOf(
      error,
      '인증번호 재발송에 실패했습니다. 잠시 후 다시 시도해 주세요.',
    );
  } finally {
    isSubmitting.value = false;
  }
};

/** 3단계 - 새 비밀번호로 변경 */
const changePassword = async () => {
  fieldErrors.newPassword = '';
  fieldErrors.newPasswordConfirm = '';
  alertMessage.value = '';

  if (!form.newPassword) {
    fieldErrors.newPassword = '새 비밀번호를 입력해 주세요.';
    return;
  }
  if (form.newPassword.length < PASSWORD_MIN_LENGTH) {
    fieldErrors.newPassword = `비밀번호는 최소 ${PASSWORD_MIN_LENGTH}자 이상 입력해야 합니다.`;
    return;
  }
  if (form.newPassword.length > PASSWORD_MAX_LENGTH) {
    fieldErrors.newPassword = `비밀번호는 최대 ${PASSWORD_MAX_LENGTH}자까지 입력할 수 있습니다.`;
    return;
  }
  if (form.newPassword !== form.newPasswordConfirm) {
    fieldErrors.newPasswordConfirm = '비밀번호가 일치하지 않습니다.';
    return;
  }

  isSubmitting.value = true;

  try {
    await authApi.resetPassword(resetToken.value, form.newPassword);

    // 사용한 토큰과 입력값은 화면에 남기지 않는다.
    resetToken.value = '';
    form.newPassword = '';
    form.newPasswordConfirm = '';
    step.value = 4;
  } catch (error) {
    alertMessage.value = messageOf(
      error,
      '비밀번호 변경 중 오류가 발생했습니다.',
    );
  } finally {
    isSubmitting.value = false;
  }
};

// 단계에 맞는 처리를 실행한다.
const submit = () => {
  if (isSubmitting.value) return;

  if (step.value === 1) return requestCode();
  if (step.value === 2) return verifyCode();
  return changePassword();
};
</script>

<template>
  <main class="find-password-page">
    <!-- 1~3단계 : 입력 화면 -->
    <section v-if="step < 4" class="find-password-card">
      <div class="brand-mark">
        <img class="brand-logo" :src="deuksilLogo" alt="득실 로고" />
        <div>
          <p class="brand-title">{{ stepTitle }}</p>
          <p class="brand-subtitle">{{ stepDescription }}</p>
        </div>
      </div>

      <!-- 진행 상태 표시 -->
      <div class="progress-bar">
        <span :style="{ width: progressWidth }"></span>
      </div>

      <!-- 가입되지 않은 이메일 등 화면 전체에 해당하는 오류 -->
      <p v-if="alertMessage" class="alert-banner" role="alert" aria-live="polite">
        {{ alertMessage }}
      </p>

      <form class="find-password-form" @submit.prevent="submit">
        <!-- 1단계 : 이메일 입력 / 2단계 : 확인용으로 계속 노출 -->
        <label v-if="step <= 2" class="field">
          <span>이메일</span>
          <input
            v-model="form.email"
            type="email"
            inputmode="email"
            autocomplete="username"
            placeholder="example@kb.co.kr"
            :readonly="step === 2"
            :class="{ 'input-error': fieldErrors.email }"
            @input="clearFieldError('email')"
          />
          <p v-if="fieldErrors.email" class="field-error">
            {{ fieldErrors.email }}
          </p>
          <p v-else-if="step === 1" class="field-hint">
            입력한 이메일로 인증번호를 보내드립니다.
          </p>
        </label>

        <!-- 2단계 : 인증번호 입력 -->
        <template v-if="step === 2">
          <label class="field">
            <span>인증번호</span>
            <input
              v-model="form.code"
              type="text"
              inputmode="numeric"
              autocomplete="one-time-code"
              maxlength="6"
              placeholder="6자리 숫자"
              :class="{ 'input-error': fieldErrors.code }"
              @input="onCodeInput"
            />
            <p v-if="fieldErrors.code" class="field-error">
              {{ fieldErrors.code }}
            </p>
            <p v-else class="field-hint">남은 시간 {{ remainingTimeLabel }}</p>
          </label>

          <button
            class="resend-button"
            type="button"
            :disabled="isSubmitting"
            @click="resendCode"
          >
            인증번호 다시 보내기
          </button>
        </template>

        <!-- 3단계 : 새 비밀번호 입력 -->
        <template v-if="step === 3">
          <label class="field">
            <span>새 비밀번호</span>
            <input
              v-model="form.newPassword"
              type="password"
              autocomplete="new-password"
              :maxlength="PASSWORD_MAX_LENGTH"
              placeholder="새 비밀번호 입력"
              :class="{ 'input-error': fieldErrors.newPassword }"
              @input="clearFieldError('newPassword')"
            />
            <p v-if="fieldErrors.newPassword" class="field-error">
              {{ fieldErrors.newPassword }}
            </p>
          </label>

          <label class="field">
            <span>새 비밀번호 확인</span>
            <input
              v-model="form.newPasswordConfirm"
              type="password"
              autocomplete="new-password"
              :maxlength="PASSWORD_MAX_LENGTH"
              placeholder="새 비밀번호 다시 입력"
              :class="{ 'input-error': fieldErrors.newPasswordConfirm }"
              @input="clearFieldError('newPasswordConfirm')"
            />
            <p v-if="fieldErrors.newPasswordConfirm" class="field-error">
              {{ fieldErrors.newPasswordConfirm }}
            </p>
          </label>
        </template>

        <!-- 버튼은 카드 아래쪽에 고정한다 -->
        <div class="form-footer">
          <button
            class="submit-button"
            :class="{ 'is-incomplete': isFormIncomplete }"
            type="submit"
            :disabled="isSubmitting"
          >
            {{ submitLabel }}
          </button>

          <div v-if="step === 1" class="page-links">
            <RouterLink to="/login">로그인</RouterLink>
            <span class="link-divider" aria-hidden="true"></span>
            <RouterLink to="/signup">회원가입</RouterLink>
          </div>
        </div>
      </form>
    </section>

    <!-- 4단계 : 변경 완료 -->
    <section v-else class="complete-card">
      <div class="check-icon">✓</div>

      <h1>비밀번호가 변경되었습니다!</h1>
      <p>
        새 비밀번호로 다시 로그인해 주세요.<br />
        이전 비밀번호는 더 이상 사용할 수 없습니다.
      </p>

      <button
        class="submit-button"
        type="button"
        @click="router.push('/login')"
      >
        로그인하러 가기
      </button>

      <button class="text-button" type="button" @click="router.push('/')">
        홈으로 이동
      </button>
    </section>
  </main>
</template>

<style scoped>
.find-password-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: #f7f6f2;
}

.find-password-card,
.complete-card {
  width: 100%;
  max-width: 390px;
  min-height: 620px;
  padding: 42px var(--kb-space-xl) 28px;
  background: #fffdfa;
  border: 1px solid #eee9df;
  border-radius: 28px;
  box-shadow: 0 18px 40px rgba(36, 30, 18, 0.12);
}

.find-password-card {
  display: flex;
  flex-direction: column;
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
  margin: 30px 0 24px;
  overflow: hidden;
  border-radius: 999px;
  background: #ece8df;
}

.progress-bar span {
  display: block;
  height: 100%;
  background: #ffbd00;
  transition: width 0.25s ease;
}

.alert-banner {
  margin: 0 0 16px;
  padding: 11px 13px;
  border: 1px solid #ef9a97;
  border-radius: 9px;
  background: #fff0ef;
  color: #dc4540;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.5;
}

.find-password-form {
  flex: 1;
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

.field input {
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

.field input:focus {
  border-color: #ffbd00;
  box-shadow: 0 0 0 3px rgba(255, 189, 0, 0.15);
}

.field input:read-only {
  background: #f7f5f1;
  color: #6f6a63;
}

.field input.input-error {
  border-color: #ef7772;
}

.field-error {
  margin: -1px 0 0;
  color: #e54848;
  font-size: 12px;
  font-weight: 700;
}

.field-hint {
  margin: -1px 0 0;
  color: #a49d93;
  font-size: 12px;
  font-weight: 600;
}

.resend-button {
  width: 100%;
  height: var(--kb-input-height);
  border: 1px solid #e4ded3;
  border-radius: 10px;
  background: #fff;
  color: #4a453e;
  font-size: var(--kb-font-sm);
  font-weight: 700;
  cursor: pointer;
}

.resend-button:disabled {
  color: #b3ada4;
  cursor: not-allowed;
}

/* 입력 항목 수와 상관없이 버튼이 카드 아래쪽에 오도록 남은 공간을 밀어낸다. */
.form-footer {
  margin-top: auto;
  padding-top: 26px;
}

.submit-button {
  width: 100%;
  height: var(--kb-btn-height);
  border: 0;
  border-radius: 10px;
  background: #ffbd00;
  color: #1f1f1f;
  font-size: var(--kb-font-lg);
  font-weight: 800;
  cursor: pointer;
}

.submit-button.is-incomplete {
  background: #e2cf96;
  color: #796f58;
}

.submit-button:disabled {
  cursor: not-allowed;
}

.page-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 13px;
  margin-top: 18px;
  color: #9d968c;
  font-size: 12px;
}

.page-links a {
  color: inherit;
  text-decoration: none;
}

.link-divider {
  width: 1px;
  height: 12px;
  background: #ddd7ce;
}

/* 4단계 완료 화면 */
.complete-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.check-icon {
  width: 58px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30px;
  border-radius: 50%;
  background: #ffbd00;
  color: #1f1f1f;
  font-size: 30px;
  font-weight: 900;
}

.complete-card h1 {
  margin: 0;
  color: #222;
  font-size: 21px;
  font-weight: 800;
}

.complete-card p {
  max-width: 270px;
  margin: 18px 0 38px;
  color: #8e877d;
  font-size: 13px;
  line-height: 1.7;
}

.text-button {
  margin-top: 22px;
  border: 0;
  background: transparent;
  color: #7f776d;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
</style>
