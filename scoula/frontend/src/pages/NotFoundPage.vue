<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = () => {
  router.replace({ name: 'home' });
};
</script>

<template>
  <main class="not-found-page">
    <section class="not-found-card" aria-labelledby="not-found-title">
      <strong class="error-code">404</strong>
      <h1 id="not-found-title">페이지를 찾을 수 없습니다</h1>
      <p>주소가 잘못됐거나 페이지가 이동됐습니다.</p>

      <button type="button" @click="goHome">홈으로 이동</button>
    </section>
  </main>
</template>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #faf9f7;
  color: #26221c;
}

.not-found-card {
  width: min(100%, 390px);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 24px;
  border: 1px solid #efe9dd;
  border-radius: 20px;
  background: #fff;
  text-align: center;
  box-shadow: 0 8px 24px rgb(38 34 28 / 8%);
}

.error-code {
  color: #ffbc00;
  font-size: 64px;
  line-height: 1;
}

h1 {
  margin: 0;
  font-size: 22px;
}

p {
  margin: 0 0 12px;
  color: #8f8678;
  font-size: 14px;
}

button {
  width: 100%;
  padding: 14px;
  border: 0;
  border-radius: 12px;
  background: #ffbc00;
  color: #26221c;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

button:hover {
  background: #f2b200;
}

button:focus-visible {
  outline: 3px solid rgb(255 188 0 / 35%);
  outline-offset: 3px;
}
</style>
