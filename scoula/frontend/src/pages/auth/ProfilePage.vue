<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import api from '@/api';
import BottomNav from '@/components/mobile/BottomNav.vue';

const router = useRouter();
const auth = useAuthStore();

onMounted(() => {
  auth.fetchProfile();
});

// --- 상태 관리 ---
const isEditingCreditScore = ref(false); // 신용점수 수정 모드 여부
const isEditingMaxPayment = ref(false);  // 월 상환 금액 수정 모드 여부

const tempCreditScore = ref(0); // 수정 중인 신용점수 임시 저장
const tempMaxPayment = ref(0);  // 수정 중인 월 상환 금액 임시 저장
const creditScoreError = ref('');
const maxPaymentError = ref('');

const MAX_CREDIT_SCORE = 1000;
const MAX_MONTHLY_AVAILABLE_AMOUNT = 100_000_000_000;

const handleCreditScoreInput = (event) => {
  const digits = String(event.target.value).replace(/[^0-9]/g, '');

  if (digits === '') {
    event.target.value = '';
    tempCreditScore.value = 0;
    creditScoreError.value = '';
    return;
  }

  const inputScore = Number(digits);
  const limitedScore = Math.min(inputScore, MAX_CREDIT_SCORE);

  event.target.value = String(limitedScore);
  tempCreditScore.value = limitedScore;
  creditScoreError.value =
    inputScore > MAX_CREDIT_SCORE
      ? `신용점수는 최대 ${MAX_CREDIT_SCORE.toLocaleString('ko-KR')}점까지 입력할 수 있어요`
      : '';
};

const handleMaxPaymentInput = (event) => {
  const digits = String(event.target.value).replace(/[^0-9]/g, '');

  if (digits === '') {
    event.target.value = '';
    tempMaxPayment.value = 0;
    maxPaymentError.value = '';
    return;
  }

  const inputAmount = Number(digits);
  const limitedAmount = Math.min(
    inputAmount,
    MAX_MONTHLY_AVAILABLE_AMOUNT,
  );

  event.target.value = String(limitedAmount);
  tempMaxPayment.value = limitedAmount;
  maxPaymentError.value =
    inputAmount > MAX_MONTHLY_AVAILABLE_AMOUNT
      ? `최대 ${formatKoreanAmount(
          MAX_MONTHLY_AVAILABLE_AMOUNT,
        )}까지 입력할 수 있어요`
      : '';
};

// --- 신용점수 수정 관련 ---
const editCreditScore = () => {
  tempCreditScore.value = Math.min(
    auth.creditScore || 0,
    MAX_CREDIT_SCORE,
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
    await api.patch('/api/users/me/credit-score', { creditScore: tempCreditScore.value });
    auth.state.user.creditScore = tempCreditScore.value;
    isEditingCreditScore.value = false;
  } catch (error) {
    console.error('Failed to update credit score', error);
    alert('신용점수 수정에 실패했습니다.');
  }
};

// --- 월 상환 가능 금액 수정 관련 ---
const editMaxPayment = () => {
  tempMaxPayment.value = auth.maxMonthlyPayment || 0;
  maxPaymentError.value = '';
  isEditingMaxPayment.value = true;
};

const cancelMaxPayment = () => {
  maxPaymentError.value = '';
  isEditingMaxPayment.value = false;
};

const saveMaxPayment = async () => {
  try {
    const actualAmount = tempMaxPayment.value;
    await api.patch('/api/users/me/max-monthly-payment', { maxMonthlyPayment: actualAmount });
    auth.state.user.maxMonthlyPayment = actualAmount;
    isEditingMaxPayment.value = false;
  } catch (error) {
    console.error('Failed to update max monthly payment', error);
    alert('월 상환 가능 금액 수정에 실패했습니다.');
  }
};

// --- 로그아웃 및 기타 유틸 ---
const handleLogout = () => {
  if (confirm('로그아웃 하시겠습니까?')) {
    auth.logout();
    alert('로그아웃 되었습니다.');
    router.push('/login');
  }
};

