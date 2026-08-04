<script setup>
import { ref, onMounted, computed } from 'vue';
import api from '@/api';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import BottomNav from '@/components/mobile/BottomNav.vue';

const auth = useAuthStore();
const isMenuOpen = ref(false);
const deposits = ref([]);
const router = useRouter();

const toastMessage = ref('');
const showToast = ref(false);

const navigateWithToast = (name, message) => {
  toastMessage.value = message;
  showToast.value = true;
  router.push({ name });
};

const goToAssetRegister = () =>
  navigateWithToast('assetRegister', '자산 등록 입력으로 이동합니다...');
const goToComparisonInput = () =>
  navigateWithToast('comparisonInput', '득실 계산기로 이동합니다...');
const goToComparisonHistory = () =>
  navigateWithToast('comparisonHistory', '나의 득실 기록으로 이동합니다...');

onMounted(async () => {
  const { data } = await api.get('/api/deposits/list');
  deposits.value = data;
});

const totalDeposit = computed(() =>
  deposits.value.reduce((sum, d) => sum + d.balance, 0),
);
const calcDDay = (maturityDate) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const maturity = new Date(maturityDate);
  const diffTime = maturity - today;
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
};
const formatDate = (isoDate) => isoDate.replaceAll('-', '.');
</script>

<template>
  <div class="dashboard">
    <!-- 헤더 -->
    <header class="header">
      <button class="menu-toggle" @click="isMenuOpen = true">☰</button>
      <span class="brand-icon">
        <svg viewBox="0 0 120 120" width="20" height="20">
          <line
            x1="25"
            y1="55"
            x2="95"
            y2="35"
            stroke="#ffbc00"
            stroke-width="9"
            stroke-linecap="round"
          />
          <path d="M60 28 L44 96 L76 96 Z" fill="#26221c" />
          <circle cx="60" cy="28" r="5" fill="#26221c" />
          <ellipse cx="60" cy="97" rx="20" ry="4" fill="#26221c" />
          <line
            x1="25"
            y1="55"
            x2="25"
            y2="84"
            stroke="#26221c"
            stroke-width="2.5"
          />
          <line
            x1="95"
            y1="35"
            x2="95"
            y2="64"
            stroke="#26221c"
            stroke-width="2.5"
          />
          <circle cx="25" cy="94" r="16" fill="#26221c" />
          <text
            x="25"
            y="95"
            fill="#fff"
            font-size="14"
            font-weight="700"
            text-anchor="middle"
            dominant-baseline="central"
          >
            득
          </text>
          <circle
            cx="95"
            cy="74"
            r="13"
            fill="#d9d3c4"
            stroke="#26221c"
            stroke-width="1.5"
          />
          <text
            x="95"
            y="75"
            fill="#26221c"
            font-size="12"
            font-weight="700"
            text-anchor="middle"
            dominant-baseline="central"
          >
            실
          </text>
        </svg>
      </span>
      <h1 class="greeting">안녕하세요, {{ auth.name || '게스트' }}님</h1>
      <div
        class="avatar"
        style="cursor: pointer"
        @click="router.push({ name: 'profile' })"
      >
        {{ (auth.name || '게').charAt(0) }}
      </div>
    </header>

    <!-- 총 보유 자산 카드 -->
    <section class="asset-card">
      <p class="asset-label">총 보유 자산</p>
      <p class="asset-amount">{{ totalDeposit.toLocaleString() }}원</p>
      <div class="asset-sub">
        <span class="asset-deposit-label">예금</span>
        <span class="asset-deposit-value">{{
          totalDeposit.toLocaleString()
        }}</span>
      </div>
    </section>

    <!-- 주 CTA -->
    <button class="cta cta-primary" @click="goToComparisonInput">
      <span class="cta-icon">₩</span>
      <span class="cta-text">
        <span class="cta-title">갑작스럽게 돈이 필요하신가요?</span>
        <span class="cta-subtitle">해지 vs 대출, 득실 계산기로 비교하기</span>
      </span>
      <span class="cta-arrow">›</span>
    </button>

    <!-- 보조 CTA -->
    <button class="cta cta-secondary" @click="goToComparisonHistory">
      <span class="cta-text-solo">나의 득실 기록</span>
      <span class="cta-arrow">›</span>
    </button>

    <!-- 내 예금 섹션 -->
    <div class="section-header">
      <h2>내 예금</h2>
      <router-link :to="{ name: 'assetList' }" class="view-all">
        수정하기 ›
      </router-link>
    </div>

    <div class="deposit-list" v-if="deposits.length">
      <div class="kb-card deposit-card" v-for="d in deposits" :key="d.id">
        <div class="deposit-info">
          <p class="deposit-bank">{{ d.bankName }}</p>
          <p class="deposit-product">{{ d.productName }}</p>
          <p class="deposit-detail">
            {{ d.balance.toLocaleString() }}원 · 연 {{ d.interestRate }}%
          </p>
        </div>
        <div class="deposit-meta">
          <span class="d-day-badge">D-{{ calcDDay(d.maturityDate) }}</span>
          <span class="deposit-maturity"
            >만기 {{ formatDate(d.maturityDate) }}</span
          >
        </div>
      </div>
    </div>

    <div class="empty-deposit" v-else>
      <div class="empty-icon">+</div>
      <p class="empty-title">등록된 자산이 없어요</p>
      <p class="empty-subtitle">
        예금과 대출을 등록하면<br />득실 비교를 시작할 수 있어요
      </p>
      <button class="empty-cta" @click="goToAssetRegister">
        자산 등록하기
      </button>
    </div>

    <!-- 하단 탭바 -->
    <BottomNav active="home" />

    <!-- 메뉴 서랍 -->
    <Transition name="fade">
      <div
        v-if="isMenuOpen"
        class="menu-overlay"
        @click="isMenuOpen = false"
      ></div>
    </Transition>

    <Transition name="slide">
      <nav v-if="isMenuOpen" class="menu-drawer">
        <div class="menu-header">
          <span class="brand-icon">
            <svg viewBox="0 0 120 120" width="18" height="18">
              <line
                x1="25"
                y1="55"
                x2="95"
                y2="35"
                stroke="#ffbc00"
                stroke-width="9"
                stroke-linecap="round"
              />
              <path d="M60 28 L44 96 L76 96 Z" fill="#26221c" />
              <circle cx="60" cy="28" r="5" fill="#26221c" />
              <ellipse cx="60" cy="97" rx="20" ry="4" fill="#26221c" />
              <line
                x1="25"
                y1="55"
                x2="25"
                y2="84"
                stroke="#26221c"
                stroke-width="2.5"
              />
              <line
                x1="95"
                y1="35"
                x2="95"
                y2="64"
                stroke="#26221c"
                stroke-width="2.5"
              />
              <circle cx="25" cy="94" r="16" fill="#26221c" />
              <text
                x="25"
                y="95"
                fill="#fff"
                font-size="14"
                font-weight="700"
                text-anchor="middle"
                dominant-baseline="central"
              >
                득
              </text>
              <circle
                cx="95"
                cy="74"
                r="13"
                fill="#d9d3c4"
                stroke="#26221c"
                stroke-width="1.5"
              />
              <text
                x="95"
                y="75"
                fill="#26221c"
                font-size="12"
                font-weight="700"
                text-anchor="middle"
                dominant-baseline="central"
              >
                실
              </text>
            </svg>
          </span>
          <span class="menu-title">득실</span>
          <button class="icon-btn menu-close" @click="isMenuOpen = false">
            ✕
          </button>
        </div>

        <ul class="menu-list">
          <li class="menu-top">
            <router-link :to="{ name: 'home' }" @click="isMenuOpen = false"
              >홈</router-link
            >
          </li>
        </ul>

        <div class="menu-group">
          <p class="menu-group-title">득실 계산기</p>
          <ul class="menu-sub">
            <li>
              <router-link
                :to="{ name: 'comparisonInput' }"
                @click="isMenuOpen = false"
                >새 득실 계산하기</router-link
              >
            </li>
            <li>
              <router-link
                :to="{ name: 'comparisonHistory' }"
                @click="isMenuOpen = false"
                >나의 득실 기록</router-link
              >
            </li>
          </ul>
        </div>

        <div class="menu-group">
          <p class="menu-group-title">내 자산</p>
          <ul class="menu-sub">
            <li>
              <router-link
                :to="{ name: 'assetList' }"
                @click="isMenuOpen = false"
                >보유 예금 목록</router-link
              >
            </li>
            <li>
              <router-link
                :to="{ name: 'assetRegister' }"
                @click="isMenuOpen = false"
                >자산 등록</router-link
              >
            </li>
          </ul>
        </div>

        <div class="menu-group">
          <p class="menu-group-title">내 정보</p>
          <ul class="menu-sub">
            <li>
              <router-link :to="{ name: 'profile' }" @click="isMenuOpen = false"
                >프로필 / 신용점수</router-link
              >
            </li>
            <li>
              <a
                href="#"
                @click.prevent="
                  auth.logout();
                  isMenuOpen = false;
                "
                >로그아웃</a
              >
            </li>
          </ul>
        </div>
      </nav>
    </Transition>
    <Transition name="fade">
      <div v-if="showToast" class="register-toast">
        {{ toastMessage }}
      </div>
    </Transition>
  </div>
