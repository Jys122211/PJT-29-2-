<script setup>
import { computed } from 'vue';
import MenuItem from './MenuItem.vue';
import AccountMenuItem from './AccountMenuItem.vue';
import LogoutMenuItem from './LogoutMenuItem.vue';
import config from '@/config';
import { useAuthStore } from '@/stores/auth.js';

const { login, join } = config.accountMenus;

const auth = useAuthStore();

const islogin = computed(() => auth.isLogin);
const email = computed(() => auth.email);
const name = computed(() => auth.name);
</script>
<template>
  <ul class="navbar-nav ms-auto">
    <template v-if="islogin">
      <AccountMenuItem :email="email" :name="name" />
      <LogoutMenuItem />
    </template>
    <template v-else>
      <MenuItem :menu="login" />
      <MenuItem :menu="join" />
    </template>
  </ul>
</template>
