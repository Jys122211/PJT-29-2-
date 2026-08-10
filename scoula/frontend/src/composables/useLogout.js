import { ref } from 'vue';
import { useAuthStore } from '@/stores/auth';

export function useLogout() {
  const { logout } = useAuthStore();
  const isConfirmLogout = ref(false);
  const requestLogout = () => {
    isConfirmLogout.value = true;
  };
  const cancelLogout = () => {
    isConfirmLogout.value = false;
  };
  const confirmLogout = async () => {
    await logout();
    isConfirmLogout.value = false;
  };

  return { isConfirmLogout, requestLogout, cancelLogout, confirmLogout };
}
