<script setup>
import { reactive } from 'vue';
import { useComparison } from '@/composables/useComparison';

const { submitting, error, submitComparison } = useComparison();

// TODO: 조윤상님의 입력 화면(자금 입력 → 자격 확인 → 우대금리 확인)이 완성되면 이 하드코딩 대신
// 그 화면에서 조립한 payload를 받아 submitComparison을 호출한다. 지금은 통합 검증용 임시 화면이라
// 득실 계산 로직 명세서 "조건1 · 가입1개월차" 시나리오 값을 팀 통합 시드(득실_시드데이터.sql) 기준으로 채워뒀다.
const payload = reactive({
  userFinancialInfo: {
    monthlyPayment: 900000,
    creditGrade: 3,
  },
  deposit: {
    userDepositId: 6, // 계산 검산용 예금 (원금 3,000만 / 적용 3.20% / 기본 2.40%)
    isPartialAllowed: true,
  },
  loan: {
    loanProductId: [1], // KB스타 신용대출Ⅱ(신규)
    loanType: 'CREDIT',
    totalDiscountRate: 0,
  },
  comparisonCondition: {
    urgentAmount: 20000000,
    isLumpSum: true,
  },
});

// submitting이 true가 되는 순간(=클릭 즉시) 버튼이 disabled로 바뀐다 — 중복 제출 방지.
const onSubmit = () => submitComparison(payload);
</script>

<template>
  <div>
    <h4 class="mb-2">득실 비교 (임시 통합 검증용)</h4>
    <p class="text-muted small mb-4">
      자금 입력 · 자격 확인 · 우대금리 확인 화면이 아직 없어, 조건1 시나리오 값으로 바로
      POST → 결과 화면(GET) 연동을 확인하기 위한 임시 화면입니다.
    </p>

    <div v-if="error" class="alert alert-danger">
      <i class="fa-solid fa-circle-exclamation me-1"></i>{{ error }}
    </div>

    <button type="button" class="btn btn-primary" :disabled="submitting" @click="onSubmit">
      <span v-if="submitting" class="spinner-border spinner-border-sm me-1" role="status"></span>
      비교하기
    </button>
  </div>
</template>
