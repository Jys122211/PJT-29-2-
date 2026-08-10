<script setup>
import { computed, nextTick, ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import profitLossApi from '@/api/profitLossApi';
import { useProfitLossStore } from '@/stores/profitLoss';
import AlertModal from '@/components/AlertModal.vue';

const router = useRouter();
const profitLossStore = useProfitLossStore();
const isSubmitting = ref(false);
const isLoading = ref(true);
const calculationError = ref('');

const isErrorModalOpen = ref(false);
const errorModalMessage = ref('');
const errorModalConfirmText = ref('확인');
const shouldReturnToInput = ref(false);

const NEXT_STEP_GUIDE = {
  PAYMENT_TOO_LOW: '월 상환 가능 금액을 늘리거나 필요 금액을 줄여보세요.',
  EXCEED_LOAN_LIMIT: '필요 금액을 줄이거나 다른 대출 상품을 선택해 보세요.',
  DEPOSIT_NOT_FOUND: '예금을 다시 선택해 주세요.',
  GRADE_RATE_UNAVAILABLE: '잠시 후 다시 시도해 주세요.',
};

function openErrorModal(message, returnToInput = false) {
  errorModalMessage.value = message;
  errorModalConfirmText.value = returnToInput
    ? '입력 화면으로 돌아가기'
    : '확인';
  shouldReturnToInput.value = returnToInput;
  isErrorModalOpen.value = true;
}

function closeErrorModal() {
  isErrorModalOpen.value = false;
}

function confirmErrorModal() {
  isErrorModalOpen.value = false;
  if (shouldReturnToInput.value) {
    router.push({ name: 'comparisonInput' });
  }
}

const preferentialGroups = computed(
  () => profitLossStore.state.jeonsePreferential.groups,
);

const visiblePreferentialGroups = computed(() => {
  const all = preferentialGroups.value;
  const visible = [];
  for (let i = 0; i < all.length; i++) {
    visible.push(all[i]);
    const ans = selectedAnswer(all[i].id);
    if (ans === undefined || ans === null) {
      break;
    }
  }
  return visible;
});

const isComplete = computed(() => profitLossStore.isJeonsePreferentialComplete);

// 답변할 때마다 다음 질문이 아래에 추가되므로, 새 질문이 화면 밖이면 스크롤로 끌어온다.
const questionElements = new Map();

function registerQuestion(groupId, el) {
  if (el) {
    questionElements.set(groupId, el);
  } else {
    questionElements.delete(groupId);
  }
}

watch(
  () => visiblePreferentialGroups.value.length,
  async (count, previousCount) => {
    // 이전 답변을 바꿔 질문이 줄어드는 경우에는 스크롤하지 않는다.
    if (previousCount === undefined || count <= previousCount) return;

    await nextTick();

    const lastGroup = visiblePreferentialGroups.value[count - 1];
    const element = questionElements.get(lastGroup?.id);
    if (!element) return;

    const prefersReducedMotion = window.matchMedia(
      '(prefers-reduced-motion: reduce)',
    ).matches;

    element.scrollIntoView({
      behavior: prefersReducedMotion ? 'auto' : 'smooth',
      block: 'nearest',
    });
  },
);

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
  const preferentialQuestionIds = [
    ...profitLossStore.jeonsePreferentialQuestionIds,
  ];

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

    const comparisonResult = await profitLossApi.createComparison(
      profitLossStore.requestPayload,
    );

    if (comparisonResult?.feasible === false) {
      openErrorModal(
        NEXT_STEP_GUIDE[comparisonResult.reason] ?? '잠시 후 다시 시도해 주세요.',
        true,
      );
      return;
    }

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

    const status = error.response?.status;
    const code = error.response?.data?.code;
    const serverMessage = error.response?.data?.message;
    const guide = NEXT_STEP_GUIDE[code] ?? '잠시 후 다시 시도해 주세요.';

    openErrorModal(
      `${serverMessage ?? '손익비교를 요청하지 못했습니다.'} ${guide}`,
      status === 400 || status === 404,
    );
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
          <i class="fa-solid fa-chevron-left"></i>
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
        <div class="group-container">
          <h2>대출 우대금리</h2>
          <p class="question-guide">
            <span class="guide-icon" aria-hidden="true">!</span>
            <span>
              잘 모르시겠다면 우선 <strong>‘해당없음 또는 아니요’</strong>를
              선택해 주세요.
            </span>
          </p>

          <TransitionGroup name="question-fade" tag="div">
            <template
              v-for="group in visiblePreferentialGroups"
              :key="group.id"
            >
              <article
                v-if="group.type === 'SINGLE_SELECT'"
                :ref="(el) => registerQuestion(group.id, el)"
                class="preferential-group"
              >
                <h3>{{ group.title }}</h3>
                <p class="group-description">
                  {{ group.description }}
                </p>

                <div class="card-usage-options">
                  <button
                    v-for="option in group.options"
                    :key="option.value"
                    type="button"
                    class="card-usage-option"
                    :class="{
                      selected: selectedAnswer(group.id) === option.value,
                    }"
                    :aria-pressed="selectedAnswer(group.id) === option.value"
                    @click="answerItem(group.id, option.value)"
                  >
                    <span class="radio-icon" aria-hidden="true"></span>
                    <span>{{ option.text }}</span>
                  </button>
                </div>
              </article>

              <article
                v-else-if="group.type === 'YES_NO'"
                :ref="(el) => registerQuestion(group.id, el)"
                class="preferential-group yes-no-group"
              >
                <h3>{{ group.title }}</h3>

                <div class="kb-card question-card">
                  <p>{{ group.text }}</p>

                  <div class="description" v-if="group.description">
                    {{ group.description }}
                  </div>

                  <div class="answer-options">
                    <button
                      type="button"
                      :class="{
                        selected: selectedAnswer(group.id) === true,
                      }"
                      :aria-pressed="selectedAnswer(group.id) === true"
                      @click="answerItem(group.id, true)"
                    >
                      예
                    </button>
                    <button
                      type="button"
                      :class="{
                        selected: selectedAnswer(group.id) === false,
                      }"
                      :aria-pressed="selectedAnswer(group.id) === false"
                      @click="answerItem(group.id, false)"
                    >
                      아니요
                    </button>
                  </div>
                </div>
              </article>
            </template>
          </TransitionGroup>
        </div>
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

    <AlertModal
      :visible="isErrorModalOpen"
      title="손익비교 실패"
      :message="errorModalMessage"
      :confirm-text="errorModalConfirmText"
      @confirm="confirmErrorModal"
      @close="closeErrorModal"
    />
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
  display: flex;
  flex-direction: column;
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
  flex: 1;
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
  overflow-y: auto;
  flex: 1;
  overscroll-behavior-y: contain;
  scrollbar-width: none;
}
.preferential-section::-webkit-scrollbar {
  display: none;
}
.preferential-section > h2 {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  color: #716a62;
}
.question-guide {
  display: flex;
  margin: 0 0 12px;
  padding: 7px 9px;
  border-left: 3px solid var(--kb-yellow);
  border-radius: 6px;
  align-items: flex-start;
  gap: 6px;
  font-size: 11px;
  line-height: 1.45;
  color: #746d65;
  background: #fff8df;
}
.question-guide strong {
  font-weight: 700;
  color: #292725;
}
.guide-icon {
  display: grid;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  flex-shrink: 0;
  place-items: center;
  font-size: 9px;
  font-weight: 700;
  color: #292725;
  background: var(--kb-yellow);
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
.group-description {
  margin: 0 0 10px 2px;
  font-size: 10px;
  color: var(--kb-muted);
}
.card-usage-options {
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  background: #fff;
  padding: 2px;
}
.card-usage-option {
  display: flex;
  width: 100%;
  min-height: 48px;
  padding: 10px 14px;
  border: 0;
  border-bottom: 1px solid #eee8de;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--kb-text);
  text-align: left;
  background: #fff;
  transition:
    transform 0.15s ease,
    background 0.15s ease;
}
.card-usage-option:hover {
  background: #fffdf6;
}
.card-usage-option:first-child {
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
}
.card-usage-option:last-child {
  border-bottom: 0;
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
}
.radio-icon {
  width: 19px;
  height: 19px;
  border: 2px solid #d9d0c2;
  border-radius: 50%;
  flex-shrink: 0;
  background: #fff;
}
.card-usage-option.selected {
  background: #fffaf0;
}
.card-usage-option.selected .radio-icon {
  border: 6px solid var(--kb-yellow);
}
.question-card {
  min-height: 102px;
  padding: var(--kb-space-md);
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  background: #fff;
}
.question-card p {
  min-height: 38px;
  margin: 0 0 12px;
  font-size: var(--kb-font-sm);
  font-weight: 600;
  line-height: 1.55;
}
.answer-options {
  display: flex;
  gap: 8px;
}
.answer-options button {
  width: clamp(56px, 15vw, 64px);
  min-width: clamp(56px, 15vw, 64px);
  height: clamp(30px, 9vw, 36px);
  padding: 0;
  border: 1px solid var(--kb-border);
  border-radius: 8px;
  flex-shrink: 0;
  color: #746d65;
  background: #fff;
  font-size: var(--kb-font-sm);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.answer-options button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
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
  height: var(--kb-btn-height);
  min-height: var(--kb-btn-height);
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
.description {
  display: block;
  margin: -3px 0 12px;
  padding: 7px 9px;
  border-radius: 7px;
  font-size: 9px;
  font-weight: 400;
  line-height: 1.45;
  color: #8d857b;
  background: #f7f3ec;
}
</style>