</template>
<style scoped>
.empty-deposit {
  width: calc(100% - 40px);
  margin: 12px auto 0;
  padding: 40px 24px;
  text-align: center;
  background: #fff;
  border: 1px solid #efe9dd;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.empty-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #efe9dd;
  color: #a49a86;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}
.empty-title {
  font-weight: 700;
  font-size: 16px;
  color: #26221c;
}
.empty-subtitle {
  font-size: 13px;
  color: #a49a86;
  line-height: 1.5;
}
.empty-cta {
  margin-top: 10px;
  padding: 10px 20px;
  background: #fff;
  border: 1px solid #ffbc00;
  border-radius: 999px;
  font-weight: 700;
  font-size: 13px;
  color: #26221c;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.empty-cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 188, 0, 0.25);
}

.register-toast {
  position: fixed;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  background: #26221c;
  color: #fff;
  padding: 10px 18px;
  border-radius: 999px;
  font-size: 12px;
  z-index: 20;
  white-space: nowrap;
}
.empty-deposit {
  width: calc(100% - 40px);
  margin: 12px auto 0;
  padding: 40px 20px;
  text-align: center;
  color: #a49a86;
  font-size: 13px;
  border: 1px dashed #efe9dd;
  border-radius: 16px;
}
.dashboard {
  max-width: 390px;
  margin: 0 auto;
  min-height: 100vh;
  background: #fff;
  font-family: '42dot Sans', sans-serif;
  padding-bottom: 90px;
  position: relative;
}

.header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
}
.icon-btn {
  width: 36px;
  height: 36px;
  background: #fff;
  border: 1px solid #e5ddce;
  border-radius: 10px;
  cursor: pointer;
}
.menu-toggle {
  background: none;
  border: none;
  font-size: 20px;
  color: #26221c;
  cursor: pointer;
  padding: 4px;
  line-height: 1;
  flex-shrink: 0;
}
.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff3cf;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.greeting {
  flex: 1;
  font-weight: 700;
  font-size: 16px;
  color: #26221c;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff3cf;
  color: #8a6400;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.avatar:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(138, 100, 0, 0.2);
}

