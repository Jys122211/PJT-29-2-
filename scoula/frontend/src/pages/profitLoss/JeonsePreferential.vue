<script setup>
import { computed, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import profitLossApi from '@/api/profitLossApi';
import { useProfitLossStore } from '@/stores/profitLoss';

const router = useRouter();
const profitLossStore = useProfitLossStore();
const isSubmitting = ref(false);
const isLoading = ref(true);
const calculationError = ref('');

const preferentialItems = computed(
  () => profitLossStore.state.jeonsePreferential.items,
);

const isComplete = computed(() => profitLossStore.isJeonsePreferentialComplete);

function selectedAnswer(itemId) {
  return profitLossStore.state.jeonsePreferential.answers[itemId];
}

function answerItem(itemId, answer) {
  profitLossStore.setJeonsePreferentialAnswer(itemId, answer);
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push({ name: 'jeonseEligibility' });
}

async function fetchItems() {
  isLoading.value = true;
  try {
    const items = await profitLossApi.getJeonsePreferentialItems();
    profitLossStore.setJeonsePreferentialItems(items);
  } catch (error) {
    console.error('우대항목 로드 실패:', error);
    calculationError.value = '우대항목을 불러오는데 실패했습니다.';
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  fetchItems();
});

async function continueToNextStep() {
  if (!isComplete.value || isSubmitting.value) return;

  const loanProductId = profitLossStore.state.loan.loanProductId[0];
  const preferentialQuestionIds = [...profitLossStore.jeonsePreferentialQuestionIds];

  if (loanProductId == null) {
    calculationError.value = '우대금리를 계산할 대출 상품이 없습니다.';
    return;
  }

  isSubmitting.value = true;
  calculationError.value = '';

  try {
    const totalDiscountRate = await profitLossApi.getJeonseFinalDiscountRate(
      loanProductId,
      preferentialQuestionIds,
    );

    profitLossStore.setTotalDiscountRate(Number(totalDiscountRate));

    console.log('전세대출 우대금리 계산 완료:', {
      loanProductId,
      preferentialQuestionIds,
      totalDiscountRate: profitLossStore.state.loan.totalDiscountRate,
    });

    const comparisonResult = await profitLossApi.createComparison(
      profitLossStore.requestPayload,
    );

    const comparisonId =
      comparisonResult?.comparisonId ??
      comparisonResult?.id ??
      comparisonResult;

    if (comparisonId == null || comparisonId === '') {
      throw new Error('손익비교 응답에 comparisonId가 없습니다.');
    }

    await router.push({
      name: 'comparisons/result',
      params: {
        comparisonId: String(comparisonId),
      },
    });
  } catch (error) {
    console.error('손익비교 요청 실패:', error);
    calculationError.value =
      '손익비교를 요청하지 못했습니다. 잠시 후 다시 시도해 주세요.';
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <main class="preferential-page">
    <section class="preferential-content">
      <header class="page-header">
        <button
          class="back-button"
          type="button"
          aria-label="이전 화면으로 이동"
          @click="goBack"
        >
          ‹
        </button>

        <div>
          <h1>전세대출 우대금리 확인</h1>
          <p>우대금리 조건을 입력해 주세요</p>
        </div>
      </header>

      <div v-if="isLoading" class="loading-message">
        우대 조건을 불러오는 중입니다...
      </div>

      <section v-else class="preferential-section">
        <h2>대출 우대금리</h2>

        <article
          v-for="item in preferentialItems"
          :key="item.id"
          class="preferential-group yes-no-group"
        >
          <h3>{{ item.conditionName }}</h3>

          <div class="question-card">
            <p>{{ item.conditionDetail }}</p>

            <div class="answer-options">
              <button
                type="button"
                :class="{
                  selected: selectedAnswer(item.id) === true,
                }"
                :aria-pressed="selectedAnswer(item.id) === true"
                @click="answerItem(item.id, true)"
              >
                예
              </button>
              <button
                type="button"
                :class="{
                  selected: selectedAnswer(item.id) === false,
                }"
                :aria-pressed="selectedAnswer(item.id) === false"
                @click="answerItem(item.id, false)"
              >
                아니요
              </button>
            </div>
          </div>
        </article>
      </section>

      <p v-if="calculationError" class="calculation-error">
        {{ calculationError }}
      </p>

      <button
        class="next-button"
        type="button"
        :disabled="!isComplete || isSubmitting || isLoading"
        @click="continueToNextStep"
      >
        {{
          isSubmitting
            ? '손익비교 계산 중...'
            : isComplete
              ? '다음'
              : '모든 항목에 답변해 주세요'
        }}
      </button>
    </section>

    <div
      v-if="isSubmitting"
      class="loading-overlay"
      role="status"
      aria-live="polite"
      aria-busy="true"
    >
      <div class="loading-card">
        <span class="loading-spinner" aria-hidden="true"></span>
        <strong>손익비교를 계산 중입니다</strong>
        <p>잠시만 기다려 주세요</p>
      </div>
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
.preferential-page {
  --kb-yellow: #ffbc00;
  --kb-text: #292725;
  --kb-muted: #aaa39a;
  --kb-border: #e8e0d4;
  width: 100%;
  max-width: 390px;
  height: 100vh;
  height: 100dvh;
  min-height: 100vh;
  margin: 0 auto;
  overflow: hidden;
  position: relative;
  color: var(--kb-text);
  background: #faf9f7;
  border-right: 1px solid #e9e0d2;
  border-left: 1px solid #e9e0d2;
}
.preferential-content {
  display: flex;
  height: 100%;
  min-height: 0;
  padding: 18px 12px 20px;
  overflow: hidden;
  flex-direction: column;
}
.page-header {
  display: flex;
  margin-bottom: 24px;
  align-items: flex-start;
  flex-shrink: 0;
  gap: 10px;
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
  line-height: 1;
  color: #716a62;
  background: #fff;
  place-items: center;
}
.page-header h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.35;
}
.page-header p {
  margin: 3px 0 0;
  font-size: 10px;
  color: var(--kb-muted);
}
.loading-message {
  text-align: center;
  padding: 20px;
  color: var(--kb-muted);
}
.preferential-section {
  min-height: 0;
  margin-bottom: 16px;
  padding-right: 5px;
  overflow-y: auto;
  flex: 1;
  overscroll-behavior-y: contain;
  scrollbar-color: #c8bfae transparent;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
}
.preferential-section::-webkit-scrollbar {
  width: 5px;
}
.preferential-section::-webkit-scrollbar-track {
  background: transparent;
}
.preferential-section::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #c8bfae;
}
.preferential-section > h2 {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  color: #716a62;
}
.preferential-group {
  margin-bottom: 18px;
}
.preferential-group h3 {
  margin: 0 2px 5px;
  font-size: 10px;
  font-weight: 600;
  color: #777067;
}
.question-card {
  min-height: 102px;
  padding: 17px 14px 14px;
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  background: #fff;
}
.question-card p {
  min-height: 38px;
  margin: 0 0 12px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.55;
}
.answer-options {
  display: flex;
  gap: 8px;
}
.answer-options button {
  width: 64px;
  min-width: 64px;
  height: 34px;
  padding: 0;
  border: 1px solid var(--kb-border);
  border-radius: 8px;
  flex-shrink: 0;
  color: #746d65;
  background: #fff;
}
.answer-options button.selected {
  border-color: var(--kb-yellow);
  font-weight: 700;
  color: var(--kb-text);
  background: #fff8df;
  box-shadow: 0 0 0 1px var(--kb-yellow);
}
.answer-options button:focus-visible,
.back-button:focus-visible,
.next-button:focus-visible {
  outline: 3px solid rgb(255 188 0 / 35%);
  outline-offset: 2px;
}
.next-button {
  width: 100%;
  height: 54px;
  min-height: 54px;
  margin-top: 0;
  border: 0;
  border-radius: 13px;
  flex-shrink: 0;
  font-weight: 700;
  color: var(--kb-text);
  background: var(--kb-yellow);
}
.calculation-error {
  margin: 0 0 8px;
  flex-shrink: 0;
  font-size: 11px;
  color: #d32f2f;
  text-align: center;
}
.loading-overlay {
  position: absolute;
  z-index: 20;
  display: grid;
  inset: 0;
  padding: 24px;
  background: rgb(41 39 37 / 34%);
  place-items: center;
}
.loading-card {
  display: flex;
  width: min(280px, 100%);
  padding: 24px 20px;
  border-radius: 16px;
  align-items: center;
  flex-direction: column;
  background: #fff;
  box-shadow: 0 12px 36px rgb(41 39 37 / 20%);
}
.loading-card strong {
  margin-top: 14px;
  font-size: 14px;
}
.loading-card p {
  margin: 5px 0 0;
  font-size: 11px;
  color: var(--kb-muted);
}
.loading-spinner {
  width: 34px;
  height: 34px;
  border: 4px solid #f0e6cf;
  border-top-color: var(--kb-yellow);
  border-radius: 50%;
  animation: loading-spin 0.8s linear infinite;
}
@keyframes loading-spin {
  to {
    transform: rotate(360deg);
  }
}
.next-button:disabled {
  color: #8c857a;
  background: #ddd5c3;
}
</style>
