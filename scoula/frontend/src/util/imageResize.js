/** OCR 업로드용 이미지 축소 */

/** 긴 변 기준 최대 픽셀. 은행 앱 캡처의 글자를 읽기에 충분한 크기다. */
const MAX_EDGE = 1600;
/** JPEG 품질. 글자 가독성과 용량의 절충값 */
const QUALITY = 0.85;
/** 이 크기 이하는 줄여도 이득이 없어 그대로 보낸다. */
const SKIP_UNDER = 700 * 1024;

/** 파일을 이미지로 디코딩한다. */
function loadImage(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      URL.revokeObjectURL(url);
      resolve(image);
    };
    image.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('이미지를 읽지 못했습니다.'));
    };
    image.src = url;
  });
}

/**
 * 긴 변이 MAX_EDGE를 넘으면 비율을 유지한 채 줄여 JPEG로 다시 만든다.
 * 줄일 필요가 없거나 실패하면 원본을 그대로 돌려준다.
 */
export async function shrinkForOcr(file) {
  if (file.size <= SKIP_UNDER) return file;

  try {
    const image = await loadImage(file);
    const scale = Math.min(1, MAX_EDGE / Math.max(image.width, image.height));

    const canvas = document.createElement('canvas');
    canvas.width = Math.round(image.width * scale);
    canvas.height = Math.round(image.height * scale);
    canvas.getContext('2d').drawImage(image, 0, 0, canvas.width, canvas.height);

    const blob = await new Promise((resolve) =>
      canvas.toBlob(resolve, 'image/jpeg', QUALITY),
    );
    if (!blob || blob.size >= file.size) return file;

    return new File([blob], 'ocr.jpg', { type: 'image/jpeg' });
  } catch (error) {
    console.warn('이미지 축소 실패, 원본을 전송합니다:', error);
    return file;
  }
}
