import { useProfitLossStore } from '@/stores/profitLoss';

export default [
  {
    path: '/comparisons/input',
    name: 'comparisonInput',
    component: () => import('@/pages/profitLoss/ComparisonInput.vue'),
    meta: {
      layout: 'mobile',
      requiresAuth: true,
    },
  },
  {
    path: '/comparison/credit/eligibility',
    name: 'creditEligibility',
    component: () => import('@/pages/profitLoss/CreditEligibility.vue'),
    meta: {
      layout: 'mobile',
      requiresAuth: true,
    },
    beforeEnter: () => {
      const profitLossStore = useProfitLossStore();
      const { state } = profitLossStore;

      const hasPreviousInput =
        state.deposit.userDepositId !== null &&
        state.userFinancialInfo.creditGrade !== null &&
        state.userFinancialInfo.creditGrade <= 4 &&
        state.userFinancialInfo.monthlyPayment !== null &&
        state.userFinancialInfo.monthlyPayment > 0 &&
        state.comparisonCondition.urgentAmount > 0 &&
        state.loan.loanType === 'CREDIT';

      if (!hasPreviousInput) {
        return {
          name: 'comparisonInput',
          replace: true,
        };
      }

      return true;
    },
  },
  {
    path: '/comparison/credit/preferential',
    name: 'creditPreferential',
    component: () => import('@/pages/profitLoss/CreditPreferential.vue'),
    meta: {
      layout: 'mobile',
      requiresAuth: true,
    },
    beforeEnter: () => {
      const profitLossStore = useProfitLossStore();

      if (
        !profitLossStore.isCreditEligibilityComplete ||
        profitLossStore.state.loan.loanProductId.length === 0
      ) {
        return {
          name: 'comparisonInput',
          replace: true,
        };
      }

      return true;
    },
  },
  {
    path: '/comparison/jeonse/eligibility',
    name: 'jeonseEligibility',
    component: () => import('@/pages/profitLoss/JeonseEligibility.vue'),
    meta: {
      layout: 'mobile',
      requiresAuth: true,
    },
    beforeEnter: () => {
      const profitLossStore = useProfitLossStore();
      const { state } = profitLossStore;

      const hasPreviousInput =
        state.deposit.userDepositId !== null &&
        state.userFinancialInfo.creditGrade !== null &&
        state.userFinancialInfo.creditGrade <= 4 &&
        state.userFinancialInfo.monthlyPayment !== null &&
        state.userFinancialInfo.monthlyPayment > 0 &&
        state.comparisonCondition.urgentAmount > 0 &&
        state.loan.loanType === 'JEONSE';

      if (!hasPreviousInput) {
        return {
          name: 'comparisonInput',
          replace: true,
        };
      }

      return true;
    },
  },
  {
    path: '/comparison/jeonse/preferential',
    name: 'jeonsePreferential',
    component: () => import('@/pages/profitLoss/JeonsePreferential.vue'),
    meta: {
      layout: 'mobile',
      requiresAuth: true,
    },
    beforeEnter: () => {
      const profitLossStore = useProfitLossStore();

      if (
        !profitLossStore.isJeonseEligibilityComplete ||
        profitLossStore.state.loan.loanProductId.length === 0
      ) {
        return {
          name: 'comparisonInput',
          replace: true,
        };
      }

      return true;
    },
  },
  {
    path: '/comparison/summary',
    name: 'comparisonSummary',
    component: () => import('@/pages/profitLoss/ComparisonSummary.vue'),
    meta: {
      layout: 'mobile',
      requiresAuth: true,
    },
    beforeEnter: () => {
      const profitLossStore = useProfitLossStore();

      if (profitLossStore.state.loan.totalDiscountRate === null) {
        return {
          name: 'comparisonInput',
          replace: true,
        };
      }

      return true;
    },
  },
  {
    path: '/comparisons/result/:comparisonId',
    name: 'comparisons/result',
    component: () => import('@/pages/profitLoss/ComparisonResultPage.vue'),
    meta: {
      layout: 'mobile',
      requiresAuth: true,
    },
    props: true,
  },
  {
    path: '/comparison/history',
    name: 'comparisonHistory',
    component: () => import('../pages/profitLoss/HistoryPage.vue'),
    meta: { layout: 'mobile', requiresAuth: true },
  },
];
