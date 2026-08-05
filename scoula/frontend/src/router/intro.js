export default [
  {
    path: '/intro',
    name: 'intro',
   component: () => import('../pages/asset/IntroPage.vue'),
    // blank 레이아웃이라 공통 헤더 없이 단독으로 그려진다
    meta: { layout: 'blank' },
  },
];