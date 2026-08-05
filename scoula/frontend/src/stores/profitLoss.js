import { computed, reactive } from 'vue';
import { defineStore } from 'pinia';

const INITIAL_REQUIRED_AMOUNT = 5_000_000;

const CREDIT_ELIGIBILITY_QUESTIONS = [
  {
    id: 1,
    type: 'QUALIFICATION',
    text: '현재 건강보험 직장 가입자 이신가요?',
  },
  {
    id: 2,
    type: 'QUALIFICATION',
    text: '현재 직장에서 6개월 이상 근무하고 있으신가요?',
  },
  {
    id: 3,
    type: 'COMPARISON_CONDITION',
    requestField: 'IS_PARTIAL_ALLOWED',
    text: '분할 인출이 가능하신가요? (계좌별 3회(해지 포함)이내 가능)',
  },
  {
    id: 4,
    type: 'COMPARISON_CONDITION',
    requestField: 'IS_LUMP_SUM',
    text: '예금이 만기시에, 만기된 예금으로 대출을 갚으실 예정인가요?',
  },
];

const JEONSE_COMPARISON_CONDITIONS = [
  {
    id: 'jeonse_comp_1',
    type: 'COMPARISON_CONDITION',
    requestField: 'IS_PARTIAL_ALLOWED',
    text: '분할 인출이 가능하신가요? (계좌별 3회(해지 포함)이내 가능)',
  },
  {
    id: 'jeonse_comp_2',
    type: 'COMPARISON_CONDITION',
    requestField: 'IS_LUMP_SUM',
    text: '예금이 만기시에, 만기된 예금으로 대출을 갚으실 예정인가요?',
  },
];

const CREDIT_PREFERENTIAL_GROUPS = [
  {
    id: 'CARD_USAGE',
    type: 'SINGLE_SELECT',
    title: '우대금리 (1) KB신용카드 이용실적',
    description:
      '※ 결제계좌를 KB국민은행으로 지정하고, 최근 3개월 이용실적 기준',
    options: [
      {
        value: 'OVER_900000',
        preferentialQuestionId: 3,
        text: '최근 3개월 KB국민은행 계좌 결제 90만원 이상',
      },
      {
        value: 'OVER_600000',
        preferentialQuestionId: 2,
        text: '최근 3개월 KB국민은행 계좌 결제 60만원 이상',
      },
      {
        value: 'OVER_300000',
        preferentialQuestionId: 1,
        text: '최근 3개월 KB국민은행 계좌 결제 30만원 이상',
      },
      {
        value: 'NONE',
        preferentialQuestionId: null,
        text: '해당 없음',
      },
    ],
  },
  {
    id: 'SALARY_TRANSFER',
    type: 'YES_NO',
    title: '우대금리 (2) 급여(연금)이체 관련 실적 우대',
    preferentialQuestionId: 4,
    text: '최근 3개월 동안 본인 명의의 KB국민은행 계좌로 건별 50만원 이상의 급여 또는 연금을 2회 이상 받으셨나요?',
  },
  {
    id: 'SAVINGS_ACCOUNT',
    type: 'YES_NO',
    title: '우대금리 (3) 적립식예금(30만원 이상) 보유 우대',
    preferentialQuestionId: 5,
    text: '현재 본인 명의의 KB국민은행 적금 계좌에 30만원 이상의 잔액이 있나요?',
  },
  {
    id: 'AUTO_TRANSFER',
    type: 'YES_NO',
    title: '우대금리 (4) 자동이체 실적 우대',
    preferentialQuestionId: 6,
    text: 'KB국민은행 계좌에서 아파트 관리비, 지로요금의 자동이체가 3건 이상 출금된 실적이 있나요?',
    description:
      '지로요금은 전기·가스·수도·통신요금 등 고지서로 청구되는 요금을 말합니다.',
  },
  {
    id: 'STAR_BANKING',
    type: 'YES_NO',
    title: '우대금리 (5) KB 스타뱅킹 이용 우대',
    preferentialQuestionId: 7,
    text: '현재 KB스타뱅킹 앱을 이용하고 계신가요?',
  },
];

