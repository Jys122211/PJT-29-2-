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
    { path: '/', name: 'home', component: HomePage },
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

router.beforeEach((to) => {
  const authStore = useAuthStore();
  const isAuthenticated = authStore.isLogin && Boolean(authStore.getToken());
  if (to.meta.requiresAuth && !isAuthenticated) {
    return { name: 'login' };
  }
  return true;
});

export default router;
