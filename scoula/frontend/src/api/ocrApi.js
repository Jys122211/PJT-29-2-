import api from '@/api';

const BASE_URL = '/api/ocr';

export default {
  // 이미지를 서버로 전송하여 OCR 결과 받아오기
  async extractImage(file) {
    const formData = new FormData();
    formData.append('file', file);

    const { data } = await api.post(`${BASE_URL}/extract`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000, // Gemini 호출 5~6초. 전역 기본값으로는 부족
    });
    return data;
  },
};