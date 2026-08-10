<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useComparison } from '@/composables/useComparison';
import { toDisplayKbAccount } from '@/util/depositFormat';
import ConfirmModal from '@/components/ConfirmModal.vue';

const route = useRoute();
const router = useRouter();
const { comparison, loading, error, fetchComparison } = useComparison();

const load = () => fetchComparison(route.params.comparisonId);

onMounted(load);

const won = (value) => (value ?? 0).toLocaleString('ko-KR');

// BigDecimal 금리(소수점 3자리)를 화면 표시용 2자리로 줄인다. 계산 정확도는
// 백엔드 setScale(3)이 그대로 유지하고 여기서는 표시만 줄인다.
const rate = (value) => {
  if (value == null) return '-';
  return Number(value).toFixed(2);
};

// 억 단위가 있으면 "억 + 만"으로, 없으면 "만"만 남긴다. 딱 떨어지는 억은 "만원"을 생략한다.
const formatManwon = (value) => {
  const man = Math.round((value ?? 0) / 10000);
  const eok = Math.floor(man / 10000);
  const remainder = man % 10000;
  if (eok > 0) {
    return remainder > 0
      ? `${eok}억 ${remainder.toLocaleString('ko-KR')}만원`
      : `${eok}억원`;
  }
  return `${man.toLocaleString('ko-KR')}만원`;
};

const isLoanWinner = computed(() => comparison.value?.winner === 'LOAN');
const isDepositWinner = computed(
  () => comparison.value?.winner === 'WITHDRAWAL',
);
const isTie = computed(() => comparison.value?.winner === 'TIE');

const loanTypeLabel = computed(() =>
  comparison.value?.loan.type === 'CREDIT' ? '신용대출' : '전세자금대출',
);

// 배너 둘째 줄(결론)용 — 대출 승이면 종류명, 예금 승이면 고정 문구.
const winnerTypeLabel = computed(() =>
  isLoanWinner.value ? loanTypeLabel.value : '중도해지',
);

// 배너 첫 줄(무엇을 비교했는지)용 — 실제 상품명. 동점은 승자가 없어 표시하지 않는다.
const winnerProductName = computed(() =>
  isLoanWinner.value
    ? comparison.value?.loan.name
    : comparison.value?.deposit.name,
);

const partialAllowedText = computed(() =>
  comparison.value?.badges.isPartialAllowed ? '부분해지 가능' : '부분해지 불가',
);
const lumpSumText = computed(() =>
  comparison.value?.badges.isLumpSum
    ? '예금 만기에 일시 상환'
    : '월 납입으로만 상환',
);

// 부분해지·상환방식은 사용자가 고른 조건, 이 배지는 계산의 전제라 줄을 나눈다.
const urgentAmountBadgeText = computed(
  () => `${formatManwon(comparison.value?.urgentAmount)} 필요할때`,
);

// 서버 메시지는 숫자 없이 고정 문구라 앞에 실제 차액/최저임금 금액을 붙여준다.
const warningMessage = computed(() => {
  const w = comparison.value?.warning;
  if (!w?.message) return '';
  if (/\d/.test(w.message)) return w.message;

  const amountPrefix = `이 차액(${won(comparison.value.savingAmount)}원)은 최저임금 하루치(${won(
    w.minimumWageDaily,
  )}원)보다 적어요.`;
  const rest = w.message.replace(
    /^이 차액은 최저임금 하루치보다 적어요\.\s*/,
    '',
  );
  return `${amountPrefix} ${rest}`.trim();
});

const loanDetailOpen = ref(false);
const depositDetailOpen = ref(false);

// 잔액은 예금 원금이라는 공통분모를 나눠 가져 막대 차이가 잘 안 보인다.
// 손실액(만기까지 뒀을 때와의 차액) 기준으로 그려야 둘의 격차가 드러난다.
// finalBalance - withdrawalProfit = 예금 원금(둘 다 aFinalBalance에서 역산되는 값이라 항상 같다).
const depositMaturityAmount = computed(() => {
  if (!comparison.value) return 0;
  const principal =
    comparison.value.deposit.finalBalance -
    comparison.value.deposit.withdrawalProfit;
  return principal + comparison.value.deposit.maintainInterest;
});

