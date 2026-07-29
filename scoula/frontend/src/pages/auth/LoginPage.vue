<script setup>
import { computed, reactive, ref } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';

const router = useRouter();
const auth = useAuthStore();

const member = reactive({
  email: '',
  password: '',
});

const error = ref('');

const disableSubmit = computed(() => !(member.email && member.password));

const login = async () => {
  console.log(member);
  try {
    await auth.login(member);
    router.push('/auth/profile');
  } catch (e) {
    // 로그인 에러
    console.log('에러=======', e);
    error.value = e.response?.data || '로그인에 실패했습니다.';
  }
};
</script>

<template>
  <div class="mt-5 mx-auto" style="width: 500px">
    <h1 class="my-5">
      <i class="fa-solid fa-right-to-bracket"></i>
      로그인
    </h1>

    <form @submit.prevent="login">
      <div class="mb-3 mt-3">
        <label for="email" class="form-label">
          <i class="fa-solid fa-envelope"></i>
          이메일:
        </label>
        <input
          type="email"
          class="form-control"
          placeholder="이메일 입력 (예: user@kb.co.kr)"
          v-model="member.email"
        />
      </div>

      <div class="mb-3">
        <label for="password" class="form-label">
          <i class="fa-solid fa-lock"></i>
          비밀번호:
        </label>
        <input
          type="password"
          class="form-control"
          placeholder="비밀번호"
          v-model="member.password"
        />
      </div>

      <div v-if="error" class="text-danger mb-3">{{ error }}</div>

      <button
        type="submit"
        class="btn btn-primary mt-2 w-100"
        :disabled="disableSubmit"
      >
        <i class="fa-solid fa-right-to-bracket"></i>
        로그인
      </button>
    </form>
  </div>
</template>
