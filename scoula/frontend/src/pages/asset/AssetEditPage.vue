<script setup>
/**
 * 보유 예금 편집 (화면 07-10 UPDATE / 07-11 DELETE 확인 팝업)
 *
 * 노란 배경  = 서버에서 불러온 기존값
 * 주황 테두리 = 사용자가 수정한 값
 */
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import depositApi from '@/api/depositApi';
import BottomNav from '@/components/mobile/BottomNav.vue';
import {
  extractApiError,
  formatNumber,
  isValidCompactDate,
  parseNumber,
  sanitizeRate,
  toCompactAccount,
  toCompactDate,
  toDisplayKbAccount,
  toDisplayDate,
} from '@/util/depositFormat';

const route = useRoute();
const router = useRouter();
const userDepositId = route.params.userDepositId;

const loading = ref(true);
const loadError = ref('');
const submitting = ref(false);
const submitError = ref('');
const toast = ref('');
const showDeleteModal = ref(false);
const deleting = ref(false);

const form = reactive({
  bankName: '',
  productName: '',
  accountNumber: '',
  joinDate: '',
  maturityDate: '',
  principalAmount: '',
  baseRate: '',
  appliedRate: '',
});

/** 서버에서 받은 원본. 수정 여부 판단용 */
const original = reactive({});

const errors = reactive({
  bankName: '',
  productName: '',
  accountNumber: '',
  joinDate: '',
  maturityDate: '',
  principalAmount: '',
  baseRate: '',
  appliedRate: '',
});

// ------------------------------------------------------------ 로드
async function load() {
  loading.value = true;
  loadError.value = '';

  try {
    const data = await depositApi.get(userDepositId);

    form.bankName = data.bankName ?? '';
    form.productName = data.productName ?? '';
    form.accountNumber = toCompactAccount(data.accountNumber);
    form.joinDate = data.joinDate ?? '';
    form.maturityDate = data.maturityDate ?? '';
    form.principalAmount = data.principalAmount ?? '';
    form.baseRate = data.baseRate == null ? '' : String(data.baseRate);
    form.appliedRate = data.appliedRate == null ? '' : String(data.appliedRate);

    Object.assign(original, { ...form });
    validateAccountNumber();
  } catch (error) {
    loadError.value = extractApiError(error).message;
  } finally {
    loading.value = false;
  }
}

// ------------------------------------------------------------ 입력
function onTextInput(field, event) {
  form[field] = event.target.value;
  errors[field] = '';
}

function onDateInput(field, event) {
  const digits = toCompactDate(event.target.value).slice(0, 8);
  form[field] = digits;
  event.target.value = toDisplayDate(digits);
  errors[field] = '';
  errors.maturityDate = '';
}

function onAmountInput(event) {
  const parsed = parseNumber(event.target.value);
  form.principalAmount = parsed === null ? '' : parsed;
  event.target.value = formatNumber(parsed);
  errors.principalAmount = '';
}

function onAccountInput(event) {
  const digits = toCompactAccount(event.target.value).slice(0, 14);
  form.accountNumber = digits;
  event.target.value = toDisplayKbAccount(digits);
  errors.accountNumber = '';
}

function validateAccountNumber() {
  if (form.accountNumber !== '' && form.accountNumber.length !== 14) {
    errors.accountNumber = 'KB 계좌번호 숫자 14자리를 입력해주세요';
  }
}

function onAccountBlur() {
  validateAccountNumber();
}

function formatKoreanAmount(amount, emptyMessage = '가입금액을 입력해주세요') {
  if (amount === null || amount === '' || Number(amount) === 0) {
    return emptyMessage;
  }

  const value = Number(amount);
  const jo = Math.floor(value / 1_000_000_000_000);
  const eok = Math.floor((value % 1_000_000_000_000) / 100_000_000);
  const man = Math.floor((value % 100_000_000) / 10_000);
  const won = value % 10_000;
  const result = [];

  if (jo > 0) result.push(`${jo.toLocaleString('ko-KR')}조`);
  if (eok > 0) result.push(`${eok.toLocaleString('ko-KR')}억`);
  if (man > 0) result.push(`${man.toLocaleString('ko-KR')}만원`);
  if (won > 0) result.push(`${won.toLocaleString('ko-KR')}원`);

  return result.join(' ');
}

