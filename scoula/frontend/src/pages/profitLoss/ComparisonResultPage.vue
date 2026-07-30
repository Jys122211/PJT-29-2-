<script setup>
defineProps({
  comparisonId: {
    type: String,
    required: true,
  },
});
</script>

<template>
  <main class="result-page">
    <section class="result-card">
      <h1>손익비교 결과</h1>
      <p>손익비교 결과를 불러오는 중입니다.</p>
      <small>요청 번호: {{ comparisonId }}</small>
    </section>
  </main>
</template>

<style scoped>
.result-page {
  display: grid;
  width: 100%;
  max-width: 390px;
  min-height: 100vh;
  min-height: 100dvh;
  margin: 0 auto;
  padding: 24px;
  color: #292725;
  background: #faf9f7;
  place-items: center;
}

.result-card {
  width: 100%;
  padding: 28px 20px;
  border: 1px solid #e8e0d4;
  border-radius: 16px;
  background: #fff;
  text-align: center;
}

.result-card h1 {
  margin: 0;
  font-size: 20px;
}

.result-card p {
  margin: 12px 0 8px;
  color: #716a62;
}

.result-card small {
  color: #aaa39a;
}
</style>
