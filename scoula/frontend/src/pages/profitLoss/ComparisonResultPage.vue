<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useComparison } from '@/composables/useComparison';

const route = useRoute();
const router = useRouter();
const { comparison, loading, error, fetchComparison } = useComparison();

const load = () => fetchComparison(route.params.comparisonId);

onMounted(load);

const won = (value) => (value ?? 0).toLocaleString('ko-KR');

const isLoanWinner = computed(() => comparison.value?.winner === 'LOAN');
const isDepositWinner = computed(() => comparison.value?.winner === 'WITHDRAWAL');
const isTie = computed(() => comparison.value?.winner === 'TIE');

const loanTypeLabel = computed(() =>
  comparison.value?.loan.type === 'CREDIT' ? '신용대출' : '전세자금대출'
);

// 시안은 추천(승자) 카드가 항상 왼쪽에 오도록 되어 있다. TIE는 기존 순서(대출→예금) 유지.
const loanCardOrder = computed(() => (isDepositWinner.value ? 2 : 1));
const depositCardOrder = computed(() => (isDepositWinner.value ? 1 : 2));

const partialAllowedText = computed(
  () => `부분해지 ${comparison.value?.badges.isPartialAllowed ? 'O' : 'X'}`
);
const lumpSumText = computed(
  () => `만기시 예금상환 ${comparison.value?.badges.isLumpSum ? 'O' : 'X'}`
);

// 서버 메시지는 숫자 없이 고정 문구라 앞에 실제 차액/최저임금 금액을 붙여준다.
const warningMessage = computed(() => {
  const w = comparison.value?.warning;
  if (!w?.message) return '';
  if (/\d/.test(w.message)) return w.message;

  const amountPrefix = `이 차액(${won(comparison.value.savingAmount)}원)은 최저임금 하루치(${won(
    w.minimumWageDaily
  )}원)보다 적어요.`;
  const rest = w.message.replace(/^이 차액은 최저임금 하루치보다 적어요\.\s*/, '');
  return `${amountPrefix} ${rest}`.trim();
});

const loanDetailOpen = ref(false);
const depositDetailOpen = ref(false);

const maxFinalBalance = computed(() => {
  if (!comparison.value) return 0;
  return Math.max(comparison.value.loan.finalBalance, comparison.value.deposit.finalBalance);
});

const loanBarWidth = computed(() => {
  if (!comparison.value || maxFinalBalance.value === 0) return 0;
  return (comparison.value.loan.finalBalance / maxFinalBalance.value) * 100;
});

const depositBarWidth = computed(() => {
  if (!comparison.value || maxFinalBalance.value === 0) return 0;
  return (comparison.value.deposit.finalBalance / maxFinalBalance.value) * 100;
});

// 추천이 아닌 쪽을 누르면 손실경고 모달을 먼저 띄운다.
const showLossModal = ref(false);
const pendingAction = ref(null); // 'LOAN' | 'WITHDRAWAL'

const actionLabel = (action) => (action === 'LOAN' ? '대출' : '예금 중도해지');

const proceed = (action) => {
  if (action !== comparison.value.winner) {
    pendingAction.value = action;
    showLossModal.value = true;
    return;
  }
  confirmProceed(action);
};

const confirmProceed = (action) => {
  showLossModal.value = false;
  // TODO(5단계): 실제 진행 로직(다음 화면 이동 등) 연결
  alert(`${actionLabel(action)}(으)로 진행합니다.`);
};

const cancelModal = () => {
  showLossModal.value = false;
  pendingAction.value = null;
};
</script>

