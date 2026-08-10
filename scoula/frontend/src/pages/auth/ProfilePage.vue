<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import api from '@/api';
import BottomNav from '@/components/mobile/BottomNav.vue';
import ConfirmModal from '@/components/ConfirmModal.vue';
import { EMAIL_DOMAINS } from '@/constants/emailDomains';
import EmailDomainSelect from '@/components/auth/EmailDomainSelect.vue';

const router = useRouter();
const auth = useAuthStore();

onMounted(() => {
  auth.fetchProfile();
});

// --- 상태 관리 ---
const isEditingProfile = ref(false);
const isEditingCreditScore = ref(false); // 신용점수 수정 모드 여부
const isEditingMaxPayment = ref(false);  // 월 상환 금액 수정 모드 여부

const tempName = ref('');
const form = reactive({
  emailLocal: '',
  emailDomain: 'naver.com',
  customEmailDomain: '',
});
const email = computed(() => {
  const domain = form.emailDomain === '직접입력' ? form.customEmailDomain.trim() : form.emailDomain;
  return `${form.emailLocal.trim()}@${domain}`;
});
const tempCreditScore = ref(''); // 편집 중에는 앞자리 삭제 상태를 보존
const tempMaxPayment = ref('');  // 편집 중에는 앞자리 삭제 상태를 보존
const nameError = ref('');
const emailError = ref('');
const creditScoreError = ref('');
const maxPaymentError = ref('');

const MAX_CREDIT_SCORE = 1000;
const MAX_MONTHLY_AVAILABLE_AMOUNT = 100_000_000_000;

const formatDigitString = (digits) =>
  String(digits ?? '').replace(/\B(?=(\d{3})+(?!\d))/g, ',');

const restoreNumericCursor = (input, formattedValue, digitsBeforeCursor) => {
  input.value = formattedValue;

  nextTick(() => {
    let cursorPosition = 0;
    let digitCount = 0;

    while (
      cursorPosition < formattedValue.length &&
      digitCount < digitsBeforeCursor
    ) {
      if (/\d/.test(formattedValue[cursorPosition])) digitCount += 1;
      cursorPosition += 1;
    }

    input.setSelectionRange(cursorPosition, cursorPosition);
  });
};

const handleCreditScoreInput = (event) => {
  const digits = String(event.target.value).replace(/[^0-9]/g, '');

  if (digits === '') {
    event.target.value = '';
    tempCreditScore.value = '';
    creditScoreError.value = '';
    return;
  }

  const inputScore = Number(digits);
  const acceptedDigits =
    inputScore > MAX_CREDIT_SCORE ? String(MAX_CREDIT_SCORE) : digits;

  event.target.value = acceptedDigits;
  tempCreditScore.value = acceptedDigits;
  creditScoreError.value =
    inputScore > MAX_CREDIT_SCORE
      ? `신용점수는 최대 ${MAX_CREDIT_SCORE.toLocaleString('ko-KR')}점까지 입력할 수 있어요`
      : '';
};

const handleMaxPaymentInput = (event) => {
  const input = event.target;
  const cursorPosition = input.selectionStart ?? input.value.length;
  const digitsBeforeCursor = input.value
    .slice(0, cursorPosition)
    .replace(/[^0-9]/g, '').length;
  const digits = String(event.target.value).replace(/[^0-9]/g, '');

  if (digits === '') {
    event.target.value = '';
    tempMaxPayment.value = '';
    maxPaymentError.value = '';
    return;
  }

  const inputAmount = Number(digits);
  const acceptedDigits =
    inputAmount > MAX_MONTHLY_AVAILABLE_AMOUNT
      ? String(MAX_MONTHLY_AVAILABLE_AMOUNT)
      : digits;

  tempMaxPayment.value = acceptedDigits;
  restoreNumericCursor(
    input,
    formatDigitString(acceptedDigits),
    digitsBeforeCursor,
  );
  maxPaymentError.value =
    inputAmount > MAX_MONTHLY_AVAILABLE_AMOUNT
      ? `최대 ${formatKoreanAmount(
          MAX_MONTHLY_AVAILABLE_AMOUNT,
        )}까지 입력할 수 있어요`
      : '';
};

// --- 기본 프로필(이름/이메일) 수정 관련 ---
const emailLocalPattern = /^[^\s@]+$/;
const domainPattern = /^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