const principalAmountText = computed(() =>
  formatKoreanAmount(form.principalAmount),
);

function onRateInput(field, event) {
  const cleaned = sanitizeRate(event.target.value);
  form[field] = cleaned;
  event.target.value = cleaned;
  errors[field] = '';
}

function isEdited(field) {
  return String(form[field]) !== String(original[field] ?? '');
}

function fieldClass(field) {
  return {
    err: !!errors[field],
    edited: isEdited(field),
    locked: !isEdited(field) && !errors[field],
  };
}

// ------------------------------------------------------------ 검증
const isComplete = computed(
  () =>
    form.bankName.trim() !== '' &&
    form.productName.trim() !== '' &&
    form.accountNumber.length === 14 &&
    form.joinDate.length === 8 &&
    form.maturityDate.length === 8 &&
    form.principalAmount !== '' &&
    Number(form.principalAmount) > 0 &&
    form.baseRate !== '' &&
    form.appliedRate !== '',
);

const isDateReversed = computed(
  () =>
    form.joinDate.length === 8 &&
    form.maturityDate.length === 8 &&
    form.maturityDate <= form.joinDate,
);

const hasChanges = computed(() =>
  Object.keys(form).some((field) => isEdited(field)),
);

const canSubmit = computed(
  () =>
    isComplete.value &&
    !isDateReversed.value &&
    hasChanges.value &&
    !submitting.value,
);

const submitLabel = computed(() => {
  if (submitting.value) return '수정 중...';
  if (!isComplete.value) return '필수 항목 입력';
  if (isDateReversed.value) return '날짜 확인 필요';
  if (!hasChanges.value) return '변경 없음';
  return '수정하기';
});

function validate() {
  Object.keys(errors).forEach((key) => (errors[key] = ''));
  let ok = true;

  const require = (field, message) => {
    if (String(form[field]).trim() === '') {
      errors[field] = message;
      ok = false;
    }
  };

  require('bankName', '은행명을 입력해주세요');
  require('productName', '상품명을 입력해주세요');
  require('accountNumber', '계좌번호를 입력해주세요');
  require('joinDate', '가입일을 입력해주세요');
  require('maturityDate', '만기일을 입력해주세요');
  require('baseRate', '기본금리를 입력해주세요');
  require('appliedRate', '적용금리를 입력해주세요');

  if (form.accountNumber !== '' && form.accountNumber.length !== 14) {
    errors.accountNumber = 'KB 계좌번호 숫자 14자리를 입력해주세요';
    ok = false;
  }

  if (form.principalAmount === '' || Number(form.principalAmount) <= 0) {
    errors.principalAmount = '가입금액을 입력해주세요';
    ok = false;
  }

  ['joinDate', 'maturityDate'].forEach((field) => {
    if (form[field].length === 8 && !isValidCompactDate(form[field])) {
      errors[field] = '존재하지 않는 날짜예요';
      ok = false;
    }
  });

  if (!errors.joinDate && !errors.maturityDate && isDateReversed.value) {
    errors.maturityDate = '만기일이 가입일보다 빠릅니다';
    form.maturityDate = '';
    ok = false;
  }

  if (
    !errors.baseRate &&
    !errors.appliedRate &&
    Number(form.appliedRate) < Number(form.baseRate)
  ) {
    errors.appliedRate = '적용금리는 기본금리보다 크거나 같아야 합니다';
    ok = false;
  }

  return ok;
}

