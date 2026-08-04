<script setup>
import { computed } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import DefaultLayout from './components/layouts/DefaultLayout.vue';

const route = useRoute();

// 로그인·회원가입(blank)과 계산기 모바일 화면(mobile)은 공통 헤더 없이 표시한다.
const isStandaloneLayout = computed(() =>
  ['blank', 'mobile'].includes(route.meta.layout),
);
</script>

<template>
  <RouterView v-slot="{ Component, route }">
    <template v-if="isStandaloneLayout">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
    </template>

    <DefaultLayout v-else>
      <transition name="page-fade" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
    </DefaultLayout>
  </RouterView>
</template>

<style>
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
