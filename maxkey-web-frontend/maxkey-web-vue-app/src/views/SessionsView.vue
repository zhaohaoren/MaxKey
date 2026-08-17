<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet } from '../api'
import { normalizeIdentityPage, type IdentityRow } from '../identity'

const { t } = useI18n({ useScope: 'global' })
const rows = ref<IdentityRow[]>([])
const loading = ref(false)
const error = ref('')
const expanded = ref(false)
const selectedKeys = ref<Array<string | number>>([])
const search = reactive<Record<string, unknown>>({ username: '', displayName: '', startDate: null, endDate: null })
const page = reactive({ current: 1, pageSize: 10, total: 0 })
const rowKey = (row: IdentityRow) => String(row.sessionId || row.id || '')

const rowSelection = computed(() => ({
  selectedRowKeys: selectedKeys.value,
  getCheckboxProps: (row: IdentityRow) => ({ disabled: row.disabled }),
  onChange: (keys: Array<string | number>) => { selectedKeys.value = keys },
}))

function formatDate(value: unknown) {
  if (!value || typeof value !== 'object' || !('format' in value)) return ''
  return (value as { format: (pattern: string) => string }).format('YYYY-MM-DD HH:mm:ss')
}

async function load(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  error.value = ''
  selectedKeys.value = []
  try {
    const result = normalizeIdentityPage(await adminGet('/access/session/fetch', {
      username: search.username,
      displayName: search.displayName,
      startDate: expanded.value ? formatDate(search.startDate) : '',
      endDate: expanded.value ? formatDate(search.endDate) : '',
      pageNumber: page.current,
      pageSize: page.pageSize,
    }))
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : '会话列表加载失败'
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  Object.assign(search, { username: '', displayName: '', startDate: null, endDate: null })
  void load(true)
}

async function terminate() {
  if (!selectedKeys.value.length) return
  try {
    await adminDelete('/access/session/terminate', { ids: selectedKeys.value.join(',') })
    message.success(t('admin.mxk.alert.operate.success'))
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('admin.mxk.alert.operate.error'))
  }
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  const changed = pagination.pageSize !== undefined && pagination.pageSize !== page.pageSize
  page.pageSize = pagination.pageSize || 10
  page.current = changed ? 1 : pagination.current || 1
  void load()
}

onMounted(() => { void load() })
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>{{ t('admin.mxk.menu.sessions') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card :bordered="false" class="identity-search-card">
      <a-form layout="inline" @submit.prevent="load(true)">
        <a-row :gutter="{ xs: 8, sm: 8, md: 8, lg: 24, xl: 48, xxl: 48 }" class="session-search-row">
          <a-col :xs="24" :md="8"><a-form-item :label="t('admin.mxk.users.username')"><a-input v-model:value="search.username" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-form-item :label="t('admin.mxk.users.displayName')"><a-input v-model:value="search.displayName" /></a-form-item></a-col>
          <a-col v-if="expanded" :span="24" class="session-time-filter-row">
            <a-row :gutter="{ xs: 8, sm: 8, md: 8, lg: 24, xl: 48, xxl: 48 }">
              <a-col :xs="24" :md="8"><a-form-item :label="t('admin.mxk.text.startDate')"><a-date-picker v-model:value="search.startDate" show-time format="YYYY-MM-DD HH:mm:ss" /></a-form-item></a-col>
              <a-col :xs="24" :md="8"><a-form-item :label="t('admin.mxk.text.endDate')"><a-date-picker v-model:value="search.endDate" show-time format="YYYY-MM-DD HH:mm:ss" /></a-form-item></a-col>
            </a-row>
          </a-col>
          <a-col :span="expanded ? 24 : 8" :class="{ 'session-search-actions-expanded': expanded }">
            <a-space><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button><a-button @click="resetSearch">{{ t('admin.mxk.text.reset') }}</a-button><a-button @click="expanded = !expanded">{{ t(expanded ? 'admin.mxk.text.collapse' : 'admin.mxk.text.expand') }}</a-button></a-space>
          </a-col>
        </a-row>
      </a-form>
    </a-card>
    <a-card>
      <div class="identity-toolbar"><a-button danger type="primary" :disabled="!selectedKeys.length" @click="terminate">{{ t('admin.mxk.text.terminate') }}</a-button></div>
      <a-table size="small" bordered :loading="loading" :data-source="rows" :row-key="rowKey" :row-selection="rowSelection"
        :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true, pageSizeOptions: ['10','20','50'] }" :scroll="{ x: 1500 }" @change="onTableChange">
        <a-table-column key="sessionId" data-index="sessionId" :title="t('admin.mxk.history.login.sessionId')" width="260" />
        <a-table-column key="username" data-index="username" :title="t('admin.mxk.history.login.username')" />
        <a-table-column key="displayName" data-index="displayName" :title="t('admin.mxk.history.login.displayName')" />
        <a-table-column key="provider" data-index="provider" :title="t('admin.mxk.history.login.message')" />
        <a-table-column key="loginType" data-index="loginType" :title="t('admin.mxk.history.login.loginType')" />
        <a-table-column key="sourceIp" data-index="sourceIp" :title="t('admin.mxk.history.login.sourceIp')" />
        <a-table-column key="browser" data-index="browser" :title="t('admin.mxk.history.login.browser')" />
        <a-table-column key="platform" data-index="platform" :title="t('admin.mxk.history.login.platform')" />
        <a-table-column key="loginTime" data-index="loginTime" :title="t('admin.mxk.history.login.loginTime')" width="180" />
      </a-table>
    </a-card>
  </DefaultLayout>
</template>
