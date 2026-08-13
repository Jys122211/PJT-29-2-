<script setup>
/**
 * 자산 등록 (화면 07-01 ~ 07-08)
 *
 *  07-01 직접 입력 정상 등록
 *  07-02 필수값 누락 차단
 *  07-03 날짜 역전 검증
 *  07-04 OCR 정상 추출 -> 자동 채움
 *  07-05 OCR 추출 실패
 *  07-06 OCR 추출 중 로딩
 *  07-07 OCR 결과 수정 후 저장
 *  07-08 OCR 응답 지연 / 실패
 */
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import depositApi from '@/api/depositApi';
import BottomNav from '@/components/mobile/BottomNav.vue';
import ocrApi from '@/api/ocrApi';
import {
  caretPositionAfterFormat,
  countDigitsBeforeCaret,
  extractApiError,
  formatNumber,
  isValidCompactDate,
  sanitizeRate,
  toCompactAccount,
  toCompactDate,
  toDisplayKbAccount,
  toDisplayDate,
  todayCompact,
} from '@/util/depositFormat';

const router = useRouter();

/** ocrState : idle | loading | success | failed */
const ocrState = ref('idle');
const ocrError = ref({ errorCode: null, message: '' });
const ocrFilledFields = ref(new Set());
const editedFields = ref(new Set());

const depositCount = ref(0);
const submitting = ref(false);
const submitError = ref('');
const toast = ref('');

const fileInput = ref(null);
const principalAmountDraft = ref(null);

/** 목 모드에서 OCR 결과를 강제하기 위한 값. 실제 연동 시 제거 */
// const mockScenario = ref('success');

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


// ------------------------------------------------------------ 입력 핸들러
function onAccountInput(event) {
  const input = event.target;
  const previousValue = form.accountNumber;
  const digitsBeforeCaret = countDigitsBeforeCaret(input);
  const digits = toCompactAccount(input.value).slice(0, 14);
  form.accountNumber = digits;
  applyFormattedValue(input, toDisplayKbAccount(digits), digitsBeforeCaret);
  if (digits !== previousValue) {
    markEdited('accountNumber');
    errors.accountNumber = '';
  }
}

function validateAccountNumber() {
  if (form.accountNumber !== '' && form.accountNumber.length !== 14) {
    errors.accountNumber = 'KB 계좌번호 숫자 14자리를 입력해주세요';
  }
}

function onAccountBlur() {
  validateAccountNumber();
}