// ------------------------------------------------------------ 수정 (07-10)
async function submit() {
  if (!validate()) return;

  submitting.value = true;
  submitError.value = '';

  try {
    await depositApi.update(userDepositId, {
      bankName: form.bankName.trim(),
      productName: form.productName.trim(),
      accountNumber: form.accountNumber,
      joinDate: form.joinDate,
      maturityDate: form.maturityDate,
      principalAmount: Number(form.principalAmount),
      baseRate: Number(form.baseRate),
      appliedRate: Number(form.appliedRate),
    });

    toast.value = '수정 완료';
    setTimeout(() => router.push({ name: 'assetList' }));
  } catch (error) {
    const { field, message } = extractApiError(error);
    if (field && field in errors) {
      errors[field] = message;
    } else {
      submitError.value = message;
    }
  } finally {
    submitting.value = false;
  }
}

// ------------------------------------------------------------ 삭제 (07-11)
async function confirmDelete() {
  deleting.value = true;
  submitError.value = '';

  try {
    await depositApi.remove(userDepositId);
    showDeleteModal.value = false;
    toast.value = '삭제 완료';
    setTimeout(() => router.push({ name: 'assetList' }));
  } catch (error) {
    showDeleteModal.value = false;
    submitError.value = extractApiError(error).message;
  } finally {
    deleting.value = false;
  }
}

onMounted(load);
</script>

<template>
  <main class="page">
    <header class="page-head">
      <button type="button" class="back-btn" @click="router.back()" aria-label="뒤로 가기">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <div class="head-titles">
        <h1>보유 예금 편집</h1>
        <p>예금 정보를 수정하거나 삭제할 수 있어요</p>
      </div>
    </header>

    <section v-if="loading" class="state-card">
      <strong>불러오는 중이에요</strong>
    </section>

    <section v-else-if="loadError" class="state-card">
      <strong>{{ loadError }}</strong>
      <p>목록으로 돌아가 다시 시도해주세요</p>
      <button type="button" class="btn outline" @click="router.push({ name: 'assetList' })">
        목록으로
      </button>
    </section>

    <template v-else>
      <h2 class="sec-label">직접 입력</h2>

      <form class="form-card" @submit.prevent="submit">
        <div class="form-row">
          <input class="fld" value="정기예금" disabled aria-label="상품 유형" />
          <input
            class="fld"
            :class="fieldClass('bankName')"
            :value="form.bankName"
            placeholder="은행명"
            aria-label="은행명"
            @input="onTextInput('bankName', $event)"
          />
        </div>
        <p v-if="errors.bankName" class="err-msg">! {{ errors.bankName }}</p>

        <div class="form-row">
          <input
            class="fld"
            :class="fieldClass('productName')"
            :value="form.productName"
            placeholder="상품명"
            aria-label="상품명"
            @input="onTextInput('productName', $event)"
          />
        </div>
        <p v-if="errors.productName" class="err-msg">! {{ errors.productName }}</p>

        <div class="form-row">
          <input
            class="fld"
            :class="fieldClass('accountNumber')"
            :value="toDisplayKbAccount(form.accountNumber)"
            placeholder="계좌번호"
            inputmode="numeric"
            maxlength="16"
            aria-label="계좌번호"
            @input="onAccountInput"
            @blur="onAccountBlur"
          />
        </div>
        <p v-if="errors.accountNumber" class="err-msg">! {{ errors.accountNumber }}</p>


        <div class="form-row two-column-row">
          <input
            class="fld"
            :class="fieldClass('joinDate')"
            :value="toDisplayDate(form.joinDate)"
            placeholder="가입일 (YYYY-MM-DD)"
            inputmode="numeric"
            aria-label="가입일"
            @input="onDateInput('joinDate', $event)"
          />
          <input
            class="fld"
            :class="fieldClass('maturityDate')"
            :value="toDisplayDate(form.maturityDate)"
            placeholder="만기일 (YYYY-MM-DD)"
            inputmode="numeric"
            aria-label="만기일"
            @input="onDateInput('maturityDate', $event)"
          />
        </div>
        <p v-if="errors.joinDate" class="err-msg">! {{ errors.joinDate }}</p>
        <p v-if="errors.maturityDate" class="err-msg">! {{ errors.maturityDate }}</p>

        <div class="form-row two-column-row amount-rate-row">
          <div class="field-with-guide">
            <input
              class="fld"
              :class="fieldClass('principalAmount')"
              :value="formatNumber(form.principalAmount)"
              placeholder="가입 금액 (원)"
              inputmode="numeric"
              aria-label="가입 금액"
              @input="onAmountInput"
            />
            <small class="amount-summary">{{ principalAmountText }}</small>
          </div>
          <input
            class="fld"
            :class="fieldClass('baseRate')"
            :value="form.baseRate"
            placeholder="기본 금리 (%)"
            inputmode="decimal"
            aria-label="기본 금리"
            @input="onRateInput('baseRate', $event)"
          />
        </div>
        <p v-if="errors.principalAmount" class="err-msg">
          ! {{ errors.principalAmount }}
        </p>
        <p v-if="errors.baseRate" class="err-msg">! {{ errors.baseRate }}</p>

        <div class="form-row">
          <input
            class="fld"
            :class="fieldClass('appliedRate')"
            :value="form.appliedRate"
            placeholder="적용금리(%)  *우대금리를 포함한 금리를 작성해주세요"
            inputmode="decimal"
            aria-label="적용금리"
            @input="onRateInput('appliedRate', $event)"
          />
        </div>
        <p v-if="errors.appliedRate" class="err-msg">! {{ errors.appliedRate }}</p>

        <p class="legend">*노란 배경 = 기존값 / 주황 테두리 = 수정한 값</p>

        <p v-if="submitError" class="err-msg center">{{ submitError }}</p>

        <div class="btn-row">
          <button
            type="submit"
            class="btn primary grow2"
            :class="{ disabled: !canSubmit }"
            :disabled="!canSubmit"
          >
            {{ submitLabel }}
          </button>
          <button
            type="button"
            class="btn danger-outline grow1"
            @click="showDeleteModal = true"
          >
            삭제
          </button>
        </div>
      </form>
    </template>

    <p v-if="toast" class="toast">{{ toast }}</p>

    <!-- 07-11 삭제 확인 팝업 -->
    <div v-if="showDeleteModal" class="modal-host">
      <div class="dimmer" @click="showDeleteModal = false"></div>

      <div class="delete-modal" role="dialog" aria-modal="true" aria-labelledby="delTitle">
        <div class="trash"><i class="fa-solid fa-trash-can"></i></div>
        <strong id="delTitle">이 예금을 삭제할까요?</strong>
        <p>예금을 삭제하면 되돌릴 수 없어요.</p>

        <div class="btn-row">
          <button
            type="button"
            class="btn outline grow1"
            :disabled="deleting"
            @click="showDeleteModal = false"
          >
            취소
          </button>
          <button
            type="button"
            class="btn danger grow1"
            :disabled="deleting"
            @click="confirmDelete"
          >
            {{ deleting ? '삭제 중...' : '삭제' }}
          </button>
        </div>
      </div>
    </div>

    <BottomNav active="asset" />
  </main>
