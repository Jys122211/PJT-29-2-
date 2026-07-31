<script setup>
import { onBeforeUnmount, onMounted } from 'vue';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '',
  },
  message: {
    type: String,
    default: '',
  },
  confirmText: {
    type: String,
    default: '확인',
  },
});

const emit = defineEmits(['confirm', 'close']);

function handleKeydown(event) {
  if (event.key === 'Escape' && props.visible) {
    emit('close');
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="alert-modal-overlay"
      @click.self="$emit('close')"
    >
      <section
        class="alert-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="alert-modal-title"
        aria-describedby="alert-modal-message"
      >
        <h2 id="alert-modal-title">
          {{ title }}
        </h2>
        <p id="alert-modal-message">
          {{ message }}
        </p>
        <button type="button" @click="$emit('confirm')">
          {{ confirmText }}
        </button>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.alert-modal-overlay {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  padding: 24px;
  background: rgb(41 39 37 / 45%);
  place-items: center;
}

.alert-modal {
  width: 100%;
  max-width: 320px;
  padding: 24px 20px 18px;
  border-radius: 16px;
  color: #292725;
  background: #fff;
  box-shadow: 0 16px 40px rgb(0 0 0 / 18%);
  text-align: center;
}

.alert-modal h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}

.alert-modal p {
  margin: 10px 0 20px;
  font-size: 13px;
  line-height: 1.6;
  color: #716a62;
}

.alert-modal button {
  width: 100%;
  height: 44px;
  border: 0;
  border-radius: 11px;
  font-weight: 700;
  color: #292725;
  background: var(--kb-yellow, #ffbc00);
}
</style>
