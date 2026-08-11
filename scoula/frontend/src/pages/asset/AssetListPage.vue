<script setup>
/**
 * 내 보유 예금 (화면 02-03 빈 상태 / 07-09 목록)
 *
 * deposits 가 비어 있으면 02-03 의 "등록된 자산이 없어요" 상태를 렌더링합니다.
 */
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import depositApi from '@/api/depositApi';
import BottomNav from '@/components/mobile/BottomNav.vue';
import {
  calcDDay,
  dDayText,
  extractApiError,
  formatNumber,
  toDotDate,
  toDisplayKbAccount,
} from '@/util/depositFormat';

const router = useRouter();

const URGENT_DAYS = 30;
const deposits = ref([]);
const count = ref(0);
const totalPrincipal = ref(0);
const loading = ref(true);
const loadError = ref('');


async function load() {
  loading.value = true;
  loadError.value = '';

  try {
    const data = await depositApi.getList();
    deposits.value = Array.isArray(data.deposits) ? data.deposits : [];
    count.value = data.count ?? deposits.value.length;
    totalPrincipal.value = data.totalPrincipal ?? 0;
  } catch (error) {
    deposits.value = [];
    count.value = 0;
    totalPrincipal.value = 0;
    loadError.value = extractApiError(error).message;
  } finally {
    loading.value = false;
  }
}

function goRegister() {
  router.push({ name: 'assetRegister' });
}

function isUrgent(maturity) {
  const days = calcDDay(maturity);
  return days !== null && days <= URGENT_DAYS;
}

function goEdit(deposit) {
  router.push({
    name: 'assetEdit',
    params: { userDepositId: deposit.userDepositId },
  });
}

onMounted(load);
</script>

<template>
  <main class="page">
    <header class="page-head">
      <h1>내 보유 예금</h1>
      <span class="badge">보유 상품 {{ count }}건</span>
    </header>

    <!-- 로딩 -->
    <section v-if="loading" class="state-card">
      <strong>불러오는 중이에요</strong>
      <p>잠시만 기다려 주세요</p>
    </section>

    <!-- 조회 실패 -->
    <section v-else-if="loadError" class="state-card">
      <strong>{{ loadError }}</strong>
      <p>네트워크 상태를 확인한 후 다시 시도해주세요</p>
      <button type="button" class="pill" @click="load">다시 시도</button>
    </section>

    <!-- 02-03 빈 상태 -->
    <section v-else-if="deposits.length === 0" class="empty-card">
      <div class="plus"><i class="fa-solid fa-plus"></i></div>
      <strong>등록된 자산이 없어요</strong>
      <p>예금과 대출을 등록하면<br />득실 비교를 시작할 수 있어요</p>
      <button type="button" class="pill" @click="goRegister">
        자산 등록하기
      </button>
    </section>

    <!-- 07-09 목록 -->
    <template v-else>
      <section class="total-card">
        <small>총 보유 예금</small>
        <strong>{{ formatNumber(totalPrincipal) }}원</strong>
      </section>

      <!-- 1. 새 예금 등록하기 버튼을 위로 이동! -->
      <button type="button" class="add-card" @click="goRegister">
        <span class="ico"><i class="fa-solid fa-plus"></i></span>
        <span class="tx">새 예금 등록하기</span>
        <i class="fa-solid fa-chevron-right chev"></i>
      </button>

      <!-- 2. 보유 예금 목록을 그 아래로 배치 -->
      <ul class="deposit-list">
        <li v-for="deposit in deposits" :key="deposit.userDepositId">
          <button type="button" class="deposit-card" @click="goEdit(deposit)">
            <div class="left">
              <strong class="name">{{ deposit.productName }}</strong>
              <small class="meta">
                {{ deposit.bankName }} ·
                {{ formatNumber(deposit.principalAmount) }}원 · 연
                {{ deposit.appliedRate }}%
              </small>
              <small v-if="deposit.accountNumber" class="acct">
                계좌번호 : {{ toDisplayKbAccount(deposit.accountNumber) }}</small
              >
            </div>

            <div class="right">
              <span
                class="dday"
                :class="{ urgent: isUrgent(deposit.maturityDate) }"
              >{{ dDayText(deposit.maturityDate) }}</span>
              <small class="mat">{{ toDotDate(deposit.maturityDate) }}</small>
            </div>
          </button>
        </li>
      </ul>
    </template>

    <BottomNav active="asset" />
  </main>
</template>

<style scoped>
.page {
  --kb-yellow: #ffbc00;
  --kb-border: #e9e0d2;
  --kb-text: #292725;
  --kb-muted: #9a938a;
  --kb-red: #c0392b;
  --kb-soft: #fff6df;
  --kb-line: #f2d89a;

  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 390px;
  height: 100vh;
  margin: 0 auto;
  padding: 16px 20px 86px;
  color: var(--kb-text);
  background: #faf9f7;
}

