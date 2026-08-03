import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '../pages/HomePage.vue';
import authRoutes from './auth';
import boardRoutes from './board';
import travelRoutes from './travel';
import galleryRoutes from './gallery';
import profitLossRoutes from './profitLoss';
import { useAuthStore } from '@/stores/auth';

// ===================================
import assetRoutes from './asset.js';
// ===================================

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
      meta: { requiresAuth: true, layout: 'mobile' },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: HomePage,
      meta: { requiresAuth: true, layout: 'mobile' },
    },
    {
      path: '/deposits/count',
      redirect: { name: 'assetRegister' },
    },
    ...authRoutes,
    ...boardRoutes,
    ...travelRoutes,
    ...galleryRoutes,
    ...profitLossRoutes,
    // ===============
    ...assetRoutes,
    // ===============
  ],
});

// 사용자별 화면은 유효한 JWT가 있을 때만 접근하도록 검사한다.
router.beforeEach((to) => {
  const auth = useAuthStore();

  // 과거 쿼리 주소로 접근해도 로그인 URL은 항상 /login으로 정리한다.
  if (to.name === 'login' && Object.keys(to.query).length > 0) {
    return {
      name: 'login',
      replace: true,
    };
  }

  // 로그인하지 않은 사용자는 쿼리 문자열 없이 로그인 화면으로 이동한다.
  if (to.meta.requiresAuth && !auth.hasValidSession()) {
    return {
      name: 'login',
      replace: true,
    };
  }

  return true;
});

export default router;