const editProfile = () => {
  tempName.value = auth.name || '';
  
  if (auth.email) {
    const parts = auth.email.split('@');
    form.emailLocal = parts[0] || '';
    const domain = parts[1] || '';
    
    const isStandardDomain = EMAIL_DOMAINS.some(opt => opt.value === domain);
    
    if (isStandardDomain) {
      form.emailDomain = domain;
      form.customEmailDomain = '';
    } else {
      form.emailDomain = '직접입력';
      form.customEmailDomain = domain;
    }
  } else {
    form.emailLocal = '';
    form.emailDomain = 'naver.com';
    form.customEmailDomain = '';
  }
  
  nameError.value = '';
  emailError.value = '';
  isEditingProfile.value = true;
};

const cancelProfile = () => {
  nameError.value = '';
  emailError.value = '';
  isEditingProfile.value = false;
};

const saveProfile = async () => {
  let hasError = false;
  nameError.value = '';
  emailError.value = '';

  const newName = tempName.value.trim();
  const newEmail = email.value;

  if (!newName) {
    nameError.value = '이름을 입력해 주세요.';
    hasError = true;
  }
  
  if (!form.emailLocal.trim()) {
    emailError.value = '이메일을 입력해 주세요.';
    hasError = true;
  } else if (!emailLocalPattern.test(form.emailLocal.trim())) {
    emailError.value = '올바른 아이디 형식을 입력해 주세요.';
    hasError = true;
  } else if (form.emailDomain === '직접입력' && !form.customEmailDomain.trim()) {
    emailError.value = '이메일 도메인을 입력해 주세요.';
    hasError = true;
  } else if (form.emailDomain === '직접입력' && !domainPattern.test(form.customEmailDomain.trim())) {
    emailError.value = '올바른 도메인 형식을 입력해 주세요. (예: example.com)';
    hasError = true;
  } else if (newEmail.length > 50) {
    emailError.value = '이메일 주소는 최대 50자까지 입력할 수 있습니다.';
    hasError = true;
  }

  if (hasError) return;

  try {
    let emailChanged = false;
    
    if (newName !== auth.name) {
      await api.patch('/api/users/me/name', { name: newName });
      auth.state.user.name = newName;
    }
    
    if (newEmail !== auth.email) {
      await api.patch('/api/users/me/email', { email: newEmail });
      emailChanged = true;
    }

    if (emailChanged) {
      alert('이메일이 변경되어 로그아웃 됩니다. 변경된 이메일로 다시 로그인해 주세요.');
      auth.logout();
      router.push('/login');
    } else {
      isEditingProfile.value = false;
    }
  } catch (error) {
    if (error.response?.status === 409 || error.response?.data?.includes('Duplicate') || error.response?.data?.includes('가입된')) {
      emailError.value = '이미 가입된 이메일입니다.';
    } else {
      console.error('Failed to update profile', error);
      alert('정보 수정에 실패했습니다.');
    }
  }
};

// --- 신용점수 수정 관련 ---
const editCreditScore = () => {
  tempCreditScore.value = String(
    Math.min(auth.creditScore || 0, MAX_CREDIT_SCORE),
  );
  creditScoreError.value = '';
  isEditingCreditScore.value = true;
};

const cancelCreditScore = () => {
  creditScoreError.value = '';
  isEditingCreditScore.value = false;
};

const saveCreditScore = async () => {
  try {
    const creditScore = Number(tempCreditScore.value || 0);
    await api.patch('/api/users/me/credit-score', { creditScore });
    auth.state.user.creditScore = creditScore;
    isEditingCreditScore.value = false;
  } catch (error) {
    console.error('Failed to update credit score', error);
    alert('신용점수 수정에 실패했습니다.');
  }
};

// --- 월 상환 가능 금액 수정 관련 ---
const editMaxPayment = () => {
  tempMaxPayment.value = String(auth.maxMonthlyPayment || 0);
  maxPaymentError.value = '';
  isEditingMaxPayment.value = true;
};

const cancelMaxPayment = () => {
  maxPaymentError.value = '';
  isEditingMaxPayment.value = false;
};

const saveMaxPayment = async () => {
  try {
    const actualAmount = Number(tempMaxPayment.value || 0);
    await api.patch('/api/users/me/max-monthly-payment', { maxMonthlyPayment: actualAmount });
    auth.state.user.maxMonthlyPayment = actualAmount;
    isEditingMaxPayment.value = false;
  } catch (error) {
    console.error('Failed to update max monthly payment', error);
    alert('월 상환 가능 금액 수정에 실패했습니다.');
  }
};

// --- 로그아웃 및 기타 유틸 ---
const showLogoutModal = ref(false);

const handleLogoutClick = () => {
  showLogoutModal.value = true;
};

