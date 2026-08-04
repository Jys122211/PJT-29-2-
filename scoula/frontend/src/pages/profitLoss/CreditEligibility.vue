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

const comparisonConditionQuestions = computed(() =>
  profitLossStore.state.creditEligibility.questions.filter(
    (question) => question.type === 'COMPARISON_CONDITION',
  ),
);

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
        <h2>대출 신청자격</h2>

        <article
          v-for="question in qualificationQuestions"
          :key="`qualification-${question.id}`"
          class="kb-card question-card"
        >
          <h3>{{ question.text }}</h3>

          <div class="answer-options">
            <button
              type="button"
              :class="{ selected: selectedAnswer(question.id) === true }"
              :aria-pressed="selectedAnswer(question.id) === true"
              @click="answerQuestion(question.id, true)"
            >
              예
            </button>
            <button
              type="button"
              :class="{ selected: selectedAnswer(question.id) === false }"
              :aria-pressed="selectedAnswer(question.id) === false"
              @click="answerQuestion(question.id, false)"
            >
              아니요
            </button>
          </div>
        </article>

        <p class="qualification-notice">
          ※ 위 두 질문은 건강보험관리공단 사이트에서 건강보험자격득실확인서로
          확인 가능합니다.
        </p>
      </section>

      <section class="question-section preferential-section">
        <h2>특별 우대 조건</h2>

        <article
          v-for="question in comparisonConditionQuestions"
          :key="`comparison-condition-${question.id}`"
          class="kb-card question-card"
        >
          <h3>{{ question.text }}</h3>

          <div class="answer-options">
            <button
              type="button"
              :class="{ selected: selectedAnswer(question.id) === true }"
              :aria-pressed="selectedAnswer(question.id) === true"
              @click="answerQuestion(question.id, true)"
            >
              예
            </button>
            <button
              type="button"
              :class="{ selected: selectedAnswer(question.id) === false }"
              :aria-pressed="selectedAnswer(question.id) === false"
              @click="answerQuestion(question.id, false)"
            >
              아니요
            </button>
          </div>
        </article>
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
  width: 100%;
  max-width: 390px;
  min-height: 100vh;
  margin: 0 auto;
  color: var(--kb-text);
  background: var(--kb-bg-light);
  border-right: 1px solid var(--kb-border-light);
  border-left: 1px solid var(--kb-border-light);
}

.eligibility-content {
  display: flex;
  min-height: 100vh;
  padding: 18px 12px 20px;
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
  margin-bottom: 22px;
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