function formatDigitString(digits) {
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

const principalAmountModel = computed(() => {
  if (principalAmountDraft.value !== null) {
    return formatDigitString(principalAmountDraft.value);
  }

  return formatNumber(form.principalAmount);
});

/**
 * 포맷된 값을 INPUT에 써넣고 커서를 원래 자리로 되돌린다.
 * 값을 다시 대입하면 브라우저가 커서를 맨 뒤로 보내기 때문에 필요하다.
 */
function applyFormattedValue(input, formattedValue, digitsBeforeCaret) {
  input.value = formattedValue;

  nextTick(() => {
    const position = caretPositionAfterFormat(formattedValue, digitsBeforeCaret);
    input.setSelectionRange(position, position);
  });
}
/** 가입금액 상한. DepositRequestDTO 의 10_000_000_000L 과 같아야 한다. */
const MAX_PRINCIPAL = 10_000_000_000;

function onAmountInput(event) {
  const input = event.target;
  const previousDigits =
    form.principalAmount === '' ? '' : String(form.principalAmount);
  const digitsBeforeCaret = countDigitsBeforeCaret(input);
const raw = String(input.value).replace(/[^0-9]/g, '');
  const digits = raw.length > 11 ? raw.slice(0, 11) : raw;

  if (digits === '') {
    principalAmountDraft.value = '';
    form.principalAmount = '';
    input.value = '';
    if (previousDigits !== '') {
      markEdited('principalAmount');
      errors.principalAmount = '';
    }
    return;
  }

  principalAmountDraft.value = digits;
  form.principalAmount = Number(digits);
  applyFormattedValue(input, formatDigitString(digits), digitsBeforeCaret);

  if (digits !== previousDigits) {
    markEdited('principalAmount');
  }

  if (Number(digits) > MAX_PRINCIPAL) {
    errors.principalAmount = '가입금액은 최대 100억원까지 입력할 수 있습니다';
  } else {
    errors.principalAmount = '';
  }
}

function onAmountFocus(event) {
  principalAmountDraft.value = String(event.target.value).replace(
    /[^0-9]/g,
    '',
  );
}

function onAmountBlur(event) {
  principalAmountDraft.value = null;
  event.target.value = formatNumber(form.principalAmount);
}

function formatKoreanAmount(amount, emptyMessage = '') {
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
  const previousValue = String(form[field]);
  const cleaned = sanitizeRate(event.target.value);
  form[field] = cleaned;
  event.target.value = cleaned;
  if (cleaned !== previousValue) {
    markEdited(field);
    errors[field] = '';
  }
}

function onDateInput(field, event) {
  const input = event.target;
  const previousValue = form[field];
  const digitsBeforeCaret = countDigitsBeforeCaret(input);
  const digits = toCompactDate(input.value).slice(0, 8);
  form[field] = digits;
  applyFormattedValue(input, toDisplayDate(digits), digitsBeforeCaret);
  if (digits !== previousValue) {
    markEdited(field);
    errors[field] = '';
    errors.maturityDate = '';
  }
  validateDateFields();
}

/**
 * 8자리를 다 채운 날짜를 곧바로 검사한다.
 * 실재하지 않는 날짜, 그리고 가입일은 오늘 이전 / 만기일은 오늘 이후 규칙.
 */
function validateDateFields() {
  const today = todayCompact();

  ['joinDate', 'maturityDate'].forEach((field) => {
    if (errors[field] || form[field].length !== 8) return;

    if (!isValidCompactDate(form[field])) {
      errors[field] = '존재하지 않는 날짜예요';
      return;
    }

    if (field === 'joinDate' && form.joinDate > today) {
      errors.joinDate = '가입일은 오늘이거나 오늘보다 전이여야 해요';
    }

    if (field === 'maturityDate' && form.maturityDate <= today) {
      errors.maturityDate = '만기일은 오늘 이후 날짜여야 해요';
    }
  });
}

/** 은행명·상품명 길이 기준. warn 을 넘으면 경고, max 에서 입력을 멈춘다. */
const NAME_RULES = {
  bankName: { warn: 12, label: '은행명' },
  productName: { warn: 25, label: '상품명' },
};

/** 은행명은 문자만, 상품명은 숫자까지 허용한다. */
function onTextInput(field, event) {
  const rule = NAME_RULES[field];
  const raw = event.target.value;

  const filtered =
    field === 'bankName'
      ? raw.replace(/[^가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z ]/g, '')
      : raw.replace(/[^가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ()\-.]/g, '');

const cleaned = filtered.slice(0, rule.warn);

  form[field] = cleaned;
  event.target.value = cleaned;
  markEdited(field);

if (filtered.length > rule.warn) {
    errors[field] = `${rule.label}은 ${rule.warn}자 이내로 입력해주세요`;
  } else {
    errors[field] = '';
  }
}

function markEdited(field) {
  if (ocrFilledFields.value.has(field)) {
    editedFields.value = new Set(editedFields.value).add(field);
  }
}

function fieldClass(field) {
  const hasValue = String(form[field] ?? '').trim() !== '';
  return {
    err: !!errors[field],
    ocr: hasValue && !editedFields.value.has(field),
    edited: editedFields.value.has(field),
  };
}

// ------------------------------------------------------------ 검증 (07-02, 07-03)
const isComplete = computed(
  () =>
    form.bankName.trim() !== '' &&
    form.bankName.trim().length <= NAME_RULES.bankName.warn &&
    form.productName.trim() !== '' &&
    form.productName.trim().length <= NAME_RULES.productName.warn &&
    form.accountNumber.length === 14 &&
    form.joinDate.length === 8 &&
    form.maturityDate.length === 8 &&
    form.principalAmount !== '' &&
    Number(form.principalAmount) > 0 &&
    Number(form.principalAmount) <= MAX_PRINCIPAL &&
    form.baseRate !== '' &&
    form.appliedRate !== '',
);

const isDateReversed = computed(
  () =>
    form.joinDate.length === 8 &&
    form.maturityDate.length === 8 &&
    form.maturityDate <= form.joinDate,
);

/** 가입일은 오늘 이전, 만기일은 오늘 이후 */
const isDateOutOfRange = computed(() => {
  const today = todayCompact();
  return (
    (isValidCompactDate(form.joinDate) && form.joinDate > today) ||
    (isValidCompactDate(form.maturityDate) && form.maturityDate <= today)
  );
});

const canSubmit = computed(
  () =>
    isComplete.value &&
    !isDateReversed.value &&
    !isDateOutOfRange.value &&
    !submitting.value,
);

const submitLabel = computed(() => {
  if (submitting.value) return '등록 중...';
  if (!isComplete.value) return '필수 항목을 입력해주세요';
  if (isDateReversed.value || isDateOutOfRange.value) return '날짜를 확인해주세요';
  return ocrState.value === 'success' ? '확인했어요 · 등록하기' : '등록하기';
});

function validate() {
  Object.keys(errors).forEach((key) => (errors[key] = ''));
  let firstInvalid = null;

  const require = (field, message) => {
    if (String(form[field]).trim() === '') {
      errors[field] = message;
      firstInvalid = firstInvalid ?? field;
    }
  };

  require('bankName', '은행명을 입력해주세요');
  require('productName', '상품명을 입력해주세요');
  require('accountNumber', '계좌번호를 입력해주세요');
  require('joinDate', '가입일을 입력해주세요');
  require('maturityDate', '만기일을 입력해주세요');
  require('baseRate', '기본금리를 입력해주세요');
  require('appliedRate', '적용금리를 입력해주세요');

  ['bankName', 'productName'].forEach((field) => {
    const rule = NAME_RULES[field];
    if (form[field].trim().length > rule.warn) {
      errors[field] = `${rule.label}은 ${rule.warn}자 이내로 입력해주세요`;
      firstInvalid = firstInvalid ?? field;
    }
  });

  if (form.accountNumber !== '' && form.accountNumber.length !== 14) {
    errors.accountNumber = 'KB 계좌번호 숫자 14자리를 입력해주세요';
    firstInvalid = firstInvalid ?? 'accountNumber';
  }

 if (form.principalAmount === '' || Number(form.principalAmount) <= 0) {
    errors.principalAmount = '가입금액을 입력해주세요';
    firstInvalid = firstInvalid ?? 'principalAmount';
  } else if (Number(form.principalAmount) < 10000) {
    errors.principalAmount = '가입금액은 1만원 이상 입력해주세요';
    firstInvalid = firstInvalid ?? 'principalAmount';
  } else if (Number(form.principalAmount) > 10000000000) {
    errors.principalAmount = '가입금액은 100억원 이하로 입력해주세요';
    firstInvalid = firstInvalid ?? 'principalAmount';
  }

  // 존재하지 않는 날짜 (20250230 등)
  ['joinDate', 'maturityDate'].forEach((field) => {
    if (form[field].length === 8 && !isValidCompactDate(form[field])) {
      errors[field] = '존재하지 않는 날짜예요';
      firstInvalid = firstInvalid ?? field;
    }
  });

  // 07-03 날짜 역전 : 만기일 INPUT을 비우고 에러 표시
  if (!errors.joinDate && !errors.maturityDate && isDateReversed.value) {
    errors.maturityDate = '만기일이 가입일보다 빠릅니다';
    form.maturityDate = '';
    firstInvalid = firstInvalid ?? 'maturityDate';
  }

  // 가입일은 오늘 이전, 만기일은 오늘 이후
  validateDateFields();
  if (errors.joinDate) firstInvalid = firstInvalid ?? 'joinDate';
  if (errors.maturityDate) firstInvalid = firstInvalid ?? 'maturityDate';

  if (
    !errors.baseRate &&
    !errors.appliedRate &&
    Number(form.appliedRate) < Number(form.baseRate)
  ) {
    errors.appliedRate = '적용금리는 기본금리보다 크거나 같아야 합니다';
    firstInvalid = firstInvalid ?? 'appliedRate';
  }

  return firstInvalid === null;
}

// ------------------------------------------------------------ OCR (07-04 ~ 07-08)
const OCR_ALLOWED_TYPES = new Set(['image/png', 'image/jpeg']);
const OCR_MAX_FILE_SIZE = 10 * 1024 * 1024;

function openFilePicker() {
  if (ocrState.value === 'loading') return;
  fileInput.value?.click();
}

async function onFileSelected(event) {
  const file = event.target.files?.[0];
  event.target.value = ''; // 같은 파일 재선택 허용
  if (!file) return;

  if (!OCR_ALLOWED_TYPES.has(file.type)) {
    ocrError.value = {
      errorCode: 'INVALID_OCR_FILE',
      message: 'PNG 또는 JPEG 이미지만 선택할 수 있습니다.',
    };
    ocrState.value = 'failed';
    return;
  }

  if (file.size > OCR_MAX_FILE_SIZE) {
    ocrError.value = {
      errorCode: 'INVALID_OCR_FILE',
      message: '이미지는 10MB 이하만 업로드할 수 있습니다.',
    };
    ocrState.value = 'failed';
    return;
  }

  await runOcr(file);
}

async function runOcr(file) {
  ocrState.value = 'loading'; // 07-06
  ocrError.value = { errorCode: null, message: '' };

  try {
    // 백엔드 OCR 호출 (ocrApi는 18행에서 이미 import 되어 있음)
    const extractedData = await ocrApi.extractImage(file);
    const filledCount = applyExtracted(extractedData);

    if (filledCount < 3) {
      ocrError.value = {
        errorCode: 'OCR_NO_DEPOSIT',
        message: '예금 정보를 찾지 못했어요.\n은행 앱의 예금 상세 화면을 올려주세요.',
      };
      ocrState.value = 'failed';
      return;
    }

    ocrState.value = 'success'; // 07-04
  } catch (error) {
    console.error('OCR 분석 실패:', error);
    ocrError.value = error.response
      ? extractApiError(error)
      : { errorCode: null, message: '이미지 분석에 실패했습니다.' };
    ocrState.value = 'failed'; // 07-05 / 07-08
  }
}

/** 07-04 : 추출값을 폼에 채우되 저장은 하지 않음 */
function applyExtracted(extracted = {}) {
  const filled = new Set();

  const assign = (field, value) => {
    if (value === null || value === undefined || value === '') return;
    form[field] = value;
    filled.add(field);
  };

  assign('bankName', extracted.bankName);
  assign('productName', extracted.productName);
  assign(
    'accountNumber',
    toCompactAccount(extracted.accountNumber).slice(0, 14),
  );
  assign('joinDate', extracted.joinDate);
  assign('maturityDate', extracted.maturityDate);
  assign('principalAmount', extracted.principalAmount);
  assign(
    'baseRate',
    extracted.baseRate == null ? '' : String(extracted.baseRate),
  );
  assign(
    'appliedRate',
    extracted.appliedRate == null ? '' : String(extracted.appliedRate),
  );

  ocrFilledFields.value = filled;
  editedFields.value = new Set();
  Object.keys(errors).forEach((key) => (errors[key] = ''));
  validateAccountNumber();
  validateDateFields();

    return filled.size;
}

/** 07-05, 07-08 -> 직접 입력으로 전환 */
function switchToManual() {
  ocrState.value = 'idle';
  ocrError.value = { errorCode: null, message: '' };
  ocrFilledFields.value = new Set();
  editedFields.value = new Set();
}

// ------------------------------------------------------------ 등록
async function submit() {
  if (!validate()) return;

  submitting.value = true;
  submitError.value = '';

  try {
    await depositApi.create({
      bankName: form.bankName.trim(),
      productName: form.productName.trim(),
      accountNumber: form.accountNumber,
      joinDate: form.joinDate,
      maturityDate: form.maturityDate,
      principalAmount: Number(form.principalAmount),
      baseRate: Number(form.baseRate),
      appliedRate: Number(form.appliedRate),
    });

    toast.value = '등록 완료 · 대시보드에 반영되었습니다';
    setTimeout(() => router.push({ name: 'assetList' }));
  } catch (error) {
    const { field, message } = extractApiError(error);
    if (field && field in errors) {
      errors[field] = message; // 서버 검증 결과를 해당 INPUT에 표시
    } else {
      submitError.value = message;
    }
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  try {
    const { count } = await depositApi.getCount();
    depositCount.value = count;
  } catch {
    depositCount.value = 0;
  }
});
</script>

<template>
  <main class="page">
    <header class="page-head">
      <h1>자산 등록</h1>
      <span class="badge">보유 상품 {{ depositCount }}건</span>
    </header>

    <!-- ============ OCR 배너 ============ -->

    <!-- 07-06 추출 중 -->
    <section v-if="ocrState === 'loading'" class="loading-card">
      <div class="spinner" aria-hidden="true"></div>
      <strong>추출 중입니다...</strong>
      <p>캡쳐에서 예금 정보를 읽고 있어요<br />보통 5~6초 정도 걸려요</p>
    </section>

    <!-- 07-04 / 07-07 추출 완료 -->
    <section v-else-if="ocrState === 'success'" class="ocr-banner">
      <span class="ico"><i class="fa-solid fa-check"></i></span>
      <div class="tx">
        <strong>OCR 추출 완료</strong>
        <small>추출값이 입력폼에 채워졌어요 · 확인 후 등록해주세요</small>
      </div>
    </section>

    <!-- 07-05 / 07-08 추출 실패 -->
    <section v-else-if="ocrState === 'failed'" class="ocr-banner fail">
      <span class="ico fail"><i class="fa-solid fa-xmark"></i></span>
      <div class="tx">
        <strong>추출에 실패했어요</strong>
        <small>{{ ocrError.message }}</small>
      </div>
    </section>

    <!-- 기본 상태 : OCR 진입 배너 -->
    <button
      v-else
      type="button"
      class="ocr-banner clickable"
      @click="openFilePicker"
    >
      <span class="ico play"><i class="fa-solid fa-play"></i></span>
      <div class="tx">
        <strong>앱 화면 캡쳐로 자동 등록</strong>
        <small>은행 앱 캡쳐를 올리면 OCR로 정보를 추출합니다</small>
      </div>
      <i class="fa-solid fa-chevron-right chev"></i>
    </button>

    <input
      ref="fileInput"
      type="file"
      accept="image/png, image/jpeg"
      class="hidden-file"
      @change="onFileSelected"
    />

    <!-- 목 모드 전용 : OCR 결과 시나리오 선택 -->
    <!-- <div v-if="USE_MOCK && ocrState === 'idle'" class="mock-switch">
      <span>[테스트 중...] OCR 결과</span>
      <label><input v-model="mockScenario" type="radio" value="success" /> 성공</label>
      <label><input v-model="mockScenario" type="radio" value="fail" /> 실패</label>
      <label><input v-model="mockScenario" type="radio" value="timeout" /> 타임아웃</label>
    </div> -->

    <p v-if="ocrState === 'success'" class="hint">
      노란 배경 = OCR 자동 입력값 · 확인 후 등록해주세요
    </p>
    <p v-if="ocrState === 'failed'" class="hint">
      다시 시도하거나 아래에서 직접 입력할 수 있어요
    </p>

    <!-- 07-05 / 07-08 재시도 버튼 -->
    <div v-if="ocrState === 'failed'" class="retry-row">
      <button type="button" class="btn primary" @click="openFilePicker">
        OCR 다시 시도
      </button>
      <button type="button" class="btn outline" @click="switchToManual">
        직접 입력으로 등록하기
      </button>
    </div>

    <!-- ============ 입력 폼 ============ -->
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
          maxlength="12"
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
          maxlength="25"
          @input="onTextInput('productName', $event)"
        />
      </div>
      <p v-if="errors.productName" class="err-msg">
        ! {{ errors.productName }}
      </p>

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
      <p v-if="errors.accountNumber" class="err-msg">
        ! {{ errors.accountNumber }}
      </p>

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
      <p v-else-if="errors.maturityDate" class="err-msg">
        ! {{ errors.maturityDate }}
      </p>

      <div class="form-row two-column-row amount-rate-row">
        <div class="field-with-guide">
          <input
            class="fld"
            :class="fieldClass('principalAmount')"
            :value="principalAmountModel"
            placeholder="가입 금액 (원)"
            inputmode="numeric"
            aria-label="가입 금액"
            @focus="onAmountFocus"
            @input="onAmountInput"
            @blur="onAmountBlur"
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
      <p v-if="errors.appliedRate" class="err-msg">
        ! {{ errors.appliedRate }}
      </p>

      <p v-if="submitError" class="err-msg center">{{ submitError }}</p>

      <button
        type="submit"
        class="btn submit"
        :class="{ disabled: !canSubmit }"
        :disabled="!canSubmit"
      >
        {{ submitLabel }}
      </button>
    </form>

    <p v-if="toast" class="toast">{{ toast }}</p>

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

  width: 100%;
  max-width: 390px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 24px 20px 96px;
  color: var(--kb-text);
  background: #faf9f7;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-head h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.badge {
  font-size: 11px;
  color: var(--kb-muted);
}