const confirmLogout = () => {
  showLogoutModal.value = false;
  auth.logout();
  alert('로그아웃 되었습니다.');
  router.push('/login');
};

const cancelLogout = () => {
  showLogoutModal.value = false;
};

const formatKoreanAmount = (amount, emptyMessage = '금액을 입력해주세요') => {
  const numericAmount = Number(amount);
  if (!numericAmount) return emptyMessage;

  const jo = Math.floor(numericAmount / 1_000_000_000_000);
  const eok = Math.floor((numericAmount % 1_000_000_000_000) / 100_000_000);
  const man = Math.floor((numericAmount % 100_000_000) / 10_000);
  const won = numericAmount % 10_000;
  const result = [];

  if (jo > 0) result.push(`${jo.toLocaleString('ko-KR')}조`);
  if (eok > 0) result.push(`${eok.toLocaleString('ko-KR')}억`);
  if (man > 0) result.push(`${man.toLocaleString('ko-KR')}만원`);
  if (won > 0) result.push(`${won.toLocaleString('ko-KR')}원`);

  return result.join(' ');
};

const getCreditGrade = (score) => {
  if (score === null || score === undefined) return '등급 없음';
  if (score > 900) return '1등급';
  if (score > 800) return '2등급';
  if (score > 700) return '3등급';
  if (score > 600) return '4등급';
  if (score > 500) return '5등급';
  if (score > 400) return '6등급';
  if (score > 300) return '7등급';
  if (score > 200) return '8등급';
  if (score > 100) return '9등급';
  return '10등급';
};

const initialChar = computed(() => {
  return auth.name ? auth.name.charAt(0) : '유';
});
</script>

