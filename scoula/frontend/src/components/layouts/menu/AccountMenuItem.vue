<script setup>
const props = defineProps({ username: String, email: String });

const avatar = `/api/users/${props.email}/avatar`;
</script>

<template>
  <li class="nav-item">
    <router-link class="nav-link" to="/profile">
      <img :src="avatar" class="avatar avatar-sm" />
      {{ username }}
    </router-link>
  </li>
</template>
<style></style>
