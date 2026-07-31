<script setup>
import { RouterView, useRoute } from 'vue-router';
import DefaultLayout from './components/layouts/DefaultLayout.vue';

const route = useRoute();
</script>

<template>
  <DefaultLayout v-if="route.meta.layout !== 'blank'">
    <RouterView />
  </DefaultLayout>
  <RouterView v-else />
</template>

<style scoped></style>
