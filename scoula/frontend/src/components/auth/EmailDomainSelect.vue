<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps({
  modelValue: {
    type: String,
    required: true,
  },
  options: {
    type: Array,
    required: true,
  },
  invalid: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['update:modelValue', 'change']);

const selectRoot = ref(null);
const isOpen = ref(false);

// 네이티브 select 대신 직접 만든 목록을 열고 닫아 항상 아래쪽에 표시한다.
const toggleDropdown = () => {
  isOpen.value = !isOpen.value;
};

const selectDomain = (domain) => {
  emit('update:modelValue', domain);
  emit('change', domain);
  isOpen.value = false;
};

// 선택창 바깥쪽을 누르면 열려 있던 목록을 닫는다.
const closeOnOutsideClick = (event) => {
  if (!selectRoot.value?.contains(event.target)) {
    isOpen.value = false;
  }
};

const closeOnEscape = (event) => {
  if (event.key === 'Escape') {
    isOpen.value = false;
  }
};

onMounted(() => {
  document.addEventListener('pointerdown', closeOnOutsideClick);
  document.addEventListener('keydown', closeOnEscape);
});

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', closeOnOutsideClick);
  document.removeEventListener('keydown', closeOnEscape);
});
</script>

<template>
  <div ref="selectRoot" class="email-domain-select">
    <button
      class="domain-trigger"
      :class="{ 'has-error': invalid, 'is-open': isOpen }"
      type="button"
      aria-haspopup="listbox"
      :aria-expanded="isOpen"
      @click="toggleDropdown"
    >
      <span>{{ modelValue }}</span>
      <span class="domain-arrow" aria-hidden="true"></span>
    </button>

    <ul
      v-if="isOpen"
      class="domain-options"
      role="listbox"
      aria-label="이메일 도메인"
    >
      <li v-for="domain in options" :key="domain" role="presentation">
        <button
          class="domain-option"
          :class="{ 'is-selected': domain === modelValue }"
          type="button"
          role="option"
          :aria-selected="domain === modelValue"
          @click="selectDomain(domain)"
        >
          {{ domain }}
        </button>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.email-domain-select {
  position: relative;
  width: 100%;
  min-width: 0;
}

.domain-trigger {
  width: 100%;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 13px;
  border: 1px solid #e4ded3;
  border-radius: 10px;
  background: #fff;
  color: #222;
  font: inherit;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}

.domain-trigger:focus,
.domain-trigger.is-open {
  border-color: #ffbd00;
  outline: none;
  box-shadow: 0 0 0 3px rgba(255, 189, 0, 0.15);
}

.domain-trigger.has-error {
  border-color: #ef7772;
}

.domain-arrow {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-right: 2px solid #555;
  border-bottom: 2px solid #555;
  transform: translateY(-2px) rotate(45deg);
  transition: transform 0.18s ease;
}

.domain-trigger.is-open .domain-arrow {
  transform: translateY(2px) rotate(225deg);
}

.domain-options {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  left: 0;
  z-index: 100;
  max-height: 210px;
  margin: 0;
  padding: 5px;
  overflow-y: auto;
  list-style: none;
  border: 1px solid #ddd6ca;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(36, 30, 18, 0.16);
}

.domain-option {
  width: 100%;
  min-height: 36px;
  padding: 8px 10px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #3b3732;
  font: inherit;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.domain-option:hover,
.domain-option:focus-visible {
  background: #fff6d9;
  outline: none;
}

.domain-option.is-selected {
  background: #ffefb8;
  color: #2d2923;
  font-weight: 700;
}
</style>