</template>

<style scoped>
.page {
  --kb-yellow: #ffbc00;
  --kb-border: #e9e0d2;
  --kb-text: #292725;
  --kb-muted: #9a938a;
  --kb-red: #c0392b;
  --kb-soft: #fff6df;
  --kb-line: #f2d89a;

  position: relative;
  width: 100%;
  max-width: 390px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 24px 20px 96px;
  color: var(--kb-text);
  background: #faf9f7;
}

/* 👇 기존 .page-head 관련 코드를 지우고 아래 내용으로 교체하세요 */

.page-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 1.5px solid var(--kb-border);
  border-radius: 12px;
  background: #fff;
  color: var(--kb-text);
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.back-btn:active {
  background-color: #f1f3f5; /* 터치했을 때 살짝 눌리는 효과 */
}

.head-titles h1 {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: var(--kb-text);
}

.head-titles p {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: var(--kb-muted);
}

.state-card {
  padding: 36px 20px 28px;
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  background: #fff;
  text-align: center;
}

.state-card strong {
  display: block;
  font-size: 15px;
}

.state-card p {
  margin: 8px 0 16px;
  font-size: 12.5px;
  color: var(--kb-muted);
}

.sec-label {
  margin: 20px 2px 10px;
  font-size: 14px;
  font-weight: 700;
}

