<script setup>
import { onMounted, onBeforeUnmount } from 'vue';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    required: true,
  },
  message: {
    type: String,
    default: '',
  },
  cancelText: {
    type: String,
    default: '취소',
  },
  confirmText: {
    type: String,
    default: '진행',
  },
  iconClass: {
    type: String,
    default: 'fa-solid fa-triangle-exclamation',
  },
  hideCancel: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['cancel', 'confirm']);

function handleKeydown(event) {
  if (event.key === 'Escape' && props.visible) {
    emit('cancel');
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
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="$emit('cancel')">
        <section class="modal-card">
          <h2>
            <i v-if="iconClass" :class="iconClass" aria-hidden="true"></i>
            {{ title }}
          </h2>
          <p v-if="message">{{ message }}</p>
          <slot v-else></slot>
          <div class="modal-actions">
            <button v-if="!hideCancel" type="button" class="modal-cancel" @click="$emit('cancel')">{{ cancelText }}</button>
            <button type="button" class="modal-confirm" @click="$emit('confirm')">{{ confirmText }}</button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  padding: 24px;
  background: rgb(0 0 0 / 50%);
  place-items: center;
}

.modal-card {
  width: 100%;
  max-width: 320px;
  padding: 24px 20px 16px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 16px 40px rgb(0 0 0 / 18%);
}

.modal-card h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--gs-warn-strong, #e54848);
}

.modal-card p {
  margin: 12px 0 20px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--gs-text, #292725);
}

.modal-actions {
  display: flex;
  gap: 8px;
}

.modal-cancel,
.modal-confirm {
  height: 44px;
  border-radius: 11px;
  flex: 1;
  font-weight: 700;
  font-size: 14px;
}

.modal-cancel {
  border: 1px solid var(--gs-line, #e9e0d2);
  color: var(--gs-text, #292725);
  background: #fff;
}

.modal-confirm {
  border: 0;
  color: #fff;
  background: var(--gs-warn-strong, #e54848);
}

/* Transition animations */
.modal-enter-active {
  transition: opacity 0.25s ease;
}
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal-card {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease;
}
.modal-leave-active .modal-card {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-card {
  opacity: 0;
  transform: scale(0.9) translateY(10px);
}

.modal-leave-to .modal-card {
  opacity: 0;
  transform: scale(0.95) translateY(5px);
}
</style>