function copyPreferentialGroups() {
  return CREDIT_PREFERENTIAL_GROUPS.map((group) => ({
    ...group,
    options: group.options?.map((option) => ({ ...option })),
  }));
}

function calculateCreditGrade(score) {
  if (score === null || score < 1 || score > 1000) return null;

  if (score > 900) return 1;
  if (score > 800) return 2;
  if (score > 700) return 3;
  if (score > 600) return 4;
  if (score > 500) return 5;
  if (score > 400) return 6;
  if (score > 300) return 7;
  if (score > 200) return 8;
  if (score > 100) return 9;

  return 10;
}

function createInitialState() {
  return {
    deposits: [],
    creditScore: null,

    userFinancialInfo: {
      monthlyPayment: null,
      creditGrade: null,
    },

    deposit: {
      userDepositId: null,
      isPartialAllowed: null,
    },

    loan: {
      loanProductId: [],
      loanType: 'CREDIT',
      totalDiscountRate: null,
    },

    comparisonCondition: {
      urgentAmount: INITIAL_REQUIRED_AMOUNT,
      isLumpSum: true,
    },

    creditEligibility: {
      questions: CREDIT_ELIGIBILITY_QUESTIONS.map((question) => ({
        ...question,
      })),
      answers: {},
    },

    creditPreferential: {
      groups: copyPreferentialGroups(),
      answers: {},
    },

    // 전세대출 데이터 구조 추가
    jeonseEligibility: {
      questions: [],
      answers: {},
    },

    jeonsePreferential: {
      groups: [],
      answers: {},
    },
  };
}

