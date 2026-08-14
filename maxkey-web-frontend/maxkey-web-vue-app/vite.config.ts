import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  base: '/maxkey/',
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/sign': {
        target: 'http://127.0.0.1:9527',
        changeOrigin: true,
      },
      '/maxkey-mgt-api': {
        target: 'http://127.0.0.1:9526',
        changeOrigin: true,
      },
    },
  },
})
