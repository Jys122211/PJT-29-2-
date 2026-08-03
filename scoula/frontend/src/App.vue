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
  <RouterView v-slot="{ Component }">
    <component :is="Component" v-if="isStandaloneLayout" />

    <DefaultLayout v-else>
      <component :is="Component" />
    </DefaultLayout>
  </RouterView>
</template>