export const useProfitLossStore = defineStore('profitLoss', () => {
  // 반응형 상태
  const state = reactive(createInitialState());

  // 최종 POST 요청에 전달할 객체
  const requestPayload = computed(() => ({
    userFinancialInfo: {
      monthlyPayment: state.userFinancialInfo.monthlyPayment,
      creditGrade: state.userFinancialInfo.creditGrade,
    },

    deposit: {
      userDepositId: state.deposit.userDepositId,
      isPartialAllowed: state.deposit.isPartialAllowed,
    },

    loan: {
      loanProductId: [...state.loan.loanProductId],
      loanType: state.loan.loanType,
      totalDiscountRate: state.loan.totalDiscountRate,
    },

    comparisonCondition: {
      urgentAmount: state.comparisonCondition.urgentAmount,
      isLumpSum: state.comparisonCondition.isLumpSum,
    },
  }));

  const qualificationQuestionIds = computed(() =>
    state.creditEligibility.questions
      .filter(
        (question) =>
          question.type === 'QUALIFICATION' &&
          state.creditEligibility.answers[question.id] === true,
      )
      .map((question) => question.id),
  );

  const preferentialQuestionIds = computed(() =>
    state.creditPreferential.groups.flatMap((group) => {
      const answer = state.creditPreferential.answers[group.id];

      if (group.type === 'SINGLE_SELECT') {
        const selectedOption = group.options.find(
          (option) => option.value === answer,
        );

        return selectedOption?.preferentialQuestionId == null
          ? []
          : [selectedOption.preferentialQuestionId];
      }

      return answer === true ? [group.preferentialQuestionId] : [];
    }),
  );

  const isCreditEligibilityComplete = computed(() =>
    state.creditEligibility.questions.every(
      (question) =>
        typeof state.creditEligibility.answers[question.id] === 'boolean',
    ),
  );

  const isCreditPreferentialComplete = computed(() =>
    state.creditPreferential.groups.every((group) => {
      const answer = state.creditPreferential.answers[group.id];

      return group.type === 'SINGLE_SELECT'
        ? typeof answer === 'string'
        : typeof answer === 'boolean';
    }),
  );

  // 전세대출 Computed
  const jeonseQualificationQuestionIds = computed(() =>
    state.jeonseEligibility.questions
      .filter(
        (question) =>
          question.type === 'QUALIFICATION' &&
          state.jeonseEligibility.answers[question.id] === true,
      )
      .map((question) => question.id),
  );

  const jeonsePreferentialQuestionIds = computed(() =>
    state.jeonsePreferential.groups.flatMap((group) => {
      const answer = state.jeonsePreferential.answers[group.id];

      if (group.type === 'SINGLE_SELECT') {
        const selectedOption = group.options.find(
          (option) => option.value === answer,
        );

        return selectedOption?.preferentialQuestionId == null
          ? []
          : [selectedOption.preferentialQuestionId];
      }

      return answer === true ? [group.preferentialQuestionId] : [];
    }),
  );

  const isJeonseEligibilityComplete = computed(() =>
    state.jeonseEligibility.questions.every(
      (question) =>
        typeof state.jeonseEligibility.answers[question.id] === 'boolean',
    ),
  );

  const isJeonsePreferentialComplete = computed(() =>
    state.jeonsePreferential.groups.every((group) => {
      const answer = state.jeonsePreferential.answers[group.id];

      return group.type === 'SINGLE_SELECT'
        ? typeof answer === 'string'
        : typeof answer === 'boolean';
    }),
  );

  // action
  const setDeposits = (deposits) => {
    state.deposits = Array.isArray(deposits) ? deposits : [];

    const selectedDepositExists = state.deposits.some(
      (deposit) => deposit.id === state.deposit.userDepositId,
    );

    if (!selectedDepositExists) {
      state.deposit.userDepositId = state.deposits[0]?.id ?? null;
    }
  };

  const selectDeposit = (userDepositId) => {
    state.deposit.userDepositId = userDepositId;
  };

  const setCreditScore = (creditScore) => {
    state.creditScore = creditScore;
    state.userFinancialInfo.creditGrade = calculateCreditGrade(creditScore);
  };

  const setMonthlyPayment = (monthlyPayment) => {
    state.userFinancialInfo.monthlyPayment = monthlyPayment;
  };

  const setUrgentAmount = (urgentAmount) => {
    state.comparisonCondition.urgentAmount = urgentAmount;
  };

  const setLoanType = (loanType) => {
    state.loan.loanType = loanType;
  };

  const setPartialAllowed = (isPartialAllowed) => {
    state.deposit.isPartialAllowed = isPartialAllowed;
  };

  const setLoanProducts = (loanProductIds) => {
    state.loan.loanProductId = Array.isArray(loanProductIds)
      ? [...loanProductIds]
      : [];
  };

  const setTotalDiscountRate = (totalDiscountRate) => {
    state.loan.totalDiscountRate = totalDiscountRate;
  };

  const setLumpSum = (isLumpSum) => {
    state.comparisonCondition.isLumpSum = isLumpSum;
  };

  const setCreditEligibilityAnswer = (questionId, answer) => {
    const question = state.creditEligibility.questions.find(
      (question) => question.id === questionId,
    );

    if (!question || typeof answer !== 'boolean') return;

    state.creditEligibility.answers[questionId] = answer;

    if (question.requestField === 'IS_PARTIAL_ALLOWED') {
      state.deposit.isPartialAllowed = answer;
    }

    if (question.requestField === 'IS_LUMP_SUM') {
      state.comparisonCondition.isLumpSum = answer;
    }
  };

  const setCreditPreferentialAnswer = (groupId, answer) => {
    const group = state.creditPreferential.groups.find(
      (group) => group.id === groupId,
    );

    if (!group) return;

    if (group.type === 'SINGLE_SELECT') {
      const optionExists = group.options.some(
        (option) => option.value === answer,
      );

      if (!optionExists) return;
    } else if (typeof answer !== 'boolean') {
      return;
    }

    state.creditPreferential.answers[groupId] = answer;
    state.loan.totalDiscountRate = null;
  };

  // 전세대출 Actions
  const setJeonseEligibilityQuestions = (questions) => {
    const apiQuestions = Array.isArray(questions)
      ? questions.map((q) => ({ ...q, type: 'QUALIFICATION' }))
      : [];
    state.jeonseEligibility.questions = [
      ...apiQuestions,
      ...JEONSE_COMPARISON_CONDITIONS,
    ];
  };

  const setJeonsePreferentialItems = (items) => {
    if (!Array.isArray(items)) {
      state.jeonsePreferential.groups = [];
      return;
    }

    const cardItems = items.filter((item) =>
      item.conditionName.includes('KB국민카드 이용실적'),
    );
    const otherItems = items.filter(
      (item) => !item.conditionName.includes('KB국민카드 이용실적'),
    );

    const groups = [];
    if (cardItems.length > 0) {
      groups.push({
        id: 'JEONSE_CARD_USAGE',
        type: 'SINGLE_SELECT',
        title: 'KB국민카드 이용실적 우대',
        description:
          '※ 결제계좌를 KB국민은행으로 지정하고, 최근 3개월 이용실적 기준',
        options: [
          ...cardItems
            .sort((a, b) => b.id - a.id)
            .map((item) => {
              let formattedText = item.conditionDetail;
              if (formattedText.includes('90'))
                formattedText = '최근 3개월 90만원 이상';
              else if (formattedText.includes('60'))
                formattedText = '최근 3개월 60만원 이상';
              else if (formattedText.includes('30'))
                formattedText = '최근 3개월 30만원 이상';

              return {
                value: item.id.toString(),
                preferentialQuestionId: item.id,
                text: formattedText,
              };
            }),
          { value: 'NONE', preferentialQuestionId: null, text: '해당 없음' },
        ],
      });
    }

    otherItems.forEach((item) => {
      groups.push({
        id: item.id.toString(),
        type: 'YES_NO',
        title: item.conditionName,
        preferentialQuestionId: item.id,
        text: item.conditionDetail,
      });
    });

    state.jeonsePreferential.groups = groups;
  };

  const setJeonseEligibilityAnswer = (questionId, answer) => {
    const question = state.jeonseEligibility.questions.find(
      (q) => q.id === questionId,
    );
    if (!question || typeof answer !== 'boolean') return;

    state.jeonseEligibility.answers[questionId] = answer;

    if (question.requestField === 'IS_PARTIAL_ALLOWED') {
      state.deposit.isPartialAllowed = answer;
    }

    if (question.requestField === 'IS_LUMP_SUM') {
      state.comparisonCondition.isLumpSum = answer;
    }
  };

  const setJeonsePreferentialAnswer = (groupId, answer) => {
    const group = state.jeonsePreferential.groups.find((g) => g.id === groupId);
    if (!group) return;

    if (group.type === 'SINGLE_SELECT') {
      const optionExists = group.options.some((opt) => opt.value === answer);
      if (!optionExists) return;
    } else if (typeof answer !== 'boolean') {
      return;
    }

    state.jeonsePreferential.answers[groupId] = answer;
    state.loan.totalDiscountRate = null;
  };

  const reset = () => {
    Object.assign(state, createInitialState());
  };

  return {
    state,
    requestPayload,
    qualificationQuestionIds,
    preferentialQuestionIds,
    isCreditEligibilityComplete,
    isCreditPreferentialComplete,
    setDeposits,
    selectDeposit,
    setCreditScore,
    setMonthlyPayment,
    setUrgentAmount,
    setLoanType,
    setPartialAllowed,
    setLoanProducts,
    setTotalDiscountRate,
    setLumpSum,
    setCreditEligibilityAnswer,
    setCreditPreferentialAnswer,
    jeonseQualificationQuestionIds,
    jeonsePreferentialQuestionIds,
    isJeonseEligibilityComplete,
    isJeonsePreferentialComplete,
    setJeonseEligibilityQuestions,
    setJeonsePreferentialItems,
    setJeonseEligibilityAnswer,
    setJeonsePreferentialAnswer,
    reset,
  };
});
