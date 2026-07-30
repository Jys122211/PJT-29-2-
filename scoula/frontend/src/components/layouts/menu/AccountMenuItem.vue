<script setup>
const props = defineProps({ email: String, name: String });

const avatar = `/api/member/${props.email}/avatar`;
</script>

<template>
  <li class="nav-item">
    <router-link class="nav-link" to="/auth/profile">
      <img :src="avatar" class="avatar avatar-sm" />
      {{ name }}
    </router-link>
  </li>
</template>
<style></style>
