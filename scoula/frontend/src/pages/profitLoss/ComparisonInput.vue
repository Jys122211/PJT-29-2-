<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import profitLossApi from '@/api/profitLossApi';
import { useProfitLossStore } from '@/stores/profitLoss';
import api from '@/api';

const router = useRouter();
const profitLossStore = useProfitLossStore();

const requiredAmount = computed({
  get: () => profitLossStore.state.comparisonCondition.urgentAmount,
  set: (value) => profitLossStore.setUrgentAmount(value),
});
const deposits = computed(() => profitLossStore.state.deposits);
const isDepositLoading = ref(false);
const depositLoadError = ref('');

const creditScore = computed(() => profitLossStore.state.creditScore);
const monthlyAvailableAmount = computed(
  () => profitLossStore.state.userFinancialInfo.monthlyPayment,
);
const requiredAmountError = ref('');
const monthlyAmountError = ref('');

const MAX_REQUIRED_AMOUNT = 100_000_000_000_000;
const MAX_MONTHLY_AVAILABLE_AMOUNT = 100_000_000_000;

const amountModel = computed(() =>
  requiredAmount.value.toLocaleString('ko-KR'),
);

function applyFormattedValue(input, formattedValue, digitsBeforeCursor) {
  input.value = formattedValue;

  nextTick(() => {
    let cursorPosition = 0;
    let digitCount = 0;

    while (
      cursorPosition < formattedValue.length &&
      digitCount < digitsBeforeCursor
    ) {
      if (/[0-9]/.test(formattedValue[cursorPosition])) {
        digitCount += 1;
      }
      cursorPosition += 1;
    }

    input.setSelectionRange(cursorPosition, cursorPosition);
  });
}

function handleRequiredAmountInput(event) {
  const input = event.target;
  const cursorPosition = input.selectionStart ?? input.value.length;
  const digitsBeforeCursor = input.value
    .slice(0, cursorPosition)
    .replace(/[^0-9]/g, '').length;
  const numbers = String(input.value).replace(/[^0-9]/g, '');

  if (numbers === '') {
    requiredAmount.value = 0;
    requiredAmountError.value = '';
    input.value = '';
    return;
  }

  const inputAmount = Number(numbers);
  const limitedAmount = Math.min(inputAmount, MAX_REQUIRED_AMOUNT);

  requiredAmount.value = limitedAmount;
  requiredAmountError.value =
    inputAmount > MAX_REQUIRED_AMOUNT
      ? `최대 ${formatKoreanAmount(MAX_REQUIRED_AMOUNT)}까지 입력할 수 있어요`
      : '';
  applyFormattedValue(
    input,
    limitedAmount.toLocaleString('ko-KR'),
    digitsBeforeCursor,
  );
}

function handleCreditScoreInput(event) {
  const input = event.target;
  const numbers = String(input.value)
    .replace(/[^0-9]/g, '')
    .slice(0, 4);

  input.value = numbers;
  profitLossStore.setCreditScore(numbers === '' ? null : Number(numbers));
}

const monthlyAmountModel = computed(() =>
  monthlyAvailableAmount.value === null
    ? ''
    : monthlyAvailableAmount.value.toLocaleString('ko-KR'),
);

function handleMonthlyAmountInput(event) {
  const input = event.target;
  const cursorPosition = input.selectionStart ?? input.value.length;
  const digitsBeforeCursor = input.value
    .slice(0, cursorPosition)
    .replace(/[^0-9]/g, '').length;
  const numbers = String(input.value).replace(/[^0-9]/g, '');

  if (numbers === '') {
    profitLossStore.setMonthlyPayment(null);
    monthlyAmountError.value = '';
    input.value = '';
    return;
  }

  const inputAmount = Number(numbers);
  const limitedAmount = Math.min(inputAmount, MAX_MONTHLY_AVAILABLE_AMOUNT);

  profitLossStore.setMonthlyPayment(limitedAmount);
  monthlyAmountError.value =
    inputAmount > MAX_MONTHLY_AVAILABLE_AMOUNT
      ? `최대 ${formatKoreanAmount(
          MAX_MONTHLY_AVAILABLE_AMOUNT,
        )}까지 입력할 수 있어요`
      : '';

  // 제한값이 기존 값과 같더라도 입력창에는 포맷된 제한값을 즉시 반영합니다.
  applyFormattedValue(
    input,
    limitedAmount.toLocaleString('ko-KR'),
    digitsBeforeCursor,
  );
}

