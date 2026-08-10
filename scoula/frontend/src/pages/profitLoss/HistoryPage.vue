<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import profitLossApi from '@/api/profitLossApi';
import BottomNav from '@/components/mobile/BottomNav.vue';
import { formatNumber, toDisplayKbAccount } from '@/util/depositFormat';

const router = useRouter();

const comparisons = ref([]);
const loading = ref(true);
const loadError = ref('');

async function load() {
  loading.value = true;
  loadError.value = '';
  try {
    comparisons.value = await profitLossApi.getComparisons();
  } catch (error) {
    comparisons.value = [];
    loadError.value = '목록을 불러오지 못했어요';
  } finally {
    loading.value = false;
  }
}

function toDotDate(isoString) {
  return isoString ? isoString.slice(0, 10).replaceAll('-', '.') : '';
}

function goDetail(item) {
  router.push({ name: 'comparisons/result', params: { comparisonId: item.comparisonId } });
}

onMounted(load);
</script>

<template>
  <main class="page">
    <header class="page-head">
      <button type="button" class="back-btn" @click="router.back()" aria-label="뒤로 가기">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <div class="head-titles">
        <h1>내 비교 내역</h1>
      </div>
      <span class="badge">총 {{ comparisons.length }}건</span>
    </header>

    <section v-if="loading" class="state-card">
      <strong>불러오는 중이에요</strong>
    </section>

    <section v-else-if="loadError" class="state-card">
      <strong>{{ loadError }}</strong>
      <button type="button" class="pill" @click="load">다시 시도</button>
    </section>

    <section v-else-if="comparisons.length === 0" class="empty-card">
      <strong>아직 비교 내역이 없어요</strong>
      <p>득실 계산기를 사용하면<br />여기에 기록이 쌓여요</p>
    </section>

    <ul v-else class="history-list">
      <li v-for="item in comparisons" :key="item.comparisonId">
        <button type="button" class="history-card" @click="goDetail(item)">
          <div class="date-row">
            <span class="date">{{ toDotDate(item.createdAt) }}</span>
            <span class="meta">
      <i class="fa-solid fa-right-left"></i>
      {{ item.depositName }} · {{ item.loanTypeLabel }} 비교
    </span>
          </div>
          <div class="row-amount">
            <strong class="amount">{{ formatNumber(item.urgentAmount) }}원 필요</strong>
            <i class="fa-solid fa-chevron-right chev"></i>
          </div>
          <div class="row-result">
    <span class="rec-badge">
      <i class="fa-solid fa-check"></i>
      {{ item.recommended }} 추천
    </span>
            <span class="saving">{{ formatNumber(item.savingAmount) }}원 이득</span>
          </div>
          <div class="account-row" v-if="item.accountNumber">
            계좌번호 : {{ toDisplayKbAccount(item.accountNumber) }}
          </div>
        </button>
      </li>
    </ul>
    <BottomNav active="calculator" />
  </main>
</template>

<style scoped>
.date-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.rec-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: var(--kb-text);
  background: var(--kb-yellow);
}

.meta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--kb-muted);
}

.page {
  --kb-yellow: #ffbc00;
  --kb-border: #e9e0d2;
  --kb-text: #292725;
  --kb-muted: #9a938a;
  --kb-soft: #fff6df;
  --kb-line: #f2d89a;

  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 390px;
  height: 100vh;
  margin: 0 auto;
  padding: 24px 20px 96px;
  color: var(--kb-text);
  background: #faf9f7;
  box-sizing: border-box;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex: none;
  border: 1.5px solid var(--kb-border);
  border-radius: 12px;
  background: #fff;
}

.head-titles {
  flex: 1;
}

.head-titles h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.badge {
  font-size: 11px;
  color: var(--kb-muted);
}

.state-card,
.empty-card {
  padding: 36px 20px 28px;
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  background: #fff;
  text-align: center;
}

.empty-card p {
  margin: 8px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--kb-muted);
}

.history-list {
  display: grid;
  gap: 12px;
  flex: 1;
  min-height: 0;
  padding: 12px;
  margin: 0;
  overflow-y: auto;
  list-style: none;
  border: 1px solid #efe9dd;
  border-radius: 16px;
  background: #f0e9da;
  box-sizing: border-box;
}
.history-list::-webkit-scrollbar {
  width: 5px;
}
.history-list::-webkit-scrollbar-track {
  background: transparent;
}
.history-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #c8bfae;
}

.history-card {
  display: block;
  width: 100%;
  padding: 18px 18px 16px;
  border: 1.5px solid var(--kb-line);
  border-radius: 16px;
  font: inherit;
  text-align: left;
  background: #fff;
  cursor: pointer;
}

.date {
  font-size: 12px;
  color: var(--kb-muted);
}

.row-amount {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}

.amount {
  font-size: 18px;
  font-weight: 700;
}

.chev {
  font-size: 14px;
  color: #8a8f94;
}

.row-result {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}



.saving {
  font-size: 13px;
  color: var(--kb-muted);
}

.account-row {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--kb-border);
  font-size: 12px;
  color: var(--kb-muted);
  text-align: left;
}
</style>