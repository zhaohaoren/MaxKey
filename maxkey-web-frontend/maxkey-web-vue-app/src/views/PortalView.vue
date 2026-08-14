<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { auth } from '../auth'
import { get } from '../api'

interface AppItem {
  id?: string
  name?: string
  appName?: string
  icon?: string
  loginUrl?: string
}

const router = useRouter()
const apps = ref<AppItem[]>([])
const loading = ref(true)
const error = ref('')

async function loadApps() {
  try {
    const data = await get<AppItem[]>('/appList')
    apps.value = Array.isArray(data) ? data : (((data as unknown as { rows?: AppItem[] })?.rows || []) as AppItem[])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '应用列表加载失败'
  } finally {
    loading.value = false
  }
}

async function logout() {
  await auth.logout()
  await router.replace('/passport/login')
}

function openApp(url?: string) {
  if (url) window.location.href = url
}

onMounted(loadApps)
</script>

<template>
  <DefaultLayout mode="portal">
    <div class="alain-default__content-title"><h1>应用 <small>已授权的应用列表</small></h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <div v-if="loading" class="page-loading">正在加载…</div>
    <div v-else-if="apps.length" class="portal-app-grid">
      <a-card v-for="item in apps" :key="String(item.id || item.name)" hoverable class="portal-app-card" @click="openApp(item.loginUrl)">
        <a-card-meta :title="item.appName || item.name || '未命名应用'">
          <template #avatar><a-avatar :src="item.icon">{{ String(item.appName || item.name || 'A').slice(0, 1) }}</a-avatar></template>
        </a-card-meta>
      </a-card>
    </div>
    <a-empty v-else description="暂无可访问应用" />
  </DefaultLayout>
</template>