.page-head {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.page-head h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.badge {
  font-size: 11px;
  color: var(--kb-muted);
}

/* ---------- 상태 카드 ---------- */
.state-card,
.empty-card {
  padding: 36px 20px 28px;
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  background: #fff;
  text-align: center;
}

.state-card strong,
.empty-card strong {
  display: block;
  font-size: 16px;
}

.state-card p,
.empty-card p {
  margin: 8px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--kb-muted);
}

.plus {
  display: grid;
  width: 54px;
  height: 54px;
  margin: 0 auto 18px;
  border-radius: 13px;
  place-items: center;
  font-size: 20px;
  color: #9a9ea2;
  background: #efede9;
}

.pill {
  margin-top: 18px;
  padding: 11px 26px;
  border: 1.5px solid var(--kb-red);
  border-radius: 22px;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  background: #fff;
  cursor: pointer;
}

/* ---------- 총액 ---------- */
.total-card {
  flex: none;
  margin-bottom: 10px;
  padding: 12px 16px;
  border-radius: 14px;
  color: #fff;
  background: #26282b;
}

.total-card small {
  font-size: 11.5px;
  color: #9ea3a8;
}

.total-card strong {
  display: block;
  margin-top: 3px;
  font-size: 19px;
  font-weight: 800;
}

/* ---------- 목록 ---------- */
.deposit-list {
  display: grid;
  flex: 1;
  min-height: 0;
  gap: 12px;
  align-content: start;
  overflow-y: auto;
  margin: 0;
  padding: 2px 6px 2px 2px;
  list-style: none;
  scrollbar-width: thin;
  scrollbar-color: #d9d2c6 transparent;
}

.deposit-list::-webkit-scrollbar {
  width: 6px;
}

.deposit-list::-webkit-scrollbar-thumb {
  border-radius: 3px;
  background: #d9d2c6;
}

.deposit-list::-webkit-scrollbar-track {
  background: transparent;
}

.deposit-card {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 19px 20px;
  border: 1.5px solid var(--kb-line);
  border-radius: 16px;
  font: inherit;
  text-align: left;
  background: #fff;
  cursor: pointer;
  transition:
    transform 0.12s ease,
    background 0.12s ease,
    border-color 0.12s ease;
}

.deposit-card:active {
  transform: scale(0.975);
  border-color: var(--kb-yellow);
  background: #fffdf4;
}

@media (hover: hover) {
  .deposit-card:hover {
    transform: translateY(-2px);
    border-color: var(--kb-yellow);
    background: #fffdf8;
    box-shadow: 0 6px 16px rgba(214, 178, 74, 0.18);
  }
}

.deposit-card:focus-visible {
  outline: 2px solid var(--kb-yellow);
  outline-offset: 2px;
}

.left {
  min-width: 0;
}

.bank {
  font-size: 11.5px;
  color: var(--kb-muted);
}

.name {
  display: block;
  margin: 0 0 4px;
  overflow: hidden;
  font-size: 17px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  font-size: 12.5px;
  color: var(--kb-muted);
}

.acct {
  display: block;
  margin-top: 4px;
  font-size: 11.5px;
  letter-spacing: 0.3px;
  color: #b3aa99;
}

.right {
  flex: none;
  text-align: right;
}

.dday {
  display: inline-block;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #8a6400;
  background: #fff3cf;
}

.dday.urgent {
  color: #fff;
  background: var(--kb-red);
}

.mat {
  display: block;
  margin-top: 8px;
  font-size: 11px;
  color: #b3aa99;
}

/* ---------- 추가 버튼 ---------- */
.add-card {
  display: flex;
  flex: none;
  width: 100%;
  align-items: center;
  gap: 14px;
  margin-top: 12px;
  margin-bottom: 12px;
  padding: 18px 19px;
  border: 0;
  border-radius: 16px;
  font: inherit;
  color: #fff;
  background: #26282b;
 text-align: left;
  cursor: pointer;
  transition:
    transform 0.14s ease,
    background 0.14s ease,
    box-shadow 0.14s ease;
}

.add-card:active {
  transform: scale(0.98);
  background: #1c1e21;
}

@media (hover: hover) {
  .add-card:hover {
    background: #303337;
    box-shadow: 0 6px 16px rgba(38, 40, 43, 0.22);
  }

  .add-card:hover .ico {
    transform: rotate(90deg);
  }

  .add-card:hover .chev {
    transform: translateX(3px);
    color: #d9d2c6;
  }
}

.add-card:focus-visible {
  outline: 2px solid var(--kb-yellow);
  outline-offset: 2px;
}

.add-card .ico {
  display: grid;
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 8px;
  place-items: center;
  font-size: 13px;
  color: #26282b;
  background: var(--kb-yellow);
  transition: transform 0.18s ease;
}

.add-card .tx {
  flex: 1;
  font-size: 15px;
  font-weight: 700;
}

.chev {
  flex: none;
  font-size: 12px;
  color: #8a8f94;
  transition:
    transform 0.14s ease,
    color 0.14s ease;
}



@media (prefers-reduced-motion: reduce) {
  .deposit-card,
  .add-card {
    transition: none;
  }
  .deposit-card:active,
  .add-card:active {
    transform: none;
  }
}
</style>
