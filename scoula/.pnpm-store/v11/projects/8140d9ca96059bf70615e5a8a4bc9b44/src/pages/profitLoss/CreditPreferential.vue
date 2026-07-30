<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useProfitLossStore } from '@/stores/profitLoss';

const router = useRouter();
const profitLossStore = useProfitLossStore();

const preferentialGroups = computed(
  () => profitLossStore.state.creditPreferential.groups,
);

const cardUsageGroup = computed(() =>
  preferentialGroups.value.find(
    (group) => group.type === 'SINGLE_SELECT',
  ),
);

const yesNoGroups = computed(() =>
  preferentialGroups.value.filter(
    (group) => group.type === 'YES_NO',
  ),
);

const isComplete = computed(
  () => profitLossStore.isCreditPreferentialComplete,
);

function selectedAnswer(groupId) {
  return profitLossStore.state.creditPreferential.answers[groupId];
}

function answerGroup(groupId, answer) {
  profitLossStore.setCreditPreferentialAnswer(groupId, answer);
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }

  router.push({ name: 'creditEligibility' });
}

function continueToNextStep() {
  if (!isComplete.value) return;

  console.log('우대금리 계산 요청값:', {
    loanProductIds: [
      ...profitLossStore.state.loan.loanProductId,
    ],
    preferentialQuestionIds: [
      ...profitLossStore.preferentialQuestionIds,
    ],
  });
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
          <h1>신용대출 우대금리 확인</h1>
          <p>우대금리 조건을 입력해 주세요</p>
        </div>
      </header>

      <section class="preferential-section">
        <h2>대출 우대금리</h2>

        <template v-if="cardUsageGroup">
          <article class="preferential-group">
            <h3>{{ cardUsageGroup.title }}</h3>
            <p class="group-description">
              {{ cardUsageGroup.description }}
            </p>

            <div class="card-usage-options">
              <button
                v-for="option in cardUsageGroup.options"
                :key="option.value"
                type="button"
                class="card-usage-option"
                :class="{
                  selected:
                    selectedAnswer(cardUsageGroup.id) ===
                    option.value,
                }"
                :aria-pressed="
                  selectedAnswer(cardUsageGroup.id) ===
                  option.value
                "
                @click="
                  answerGroup(cardUsageGroup.id, option.value)
                "
              >
                <span class="radio-icon" aria-hidden="true"></span>
                <span>{{ option.text }}</span>
              </button>
            </div>
          </article>
        </template>

        <article
          v-for="group in yesNoGroups"
          :key="group.id"
          class="preferential-group yes-no-group"
        >
          <h3>{{ group.title }}</h3>

          <div class="question-card">
            <p>{{ group.text }}</p>

            <div class="answer-options">
              <button
                type="button"
                :class="{
                  selected: selectedAnswer(group.id) === true,
                }"
                :aria-pressed="
                  selectedAnswer(group.id) === true
                "
                @click="answerGroup(group.id, true)"
              >
                예
              </button>
              <button
                type="button"
                :class="{
                  selected: selectedAnswer(group.id) === false,
                }"
                :aria-pressed="
                  selectedAnswer(group.id) === false
                "
                @click="answerGroup(group.id, false)"
              >
                아니요
              </button>
            </div>
          </div>
        </article>
      </section>

      <button
        class="next-button"
        type="button"
        :disabled="!isComplete"
        @click="continueToNextStep"
      >
        {{ isComplete ? '다음' : '모든 항목에 답변해 주세요' }}
      </button>
    </section>
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
  color: var(--kb-text);
  background: #faf9f7;
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

.group-description {
  margin: 0 2px 10px;
  font-size: 9px;
  line-height: 1.5;
  color: #b0a89c;
}

.card-usage-options {
  overflow: hidden;
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  background: #fff;
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
}

.card-usage-option:last-child {
  border-bottom: 0;
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

.card-usage-option:focus-visible,
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

.next-button:disabled {
  color: #8c857a;
  background: #ddd5c3;
}
</style>