const isCreditScoreValid = computed(
  () =>
    creditScore.value !== null &&
    creditScore.value >= 1 &&
    creditScore.value <= 1000,
);

const creditGrade = computed(
  () => profitLossStore.state.userFinancialInfo.creditGrade,
);

const isCreditGradeEligible = computed(
  () => creditGrade.value !== null && creditGrade.value <= 4,
);

const creditGradeText = computed(() => {
  if (creditScore.value === null) {
    return '신용점수를 입력해주세요';
  }

  if (!isCreditScoreValid.value) {
    return '1~1000점 사이로 입력해주세요';
  }

  if (!isCreditGradeEligible.value) {
    return `${creditGrade.value}등급 · 1~4등급만 비교할 수 있어요`;
  }

  return `${creditGrade.value}등급 · 손익 비교 가능`;
});

function formatKoreanAmount(amount, emptyMessage = '금액을 입력해주세요') {
  if (amount === null || amount === 0) {
    return emptyMessage;
  }

  const jo = Math.floor(amount / 1_000_000_000_000);
  const eok = Math.floor((amount % 1_000_000_000_000) / 100_000_000);
  const man = Math.floor((amount % 100_000_000) / 10_000);
  const won = amount % 10_000;
  const result = [];

  if (jo > 0) {
    result.push(`${jo.toLocaleString('ko-KR')}조`);
  }
  if (eok > 0) {
    result.push(`${eok.toLocaleString('ko-KR')}억`);
  }
  if (man > 0) {
    result.push(`${man.toLocaleString('ko-KR')}만원`);
  }
  if (won > 0) {
    result.push(`${won.toLocaleString('ko-KR')}원`);
  }

  return result.join(' ');
}

const requiredAmountText = computed(() =>
  formatKoreanAmount(requiredAmount.value, '필요한 금액을 입력해주세요'),
);

const monthlyAmountText = computed(() =>
  formatKoreanAmount(
    monthlyAvailableAmount.value,
    '상환 가능 금액을 입력해주세요',
  ),
);

const isMonthlyAmountValid = computed(
  () =>
    monthlyAvailableAmount.value !== null && monthlyAvailableAmount.value > 0,
);

const selectedDeposit = computed(() =>
  deposits.value.find(
    (deposit) => deposit.id === profitLossStore.state.deposit.userDepositId,
  ),
);

const canCompare = computed(
  () =>
    selectedDeposit.value !== undefined &&
    requiredAmount.value > 0 &&
    isCreditScoreValid.value &&
    isCreditGradeEligible.value &&
    isMonthlyAmountValid.value,
);

function addAmount(amount) {
  const nextAmount = requiredAmount.value + amount;
  requiredAmount.value = Math.min(nextAmount, MAX_REQUIRED_AMOUNT);
  requiredAmountError.value =
    nextAmount > MAX_REQUIRED_AMOUNT
      ? `최대 ${formatKoreanAmount(MAX_REQUIRED_AMOUNT)}까지 입력할 수 있어요`
      : '';
}

function resetAmount() {
  requiredAmount.value = 0;
  requiredAmountError.value = '';
}

function registerAsset() {
  // 실제 자산 등록 페이지가 만들어지면 변경
  console.log('자산 등록 페이지로 이동');
}

async function compareProfitLoss() {
  if (!canCompare.value) return;

  console.log('손익 비교 요청:', profitLossStore.requestPayload);
  try {
    await api.patch('/api/users/me/credit-score', {
      creditScore: creditScore.value,
    });
  } catch (error) {
    console.error('신용점수 업데이트 실패:', error);
    alert('신용점수 수정에 실패했습니다.');
    return;
  }

  try {
    await api.patch('/api/users/me/max-monthly-payment', {
      maxMonthlyPayment: monthlyAvailableAmount.value,
    });
  } catch (error) {
    console.error('월 상환 가능 금액 업데이트 실패:', error);
    alert('월 상환 가능 금액 수정에 실패했습니다.');
    return;
  }

  if (profitLossStore.state.loan.loanType === 'CREDIT') {
    router.push({ name: 'creditEligibility' });
    return;
  } else if (profitLossStore.state.loan.loanType === 'JEONSE') {
    router.push({ name: 'jeonseEligibility' });
    return;
  }

  // 추후 백엔드가 구현되면:
  // await api.post('/profit-loss/compare', profitLossStore.requestPayload);
}