<template>
  <main class="result-page">
    <div v-if="comparison" class="result-content">
      <!-- 1. 헤더 -->
      <header class="page-header">
        <button
          type="button"
          class="back-button"
          aria-label="이전 화면으로 이동"
          @click="router.back()"
        >
          ‹
        </button>
        <h1>득실 비교 결과</h1>
        <span class="header-hint">만기 기준</span>
      </header>

      <!-- 2. 결론 배너 카드 -->
      <section class="card banner-card">
        <p class="banner-lead">{{ won(comparison.urgentAmount) }}원이 필요할 때, 가장 남는 선택은</p>
        <p class="banner-main">
          <template v-if="isTie">두 방법의 결과가 같습니다</template>
          <template v-else>
            {{ comparison.badges.recommended }}
            <span class="gold">{{ won(comparison.savingAmount) }}원</span> 더 이득
          </template>
        </p>

        <div v-if="!isTie" class="badge-row">
          <span class="badge badge-recommended">추천 · {{ comparison.badges.recommended }}</span>
          <span class="badge badge-soft">{{ partialAllowedText }}</span>
          <span class="badge badge-soft">{{ lumpSumText }}</span>
        </div>
      </section>

      <!-- 3. 최저임금 경고 박스 -->
      <section v-if="comparison.warning.isBelowMinimumWage" class="warning-box">
        <i class="fa-solid fa-triangle-exclamation warning-icon" aria-hidden="true"></i>
        <p>{{ warningMessage }}</p>
      </section>

      <!-- 4. 비교 카드 2장 -->
      <section class="compare-grid">
        <div
          class="card compare-card"
          :class="{ winner: isLoanWinner }"
          :style="{ order: loanCardOrder }"
        >
          <span v-if="isLoanWinner" class="winner-tag">추천</span>
          <p class="compare-title">② {{ loanTypeLabel }} · 예금 유지</p>
          <p class="compare-amount">{{ won(comparison.loan.finalBalance) }}원</p>

          <div class="compare-divider"></div>

          <button
            type="button"
            class="detail-toggle"
            @click="loanDetailOpen = !loanDetailOpen"
          >
            상세 보기 {{ loanDetailOpen ? '▲' : '▼' }}
          </button>

          <div v-show="loanDetailOpen" class="detail-table">
            <div class="detail-row">
              <span>비용 (이자+수수료)</span>
              <span class="detail-value">
                {{ won(comparison.loan.cost) }}원
                <small>(이자 {{ won(comparison.loan.interest) }} + 수수료 {{ won(comparison.loan.penalty) }})</small>
              </span>
            </div>
            <div class="detail-row">
              <span>만기이자</span>
              <span class="detail-value">{{ won(comparison.deposit.maintainInterest) }}원</span>
            </div>
            <div class="detail-row">
              <span>총 이득</span>
              <span class="detail-value strong">{{ won(comparison.loan.netProfit) }}원</span>
            </div>
            <p v-if="comparison.loan.isRateEstimated" class="detail-note">
              ※ 추정치이며 실제 심사금리와 다를 수 있습니다
            </p>
          </div>
        </div>

        <div
          class="card compare-card"
          :class="{ winner: isDepositWinner }"
          :style="{ order: depositCardOrder }"
        >
          <span v-if="isDepositWinner" class="winner-tag">추천</span>
          <p class="compare-title">① 중도 또는 부분해지 <span class="info-icon">ⓘ</span></p>
          <p class="compare-amount">{{ won(comparison.deposit.finalBalance) }}원</p>

          <div class="compare-divider"></div>

          <button
            type="button"
            class="detail-toggle"
            @click="depositDetailOpen = !depositDetailOpen"
          >
            상세 보기 {{ depositDetailOpen ? '▲' : '▼' }}
          </button>

          <div v-show="depositDetailOpen" class="detail-table">
            <div class="detail-row">
              <span>중도해지이율</span>
              <span class="detail-value">연 {{ comparison.deposit.cancelInterestRate }}%</span>
            </div>
            <div class="detail-row">
              <span>해지수익</span>
              <span class="detail-value strong">{{ won(comparison.deposit.withdrawalProfit) }}원</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 5. 바 차트 카드 -->
      <section class="card chart-card">
        <div class="chart-header">
          <span>만기 시점 최종 잔액</span>
          <span class="gold">차이 {{ won(comparison.savingAmount) }}원</span>
        </div>

        <div class="chart-row">
          <div class="chart-row-label">
            <span>신용대출</span>
            <strong>{{ won(comparison.loan.finalBalance) }}원</strong>
          </div>
          <div class="chart-bar-track">
            <div
              class="chart-bar-fill"
              :class="isLoanWinner ? 'gold' : 'gray'"
              :style="{ width: loanBarWidth + '%' }"
            ></div>
          </div>
        </div>

        <div class="chart-row">
          <div class="chart-row-label">
            <span>중도해지</span>
            <strong>{{ won(comparison.deposit.finalBalance) }}원</strong>
          </div>
          <div class="chart-bar-track">
            <div
              class="chart-bar-fill"
              :class="isDepositWinner ? 'gold' : 'gray'"
              :style="{ width: depositBarWidth + '%' }"
            ></div>
          </div>
        </div>
      </section>

      <!-- 6. 월 예상 상환액 -->
      <section class="card monthly-card">
        <span>월 예상 상환액</span>
        <strong>{{ won(comparison.monthlyPayment) }}원</strong>
      </section>

      <!-- 7. 하단 안내 문구 -->
      <p class="footnote">
        ※ 본 결과는 [득실]만의 계산기로 산출한 참고용 예상값이며, 실제 적용금리와 대출한도는
        개인의 신용조건 및 금융기관 심사 결과에 따라 달라질 수 있습니다.
      </p>

      <!-- 8. 진행 버튼 -->
      <div class="proceed-row">
        <button
          type="button"
          class="proceed-button"
          :class="isLoanWinner ? 'primary' : 'secondary'"
          @click="proceed('LOAN')"
        >
          {{ loanTypeLabel }}로 진행
        </button>
        <button
          type="button"
          class="proceed-button"
          :class="!isLoanWinner ? 'primary' : 'secondary'"
          @click="proceed('WITHDRAWAL')"
        >
          예금 중도해지로 진행
        </button>
      </div>

      <!-- 손실경고 모달 -->
      <div v-if="showLossModal" class="modal-overlay" @click.self="cancelModal">
        <section class="modal-card">
          <h2><i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i> 손실 경고</h2>
          <p>{{ won(comparison.savingAmount) }}원 손해를 보는 선택입니다. 진행하시겠습니까?</p>
          <div class="modal-actions">
            <button type="button" class="modal-cancel" @click="cancelModal">취소</button>
            <button type="button" class="modal-confirm" @click="confirmProceed(pendingAction)">진행</button>
          </div>
        </section>
      </div>
    </div>

    <div v-else-if="loading" class="state-view">
      <span class="loading-spinner" aria-hidden="true"></span>
      <p>결과를 불러오는 중이에요...</p>
    </div>

    <div v-else-if="error" class="state-view">
      <i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i>
      <p>{{ error }}</p>
      <button type="button" class="retry-button" @click="load">
        <i class="fa-solid fa-rotate-right" aria-hidden="true"></i> 다시 시도
      </button>
    </div>
  </main>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

