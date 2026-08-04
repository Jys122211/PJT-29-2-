<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import profitLossApi from '@/api/profitLossApi';
import { useProfitLossStore } from '@/stores/profitLoss';
import AlertModal from '@/components/AlertModal.vue';

const router = useRouter();
const profitLossStore = useProfitLossStore();
const isSubmitting = ref(false);
const isResultModalOpen = ref(false);
const resultModalTitle = ref('');
const resultModalMessage = ref('');

const qualificationQuestions = computed(() =>
  profitLossStore.state.creditEligibility.questions.filter(
    (question) => question.type === 'QUALIFICATION',
  ),
);

const visibleQualificationQuestions = computed(() => {
  const all = qualificationQuestions.value;
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

const isQualificationComplete = computed(() => {
  return qualificationQuestions.value.length > 0 &&
         qualificationQuestions.value.every(q => selectedAnswer(q.id) !== undefined && selectedAnswer(q.id) !== null);
});

const comparisonConditionQuestions = computed(() =>
  profitLossStore.state.creditEligibility.questions.filter(
    (question) => question.type === 'COMPARISON_CONDITION',
  ),
);

const visibleComparisonConditionQuestions = computed(() => {
  if (!isQualificationComplete.value) return [];
  const all = comparisonConditionQuestions.value;
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

const isComplete = computed(() => profitLossStore.isCreditEligibilityComplete);

function selectedAnswer(questionId) {
  return profitLossStore.state.creditEligibility.answers[questionId];
}

function answerQuestion(questionId, answer) {
  profitLossStore.setCreditEligibilityAnswer(questionId, answer);
  profitLossStore.setLoanProducts([]);
  isResultModalOpen.value = false;
}

function openResultModal(title, message) {
  resultModalTitle.value = title;
  resultModalMessage.value = message;
  isResultModalOpen.value = true;
}

function closeResultModal() {
  isResultModalOpen.value = false;
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }

  router.push({ name: 'comparisonInput' });
}

async function continueToNextStep() {
  if (!isComplete.value || isSubmitting.value) return;

  isSubmitting.value = true;
  isResultModalOpen.value = false;

  try {
    const qualificationQuestionIds = [
      ...profitLossStore.qualificationQuestionIds,
    ];
    const loanProductIds =
      await profitLossApi.getQualifiedLoanProductIds(
        qualificationQuestionIds,
      );

    profitLossStore.setLoanProducts(loanProductIds);

    console.log('자격조건 상품 조회:', {
      qualificationQuestionIds,
      loanProductIds,
    });

    if (loanProductIds.length === 0) {
      openResultModal(
        '신청 가능한 상품 없음',
        '입력하신 신청 자격에 맞는 신용대출 상품이 없습니다.',
      );
      return;
    }

    router.push({ name: 'creditPreferential' });
  } catch (error) {
    console.error('신청 가능한 대출 상품 조회 실패:', error);
    profitLossStore.setLoanProducts([]);
    openResultModal(
      '상품 조회 실패',
      '신청 가능한 상품을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.',
    );
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <main class="eligibility-page">
    <section class="eligibility-content">
      <header class="page-header">
        <button
          class="kb-btn kb-btn-secondary back-button"
          type="button"
          aria-label="이전 화면으로 이동"
          @click="goBack"
        >
          ‹
        </button>

        <div>
          <h1>신용대출 자격 확인</h1>
          <p>자격조건을 입력해 주세요</p>
        </div>
      </header>

      <section class="question-section">
        <div class="group-container">
          <h2>자격조건 질문</h2>
          <TransitionGroup name="question-fade" tag="div">
            <article
              v-for="item in visibleQualificationQuestions"
              :key="item.id"
              class="kb-card question-card"
            >
              <h3>{{ item.text }}</h3>

              <div class="answer-options">
                <button
                  type="button"
                  :class="{
                    selected: selectedAnswer(item.id) === true,
                  }"
                  :aria-pressed="selectedAnswer(item.id) === true"
                  @click="answerQuestion(item.id, true)"
                >
                  예
                </button>
                <button
                  type="button"
                  :class="{
                    selected: selectedAnswer(item.id) === false,
                  }"
                  :aria-pressed="selectedAnswer(item.id) === false"
                  @click="answerQuestion(item.id, false)"
                >
                  아니요
                </button>
              </div>
            </article>
          </TransitionGroup>

          <p class="qualification-notice">
            ※ 위 두 질문은 건강보험관리공단 사이트에서 건강보험자격득실확인서로
            확인 가능합니다.
          </p>
        </div>

        <div class="group-container" v-if="visibleComparisonConditionQuestions.length > 0">
          <h2>득실 계산 조건</h2>
          <TransitionGroup name="question-fade" tag="div">
            <article
              v-for="item in visibleComparisonConditionQuestions"
              :key="item.id"
              class="kb-card question-card"
            >
              <h3>{{ item.text }}</h3>

              <div class="answer-options">
                <button
                  type="button"
                  :class="{ selected: selectedAnswer(item.id) === true }"
                  :aria-pressed="selectedAnswer(item.id) === true"
                  @click="answerQuestion(item.id, true)"
                >
                  예
                </button>
                <button
                  type="button"
                  :class="{ selected: selectedAnswer(item.id) === false }"
                  :aria-pressed="selectedAnswer(item.id) === false"
                  @click="answerQuestion(item.id, false)"
                >
                  아니요
                </button>
              </div>
            </article>
          </TransitionGroup>
        </div>
      </section>

      <button
        class="kb-btn kb-btn-primary next-button"
        type="button"
        :disabled="!isComplete || isSubmitting"
        @click="continueToNextStep"
      >
        {{
          isSubmitting
            ? '상품 조회 중...'
            : isComplete
              ? '다음'
              : '모든 항목에 답변해 주세요'
        }}
      </button>
    </section>

    <AlertModal
      :visible="isResultModalOpen"
      :title="resultModalTitle"
      :message="resultModalMessage"
      @confirm="closeResultModal"
      @close="closeResultModal"
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

.eligibility-page {
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
  background: var(--kb-bg-light);
  border-right: 1px solid var(--kb-border-light);
  border-left: 1px solid var(--kb-border-light);
}

.eligibility-content {
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
  font-size: var(--kb-font-lg);
  font-weight: 700;
  line-height: 1.35;
}

.page-header p {
  margin: 3px 0 0;
  font-size: 10px;
  color: var(--kb-muted);
}

.question-section {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  margin-bottom: 22px;
  overscroll-behavior-y: contain;
  scrollbar-width: none;
}
.question-section::-webkit-scrollbar {
  display: none;
}

.question-section h2 {
  margin: 0 0 11px;
  font-size: var(--kb-font-sm);
  font-weight: 700;
  color: #716a62;
}

.question-card {
  min-height: 102px;
  margin-bottom: 14px;
  padding: var(--kb-space-md);
}

.question-card h3 {
  min-height: 34px;
  margin: 0 0 12px;
  font-size: var(--kb-font-sm);
  font-weight: 700;
  line-height: 1.5;
}

.answer-options {
  display: flex;
  gap: 8px;
}

.answer-options button {
  width: clamp(56px, 15vw, 64px);
  min-width: clamp(56px, 15vw, 64px);
  height: clamp(30px, 9vw, 36px);
  font-size: var(--kb-font-sm);
  padding: 0;
  border: 1px solid var(--kb-border);
  border-radius: 8px;
  flex-shrink: 0;
  color: #746d65;
  background: #fff;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.answer-options button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.answer-options button.selected {
  border-color: var(--kb-yellow);
  font-weight: 700;
  color: #292725;
  background: #fff8df;
  box-shadow: 0 0 0 1px var(--kb-yellow);
}

.answer-options button:focus-visible,
.back-button:focus-visible,
.next-button:focus-visible {
  outline: 3px solid rgb(255 188 0 / 35%);
  outline-offset: 2px;
}

.qualification-notice {
  margin: 0;
  padding: 14px 12px;
  border-radius: 11px;
  font-size: 10px;
  line-height: 1.6;
  color: #b0a89c;
  background: #f3ede2;
}

.preferential-section {
  margin-bottom: 26px;
}

.next-button {
  width: 100%;
  height: var(--kb-btn-height);
  min-height: var(--kb-btn-height);
  margin-top: auto;
  flex-shrink: 0;
}
</style>
