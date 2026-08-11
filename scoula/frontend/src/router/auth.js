export default [
  // 로그인·회원가입 화면은 공통 헤더와 메뉴가 없는 blank 레이아웃을 사용한다.
  {
    path: '/login',
    name: 'login',
    component: () => import('../pages/auth/LoginPage.vue'),
    meta: { layout: 'blank' },
  },
  {
    path: '/signup',
    name: 'join',
    component: () => import('../pages/auth/JoinPage.vue'),
    meta: { layout: 'blank' },
  },
  {
    // 비밀번호 찾기는 로그인하지 못한 사용자가 쓰는 화면이라 인증을 요구하지 않는다.
    path: '/password/find',
    name: 'findPassword',
    component: () => import('../pages/auth/FindPasswordPage.vue'),
    meta: { layout: 'blank' },
  },
  {
    path: '/signup/complete',
    name: 'signupComplete',
    component: () => import('../pages/auth/SignupCompletePage.vue'),
    // 회원가입 직후 자동 로그인된 사용자만 완료 화면에 접근할 수 있다.
    meta: { layout: 'blank', requiresAuth: true },
  },
  {
    path: '/auth/profile',
    name: 'profile',
    component: () => import('../pages/auth/ProfilePage.vue'),
    meta: {
      layout: 'mobile',
      requiresAuth: true,
    },
  },
];