// onMounted 함수 정의

async function loadDeposits() {
  isDepositLoading.value = true;
  depositLoadError.value = '';

  try {
    const data = await profitLossApi.getDeposits();

    profitLossStore.setDeposits(data);
  } catch (error) {
    console.error('보유예금 조회 실패:', error);

    profitLossStore.setDeposits([]);
    depositLoadError.value = '보유예금을 불러오지 못했습니다.';
  } finally {
    isDepositLoading.value = false;
  }
}

async function loadUserFinancialInfo() {
  try {
    const data = await profitLossApi.getUserFinancialInfo();
    profitLossStore.setCreditScore(data.creditScore);
    profitLossStore.setMonthlyPayment(data.maxMonthlyPayment);
  } catch (error) {
    console.error('사용자 금융정보 조회 실패:', error);
  }
}

onMounted(() => {
  loadDeposits();
  loadUserFinancialInfo();
});
</script>

<template>
  <main class="calculator-page">
    <section class="calculator-card">
      <header class="page-header">
        <h1>자금 필요 상황 입력</h1>
      </header>

      <!-- 필요한 금액 -->
      <section class="form-section">
        <label for="requiredAmount">필요한 금액</label>

        <div class="amount-input">
          <input
            id="requiredAmount"
            :value="amountModel"
            @input="handleRequiredAmountInput"
            inputmode="numeric"
            aria-label="필요한 금액"
          />
          <span>원</span>
        </div>
        <small :class="['amount-guide', { error: requiredAmountError }]">
          {{ requiredAmountError || requiredAmountText }}
        </small>

        <div class="amount-actions">
          <button type="button" @click="addAmount(1_000_000)">+100만</button>
          <button type="button" @click="addAmount(5_000_000)">+500만</button>
          <button type="button" @click="resetAmount">초기화</button>
        </div>
      </section>

      <!-- 보유 예금 -->
      <section class="form-section">
        <h2>비교에 사용할 보유 예금</h2>

        <div v-if="isDepositLoading" class="empty-deposit">
          <strong>보유예금을 불러오는 중이에요</strong>
          <p>잠시만 기다려주세요</p>
        </div>

        <div v-else-if="depositLoadError" class="empty-deposit">
          <strong>{{ depositLoadError }}</strong>
          <p>네트워크 상태를 확인한 후 다시 시도해주세요</p>
          <button type="button" @click="loadDeposits">다시 시도</button>
        </div>

        <!-- 첫 번째 그림 -->
        <div v-else-if="deposits.length === 0" class="empty-deposit">
          <strong>비교할 보유 예금이 없어요</strong>
          <p>자산 등록에서 예금을 먼저 등록해주세요</p>

          <button type="button" @click="registerAsset">자산 등록하기</button>
        </div>

        <!-- 두 번째 그림 -->
        <div v-else class="deposit-list">
          <button
            v-for="deposit in deposits"
            :key="deposit.id"
            type="button"
            class="deposit-card"
            :class="{
              selected:
                profitLossStore.state.deposit.userDepositId === deposit.id,
            }"
            :aria-pressed="
              profitLossStore.state.deposit.userDepositId === deposit.id
            "
            @click="profitLossStore.selectDeposit(deposit.id)"
          >
            <span class="deposit-info">
              <strong>{{ deposit.productName }}</strong>

              <small>
                {{ deposit.balance.toLocaleString('ko-KR') }}원 · 연
                {{ deposit.interestRate }}% · {{ deposit.maturityText }}
              </small>
            </span>

            <span
              v-if="profitLossStore.state.deposit.userDepositId === deposit.id"
              class="check-icon"
            >
              ✓
            </span>
          </button>
        </div>
      </section>

      <!-- 신용정보 -->
      <section class="credit-grid">
        <div>
          <label for="creditScore">신용점수 (KCB)</label>

          <article class="information-box">
            <div class="information-input-wrapper">
              <input
                id="creditScore"
                :value="creditScore ?? ''"
                @input="handleCreditScoreInput"
                class="information-input"
                type="text"
                inputmode="numeric"
                pattern="[0-9]*"
                maxlength="4"
                placeholder="예: 942"
                aria-label="KCB 신용점수"
              />
              <span class="input-unit">점</span>
            </div>
            <small
              :class="{
                error:
                  creditScore !== null &&
                  (!isCreditScoreValid || !isCreditGradeEligible),
              }"
            >
              {{ creditGradeText }}
            </small>
          </article>
        </div>

        <div>
          <label for="monthlyAmount">월 상환 가능 금액</label>

          <article class="information-box">
            <div class="information-input-wrapper">
              <input
                id="monthlyAmount"
                :value="monthlyAmountModel"
                @input="handleMonthlyAmountInput"
                class="information-input"
                type="text"
                inputmode="numeric"
                placeholder="예: 1,500,000"
                aria-label="월 상환 가능 금액"
              />
              <span class="input-unit">원</span>
            </div>
            <small :class="{ error: monthlyAmountError }">
              {{ monthlyAmountError || monthlyAmountText }}
            </small>
          </article>
        </div>
      </section>

      <aside class="notice">
        ※ 등급별 금리는 KB국민은행 실제 심사금리가 아닙니다. 동일 은행
        일반신용대출의 신용점수 구간별 평균 비율 기반 추정값입니다.
      </aside>

      <!-- 대출 종류 -->
      <section class="loan-section">
        <h2>대출을 받는다면, 어떤 대출로 알아볼까요?</h2>

        <div class="loan-options">
          <button
            type="button"
            :class="{
              selected: profitLossStore.state.loan.loanType === 'JEONSE',
            }"
            :aria-pressed="profitLossStore.state.loan.loanType === 'JEONSE'"
            @click="profitLossStore.setLoanType('JEONSE')"
          >
            전세대출
          </button>

          <button
            type="button"
            :class="{
              selected: profitLossStore.state.loan.loanType === 'CREDIT',
            }"
            :aria-pressed="profitLossStore.state.loan.loanType === 'CREDIT'"
            @click="profitLossStore.setLoanType('CREDIT')"
          >
            신용대출
          </button>
        </div>
      </section>

      <!-- 상태에 따라 같은 버튼의 문구와 활성 상태 변경 -->
      <button
        type="button"
        class="compare-button"
        :disabled="!canCompare"
        @click="compareProfitLoss"
      >
        {{ canCompare ? '손익 비교하기' : '손익 비교하기' }}
      </button>
    </section>

    <nav class="bottom-navigation">
      <button type="button">
        <span>⌂</span>
        홈
      </button>

      <button type="button" class="active">
        <span>▮</span>
        계산기
      </button>

      <button type="button">
        <span>▰</span>
        자산
      </button>

      <button type="button">
        <span>●</span>
        내정보
      </button>
    </nav>
  </main>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