.asset-card {
  box-sizing: border-box;
  width: calc(100% - 40px);
  margin: 6px auto 0;
  padding: 20px 22px;
  background: #26221c;
  border-radius: 18px;
}
.asset-label {
  font-size: 12px;
  color: #a89f8e;
}
.asset-amount {
  margin-top: 6px;
  font-weight: 700;
  font-size: 26px;
  letter-spacing: -0.5px;
  color: #fff;
}
.asset-sub {
  margin-top: 12px;
  display: flex;
  gap: 14px;
  font-size: 12px;
}
.asset-deposit-label {
  color: #c9c0ae;
}
.asset-deposit-value {
  color: #ffbc00;
  margin-left: 4px;
}
.cta {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  width: calc(100% - var(--kb-space-xl));
  height: clamp(66px, 18vw, 76px);
  margin: var(--kb-space-md) auto 0;
  padding: 0 var(--kb-space-lg);
  border-radius: 18px;
  border: none;
  cursor: pointer;
  text-align: left;
  text-decoration: none;
  color: inherit;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.cta:hover {
  transform: translateY(-3px);
}
.cta-primary {
  background: #ffbc00;
  box-shadow: 0 6px 16px rgba(255, 188, 0, 0.3);
}
.cta-primary:hover {
  box-shadow: 0 10px 24px rgba(255, 188, 0, 0.4);
}
.cta-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 19px;
  color: #4a4237;
}
.cta-text {
  flex: 1;
  margin-left: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.cta-title {
  font-weight: 700;
  font-size: var(--kb-font-md);
  color: #4a4237;
}
.cta-subtitle {
  font-size: var(--kb-font-sm);
  color: #7a6a45;
}
.cta-arrow {
  font-weight: 700;
  font-size: 18px;
  color: #7a6a45;
}

.cta-secondary {
  height: var(--kb-btn-height);
  background: #fff;
  border: 1px solid #ffbc00;
  justify-content: space-between;
}
.cta-secondary:hover {
  box-shadow: 0 8px 20px rgba(255, 188, 0, 0.2);
}
.cta-text-solo {
  font-weight: 700;
  font-size: var(--kb-font-md);
  color: #4a4237;
}

.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  width: calc(100% - 40px);
  margin: 26px auto 0;
}
.section-header h2 {
  font-weight: 700;
  font-size: 15px;
  color: #26221c;
}
.view-all {
  font-size: 12px;
  color: #8c8371;
  text-decoration: none;
}

