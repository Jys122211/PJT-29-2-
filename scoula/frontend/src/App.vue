<script setup>
import { RouterView } from 'vue-router';
</script>

<template>
  <!-- 모든 화면이 blank/mobile 레이아웃이라 공통 헤더 없이 화면을 그대로 그린다. -->
  <RouterView v-slot="{ Component, route }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
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