<template>
  <main class="profile-container">
    <h3 class="fw-bold mb-4">내 정보</h3>

    <!-- Profile Card -->
    <div class="card profile-card border-0 shadow-sm mb-4 rounded-4">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-start">
          <div class="d-flex align-items-center flex-grow-1">
            <div class="avatar-circle me-3 fw-bold d-flex align-items-center justify-content-center flex-shrink-0">
              {{ initialChar }}
            </div>
            
            <div class="flex-grow-1" style="min-width: 0; padding-right: 10px;">
              <transition name="fade-slide" mode="out-in">
                <!-- Read Mode -->
                <div v-if="!isEditingProfile" key="read">
                  <h5 class="card-title fw-bold mb-1 text-truncate">{{ auth.name || '유저' }}</h5>
                  <p class="card-text text-muted mb-0 email-text text-truncate">{{ auth.email }}</p>
                </div>
                
                <!-- Edit Mode -->
                <div v-else key="edit" class="premium-edit-form w-100 mt-2">
                  <div class="field-group">
                    <label class="field-label">이름</label>
                    <input type="text" class="premium-input" v-model="tempName" placeholder="이름 입력" />
                    <div v-if="nameError" class="field-error mt-1">{{ nameError }}</div>
                  </div>
                  
                  <div class="field-group mt-3">
                    <label class="field-label">이메일</label>
                    <div class="email-input-wrapper" :class="{ 'has-error': emailError }">
                      <div class="d-flex align-items-center gap-2" :class="{'mb-2': form.emailDomain === '직접입력'}">
                        <input
                          v-model="form.emailLocal"
                          type="text"
                          inputmode="email"
                          autocomplete="off"
                          placeholder="아이디"
                          class="premium-input flex-grow-1"
                          style="min-width: 0;"
                        />
                        <span class="email-at flex-shrink-0">@</span>
                        <EmailDomainSelect
                          v-model="form.emailDomain"
                          :options="EMAIL_DOMAINS"
                          :invalid="Boolean(emailError)"
                          class="premium-dropdown flex-grow-1"
                          style="min-width: 0;"
                        />
                      </div>
                      <input
                        v-if="form.emailDomain === '직접입력'"
                        v-model="form.customEmailDomain"
                        type="text"
                        placeholder="이메일 도메인 입력"
                        class="premium-input w-100"
                      />
                    </div>
                    <div v-if="emailError" class="field-error mt-1">{{ emailError }}</div>
                  </div>
                </div>
              </transition>
            </div>
          </div>
          
          <button v-if="!isEditingProfile" class="btn btn-outline-warning edit-btn fw-bold rounded-pill px-3 py-1 flex-shrink-0" @click="editProfile">수정</button>
        </div>

        <transition name="fade-slide">
          <div v-if="isEditingProfile" class="d-flex gap-2 mt-4 pt-4 border-top">
            <button class="btn btn-warning fw-bold flex-grow-1 text-white save-btn rounded-pill py-2" @click="saveProfile">저장</button>
            <button class="btn btn-light fw-bold flex-grow-1 cancel-btn rounded-pill py-2" @click="cancelProfile">취소</button>
          </div>
        </transition>
      </div>
    </div>

    <!-- Credit Score Card -->
    <div class="card info-card border-0 shadow-sm mb-4 rounded-4">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-start mb-2">
          <div>
            <h5 class="fw-bold mb-1">내 신용점수</h5>
            <small class="text-muted">KCB에서 직접 조회 후 입력합니다</small>
          </div>
          <button v-if="!isEditingCreditScore" class="btn btn-outline-warning edit-btn fw-bold rounded-pill px-3 py-1" @click="editCreditScore">수정</button>
        </div>
        
        <transition name="fade-slide" mode="out-in">
          <div v-if="!isEditingCreditScore" key="read" class="data-box rounded-3 px-3 py-3 mt-3 d-flex justify-content-between align-items-center">
            <span class="text-secondary fw-semibold">KCB {{ getCreditGrade(auth.creditScore) }}</span>
            <span class="fw-bold fs-5">{{ auth.creditScore || 0 }}점</span>
          </div>

          <div v-else key="edit" class="premium-edit-card mt-3">
            <div class="d-flex flex-column mb-4">
              <span class="text-secondary fw-semibold mb-2">현재 설정 점수</span>
              <div class="d-flex align-items-center justify-content-end w-100">
                <input
                  type="text"
                  inputmode="numeric"
                  pattern="[0-9]*"
                  maxlength="4"
                  class="premium-inline-input"
                  :value="tempCreditScore"
                  @input="handleCreditScoreInput"
                />
                <span class="fw-bold fs-4 ms-2 flex-shrink-0">점</span>
              </div>
              <div v-if="creditScoreError" class="field-error mt-2 text-end">{{ creditScoreError }}</div>
            </div>
            <div class="d-flex gap-2">
              <button class="btn btn-warning fw-bold flex-grow-1 text-white save-btn rounded-pill py-2" @click="saveCreditScore">저장</button>
              <button class="btn btn-light fw-bold flex-grow-1 cancel-btn rounded-pill py-2" @click="cancelCreditScore">취소</button>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- Monthly Payment Card -->
    <div class="card info-card border-0 shadow-sm mb-4 rounded-4">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-start mb-2">
          <div>
            <h5 class="fw-bold mb-1">월 상환 가능 금액</h5>
            <small class="text-muted">매월 부담할 수 있는 최대 상환 금액입니다.</small>
          </div>
          <button v-if="!isEditingMaxPayment" class="btn btn-outline-warning edit-btn fw-bold rounded-pill px-3 py-1" @click="editMaxPayment">수정</button>
        </div>
        
        <transition name="fade-slide" mode="out-in">
          <div v-if="!isEditingMaxPayment" key="read" class="data-box rounded-3 px-3 py-3 mt-3 d-flex justify-content-between align-items-center">
            <span class="text-secondary fw-semibold">현재 설정 금액</span>
            <div class="text-end">
              <div class="fw-bold fs-5">{{ (auth.maxMonthlyPayment || 0).toLocaleString() }} <span class="text-secondary fs-6">원</span></div>
              <div class="text-secondary mt-1" style="font-size: 0.9rem;">
                {{ formatKoreanAmount(auth.maxMonthlyPayment, '0원') }}
              </div>
            </div>
          </div>

          <div v-else key="edit" class="premium-edit-card mt-3">
            <div class="d-flex flex-column mb-4">
              <span class="text-secondary fw-semibold mb-2">현재 설정 금액</span>
              <div class="d-flex align-items-center justify-content-end w-100">
                <input
                  type="text"
                  inputmode="numeric"
                  pattern="[0-9]*"
                  class="premium-inline-input flex-grow-1"
                  :value="formatDigitString(tempMaxPayment)"
                  @input="handleMaxPaymentInput"
                  style="max-width: 220px;"
                />
                <span class="fw-bold fs-4 ms-2 flex-shrink-0">원</span>
              </div>
              <div class="mt-2 fw-semibold text-end" :class="maxPaymentError ? 'field-error' : 'text-secondary'" style="font-size: 0.95rem;">
                {{ maxPaymentError || formatKoreanAmount(tempMaxPayment) }}
              </div>
            </div>
            <div class="d-flex gap-2">
              <button class="btn btn-warning fw-bold flex-grow-1 text-white save-btn rounded-pill py-2" @click="saveMaxPayment">저장</button>
              <button class="btn btn-light fw-bold flex-grow-1 cancel-btn rounded-pill py-2" @click="cancelMaxPayment">취소</button>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- Logout Button -->
    <button class="btn logout-btn w-100 fw-bold py-3 mt-3 rounded-4" @click="handleLogoutClick">
      로그아웃
    </button>
    
    <ConfirmModal
      :visible="showLogoutModal"
      title="로그아웃"
      message="정말 로그아웃 하시겠습니까?"
      cancel-text="취소"
      confirm-text="로그아웃"
      @cancel="cancelLogout"
      @confirm="confirmLogout"
    />
    
    <!-- Toast Message Placeholder (Based on UI image) -->
    <div v-if="false" class="toast-overlay mt-3 py-3 text-center text-white rounded-3 fw-bold">
      로그아웃 되었습니다 &rarr; 01 로그인
    </div>

    <BottomNav active="profile" />

  </main>