/* ---------- OCR 배너 ---------- */
.ocr-banner {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 13px;
  padding: 15px 16px;
  border: 0;
  border-radius: 14px;
  color: #fff;
  background: #26282b;
  text-align: left;
  font: inherit;
}

.ocr-banner.clickable {
  cursor: pointer;
}

.ocr-banner .ico {
  display: grid;
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 10px;
  place-items: center;
  font-size: 15px;
  color: #26282b;
  background: var(--kb-yellow);
}

.ocr-banner .ico.fail {
  color: #fff;
  background: var(--kb-red);
}

.ocr-banner .tx {
  flex: 1;
  min-width: 0;
}

.ocr-banner strong {
  display: block;
  font-size: 14px;
}

.ocr-banner.fail strong {
  color: #e8776a;
}

.ocr-banner small {
  display: block;
  margin-top: 3px;
  font-size: 11.5px;
  line-height: 1.5;
  color: #a7acb1;
}

.chev {
  flex: none;
  font-size: 12px;
  color: #8a8f94;
}

.hidden-file {
  display: none;
}

/* ---------- 로딩 (07-06) ---------- */
.loading-card {
  padding: 40px 20px;
  border-radius: 16px;
  color: #fff;
  background: #26282b;
  text-align: center;
}

.spinner {
  width: 44px;
  height: 44px;
  margin: 0 auto 20px;
  border: 4px solid var(--kb-yellow);
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-card strong {
  font-size: 15px;
  color: var(--kb-yellow);
}

.loading-card p {
  margin: 10px 0 0;
  font-size: 11.5px;
  line-height: 1.7;
  color: #a7acb1;
}

.hint {
  margin: 10px 0 0;
  padding: 10px 12px;
  border: 1px solid var(--kb-line);
  border-radius: 10px;
  font-size: 11.5px;
  line-height: 1.6;
  color: #6b5716;
  background: var(--kb-soft);
}

.ocr-banner .tx small {
  white-space: pre-line;
}

.mock-switch {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding: 9px 11px;
  border: 1px dashed #cfc6b6;
  border-radius: 9px;
  font-size: 11px;
  color: #7d766c;
}

.mock-switch label {
  display: flex;
  align-items: center;
  gap: 3px;
}

.retry-row {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

/* ---------- 폼 ---------- */
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
}

.fld::placeholder {
  color: #b8b1a8;
}

.fld:disabled {
  color: var(--kb-text);
  background: #f6f3ee;
}

.fld:focus {
  border-color: #f4a70b;
  outline: 0;
}

.fld.ocr {
  border-color: var(--kb-line);
  background: var(--kb-soft);
}

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
.warn-msg {
  margin: -4px 2px 10px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--kb-deep, #8a6d1f);
}

/* ---------- 버튼 ---------- */
.btn {
  width: 100%;
  padding: 15px;
  border: 0;
  border-radius: 11px;
  font: inherit;
  font-size: 14.5px;
  font-weight: 700;
  cursor: pointer;
}

.btn.primary,
.btn.submit {
  color: var(--kb-text);
  background: var(--kb-yellow);
}

.btn.outline {
  border: 1.5px solid var(--kb-border);
  color: var(--kb-text);
  background: #fff;
}

.btn.submit {
  margin-top: 6px;
}

.btn.disabled,
.btn:disabled {
  color: #8c8f93;
  background: #dfdcd6;
  cursor: not-allowed;
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
</style>
