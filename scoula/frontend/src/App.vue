<script setup>
import { computed } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import DefaultLayout from './components/layouts/DefaultLayout.vue';

const route = useRoute();

const isMobileLayout = computed(() => route.meta.layout === 'mobile');
</script>

<template>
  <RouterView v-slot="{ Component }">
    <component :is="Component" v-if="isMobileLayout" />

    <DefaultLayout v-else>
      <component :is="Component" />
    </DefaultLayout>
  </RouterView>
</template>