</template>

<style scoped>
.profile-container {
  --kb-yellow: #ffbc00;

  position: relative;
  box-sizing: border-box;
  width: 100%;
  max-width: 390px;
  min-height: 100vh;
  margin: 0 auto;
  padding: var(--kb-space-xl) var(--kb-space-lg) 90px;
  font-family: '42dot Sans', sans-serif;
  color: #26221c;
  background: #fff;
}

.profile-container > h3 {
  margin: 0 0 24px !important;
  font-size: var(--kb-font-lg);
  font-weight: 700;
  line-height: 1.35;
}

.profile-container h5 {
  font-size: var(--kb-font-md);
}

.avatar-circle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background-color: #fdefc0;
  color: #725a00;
  font-size: 1.25rem;
}

.email-text {
  font-size: var(--kb-font-sm);
}

.edit-btn {
  color: #b5852a;
  border-color: #e5cc98;
  font-size: var(--kb-font-xs);
}

.edit-btn:hover {
  background-color: #fdefc0;
  border-color: #e5cc98;
  color: #725a00;
}

.data-box {
  background-color: #f5f4ef;
}

.logout-btn {
  background-color: white;
  border: 2px solid #b3261e;
  color: #b3261e;
}

.toast-overlay {
  background-color: #3b3b3b;
  font-size: 0.95rem;
}

.edit-input-wrapper {
  border: 2px solid #e5cc98;
  background-color: #fcfbf8;
}

.premium-edit-form {
  background: transparent;
}
.field-group {
  display: flex;
  flex-direction: column;
}
.field-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #797979;
  margin-bottom: 6px;
}
.premium-input {
  width: 100%;
  height: 48px;
  padding: 0 12px;
  background: #f8f9fa;
  border: 1px solid transparent;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 500;
  color: #333;
  transition: all 0.2s ease;
}
.premium-input:focus {
  background: #fff;
  border-color: #ffbc00;
  box-shadow: 0 0 0 3px rgba(255, 188, 0, 0.15);
  outline: none;
}
:deep(.domain-trigger) {
  height: 48px;
  padding: 0 10px;
  background: #f8f9fa;
  border: 1px solid transparent;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 500;
  color: #333;
  transition: all 0.2s ease;
  white-space: nowrap;
}
:deep(.domain-trigger:focus),
:deep(.domain-trigger.is-open) {
  background: #fff;
  border-color: #ffbc00;
  box-shadow: 0 0 0 3px rgba(255, 188, 0, 0.15);
  outline: none;
}
:deep(.domain-trigger > span:first-child) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.email-input-wrapper {
  display: flex;
  flex-direction: column;
}
.email-at {
  color: #9a948a;
  font-size: 16px;
  font-weight: 700;
}
.premium-edit-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
}
.premium-inline-input {
  border: none;
  border-bottom: 3px solid #e0e0e0;
  border-radius: 0;
  padding: 4px 8px;
  font-size: 1.5rem;
  font-weight: 800;
  background: transparent;
  text-align: right;
  width: 120px;
  color: #222;
  transition: border-color 0.2s;
}
.premium-inline-input:focus {
  border-bottom-color: #ffbc00;
  outline: none;
}
.field-error {
  color: #e54848;
  font-size: 0.8rem;
  font-weight: 700;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.35s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.save-btn {
  background-color: #ffb800;
  border: none;
}
.save-btn:hover {
  background-color: #e5a600;
}
.cancel-btn {
  background: #f1f1f1;
  border: none;
  color: #555;
}
.cancel-btn:hover {
  background: #e8e8e8;
}
</style>
