<script setup>
import { useRouter } from 'vue-router';

const props = defineProps({
  /** 'home' | 'calculator' | 'asset' | 'profile' */
  active: {
    type: String,
    default: 'asset',
  },
});

const router = useRouter();

const tabs = [
  { key: 'home', label: '홈', icon: 'fa-solid fa-house', to: { name: 'home' } },
  {
    key: 'calculator',
    label: '계산기',
    icon: 'fa-solid fa-calculator',
    to: { name: 'comparisonInput' },
  },
  {
    key: 'asset',
    label: '자산',
    icon: 'fa-solid fa-wallet',
    to: { name: 'assetList' },
  },
  { key: 'profile', label: '내정보', icon: 'fa-solid fa-user', to: null },
];

function move(tab) {
  if (!tab.to) return; // 내정보는 담당자 미정
  router.push(tab.to);
}
</script>

<template>
  <nav class="bottom-nav">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      :class="{ active: props.active === tab.key }"
      @click="move(tab)"
    >
      <span class="dot"><i :class="tab.icon"></i></span>
      {{ tab.label }}
    </button>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;                    /* right: 0; left: 0; margin: auto; 를 대체 */
  transform: translateX(-50%);  /* 추가 */
  display: grid;
  width: 100%;
  max-width: 390px;
  height: 70px;
  border-top: 1px solid #ece8e1;
  grid-template-columns: repeat(4, 1fr);
  background: #fff;
}

.bottom-nav button {
  display: flex;
  border: 0;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
  font: inherit;
  font-size: 10px;
  color: #aaa39a;
  background: transparent;
  cursor: pointer;
}

.dot {
  display: grid;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  place-items: center;
  font-size: 12px;
  color: #b6ae9f;
  background: #ede9e1;
}

.bottom-nav button.active {
  font-weight: 700;
  color: #292725;
}

.bottom-nav button.active .dot {
  color: #292725;
  background: #ffbc00;
}
</style>
