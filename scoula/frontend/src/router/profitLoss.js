import ComparisonResultPage from '@/pages/profitLoss/ComparisonResultPage.vue';

// 기존 '/comparison/input' 라우트는 ProfitLossPage를 import 없이 참조하고 있어
// 모듈 로드 시 ReferenceError로 라우터 전체가 깨지는 상태였다 (해당 컴포넌트 파일 자체가 없음).
// 그 컴포넌트가 아직 만들어지지 않아 일단 제거했다 — 입력 화면 담당자가 만들면 다시 추가할 것.
export default [
  {
    path: '/comparisons/result',
    name: 'comparisons/result',
    component: ComparisonResultPage,
  },
];