button {
  font: inherit;
}

.result-page {
  --gs-bg: #faf9f7;
  --gs-card: #ffffff;
  --gs-text: #292725;
  --gs-text-sub: #9a938a;
  --gs-gold: #ffbc00;
  --gs-gold-soft: #fff8df;
  --gs-gold-deep: #c8bfae;
  --gs-warn-bg: #fdecef;
  --gs-warn-line: #ffb8d6;
  --gs-warn-text: #99295a;
  --gs-line: #e9e0d2;

  width: 100%;
  max-width: 390px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 18px 16px 28px;
  color: var(--gs-text);
  background: var(--gs-bg);
}

.gold {
  color: var(--gs-gold);
}

.card {
  padding: 16px;
  border-radius: 16px;
  background: var(--gs-card);
  box-shadow: 0 1px 3px rgb(0 0 0 / 4%);
}

.result-content > * + * {
  margin-top: 14px;
}

/* 1. 헤더 */
.page-header {
  display: flex;
  align-items: center;
}

.back-button {
  display: grid;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 12px;
  flex-shrink: 0;
  font-size: 20px;
  line-height: 1;
  color: var(--gs-text);
  background: #fff;
  place-items: center;
}

.page-header h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  flex: 1;
  text-align: center;
}

.header-hint {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--gs-text-sub);
  white-space: nowrap;
  text-align: right;
}

/* 2. 결론 배너 */
.banner-lead {
  margin: 0;
  font-size: 14px;
  color: var(--gs-text-sub);
}

.banner-main {
  margin: 8px 0 0;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
}

.badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.badge {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
}

.badge-recommended {
  color: var(--gs-text);
  background: var(--gs-gold-deep);
}

.badge-soft {
  color: var(--gs-text);
  background: var(--gs-gold-soft);
}

/* 3. 최저임금 경고 */
.warning-box {
  display: flex;
  padding: 14px;
  border: 1px solid var(--gs-warn-line);
  border-radius: 12px;
  align-items: flex-start;
  gap: 8px;
  background: var(--gs-warn-bg);
}