button {
  font: inherit;
}

.calculator-page {
  --kb-yellow: #ffbc00;
  --kb-border: #e9e0d2;
  --kb-text: #292725;
  --kb-muted: #9a938a;

  width: 100%;
  max-width: 390px;
  min-height: 100vh;
  margin: 0 auto;
  padding-bottom: 76px;
  color: var(--kb-text);
  background: #faf9f7;
  border-right: 1px solid #e9e0d2;
  border-left: 1px solid #e9e0d2;
}

.calculator-card {
  display: flex;
  min-height: calc(100vh - 76px);
  padding: 28px 20px 22px;
  flex-direction: column;
}

.page-header {
  display: flex;
  margin-bottom: 24px;
  align-items: flex-start;
  gap: 10px;
}

.page-header h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.35;
}

.form-section {
  margin-bottom: 18px;
}

.form-section label,
.form-section h2,
.loan-section h2,
.credit-grid label {
  display: block;
  margin: 0 0 7px;
  font-size: 12px;
  font-weight: 600;
  color: #686158;
}

.amount-input {
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 16px;
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  background: #fff;
}

.amount-input input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  overflow: hidden;
  font-size: 23px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
}

.amount-input span {
  font-size: 12px;
  color: #777;
}

.amount-guide {
  display: block;
  min-height: 16px;
  margin: 4px 3px 0;
  font-size: 11px;
  color: var(--kb-muted);
}

.amount-guide.error {
  color: #e54848;
}

.amount-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 8px;
}

