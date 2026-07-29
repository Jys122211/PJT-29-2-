<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useComparison } from '@/composables/useComparison';

const route = useRoute();
const { comparison, loading, error, fetchComparison } = useComparison();

const load = () => fetchComparison(route.params.comparisonId);

onMounted(load);

const won = (value) => (value ?? 0).toLocaleString('ko-KR');

const isLoanWinner = computed(() => comparison.value?.winner === 'LOAN');

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

const finalBalanceGap = computed(() => {
  if (!comparison.value) return 0;
  return Math.abs(comparison.value.loan.finalBalance - comparison.value.deposit.finalBalance);
});

// 추천이 아닌 쪽을 누르면 손실경고 모달을 먼저 띄운다. bootstrap.js가 안 붙어 있어 v-if로 직접 제어한다.
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
  <div v-if="comparison">
    <!-- 1. 상단 결론 배너 -->
    <div class="p-4 mb-4 rounded-3 bg-primary bg-opacity-10 text-center">
      <div class="fs-5">
        <strong>{{ won(comparison.urgentAmount) }}원</strong>이 필요할 때
      </div>
      <div class="fs-3 fw-bold mt-1">
        가장 남는 선택은
        <span class="text-primary">{{ comparison.badges.recommended }}</span
        >이<br class="d-block d-sm-none" />
        <span class="text-primary">{{ won(comparison.savingAmount) }}원</span> 더 이득
      </div>
    </div>

    <!-- 2. 최저임금 경고 박스 -->
    <div
      v-if="comparison.warning.isBelowMinimumWage"
      class="alert mb-4"
      style="background-color: #ffe4ef; border-color: #ffb8d6; color: #99295a"
    >
      <i class="fa-solid fa-triangle-exclamation me-2"></i>
      {{ comparison.warning.message }}
    </div>

    <!-- 3. 카드 2장 -->
    <div class="row g-3 mb-4">
      <div class="col-md-6">
        <div
          class="card h-100"
          :class="isLoanWinner ? 'border-primary border-2 shadow-sm' : ''"
        >
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <h5 class="card-title mb-0">
                <i class="fa-solid fa-building-columns me-1"></i>
                {{ comparison.loan.name }}
              </h5>
              <span v-if="isLoanWinner" class="badge bg-primary">추천</span>
            </div>

            <div class="text-muted mb-2">
              연 {{ comparison.loan.interestRate }}% (변동 {{ comparison.loan.ratePeriodMonths }}개월)
            </div>
            <div class="fs-4 fw-bold mb-3">{{ won(comparison.loan.finalBalance) }}원</div>

            <button
              type="button"
              class="btn btn-sm btn-outline-secondary mb-3"
              @click="loanDetailOpen = !loanDetailOpen"
            >
              상세 보기
              <i :class="loanDetailOpen ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'"></i>
            </button>

            <div v-show="loanDetailOpen">
              <table class="table table-sm">
                <tbody>
                  <tr>
                    <th class="text-muted fw-normal">비용 (이자+수수료)</th>
                    <td class="text-end">
                      {{ won(comparison.loan.cost) }}원
                      <span class="text-muted small">
                        (이자 {{ won(comparison.loan.interest) }} + 수수료 {{ won(comparison.loan.penalty) }})
                      </span>
                    </td>
                  </tr>
                  <tr>
                    <th class="text-muted fw-normal">만기이자</th>
                    <td class="text-end">{{ won(comparison.deposit.maintainInterest) }}원</td>
                  </tr>
                  <tr>
                    <th class="text-muted fw-normal">총 이득</th>
                    <td class="text-end fw-bold">{{ won(comparison.loan.netProfit) }}원</td>
                  </tr>
                </tbody>
              </table>

              <div v-if="comparison.loan.isRateEstimated" class="text-danger small">
                ※ 추정치이며 실제 심사금리와 다를 수 있습니다
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="col-md-6">
        <div
          class="card h-100"
          :class="!isLoanWinner ? 'border-primary border-2 shadow-sm' : ''"
        >
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <h5 class="card-title mb-0">
                <i class="fa-solid fa-piggy-bank me-1"></i>
                {{ comparison.deposit.name }}
              </h5>
              <span v-if="!isLoanWinner" class="badge bg-primary">추천</span>
            </div>

            <div class="text-muted mb-2">중도해지이율 연 {{ comparison.deposit.cancelInterestRate }}%</div>
            <div class="fs-4 fw-bold mb-3">{{ won(comparison.deposit.finalBalance) }}원</div>

            <button
              type="button"
              class="btn btn-sm btn-outline-secondary mb-3"
              @click="depositDetailOpen = !depositDetailOpen"
            >
              상세 보기
              <i :class="depositDetailOpen ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'"></i>
            </button>

            <div v-show="depositDetailOpen">
              <table class="table table-sm">
                <tbody>
                  <tr>
                    <th class="text-muted fw-normal">중도해지이율</th>
                    <td class="text-end">연 {{ comparison.deposit.cancelInterestRate }}%</td>
                  </tr>
                  <tr>
                    <th class="text-muted fw-normal">해지수익</th>
                    <td class="text-end fw-bold">{{ won(comparison.deposit.withdrawalProfit) }}원</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. 만기 시점 최종 잔액 바 차트 (라이브러리 없이 div 너비 비율) -->
    <div class="card mb-4">
      <div class="card-body">
        <h6 class="card-title mb-3">만기 시점 최종 잔액 비교</h6>

        <div class="mb-1 d-flex justify-content-between">
          <span>{{ comparison.loan.name }}</span>
          <strong>{{ won(comparison.loan.finalBalance) }}원</strong>
        </div>
        <div class="mb-3" style="height: 28px; background-color: #eee; border-radius: 6px; overflow: hidden">
          <div
            class="bg-primary h-100"
            :style="{ width: loanBarWidth + '%' }"
          ></div>
        </div>

        <div class="mb-1 d-flex justify-content-between">
          <span>{{ comparison.deposit.name }}</span>
          <strong>{{ won(comparison.deposit.finalBalance) }}원</strong>
        </div>
        <div style="height: 28px; background-color: #eee; border-radius: 6px; overflow: hidden">
          <div
            class="bg-secondary h-100"
            :style="{ width: depositBarWidth + '%' }"
          ></div>
        </div>

        <div class="text-center text-muted mt-3">
          차이 <strong class="text-primary">{{ won(finalBalanceGap) }}원</strong>
        </div>
      </div>
    </div>

    <!-- 5. 월 예상 상환액 -->
    <div class="d-flex justify-content-between align-items-center mb-4 px-1">
      <span class="text-muted">월 예상 상환액</span>
      <span class="fs-5 fw-bold">{{ won(comparison.monthlyPayment) }}원</span>
    </div>

    <!-- 6. 하단 버튼 -->
    <div class="d-flex gap-2">
      <button
        type="button"
        class="btn flex-fill"
        :class="isLoanWinner ? 'btn-primary' : 'btn-outline-secondary'"
        @click="proceed('LOAN')"
      >
        {{ comparison.badges.recommended }}로 진행
      </button>
      <button
        type="button"
        class="btn flex-fill"
        :class="!isLoanWinner ? 'btn-primary' : 'btn-outline-secondary'"
        @click="proceed('WITHDRAWAL')"
      >
        예금 중도해지로 진행
      </button>
    </div>

    <!-- 손실경고 모달 -->
    <div v-if="showLossModal" class="modal d-block" tabindex="-1" style="background-color: rgba(0, 0, 0, 0.5)">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title"><i class="fa-solid fa-triangle-exclamation text-danger me-2"></i>손실 경고</h5>
            <button type="button" class="btn-close" @click="cancelModal"></button>
          </div>
          <div class="modal-body">
            {{ won(comparison.savingAmount) }}원 손해를 보는 선택입니다. 진행하시겠습니까?
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" @click="cancelModal">취소</button>
            <button type="button" class="btn btn-danger" @click="confirmProceed(pendingAction)">진행</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-else-if="loading" class="text-center py-5">
    <div class="spinner-border text-primary" role="status">
      <span class="visually-hidden">불러오는 중...</span>
    </div>
    <div class="text-muted mt-2">결과를 불러오는 중이에요...</div>
  </div>

  <div v-else-if="error" class="text-center py-5">
    <i class="fa-solid fa-circle-exclamation text-danger fs-1 mb-3"></i>
    <div class="mb-3">{{ error }}</div>
    <button type="button" class="btn btn-primary" @click="load">
      <i class="fa-solid fa-rotate-right me-1"></i> 다시 시도
    </button>
  </div>
</template>
