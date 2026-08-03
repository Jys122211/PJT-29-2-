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
  extractApiError,
  formatNumber,
  toDotDate,
} from '@/util/depositFormat';

const router = useRouter();

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

function goEdit(deposit) {
  router.push({
    name: 'assetEdit',
    params: { userDepositId: deposit.userDepositId },
  });
}

/** 만기일 기준으로 D-Day 직접 계산 */
function dDayText(deposit) {
  if (!deposit || !deposit.maturityDate) return '만기일';

  // 1. 숫자만 추출 (예: '20261016')
  const dateString = String(deposit.maturityDate).replace(/[^0-9]/g, '');
  if (dateString.length !== 8) return '만기일';

  // 2. 날짜 객체 생성 (자바스크립트 Date는 월을 0부터 시작하므로 -1)
  const year = parseInt(dateString.substring(0, 4), 10);
  const month = parseInt(dateString.substring(4, 6), 10) - 1;
  const day = parseInt(dateString.substring(6, 8), 10);

  const targetDate = new Date(year, month, day);
  const today = new Date();
  today.setHours(0, 0, 0, 0); // 시간은 제외하고 날짜만 비교

  // 3. 차이 일수 계산
  const diffTime = targetDate.getTime() - today.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

  // 4. 결과 텍스트 반환
  if (diffDays > 0) return `D-${diffDays}`;
  if (diffDays === 0) return 'D-Day';
  return `D+${Math.abs(diffDays)}`; // 만기가 지났을 경우 D+1 등으로 표시
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
      <button type="button" class="pill" @click="goRegister">자산 등록하기</button>
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
              <small class="bank">{{ deposit.bankName }}</small>
              <strong class="name">{{ deposit.productName }}</strong>
              <small class="meta">
                {{ formatNumber(deposit.principalAmount) }}원 · 연
                {{ deposit.appliedRate }}%
              </small>
            </div>

            <div class="right">
              <span class="dday">{{ dDayText(deposit) }}</span>
              <small class="mat">만기 {{ toDotDate(deposit.maturityDate) }}</small>
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

  width: 100%;
  max-width: 390px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 24px 20px 96px;
  color: var(--kb-text);
  background: #faf9f7;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
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
  margin-bottom: 14px;
  padding: 16px 18px;
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
  margin-top: 5px;
  font-size: 22px;
  font-weight: 800;
}

/* ---------- 목록 ---------- */
.deposit-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.deposit-card {
  display: flex;
  width: 100%;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 15px 16px;
  border: 1.5px solid var(--kb-line);
  border-radius: 14px;
  font: inherit;
  text-align: left;
  background: #fff;
  cursor: pointer;
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
  margin: 4px 0 8px;
  overflow: hidden;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  font-size: 12.5px;
  color: var(--kb-muted);
}

.right {
  flex: none;
  text-align: right;
}

.dday {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 11px;
  font-size: 11px;
  font-weight: 700;
  color: #7a6218;
  background: var(--kb-soft);
}

.mat {
  display: block;
  margin-top: 8px;
  font-size: 11px;
  color: #a8adb2;
}

/* ---------- 추가 버튼 ---------- */
.add-card {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 13px;
  margin-top: 16px;
  margin-bottom: 16px;
  padding: 15px 16px;
  border: 0;
  border-radius: 14px;
  font: inherit;
  color: #fff;
  background: #26282b;
  text-align: left;
  cursor: pointer;
}

.add-card .ico {
  display: grid;
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 9px;
  place-items: center;
  font-size: 14px;
  color: #26282b;
  background: var(--kb-yellow);
}

.add-card .tx {
  flex: 1;
  font-size: 14px;
  font-weight: 700;
}

.chev {
  flex: none;
  font-size: 12px;
  color: #8a8f94;
}
</style>
