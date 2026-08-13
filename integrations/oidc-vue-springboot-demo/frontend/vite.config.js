import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: 'localhost',
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8088',
      '/oauth2': 'http://localhost:8088',
      '/login': 'http://localhost:8088',
      '/logout': 'http://localhost:8088'
    }
  }
})