const depositLoss = computed(() =>
  comparison.value
    ? depositMaturityAmount.value - comparison.value.deposit.finalBalance
    : 0,
);

const loanLoss = computed(() =>
  comparison.value
    ? depositMaturityAmount.value - comparison.value.loan.finalBalance
    : 0,
);

const maxLoss = computed(() => Math.max(depositLoss.value, loanLoss.value));

const loanBarWidth = computed(() => {
  if (!comparison.value || maxLoss.value === 0) return 0;
  return (loanLoss.value / maxLoss.value) * 100;
});

const depositBarWidth = computed(() => {
  if (!comparison.value || maxLoss.value === 0) return 0;
  return (depositLoss.value / maxLoss.value) * 100;
});

// 추천이 아닌 쪽을 누르면 손실경고 모달을 먼저 띄운다.
const showLossModal = ref(false);
const pendingAction = ref(null); // 'LOAN' | 'WITHDRAWAL'

// 진행이 확정되면 완료 모달을 띄운다.
const showDoneModal = ref(false);
const doneAction = ref(null);

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
  doneAction.value = action;
  showDoneModal.value = true;
};

const cancelModal = () => {
  showLossModal.value = false;
  pendingAction.value = null;
};

const closeDoneModal = () => {
  showDoneModal.value = false;
  doneAction.value = null;
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
          <i class="fa-solid fa-chevron-left"></i>
        </button>
        <h1>득실 비교 결과</h1>
        <span class="header-hint">만기 기준</span>
        <router-link
          to="/"
          class="header-home-link"
          aria-label="홈 화면으로 이동"
        >
          <i class="fa-solid fa-house" aria-hidden="true"></i>
        </router-link>
      </header>

      <!-- 2. 결론 배너 카드 -->
      <section class="card banner-card">
        <template v-if="isTie">
          <p class="banner-main">두 방법의 결과가 같습니다</p>
        </template>
        <template v-else>
          <p class="banner-product-name">{{ winnerProductName }}</p>
          <p class="banner-main">
            {{ winnerTypeLabel }}
            <span class="gold">{{ won(comparison.savingAmount) }}원</span>
            <span class="banner-nowrap">더 이득</span>
          </p>
        </template>

        <div v-if="!isTie" class="badge-row">
          <span class="badge badge-soft">{{ partialAllowedText }}</span>
          <span class="badge badge-soft">{{ lumpSumText }}</span>
        </div>
        <div v-if="!isTie" class="badge-row badge-row-premise">
          <span class="badge badge-soft">{{ urgentAmountBadgeText }}</span>
        </div>
      </section>

      <!-- 3. 최저임금 경고 박스 -->
      <section v-if="comparison.warning.isBelowMinimumWage" class="warning-box">
        <i
          class="fa-solid fa-triangle-exclamation warning-icon"
          aria-hidden="true"
        ></i>
        <p>{{ warningMessage }}</p>
      </section>

      <!-- 4. 비교 카드 2장 -->
      <section class="compare-grid">
        <div class="card compare-card" :class="{ winner: isDepositWinner }">
          <span v-if="isDepositWinner" class="winner-tag">추천</span>
          <p class="compare-title">
            ① 중도 또는 부분해지 <span class="info-icon">ⓘ</span>
          </p>
          <p
            v-if="comparison.deposit.name"
            class="compare-product"
            :title="comparison.deposit.name"
          >
            {{ comparison.deposit.name }}
          </p>
          <p
            v-if="comparison.deposit.accountNumber"
            class="compare-account"
            :title="toDisplayKbAccount(comparison.deposit.accountNumber)"
          >
            {{ toDisplayKbAccount(comparison.deposit.accountNumber) }}
          </p>
          <p class="compare-amount-label">만기 시점 잔액</p>
          <p class="compare-amount">
            {{ won(comparison.deposit.finalBalance) }}원
          </p>

          <div class="compare-divider"></div>

          <div class="compare-actions">
            <button
              type="button"
              class="detail-toggle"
              @click="depositDetailOpen = !depositDetailOpen"
            >
              상세 보기 {{ depositDetailOpen ? '▲' : '▼' }}
            </button>

            <div v-show="depositDetailOpen" class="detail-table">
              <div class="detail-row">
                <span>적용금리</span>
                <span class="detail-value"
                  >연 {{ rate(comparison.deposit.appliedRate) }}%</span
                >
              </div>
              <div class="detail-row">
                <span>중도해지이율</span>
                <span class="detail-value"
                  >연 {{ rate(comparison.deposit.cancelInterestRate) }}%</span
                >
              </div>
              <div class="detail-row">
                <span>해지수익</span>
                <span class="detail-value strong"
                  >{{ won(comparison.deposit.withdrawalProfit) }}원</span
                >
              </div>
            </div>
          </div>
        </div>

        <div class="card compare-card" :class="{ winner: isLoanWinner }">
          <span v-if="isLoanWinner" class="winner-tag">추천</span>
          <p class="compare-title">② {{ loanTypeLabel }} · 예금 유지</p>
          <p
            v-if="comparison.loan.name"
            class="compare-product"
            :title="comparison.loan.name"
          >
            {{ comparison.loan.name }}
          </p>
          <p class="compare-amount-label">만기 시점 잔액</p>
          <p class="compare-amount">
            {{ won(comparison.loan.finalBalance) }}원
          </p>

          <div class="compare-divider"></div>

          <div class="compare-actions">
            <button
              type="button"
              class="detail-toggle"
              @click="loanDetailOpen = !loanDetailOpen"
            >
              상세 보기 {{ loanDetailOpen ? '▲' : '▼' }}
            </button>

            <div v-show="loanDetailOpen" class="detail-table">
              <div class="detail-row">
                <span>대출금리</span>
                <span class="detail-value">
                  연 {{ rate(comparison.loan.interestRate) }}%
                  <small class="detail-sub"
                    >({{ comparison.loan.ratePeriodMonths }}개월)</small
                  >
                </span>
              </div>
              <div class="detail-row">
                <span>총비용</span>
                <span class="detail-value">
                  {{ won(comparison.loan.cost) }}원
                  <small class="detail-sub"
                    >이자 {{ won(comparison.loan.interest) }} + 수수료
                    {{ won(comparison.loan.penalty) }}</small
                  >
                </span>
              </div>
              <div class="detail-row">
                <span>만기이자</span>
                <span class="detail-value"
                  >{{ won(comparison.deposit.maintainInterest) }}원</span
                >
              </div>
              <div class="detail-row">
                <span>총 이득</span>
                <span class="detail-value strong"
                  >{{ won(comparison.loan.netProfit) }}원</span
                >
              </div>
              <p v-if="comparison.loan.isRateEstimated" class="detail-note">
                ※ 추정치이며 실제 심사금리와 다를 수 있습니다
              </p>
            </div>
          </div>
        </div>
      </section>

      <!-- 5. 바 차트 카드 -->
      <section class="card chart-card">
        <div class="chart-header">
          <span>손실 비교</span>
          <span class="gold">차이 {{ won(comparison.savingAmount) }}원</span>
        </div>

        <div class="chart-row">
          <div class="chart-row-label">
            <span>신용대출</span>
            <strong>{{ won(loanLoss) }}원</strong>
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
            <strong>{{ won(depositLoss) }}원</strong>
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
        <div class="detail-row">
          <span>월 예상 상환액</span>
          <span class="detail-value strong"
            >{{ won(comparison.monthlyPayment) }}원</span
          >
        </div>
      </section>

      <!-- 7. 하단 안내 문구 -->
      <p class="footnote">
        ※ 본 결과는 [득실]만의 계산기로 산출한 참고용 예상값이며, 실제
        적용금리와 대출한도는 개인의 신용조건 및 금융기관 심사 결과에 따라
        달라질 수 있습니다.
      </p>

      <!-- 8. 진행 버튼 -->
      <div class="proceed-row">
        <button
          type="button"
          class="proceed-button"
          :class="!isLoanWinner ? 'primary' : 'secondary'"
          @click="proceed('WITHDRAWAL')"
        >
          예금 중도해지로 진행
        </button>
        <button
          type="button"
          class="proceed-button"
          :class="isLoanWinner ? 'primary' : 'secondary'"
          @click="proceed('LOAN')"
        >
          {{ loanTypeLabel }}로 진행
        </button>
      </div>

      <!-- 손실경고 모달 -->
      <ConfirmModal
        :visible="showLossModal"
        title="손실 경고"
        :message="`${won(comparison.savingAmount)}원 손해를 보는 선택입니다. 진행하시겠습니까?`"
        cancel-text="취소"
        confirm-text="진행"
        @cancel="cancelModal"
        @confirm="confirmProceed(pendingAction)"
      />

      <!-- 완료 안내 모달 -->
      <div
        v-if="showDoneModal"
        class="done-overlay"
        @click.self="closeDoneModal"
      >
        <section class="done-card">
          <h2>
            <i class="fa-solid fa-circle-check" aria-hidden="true"></i> 신청
            완료
          </h2>
          <p>{{ actionLabel(doneAction) }}(으)로 진행합니다.</p>
          <div class="done-actions">
            <button type="button" class="done-confirm" @click="closeDoneModal">
              확인
            </button>
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
  --gs-warn-icon: #e0316f;
  --gs-warn-strong: #e54848;
  --gs-line: #e9e0d2;

  width: 100%;
  max-width: 390px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 16px 16px 28px;
  color: var(--gs-text);
  background: var(--gs-bg);
  border-right: 1px solid var(--gs-line);
  border-left: 1px solid var(--gs-line);
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
  margin-top: 16px;
}