.amount-actions button {
  height: 36px;
  border: 1px solid var(--kb-border);
  border-radius: 9px;
  color: #615b54;
  background: #fff;
}

.empty-deposit {
  display: flex;
  min-height: 145px;
  padding: 18px 24px;
  border: 4px solid #eee8dc;
  border-radius: 15px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  background: #fff;
  text-align: center;
}

.empty-deposit strong {
  font-size: 17px;
}

.empty-deposit p {
  margin: 8px 0 14px;
  font-size: 12px;
  color: var(--kb-muted);
}

.empty-deposit button {
  min-width: 180px;
  height: 40px;
  border: 0;
  border-radius: 22px;
  background: var(--kb-yellow);
}

.deposit-list {
  display: grid;
  gap: 9px;
  max-height: 175px;
  padding-right: 4px;
  overflow-y: auto;
  overscroll-behavior-y: contain;
  scrollbar-color: #c8bfae transparent;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
}

.deposit-list::-webkit-scrollbar {
  width: 5px;
}

.deposit-list::-webkit-scrollbar-track {
  background: transparent;
}

.deposit-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #c8bfae;
}

.deposit-card {
  display: flex;
  width: 100%;
  min-height: 70px;
  padding: 13px 14px;
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  align-items: center;
  justify-content: space-between;
  text-align: left;
  background: #fff;
}

.deposit-card.selected {
  border: 2px solid var(--kb-yellow);
}

.deposit-info {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.deposit-info strong {
  margin-bottom: 4px;
  font-size: 14px;
}

.deposit-info small {
  overflow: hidden;
  color: var(--kb-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.check-icon {
  display: grid;
  width: 23px;
  height: 23px;
  border-radius: 50%;
  flex-shrink: 0;
  place-items: center;
  background: var(--kb-yellow);
}

.credit-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
  margin-bottom: 10px;
}

.information-box {
  display: flex;
  min-height: 50px;
  padding: 10px 12px;
  border: 1px solid var(--kb-border);
  border-radius: 11px;
  flex-direction: column;
  background: #fff;
}

.information-input-wrapper {
  display: flex;
  min-width: 0;
  align-items: center;
}

.information-input {
  width: 100%;
  min-width: 0;
  padding: 0;
  border: 0;
  outline: 0;
  overflow: hidden;
  font-size: 14px;
  font-weight: 700;
  color: var(--kb-text);
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
}

.information-input::placeholder {
  font-size: 11px;
  font-weight: 400;
  color: #b8b1a8;
}

.input-unit {
  margin-left: 3px;
  flex-shrink: 0;
  font-size: 11px;
  color: #777;
}

.information-box small {
  min-height: 15px;
  margin-top: 2px;
  font-size: 10px;
  color: var(--kb-muted);
}

.information-box small.error {
  color: #e54848;
}

.notice {
  margin-bottom: 20px;
  padding: 13px;
  border-radius: 11px;
  font-size: 10px;
  line-height: 1.6;
  color: #b0a89c;
  background: #f3ede2;
}

.loan-section {
  margin-bottom: 20px;
}

.loan-options {
  display: flex;
  gap: 9px;
}

.loan-options button {
  min-width: 82px;
  height: 38px;
  border: 1px solid var(--kb-border);
  border-radius: 22px;
  background: #fff;
}

.loan-options button.selected {
  border: 2px solid var(--kb-yellow);
  font-weight: 600;
}

.compare-button {
  width: 100%;
  height: 54px;
  min-height: 54px;
  margin-top: auto;
  border: 0;
  border-radius: 13px;
  flex-shrink: 0;
  font-weight: 700;
  color: #292725;
  background: var(--kb-yellow);
}

.compare-button:disabled {
  color: #8c857a;
  background: #ddd5c3;
}

.bottom-navigation {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  display: grid;
  width: 100%;
  max-width: 390px;
  height: 70px;
  margin: auto;
  border-top: 1px solid #ece8e1;
  grid-template-columns: repeat(4, 1fr);
  background: #fff;
}

.bottom-navigation button {
  display: flex;
  border: 0;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 3px;
  font-size: 10px;
  color: #aaa39a;
  background: transparent;
}

.bottom-navigation button span {
  font-size: 18px;
}

.bottom-navigation button.active {
  font-weight: 700;
  color: #292725;
}

.bottom-navigation button.active span {
  color: var(--kb-yellow);
}
</style>
