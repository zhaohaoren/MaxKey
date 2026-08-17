<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DefaultLayout from '../components/DefaultLayout.vue'
import { del, get } from '../api'
import { normalizeIdentityPage, type IdentityRow } from '../identity'

const { t } = useI18n({ useScope: 'global' })
const rows = ref<IdentityRow[]>([])
const selectedKeys = ref<Array<string | number>>([])
const loading = ref(false)
const error = ref('')
const search = reactive<Record<string, unknown>>({ startDate: null, endDate: null })
const page = reactive({ current: 1, pageSize: 10, total: 0 })

const rowSelection = computed(() => ({
  selectedRowKeys: selectedKeys.value,
  getCheckboxProps: (row: IdentityRow) => ({ disabled: Boolean(row.disabled) }),
  onChange: (keys: Array<string | number>) => { selectedKeys.value = keys },
}))

function formatDate(value: unknown) {
  if (!value || typeof value !== 'object' || !('format' in value)) return ''
  return (value as { format: (pattern: string) => string }).format('YYYY-MM-DD HH:mm:ss')
}

async function load(resetPage = false) {
  if (resetPage) page.current = 1
  loading.value = true
  error.value = ''
  selectedKeys.value = []
  try {
    const result = normalizeIdentityPage(await get('/access/session/fetch', {
      startDate: formatDate(search.startDate),
      endDate: formatDate(search.endDate),
      pageNumber: page.current,
      pageSize: page.pageSize,
    }))
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : t('ui.sessionLoadError')
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  search.startDate = null
  search.endDate = null
  void load(true)
}

function terminate() {
  if (!selectedKeys.value.length) return
  Modal.confirm({
    title: t('ui.terminateSessionsTitle'),
    okType: 'danger',
    async onOk() {
      try {
        await del('/access/session/terminate', { ids: selectedKeys.value.join(',') })
        message.success(t('portal.mxk.alert.operate.success'))
        await load()
      } catch (error) {
        message.error(error instanceof Error ? error.message : t('portal.mxk.alert.operate.error'))
      }
    },
  })
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  const pageSizeChanged = pagination.pageSize !== undefined && pagination.pageSize !== page.pageSize
  page.pageSize = pagination.pageSize || 10
  page.current = pageSizeChanged ? 1 : pagination.current || 1
  void load()
}

onMounted(() => { void load() })
</script>

<template>
  <DefaultLayout mode="portal">
    <div class="alain-default__content-title"><h1>{{ t('portal.mxk.menu.sessions') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card :bordered="false" class="portal-search-card">
      <a-form layout="inline" @submit.prevent="load(true)">
        <a-row :gutter="{ xs: 8, sm: 8, md: 8, lg: 24, xl: 48, xxl: 48 }" class="portal-search-row">
          <a-col :xs="24" :md="8"><a-form-item :label="t('portal.mxk.text.startDate')"><a-date-picker v-model:value="search.startDate" show-time format="YYYY-MM-DD HH:mm:ss" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item :label="t('portal.mxk.text.endDate')"><a-date-picker v-model:value="search.endDate" show-time format="YYYY-MM-DD HH:mm:ss" /></a-form-item></a-col>
          <a-col :xs="24" :md="8" class="portal-search-actions"><a-space><a-button type="primary" html-type="submit">{{ t('portal.mxk.text.query') }}</a-button><a-button @click="resetSearch">{{ t('portal.mxk.text.reset') }}</a-button></a-space></a-col>
        </a-row>
      </a-form>
    </a-card>
    <a-card>
      <div class="identity-toolbar"><a-button danger type="primary" :disabled="!selectedKeys.length" @click="terminate">{{ t('portal.mxk.text.terminate') }}</a-button></div>
      <a-table size="small" bordered :loading="loading" :data-source="rows" row-key="sessionId" :row-selection="rowSelection"
        :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true, pageSizeOptions: ['10','20','50'] }" :scroll="{ x: 1300 }" @change="onTableChange">
        <a-table-column key="sessionId" data-index="sessionId" :title="t('portal.mxk.history.login.sessionId')" />
        <a-table-column key="username" data-index="username" :title="t('portal.mxk.history.login.username')" />
        <a-table-column key="displayName" data-index="displayName" :title="t('portal.mxk.history.login.displayName')" />
        <a-table-column key="sourceIp" data-index="sourceIp" :title="t('portal.mxk.history.login.sourceIp')" />
        <a-table-column key="location" data-index="location" :title="t('portal.mxk.history.login.location')" />
        <a-table-column key="browser" data-index="browser" :title="t('portal.mxk.history.login.browser')" />
        <a-table-column key="platform" data-index="platform" :title="t('portal.mxk.history.login.platform')" />
        <a-table-column key="loginTime" data-index="loginTime" :title="t('portal.mxk.history.login.loginTime')" />
      </a-table>
    </a-card>
  </DefaultLayout>
</template>

<style scoped>
.portal-search-card { margin-bottom: 24px; }
.portal-search-row { width: 100%; }
.portal-search-row :deep(.ant-form-item) { width: 100%; margin-right: 0; }
.portal-search-row :deep(.ant-form-item-control) { min-width: 0; flex: 1; }
.portal-search-row :deep(.ant-picker) { width: 100%; }
.portal-search-actions { text-align: right; }
@media (max-width: 767px) { .portal-search-actions { margin-top: 8px; text-align: left; } }
</style>
