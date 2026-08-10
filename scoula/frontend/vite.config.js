import { fileURLToPath, URL } from 'node:url';

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueDevTools from 'vite-plugin-vue-devtools';

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
      },
      '/deposits': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/credit-loans': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/jeonse-loans': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: '../backend/src/main/webapp/resources',
    // outDir이 프로젝트 밖이라 vite가 기본으로 비우지 않는다.
    // 끄면 해시가 바뀐 옛 청크가 빌드마다 쌓이므로 매번 비운다.
    emptyOutDir: true,
  },
});