/* 1. 헤더 */
.page-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex: none;
  border: 1.5px solid var(--kb-border);
  border-radius: 12px;
  background: #fff;
}

.page-header h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  flex: 1;
  text-align: left;
}

.header-hint {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--gs-text-sub);
  white-space: nowrap;
  text-align: right;
}

.header-home-link {
  display: grid;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  color: var(--gs-text);
  background: var(--gs-gold);
  text-decoration: none;
  place-items: center;
}

.header-home-link:focus-visible {
  outline: 3px solid rgb(255 188 0 / 35%);
  outline-offset: 2px;
}

/* 2. 결론 배너 — 은행 앱이라 어떤 상품인지가 먼저 보여야 한다. 상품명이
   가장 진한 색·17px로 위에 오고, 종류명+금액 결론 줄은 22px로 더 크게
   눌러 위계를 유지한다. keep-all은 상품명·종류명이 길어져도 단어 중간이
   아니라 스페이스에서만 줄바꿈되게 한다. "더 이득"은 통째로 nowrap해서
   "더"와 "이득" 사이가 아니라 금액과 "더 이득" 사이에서만 줄바꿈된다. */
.banner-product-name {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--gs-text);
  word-break: keep-all;
}

.banner-main {
  margin: 8px 0 0;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  word-break: keep-all;
}

