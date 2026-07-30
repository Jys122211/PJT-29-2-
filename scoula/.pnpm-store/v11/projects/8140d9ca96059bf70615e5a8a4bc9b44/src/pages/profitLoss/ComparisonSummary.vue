<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useProfitLossStore } from '@/stores/profitLoss';

const router = useRouter();
const profitLossStore = useProfitLossStore();

const payload = computed(() => profitLossStore.requestPayload);

const qualificationQuestionIds = computed(() => [
  ...profitLossStore.qualificationQuestionIds,
]);

const preferentialQuestionIds = computed(() => [
  ...profitLossStore.preferentialQuestionIds,
]);

const formattedState = computed(() =>
  JSON.stringify(
    {
      requestPayload: payload.value,
      qualificationQuestionIds: qualificationQuestionIds.value,
      preferentialQuestionIds: preferentialQuestionIds.value,
      creditEligibility: profitLossStore.state.creditEligibility,
      creditPreferential: profitLossStore.state.creditPreferential,
    },
    null,
    2,
  ),
);

function formatNumber(value) {
  if (value == null) return '-';
  return Number(value).toLocaleString('ko-KR');
}

function formatBoolean(value) {
  if (value == null) return '-';
  return value ? 'true (예)' : 'false (아니요)';
}

function formatIds(ids) {
  return ids.length > 0 ? ids.join(', ') : '없음';
}

function goBack() {
  router.push({ name: 'creditPreferential' });
}
</script>

<template>
  <main class="summary-page">
    <section class="summary-content">
      <header class="page-header">
        <button
          type="button"
          class="back-button"
          aria-label="우대금리 화면으로 돌아가기"
          @click="goBack"
        >
          ‹
        </button>
        <div>
          <h1>손익비교 요청값 확인</h1>
          <p>Pinia에 저장된 최종 입력값입니다</p>
        </div>
      </header>

      <div class="summary-scroll">
        <section class="summary-card">
          <h2>사용자 금융정보</h2>
          <dl>
            <div>
              <dt>월 상환 가능 금액</dt>
              <dd>
                {{ formatNumber(payload.userFinancialInfo.monthlyPayment) }}원
              </dd>
            </div>
            <div>
              <dt>신용등급</dt>
              <dd>{{ payload.userFinancialInfo.creditGrade ?? '-' }}등급</dd>
            </div>
          </dl>
        </section>

        <section class="summary-card">
          <h2>예금정보</h2>
          <dl>
            <div>
              <dt>보유예금 ID</dt>
              <dd>{{ payload.deposit.userDepositId ?? '-' }}</dd>
            </div>
            <div>
              <dt>분할 인출 가능 여부</dt>
              <dd>
                {{ formatBoolean(payload.deposit.isPartialAllowed) }}
              </dd>
            </div>
          </dl>
        </section>

        <section class="summary-card">
          <h2>대출정보</h2>
          <dl>
            <div>
              <dt>대출상품 ID</dt>
              <dd>
                {{ formatIds(payload.loan.loanProductId) }}
              </dd>
            </div>
            <div>
              <dt>대출 종류</dt>
              <dd>{{ payload.loan.loanType }}</dd>
            </div>
            <div>
              <dt>최종 우대금리</dt>
              <dd>
                {{
                  payload.loan.totalDiscountRate == null
                    ? '-'
                    : `${payload.loan.totalDiscountRate}%`
                }}
              </dd>
            </div>
          </dl>
        </section>

        <section class="summary-card">
          <h2>비교조건</h2>
          <dl>
            <div>
              <dt>필요 금액</dt>
              <dd>
                {{ formatNumber(payload.comparisonCondition.urgentAmount) }}원
              </dd>
            </div>
            <div>
              <dt>만기 일시상환 여부</dt>
              <dd>
                {{ formatBoolean(payload.comparisonCondition.isLumpSum) }}
              </dd>
            </div>
          </dl>
        </section>

        <section class="summary-card">
          <h2>선택한 질문 ID</h2>
          <dl>
            <div>
              <dt>자격조건 ID</dt>
              <dd>
                {{ formatIds(qualificationQuestionIds) }}
              </dd>
            </div>
            <div>
              <dt>우대조건 ID</dt>
              <dd>
                {{ formatIds(preferentialQuestionIds) }}
              </dd>
            </div>
          </dl>
        </section>

        <section class="json-card">
          <h2>Pinia 전체 상태</h2>
          <pre>{{ formattedState }}</pre>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

.summary-page {
  --kb-yellow: #ffbc00;
  --kb-text: #292725;
  --kb-muted: #918a80;
  --kb-border: #e8e0d4;

  width: 100%;
  max-width: 390px;
  height: 100vh;
  height: 100dvh;
  min-height: 100vh;
  margin: 0 auto;
  overflow: hidden;
  color: var(--kb-text);
  background: #faf9f7;
}

.summary-content {
  display: flex;
  height: 100%;
  min-height: 0;
  padding: 18px 12px 20px;
  overflow: hidden;
  flex-direction: column;
}

.page-header {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
  flex-shrink: 0;
  gap: 10px;
}

.page-header h1 {
  margin: 0;
  font-size: 17px;
  line-height: 1.35;
}

.page-header p {
  margin: 3px 0 0;
  font-size: 10px;
  color: var(--kb-muted);
}

.back-button {
  display: grid;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid var(--kb-border);
  border-radius: 9px;
  flex-shrink: 0;
  font-size: 23px;
  color: #716a62;
  background: #fff;
  place-items: center;
}

.summary-scroll {
  min-height: 0;
  padding-right: 5px;
  overflow-y: auto;
  flex: 1;
  scrollbar-color: #c8bfae transparent;
  scrollbar-width: thin;
}

.summary-scroll::-webkit-scrollbar {
  width: 5px;
}

.summary-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #c8bfae;
}

.summary-card,
.json-card {
  margin-bottom: 12px;
  padding: 15px;
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  background: #fff;
}

.summary-card h2,
.json-card h2 {
  margin: 0 0 10px;
  font-size: 13px;
}

dl {
  margin: 0;
}

dl div {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f2ede5;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

dl div:last-child {
  border-bottom: 0;
}

dt {
  font-size: 11px;
  color: var(--kb-muted);
}

dd {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  text-align: right;
  overflow-wrap: anywhere;
}

.json-card {
  border-color: #ded4c2;
  background: #f5efe4;
}

pre {
  margin: 0;
  padding: 12px;
  overflow-x: auto;
  border-radius: 9px;
  font-size: 10px;
  line-height: 1.55;
  color: #413c36;
  background: #fff;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