const formatKoreanAmount = (amount, emptyMessage = '금액을 입력해주세요') => {
  if (!amount) return emptyMessage;

  const jo = Math.floor(amount / 1_000_000_000_000);
  const eok = Math.floor((amount % 1_000_000_000_000) / 100_000_000);
  const man = Math.floor((amount % 100_000_000) / 10_000);
  const won = amount % 10_000;
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
      <div class="card-body d-flex align-items-center">
        <div class="avatar-circle me-3 fw-bold d-flex align-items-center justify-content-center">
          {{ initialChar }}
        </div>
        <div>
          <h5 class="card-title fw-bold mb-1">{{ auth.name || '유저' }}</h5>
          <p class="card-text text-muted mb-0 email-text">{{ auth.email }}</p>
        </div>
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
        
        <div v-if="!isEditingCreditScore" class="data-box rounded-3 px-3 py-3 mt-3 d-flex justify-content-between align-items-center">
          <span class="text-secondary fw-semibold">KCB {{ getCreditGrade(auth.creditScore) }}</span>
          <span class="fw-bold fs-5">{{ auth.creditScore || 0 }}점</span>
        </div>

        <div v-else class="mt-3">
          <div class="data-box rounded-3 px-3 py-3 d-flex justify-content-between align-items-center mb-3 edit-input-wrapper">
            <span class="text-secondary fw-semibold">현재 설정 점수</span>
            <div class="text-end">
              <div class="d-flex align-items-center justify-content-end">
                <input
                  type="text"
                  inputmode="numeric"
                  pattern="[0-9]*"
                  maxlength="4"
                  class="form-control text-end border-0 bg-transparent fw-bold fs-5 p-0 me-1 edit-input"
                  :value="tempCreditScore"
                  @input="handleCreditScoreInput"
                />
                <span class="fw-bold fs-5">점</span>
              </div>
              <div
                v-if="creditScoreError"
                class="text-danger mt-1 input-message"
              >
                {{ creditScoreError }}
              </div>
            </div>
          </div>
          <div class="d-flex gap-2">
            <button class="btn btn-warning fw-bold flex-grow-1 text-white save-btn rounded-3 py-2" @click="saveCreditScore">저장</button>
            <button class="btn btn-outline-secondary fw-bold flex-grow-1 cancel-btn rounded-3 bg-white py-2" @click="cancelCreditScore">취소</button>
          </div>
        </div>
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
        
        <div v-if="!isEditingMaxPayment" class="data-box rounded-3 px-3 py-3 mt-3 d-flex justify-content-between align-items-center">
          <span class="text-secondary fw-semibold">현재 설정 금액</span>
          <div class="text-end">
            <div class="fw-bold fs-5">{{ (auth.maxMonthlyPayment || 0).toLocaleString() }} <span class="text-secondary fs-6">원</span></div>
            <div class="text-secondary mt-1" style="font-size: 0.9rem;">
              {{ formatKoreanAmount(auth.maxMonthlyPayment, '0원') }}
            </div>
          </div>
        </div>

        <div v-else class="mt-3">
          <div class="data-box rounded-3 px-3 py-3 d-flex justify-content-between align-items-center gap-3 mb-3 edit-input-wrapper">
            <span class="text-secondary fw-semibold flex-shrink-0">현재 설정 금액</span>
            <div class="text-end monthly-edit-value">
              <div class="d-flex align-items-center justify-content-end">
                <input
                  type="text"
                  inputmode="numeric"
                  pattern="[0-9]*"
                  class="form-control text-end border-0 bg-transparent fw-bold fs-5 p-0 me-1 edit-input amount-edit-input"
                  :value="tempMaxPayment"
                  @input="handleMaxPaymentInput"
                />
                <span class="fw-bold fs-5">원</span>
              </div>
              <div
                class="mt-1"
                :class="maxPaymentError ? 'text-danger' : 'text-secondary'"
                style="font-size: 0.9rem;"
              >
                {{
                  maxPaymentError ||
                  formatKoreanAmount(tempMaxPayment)
                }}
              </div>
            </div>
          </div>
          <div class="d-flex gap-2">
            <button class="btn btn-warning fw-bold flex-grow-1 text-white save-btn rounded-3 py-2" @click="saveMaxPayment">저장</button>
            <button class="btn btn-outline-secondary fw-bold flex-grow-1 cancel-btn rounded-3 bg-white py-2" @click="cancelMaxPayment">취소</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Logout Button -->
    <button class="btn logout-btn w-100 fw-bold py-3 mt-3 rounded-4" @click="handleLogout">
      로그아웃
    </button>
    
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

.edit-input {
  width: 100px;
}

.monthly-edit-value {
  flex: 1 1 220px;
  min-width: 0;
  max-width: 260px;
}

.amount-edit-input {
  width: 190px;
  max-width: 100%;
  min-width: 0;
}

.input-message {
  font-size: var(--kb-font-xs);
  line-height: 1.35;
}

.edit-input:focus {
  box-shadow: none;
}

.save-btn {
  background-color: #ffb800;
  border: none;
}
.save-btn:hover {
  background-color: #e5a600;
}
.cancel-btn {
  border: 1px solid #c9c9c9;
  color: #555;
}
.cancel-btn:hover {
  background-color: #f5f5f5;
}
</style>
