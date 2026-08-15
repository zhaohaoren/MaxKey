<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminGet } from '../api'

interface ReportItem {
  reportstring?: string
  reportcount?: number
  appname?: string
}

const loading = ref(true)
const error = ref('')
const data = reactive<Record<string, unknown>>({})
const { t } = useI18n({ useScope: 'global' })
const statistics = computed(() => [
  { key: 'totalUsers', title: t('admin.mxk.home.totalUsers') },
  { key: 'totalDepts', title: t('admin.mxk.home.totalDepts') },
  { key: 'totalGroups', title: t('admin.mxk.home.totalGroups') },
  { key: 'totalApps', title: t('admin.mxk.home.totalApps') },
])

async function load() {
  loading.value = true
  error.value = ''
  try {
    Object.assign(data, await adminGet<Record<string, unknown>>('/dashboard'))
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('ui.dashboardLoadError')
  } finally {
    loading.value = false
  }
}

function list(key: string) {
  return Array.isArray(data[key]) ? data[key] as ReportItem[] : []
}

onMounted(load)
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><div><h1>{{ t('admin.mxk.menu.home') }}</h1><small>{{ t('ui.dashboardSubtitle') }}</small></div><a-button @click="load">{{ t('ui.refresh') }}</a-button></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-spin :spinning="loading">
      <a-row :gutter="16">
        <a-col v-for="item in statistics" :key="item.key" :xs="24" :sm="12" :lg="6">
          <a-card><a-statistic :title="item.title" :value="Number(data[item.key] || 0)" /></a-card>
        </a-col>
      </a-row>
      <a-row :gutter="16" class="feature-actions">
        <a-col :xs="24" :lg="12"><a-card :title="t('admin.mxk.home.dayAccessCount')"><a-descriptions bordered :column="1"><a-descriptions-item :label="t('admin.mxk.home.dayAccessCount')">{{ data.dayCount || 0 }}</a-descriptions-item><a-descriptions-item :label="t('admin.mxk.home.monthAccessCount')">{{ data.monthCount || 0 }}</a-descriptions-item><a-descriptions-item :label="t('admin.mxk.home.onlineUsers')">{{ data.onlineUsers || 0 }}</a-descriptions-item><a-descriptions-item :label="t('admin.mxk.home.activeUsers')">{{ data.activeUsers || 0 }}</a-descriptions-item><a-descriptions-item :label="t('admin.mxk.home.newUsers')">{{ data.newUsers || 0 }}</a-descriptions-item></a-descriptions></a-card></a-col>
        <a-col :xs="24" :lg="12"><a-card :title="t('admin.mxk.home.monthAppCount')"><a-table :data-source="list('reportApp')" :pagination="false" size="small" row-key="appname"><a-table-column :title="t('admin.mxk.home.appName')" data-index="appname" /><a-table-column :title="t('admin.mxk.home.accessCount')" data-index="reportcount" /></a-table></a-card></a-col>
      </a-row>
      <a-card :title="t('admin.mxk.home.monthBrowserCount')" class="admin-page-card"><a-table :data-source="list('reportBrowser')" :pagination="false" size="small" row-key="reportstring"><a-table-column :title="t('admin.mxk.home.browser')" data-index="reportstring" /><a-table-column :title="t('admin.mxk.home.accessCount')" data-index="reportcount" /></a-table></a-card>
    </a-spin>
  </DefaultLayout>
</template>
