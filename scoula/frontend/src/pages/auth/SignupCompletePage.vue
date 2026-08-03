<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

// "자산 등록하러 가기"를 누르면 보유예금 개수 입력 화면으로 이동한다.
const goToDeposits = () => {
  router.push('/deposits/count');
};

// 자산 등록을 건너뛰면 로그인 상태로 홈 대시보드에 이동한다.
const skipToDashboard = () => {
  router.push('/dashboard');
};
</script>

<template>
  <main class="complete-page">
    <section class="complete-card">
      <div class="check-icon">✓</div>

      <h1>회원가입이 완료되었습니다!</h1>
      <p>
        지금 예적금을 등록하면 급전이 필요할 때 예적금 해지 vs 대출 득실을
        정확하게 비교해 드려요.
      </p>

      <button class="primary-button" type="button" @click="goToDeposits">
        자산 등록하러 가기
      </button>

      <button class="text-button" type="button" @click="skipToDashboard">
        건너뛰기
      </button>
    </section>
  </main>
</template>

<style scoped>
.complete-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: #f7f6f2;
}

.complete-card {
  width: 100%;
  max-width: 390px;
  min-height: 620px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 42px 28px;
  text-align: center;
  background: #fffdfa;
  border: 1px solid #eee9df;
  border-radius: 28px;
  box-shadow: 0 18px 40px rgba(36, 30, 18, 0.12);
}

.check-icon {
  width: 58px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30px;
  border-radius: 50%;
  background: #ffbd00;
  color: #1f1f1f;
  font-size: 30px;
  font-weight: 900;
}

h1 {
  margin: 0;
  color: #222;
  font-size: 21px;
  font-weight: 800;
}

p {
  max-width: 270px;
  margin: 18px 0 38px;
  color: #8e877d;
  font-size: 13px;
  line-height: 1.7;
}

.primary-button {
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: 10px;
  background: #ffbd00;
  color: #1f1f1f;
  font-size: 15px;
  font-weight: 800;
}

.text-button {
  margin-top: 22px;
  border: 0;
  background: transparent;
  color: #7f776d;
  font-size: 13px;
  font-weight: 700;
}
</style>
