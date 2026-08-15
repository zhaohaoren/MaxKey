<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import DefaultLayout from '../components/DefaultLayout.vue'
import { get } from '../api'

interface AppItem {
  id?: string
  name?: string
  appName?: string
  iconBase64?: string
  loginUrl?: string
  protocol?: string
  inducer?: string
}

const router = useRouter()
const { t } = useI18n({ useScope: 'global' })
const apps = ref<AppItem[]>([])
const loading = ref(true)
const error = ref('')

async function loadApps() {
  try {
    const data = await get<AppItem[]>('/appList')
    apps.value = Array.isArray(data) ? data : (((data as unknown as { rows?: AppItem[] })?.rows || []) as AppItem[])
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('ui.appLoadError')
  } finally {
    loading.value = false
  }
}

function openApp(item: AppItem) {
  if ((item.protocol === 'Basic' || item.inducer === 'SP') && item.loginUrl) {
    window.open(item.loginUrl, '_blank', 'noopener')
    return
  }
  if (item.id) window.open(`/sign/authz/${encodeURIComponent(item.id)}`, '_blank', 'noopener')
}

function configureAccount(item: AppItem) {
  if (item.id) router.push({ path: '/authz/credential', query: { appId: item.id } })
}

function displayName(item: AppItem) {
  return item.appName || item.name || t('ui.unnamedApp')
}

onMounted(loadApps)
</script>

<template>
  <DefaultLayout mode="portal">
    <div class="alain-default__content-title"><h1>{{ t('portal.mxk.menu.applist') }} <small>{{ t('ui.portalSubtitle') }}</small></h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <div v-if="loading" class="page-loading">{{ t('ui.portalLoading') }}</div>
    <div v-else-if="apps.length" class="portal-app-grid">
      <a-card v-for="item in apps" :key="String(item.id || item.name)" hoverable class="portal-app-card" @click="openApp(item)">
        <div class="portal-app-card__content">
          <img class="portal-app-card__icon" :src="item.iconBase64" alt="" />
          <strong class="portal-app-card__name" :title="displayName(item)">{{ displayName(item) }}</strong>
        </div>
        <template v-if="item.protocol === 'Form_Based'" #actions><a-button type="link" @click.stop="configureAccount(item)">{{ t('ui.configureAccount') }}</a-button></template>
      </a-card>
    </div>
    <a-empty v-else :description="t('ui.noApps')" />
  </DefaultLayout>
</template>