.banner-nowrap {
  white-space: nowrap;
}

.badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

/* 조건 배지(부분해지·상환방식)와 전제 배지(필요 금액) 사이는 배지 한 줄 안의
   gap(8px)과 같은 간격을 준다 — 위 두 줄과의 16px보다 좁혀 성격 차이를 보여준다. */
.badge-row-premise {
  margin-top: 8px;
}

.badge {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
}

.badge-soft {
  color: var(--gs-text);
  background: var(--gs-gold-soft);
}

/* 3. 최저임금 경고 */
.warning-box {
  display: flex;
  padding: 16px;
  border: 1px solid var(--gs-warn-line);
  border-radius: 12px;
  align-items: flex-start;
  gap: 8px;
  background: var(--gs-warn-bg);
}

.warning-icon {
  margin-top: 1px; /* 아이콘을 텍스트 첫 줄 높이에 맞추는 미세 보정, 4px 격자와 무관 */
  color: var(--gs-warn-icon);
}

.warning-box p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--gs-warn-text);
}

/* 4. 비교 카드 2장 — 두 카드가 행 트랙을 공유하는 subgrid.
   행 1 제목 / 2 상품명 / 3 계좌번호 / 4 금액 라벨 / 5 금액 / 6 구분선 / 7 상세 보기.
   계좌번호가 없는 카드는 3행이 비어서, 4행(금액 라벨) 위치는 항상 같다.
   상세 보기 버튼+펼침 내용을 .compare-actions로 묶어 7행 하나에 둔다 —
   펼침 내용을 8행으로 따로 두면 subgrid 범위 밖이라 Chrome이 7행 위에
   겹쳐 그린다. 추천 배지는 position: absolute라 행을 배정하지 않는다.
   subgrid 미지원 브라우저는 각 카드가 독립된 행으로 돌아가 정렬만
   어긋날 뿐 레이아웃이 깨지지는 않는다. */
