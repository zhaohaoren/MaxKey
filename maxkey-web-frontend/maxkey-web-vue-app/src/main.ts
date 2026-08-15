import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import App from './App.vue'
import { i18n, initializeI18n } from './i18n'
import router from './router'
import './style.css'

async function bootstrap() {
  await initializeI18n()
  createApp(App).use(Antd).use(i18n).use(router).mount('#app')
}

void bootstrap()
