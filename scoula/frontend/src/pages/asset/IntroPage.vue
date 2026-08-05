<script setup>
/** 앱 진입 스플래시. 2초 후 로그인 화면으로 넘어간다. */
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const leaving = ref(false);

let fadeTimer = null;
let navTimer = null;

onMounted(() => {
  fadeTimer = setTimeout(() => (leaving.value = true), 1600); // 페이드아웃 시작
  navTimer = setTimeout(() => router.replace({ name: 'login' }), 2000);
});

// 도중에 화면을 벗어나도 타이머가 남지 않게 정리
onUnmounted(() => {
  clearTimeout(fadeTimer);
  clearTimeout(navTimer);
});

/** 기다리기 싫은 사용자는 눌러서 건너뛴다 */
function skip() {
  router.replace({ name: 'login' });
}
</script>

<template>
  <main class="intro" :class="{ leaving }" @click="skip">
    <div class="brand">
      <svg class="scale" viewBox="0 0 200 132" role="img" aria-label="득실">
        <!-- 삼각 받침 (고정) -->
        <path class="fulcrum" d="M100 50 L128 114 H72 Z" />

        <!-- 저울대 (회전) -->
        <g class="beam">
          <path class="bar" d="M28 50 H172" />
          <circle class="pan-l" cx="32" cy="50" r="24" />
          <circle class="pan-r" cx="168" cy="50" r="20" />
          <text class="tx-l" x="32" y="50">득</text>
          <text class="tx-r" x="168" y="50">실</text>
          <circle class="pivot" cx="100" cy="50" r="8" />
        </g>
      </svg>

      <h1>득실</h1>
      <p>기울여 보면 답이 보인다</p>
    </div>
  </main>
</template>

<style scoped>
.intro {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #ffd45c;
  cursor: pointer;
  transition: opacity 0.4s ease;
}

.intro.leaving {
  opacity: 0;
}

.brand {
  width: 100%;
  max-width: 390px;
  text-align: center;
  animation: rise 0.55s ease both;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.scale {
  width: 168px;
  height: 111px;
}

/* ---------- 저울 ---------- */
.fulcrum {
  fill: #1c1c1c;
}

.bar {
  fill: none;
  stroke: #1c1c1c;
  stroke-width: 9;
  stroke-linecap: round;
}

.pivot {
  fill: #fbbf3c;
}

.pan-l {
  fill: #fbbf3c;
  stroke: #1c1c1c;
  stroke-width: 3;
}

.pan-r {
  fill: #fcf8ee;
  stroke: #b9b2a2;
  stroke-width: 3;
}

.tx-l,
.tx-r {
  font-weight: 800;
  text-anchor: middle;
  dominant-baseline: central;
}

.tx-l {
  font-size: 19px;
  fill: #1c1c1c;
}

.tx-r {
  font-size: 16px;
  fill: #4a4a4a;
}

/* 좌우로 흔들리다 '득' 쪽으로 기울어 멈춘다 */
.beam {
  transform-origin: 100px 46px;
  animation: tilt 1.5s cubic-bezier(0.34, 1.2, 0.64, 1) both;
}

@keyframes tilt {
  0% {
    transform: rotate(0deg);
  }
  14% {
    transform: rotate(10deg);
  }
  30% {
    transform: rotate(-8deg);
  }
  44% {
    transform: rotate(6deg);
  }
  58% {
    transform: rotate(-4deg);
  }
  70% {
    transform: rotate(2deg);
  }
 84% {
    transform: rotate(-17deg);
  }
  92% {
    transform: rotate(-13deg);
  }
  100% {
    transform: rotate(-15deg);
  }
}

/* ---------- 워드마크 ---------- */
.brand h1 {
  margin: 14px 0 0;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: #26282b;
}

.brand p {
  margin: 5px 0 0;
  font-size: 12.5px;
  color: #7a6218;
}

/* 모션 최소화 설정을 켠 사용자는 흔들림 없이 */
@media (prefers-reduced-motion: reduce) {
  .beam,
  .brand {
    animation: none;
  }
}

/* 로고 각도 관리 */
.tx-l {
  transform-origin: 32px 50px;
  animation: upright 1.5s cubic-bezier(0.34, 1.2, 0.64, 1) both;
}

.tx-r {
  transform-origin: 168px 50px;
  animation: upright 1.5s cubic-bezier(0.34, 1.2, 0.64, 1) both;
}

@keyframes upright {
  0%   { transform: rotate(0deg); }
  14%  { transform: rotate(-10deg); }
  30%  { transform: rotate(8deg); }
  44%  { transform: rotate(-6deg); }
  58%  { transform: rotate(4deg); }
  70%  { transform: rotate(-2deg); }
  84%  { transform: rotate(17deg); }
  92%  { transform: rotate(13deg); }
  100% { transform: rotate(15deg); }
}
</style>