import api from '@/api';

const BASE_URL = '/api/deposits';

/**
 * 백엔드가 완성되기 전까지 화면 개발용 Mock 데이터를 사용합니다.
 * 백엔드 연동 시 false 로만 바꾸면 됩니다.
 */
export const USE_MOCK = true;

// ---------------------------------------------------------------- mock
const mockDeposits = [
  {
    userDepositId: 1,
    bankName: '국민은행',
    productName: 'KB Star 정기예금',
    joinDate: '20250101',
    maturityDate: '20261016',
    principalAmount: 10000000,
    baseRate: 2.0,
    appliedRate: 2.15,
    dDay: 92,
  },
];

let mockSeq = 100;
const delay = (ms = 350) => new Promise((resolve) => setTimeout(resolve, ms));

const mockApi = {
  async getList() {
    await delay();
    const list = mockDeposits.filter((d) => !d._deleted);
    return {
      count: list.length,
      totalPrincipal: list.reduce((sum, d) => sum + d.principalAmount, 0),
      deposits: list,
    };
  },

  async getCount() {
    await delay(150);
    return { count: mockDeposits.filter((d) => !d._deleted).length };
  },

  async get(id) {
    await delay();
    const found = mockDeposits.find(
      (d) => d.userDepositId === Number(id) && !d._deleted,
    );
    if (!found) {
      throw buildError(404, { errorCode: 'DEPOSIT_NOT_FOUND' });
    }
    return { ...found };
  },

  async create(payload) {
    await delay(500);
    const created = { ...payload, userDepositId: (mockSeq += 1), dDay: 0 };
    mockDeposits.push(created);
    return { userDepositId: created.userDepositId, message: '등록 완료' };
  },

  async update(id, payload) {
    await delay(500);
    const index = mockDeposits.findIndex((d) => d.userDepositId === Number(id));
    if (index === -1) {
      throw buildError(404, { errorCode: 'DEPOSIT_NOT_FOUND' });
    }
    mockDeposits[index] = { ...mockDeposits[index], ...payload };
    return { userDepositId: Number(id), message: '수정 완료' };
  },

  async remove(id) {
    await delay(400);
    const found = mockDeposits.find((d) => d.userDepositId === Number(id));
    if (!found) {
      throw buildError(404, { errorCode: 'DEPOSIT_NOT_FOUND' });
    }
    found._deleted = true;
    return { userDepositId: Number(id), message: '삭제 완료' };
  },

  /**
   * OCR 목 응답. 화면 07-04 ~ 07-08 검증용으로 결과를 강제할 수 있습니다.
   * @param {'success'|'fail'|'timeout'} scenario
   */
  async ocr(_file, scenario = 'success') {
    await delay(2500); // 07-06 로딩 화면 확인용

    if (scenario === 'fail') {
      throw buildError(422, {
        status: 'FAILED',
        errorCode: 'OCR_FAILED',
        message: '예금 화면 캡쳐를 다시 올려주세요',
      });
    }
    if (scenario === 'timeout') {
      throw buildError(504, {
        status: 'FAILED',
        errorCode: 'OCR_TIMEOUT',
        message: '일시적 오류에요. 다시 시도하거나 직접 입력해주세요',
      });
    }

    return {
      status: 'SUCCESS',
      extracted: {
        bankName: '국민은행',
        productName: 'KB아이사랑예금',
        joinDate: '20261212',
        maturityDate: '20281212',
        principalAmount: 7000000,
        baseRate: 2.0,
        appliedRate: null, // 우대금리 포함값은 OCR로 읽을 수 없음
      },
    };
  },
};

function buildError(status, data) {
  const error = new Error(data.message ?? 'mock error');
  error.response = { status, data };
  return error;
}

// ---------------------------------------------------------------- real
const realApi = {
  async getList() {
    const { data } = await api.get(BASE_URL);
    return data;
  },

  async getCount() {
    const { data } = await api.get(`${BASE_URL}/count`);
    return data;
  },

  async get(id) {
    const { data } = await api.get(`${BASE_URL}/${id}`);
    return data;
  },

  async create(payload) {
    const { data } = await api.post(BASE_URL, payload);
    return data;
  },

  async update(id, payload) {
    const { data } = await api.put(`${BASE_URL}/${id}`, payload);
    return data;
  },

  async remove(id) {
    const { data } = await api.delete(`${BASE_URL}/${id}`);
    return data;
  },

  async ocr(file) {
    const formData = new FormData();
    formData.append('image', file);

    const { data } = await api.post(`${BASE_URL}/ocr`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000, // OCR은 5~6초 소요. 전역 1000ms로는 무조건 실패
    });
    return data;
  },
};

export default USE_MOCK ? mockApi : realApi;
