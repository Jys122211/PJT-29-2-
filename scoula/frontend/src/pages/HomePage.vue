<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goToProfile = () => {
  router.push('/auth/profile');
};
</script>

<template>
  <div class="text-center my-5">
    <h1 class="mb-4">메인 페이지</h1>
    <button class="btn btn-warning btn-lg fw-bold shadow-sm px-4 py-3" @click="goToProfile">
      <i class="fa-solid fa-user me-2"></i> 내 정보 페이지로 이동하기
    </button>
  </div>
</template>