.compare-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: repeat(7, auto);
  column-gap: 12px;
  row-gap: 0; /* subgrid 행은 margin으로만 간격을 두므로 0 고정 — 바꾸면 정렬이 깨진다 */
  align-items: stretch;
}

.compare-card {
  display: grid;
  grid-template-rows: subgrid;
  grid-row: span 7;
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
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--gs-text);
  background: var(--gs-gold);
}

.compare-title {
  grid-row: 1;
  margin: 0;
  font-size: 13px;
  color: var(--gs-text-sub);
}

.compare-product {
  grid-row: 2;
  margin: 4px 0 0;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--gs-text-sub);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.compare-account {
  grid-row: 3;
  margin: 4px 0 0;
  overflow: hidden;
  font-size: 11px;
  line-height: 1.35;
  color: var(--gs-text-sub);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-icon {
  font-size: 11px;
}

.compare-amount-label {
  grid-row: 4;
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--gs-text-sub);
}

.compare-amount {
  grid-row: 5;
  margin: 2px 0 0;
  font-size: 20px;
  font-weight: 700;
  word-break: break-all;
}

.compare-divider {
  grid-row: 6;
  margin: 12px 0;
  border-top: 1px solid var(--gs-line);
}

.compare-actions {
  grid-row: 7;
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

/* keep-all: 짧은 라벨은 flex-shrink 없이도 안 찌그러지고, "670,558원"처럼
   스페이스 없는 값은 한글 음절 사이 줄바꿈으로 숫자와 단위가 갈라지지 않는다. */
.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--gs-text-sub);
  word-break: keep-all;
}

.detail-value {
  color: var(--gs-text);
  text-align: right;
  white-space: nowrap;
}

.detail-value.strong {
  font-weight: 700;
}

/* 금액 줄(.detail-value)에만 nowrap을 걸고 내역 줄은 상속을 끊어 필요하면 줄바꿈되게 한다. */
.detail-sub {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: var(--gs-text-sub);
  text-align: right;
  white-space: normal;
}

.detail-note {
  margin: 8px 0 0;
  font-size: 11px;
  color: var(--gs-warn-strong);
}

/* 5. 바 차트 */
.chart-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 700;
}

.chart-header .gold {
  font-size: 13px;
}

.chart-row + .chart-row {
  margin-top: 16px;
}

.chart-row-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
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

/* 6. 월 예상 상환액 — 행이 하나뿐이라 detail-row의 margin-bottom을 지운다. */
.monthly-card .detail-row {
  margin-bottom: 0;
}

/* 7. 하단 안내 문구 */
.footnote {
  margin: 0;
  padding: 16px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--gs-text-sub);
}

/* 8. 진행 버튼 */
.proceed-row {
  display: flex;
  gap: 12px;
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

/* 완료 안내 모달 — ConfirmModal과 카드 치수를 맞추되(크기가 이어져도 튀지 않게),
   손실 경고(빨강)와 구분되는 골드 팔레트를 쓴다. ConfirmModal은 버튼이 항상
   둘이고 색이 --gs-warn-strong 고정이라 이 용도로 재사용할 수 없다. */
.done-overlay {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  padding: 24px;
  background: rgb(0 0 0 / 50%);
  place-items: center;
}

.done-card {
  width: 100%;
  max-width: 320px;
  padding: 24px 20px 16px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 16px 40px rgb(0 0 0 / 18%);
}

.done-card h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--gs-text);
}

.done-card h2 i {
  color: var(--gs-gold);
}

.done-card p {
  margin: 12px 0 20px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--gs-text);
}

.done-actions {
  display: flex;
  gap: 8px;
}

.done-confirm {
  height: 44px;
  border-radius: 11px;
  flex: 1;
  font-weight: 700;
  font-size: 14px;
  border: 0;
  color: #212121;
  background: #f3c13a;
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
  padding: 12px 20px;
  border: 0;
  border-radius: 12px;
  color: var(--gs-text);
  background: var(--gs-gold);
}
</style>