.deposit-list {
  width: calc(100% - 40px);
  margin: 12px auto 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid #efe9dd;
  border-radius: 16px;
  padding: 12px;
  box-sizing: border-box;
  background: #faf8f4;
}
.deposit-list::-webkit-scrollbar {
  width: 5px;
}
.deposit-list::-webkit-scrollbar-track {
  background: transparent;
}
.deposit-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #c8bfae;
}
.deposit-card {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 17px 19px;
}
.deposit-bank {
  font-size: 12px;
  color: #8c8371;
}
.deposit-product {
  margin-top: 4px;
  font-weight: 700;
  font-size: 14px;
  color: #26221c;
}
.deposit-detail {
  margin-top: 4px;
  font-size: 12px;
  color: #a49a86;
}
.deposit-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
.d-day-badge {
  padding: 5px 10px;
  background: #fff3cf;
  border-radius: 999px;
  font-weight: 700;
  font-size: 11px;
  color: #8a6400;
}
.deposit-maturity {
  font-size: 11px;
  color: #b3aa99;
}

.menu-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 10;
}
.menu-drawer {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 78%;
  max-width: 300px;
  background: #fff;
  z-index: 11;
  padding: 20px;
  box-sizing: border-box;
  overflow-y: auto;
}
.menu-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 16px;
  border-bottom: 1px solid #efe9dd;
}
.menu-title {
  flex: 1;
  font-weight: 700;
  font-size: 16px;
  color: #26221c;
}
.menu-close {
  margin-left: auto;
}

.menu-list {
  list-style: none;
  margin: 18px 0 0;
  padding: 0;
}
.menu-top a {
  display: block;
  padding: 8px;
  font-weight: 700;
  font-size: 16px;
  color: #26221c;
  text-decoration: none;
}

.menu-group {
  margin-top: 22px;
}
.menu-group-title {
  margin: 0 0 10px;
  padding: 0 8px;
  font-weight: 700;
  font-size: 15px;
  color: #26221c;
}
.menu-sub {
  list-style: none;
  margin: 0;
  padding: 0 0 0 16px;
  border-left: 2px solid #efe9dd;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.menu-sub li a {
  display: block;
  padding: 8px;
  font-size: 13px;
  color: #8c8371;
  text-decoration: none;
}
.menu-sub li a:hover {
  color: #26221c;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.25s ease;
}
.slide-enter-from,
.slide-leave-to {
  transform: translateX(-100%);
}
</style>
