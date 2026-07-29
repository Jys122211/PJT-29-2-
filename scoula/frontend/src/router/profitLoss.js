import ComparisonResultPage from '@/pages/profitLoss/ComparisonResultPage.vue';
import ComparisonSubmitPage from '@/pages/profitLoss/ComparisonSubmitPage.vue';

// 기존 '/comparison/input' 라우트는 ProfitLossPage를 import 없이 참조하고 있어
// 모듈 로드 시 ReferenceError로 라우터 전체가 깨지는 상태였다 (해당 컴포넌트 파일 자체가 없음).
// 그 컴포넌트가 아직 만들어지지 않아 일단 제거했다 — 입력 화면 담당자가 만들면 다시 추가할 것.
export default [
  {
    // TODO: 조윤상님 입력 화면(자금 입력→자격확인→우대금리확인)이 완성되면 이 라우트는 지우고
    // 그쪽 마지막 화면의 "비교하기" 버튼이 useComparison().submitComparison을 호출하도록 연결한다.
    // 지금은 통합 검증용 임시 제출 화면이다.
    path: '/comparisons/submit',
    name: 'comparisons/submit',
    component: ComparisonSubmitPage,
  },
  {
    path: '/comparisons/result/:comparisonId',
    name: 'comparisons/result',
    component: ComparisonResultPage,
  },
];
