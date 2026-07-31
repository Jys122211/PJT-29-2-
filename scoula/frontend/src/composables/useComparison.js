import { ref } from 'vue';
import { useRouter } from 'vue-router';
import profitLossApi from '@/api/profitLossApi';
import { resolveErrorMessage } from '@/constants/errorMessages';

export function useComparison() {
  const router = useRouter();

  const comparison = ref(null);
  const loading = ref(false);
  const submitting = ref(false);
  const error = ref(null);

  // 결과 화면이 마운트될 때 호출한다. POST 응답을 그대로 쓰지 않고 항상 GET으로 다시 조회해서
  // 새로고침해도 같은 화면이 나오게 한다.
  const fetchComparison = async (comparisonId) => {
    loading.value = true;
    error.value = null;
    try {
      comparison.value = await profitLossApi.getComparison(comparisonId);
    } catch (e) {
      error.value = resolveErrorMessage(e);
    } finally {
      loading.value = false;
    }
  };

  // "비교하기" 버튼에서 호출한다. 성공하면 응답의 comparisonId로 결과 화면으로 이동한다.
  const submitComparison = async (payload) => {
    submitting.value = true;
    error.value = null;
    try {
      const response = await profitLossApi.createComparison(payload);
      await router.push({ name: 'comparisons/result', params: { comparisonId: response.comparisonId } });
      return true;
    } catch (e) {
      error.value = resolveErrorMessage(e);
      return false;
    } finally {
      submitting.value = false;
    }
  };

  return { comparison, loading, submitting, error, fetchComparison, submitComparison };
}