.form-card {
  padding: 16px 15px;
  border: 1px solid var(--kb-border);
  border-radius: 14px;
  background: #fff;
}

.form-row {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.form-row > * {
  flex: 1;
  min-width: 0;
}

.two-column-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.two-column-row > * {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.field-with-guide {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.amount-rate-row {
  align-items: flex-start;
}

.amount-rate-row > .field-with-guide,
.amount-rate-row > .fld {
  width: 100%;
  min-width: 0;
}

.field-with-guide > .fld {
  width: 100%;
  min-width: 0;
  max-width: none;
  margin: 0;
  align-self: stretch;
  box-sizing: border-box;
}

.amount-summary {
  min-height: 14px;
  padding-left: 2px;
  font-size: 10px;
  line-height: 1.4;
  color: var(--kb-muted);
}

.fld {
  width: 100%;
  padding: 13px;
  border: 1px solid var(--kb-border);
  border-radius: 10px;
  font: inherit;
  font-size: 13px;
  color: var(--kb-text);
  background: #fff;
  box-sizing: border-box;
}

.fld::placeholder {
  color: #b8b1a8;
}

.fld:focus {
  border-color: #f4a70b;
  outline: 0;
}

.fld:disabled {
  color: var(--kb-text);
  background: #f6f3ee;
}

/* 기존값 */
.fld.locked {
  border-color: var(--kb-line);
  color: var(--kb-text);
  background: var(--kb-soft);
}

/* 수정한 값 */
.fld.edited {
  border: 1.6px solid #f4a70b;
  background: #fffdf6;
}

.fld.err {
  border: 1.6px solid var(--kb-red);
}

.err-msg {
  margin: -4px 2px 10px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--kb-red);
}

.err-msg.center {
  text-align: center;
}

.legend {
  margin: 10px 2px 0;
  font-size: 11px;
  color: var(--kb-muted);
}

/* ---------- 버튼 ---------- */
.btn {
  padding: 15px 12px;
  border: 0;
  border-radius: 11px;
  font: inherit;
  font-size: 14.5px;
  font-weight: 700;
  line-height: 1.35;
  word-break: keep-all;
  cursor: pointer;
}

.btn.primary {
  color: var(--kb-text);
  background: var(--kb-yellow);
}

.btn.outline {
  border: 1.5px solid var(--kb-border);
  color: var(--kb-text);
  background: #fff;
}

.btn.danger-outline {
  border: 1.5px solid var(--kb-red);
  color: var(--kb-red);
  background: #fff;
}

.btn.danger {
  color: #fff;
  background: var(--kb-red);
}

.btn.disabled,
.btn:disabled {
  color: #8c8f93;
  background: #dfdcd6;
  border-color: #dfdcd6;
  cursor: not-allowed;
}

.btn-row {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.grow2 {
  flex: 2;
}

.grow1 {
  flex: 1;
}

.toast {
  margin: 14px auto 0;
  padding: 9px 14px;
  border-radius: 8px;
  font-size: 11.5px;
  color: #fff;
  background: #26282b;
  text-align: center;
}

/* ---------- 모달 ---------- */
.modal-host {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  width: 100%;
  max-width: 390px;
  margin: auto;
  place-items: center;
}

.dimmer {
  position: absolute;
  inset: 0;
  background: rgb(60 60 60 / 45%);
}

.delete-modal {
  position: relative;
  z-index: 1;
  width: 86%;
  padding: 26px 22px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 30px rgb(0 0 0 / 20%);
  text-align: center;
}

.trash {
  display: grid;
  width: 52px;
  height: 52px;
  margin: 0 auto 16px;
  border-radius: 50%;
  place-items: center;
  font-size: 19px;
  color: #a5493f;
  background: #f6dada;
}

.delete-modal strong {
  display: block;
  font-size: 16px;
}

.delete-modal p {
  margin: 8px 0 4px;
  font-size: 12.5px;
  color: var(--kb-muted);
}
</style>