.warning-icon {
  margin-top: 1px;
  color: #e0316f;
}

.warning-box p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--gs-warn-text);
}

/* 4. 비교 카드 2장 */
.compare-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.compare-card {
  position: relative;
  border: 1px solid var(--gs-line);
}

.compare-card.winner {
  border: 2px solid var(--gs-gold);
}

.winner-tag {
  position: absolute;
  top: -10px;
  left: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--gs-text);
  background: var(--gs-gold);
}

.compare-title {
  margin: 0;
  font-size: 13px;
  color: var(--gs-text-sub);
}

.info-icon {
  font-size: 11px;
}

.compare-amount {
  margin: 6px 0 0;
  font-size: 20px;
  font-weight: 700;
  word-break: break-all;
}

.compare-divider {
  margin: 12px 0;
  border-top: 1px solid var(--gs-line);
}

.detail-toggle {
  width: 100%;
  padding: 0;
  border: 0;
  font-size: 13px;
  color: var(--gs-text-sub);
  text-align: center;
  background: transparent;
}

.detail-table {
  margin-top: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--gs-text-sub);
}

.detail-value {
  color: var(--gs-text);
  text-align: right;
}

.detail-value.strong {
  font-weight: 700;
}

.detail-value small {
  display: block;
  font-size: 10px;
  color: var(--gs-text-sub);
}

.detail-note {
  margin: 8px 0 0;
  font-size: 10px;
  color: #e54848;
}

/* 5. 바 차트 */
.chart-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 700;
}

.chart-header .gold {
  font-size: 14px;
}

.chart-row + .chart-row {
  margin-top: 14px;
}

.chart-row-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}

.chart-bar-track {
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  background: var(--gs-line);
}

.chart-bar-fill {
  height: 100%;
  border-radius: 999px;
}

.chart-bar-fill.gold {
  background: var(--gs-gold);
}

.chart-bar-fill.gray {
  background: var(--gs-gold-deep);
}

/* 6. 월 예상 상환액 */
.monthly-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.monthly-card span {
  font-size: 14px;
  color: var(--gs-text-sub);
}

.monthly-card strong {
  font-size: 17px;
  font-weight: 700;
}

/* 7. 하단 안내 문구 */
.footnote {
  margin: 0;
  padding: 16px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--gs-text-sub);
}

/* 8. 진행 버튼 */
.proceed-row {
  display: flex;
  gap: 10px;
}

.proceed-button {
  height: 54px;
  border-radius: 13px;
  flex: 1;
  font-weight: 700;
}

.proceed-button.primary {
  border: 0;
  color: var(--gs-text);
  background: var(--gs-gold);
}

.proceed-button.secondary {
  border: 1px solid var(--gs-line);
  color: var(--gs-text);
  background: #fff;
}

/* 손실경고 모달 */
.modal-overlay {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  padding: 24px;
  background: rgb(0 0 0 / 50%);
  place-items: center;
}

.modal-card {
  width: 100%;
  max-width: 320px;
  padding: 22px 20px 18px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 16px 40px rgb(0 0 0 / 18%);
}

.modal-card h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #e54848;
}

.modal-card p {
  margin: 12px 0 20px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--gs-text);
}

.modal-actions {
  display: flex;
  gap: 8px;
}

.modal-cancel,
.modal-confirm {
  height: 44px;
  border-radius: 11px;
  flex: 1;
  font-weight: 700;
}

.modal-cancel {
  border: 1px solid var(--gs-line);
  color: var(--gs-text);
  background: #fff;
}

.modal-confirm {
  border: 0;
  color: #fff;
  background: #e54848;
}

/* 로딩 / 에러 */
.state-view {
  display: flex;
  min-height: 60vh;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
}

.state-view i {
  font-size: 32px;
  color: var(--gs-warn-text);
}

.state-view p {
  margin: 0;
  color: var(--gs-text-sub);
}

.loading-spinner {
  width: 34px;
  height: 34px;
  border: 4px solid var(--gs-gold-soft);
  border-top-color: var(--gs-gold);
  border-radius: 50%;
  animation: loading-spin 0.8s linear infinite;
}

@keyframes loading-spin {
  to {
    transform: rotate(360deg);
  }
}

.retry-button {
  padding: 10px 20px;
  border: 0;
  border-radius: 12px;
  color: var(--gs-text);
  background: var(--gs-gold);
}
</style>
