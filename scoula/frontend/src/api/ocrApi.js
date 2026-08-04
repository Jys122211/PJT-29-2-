import api from '@/api';

const BASE_URL = '/api/ocr';

export default {
  // 이미지를 서버로 전송하여 OCR 결과 받아오기
  async extractImage(file) {
    const formData = new FormData();
    formData.append('file', file);

    const { data } = await api.post(`${BASE_URL}/extract`, formData, {
      // Content-Type은 브라우저가 multipart boundary와 함께 설정합니다.
      timeout: 45000,
    });
    return data;
  },
};
