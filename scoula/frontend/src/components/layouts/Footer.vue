<script>
</script>
<template>
  <nav class="bottom-nav">
    <router-link :to="{ name: 'home' }" class="nav-item" exact-active-class="active">
      <span class="nav-dot"></span>
      <span>홈</span>
    </router-link>
    <router-link :to="{ name: 'comparisonInput' }" class="nav-item" active-class="active">
      <span class="nav-dot"></span>
      <span>계산기</span>
    </router-link>
    <router-link :to="{ name: 'assetRegister' }" class="nav-item" active-class="active">
      <span class="nav-dot"></span>
      <span>자산</span>
    </router-link>
    <router-link :to="{ name: 'profile' }" class="nav-item" active-class="active">
      <span class="nav-dot"></span>
      <span>내정보</span>
    </router-link>
  </nav>
</template>

<style>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 390px;
  display: flex;
  justify-content: space-around;
  background: #fff;
  border-top: 1px solid #efe9dd;
  padding: 10px 0 16px;
}
.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #a49a86;
  text-decoration: none;
}
.nav-item.active { color: #26221c; font-weight: 700; }
.nav-dot { width: 20px; height: 20px; border-radius: 6px; background: #efe9dd; }
.nav-item.active .nav-dot { background: #ffbc00; }
</style>


