<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminGet, get } from '../api'

interface AuditConfig {
  title: string
  endpoint: string
  search: Array<{ key: string; label: string }>
  columns: Array<{ key: string; label: string }>
}

interface AuditRow {
  id?: string
  [key: string]: unknown
}

const configs: Record<string, AuditConfig> = {
  logins: {
    title: '登录审计', endpoint: '/historys/loginHistory/fetch',
    search: [{ key: 'username', label: '用户名' }, { key: 'displayName', label: '显示名称' }],
    columns: [
      { key: 'sessionId', label: '会话标识' }, { key: 'username', label: '用户名' },
      { key: 'displayName', label: '显示名称' }, { key: 'message', label: '结果信息' },
      { key: 'loginType', label: '登录方式' }, { key: 'sourceIp', label: '来源 IP' },
      { key: 'location', label: '位置' }, { key: 'browser', label: '浏览器' },
      { key: 'platform', label: '平台' }, { key: 'loginTime', label: '登录时间' },
      { key: 'logoutTime', label: '退出时间' },
    ],
  },
  apps: {
    title: '应用访问审计', endpoint: '/historys/loginAppsHistory/fetch',
    search: [{ key: 'username', label: '用户名' }, { key: 'displayName', label: '显示名称' }],
    columns: [
      { key: 'sessionId', label: '会话标识' }, { key: 'username', label: '用户名' },
      { key: 'displayName', label: '显示名称' }, { key: 'appName', label: '应用名称' },
      { key: 'loginTime', label: '访问时间' },
    ],
  },
  systems: {
    title: '系统操作日志', endpoint: '/historys/systemLogs/fetch',
    search: [{ key: 'username', label: '用户名' }, { key: 'displayName', label: '显示名称' }, { key: 'employeeNumber', label: '员工编号' }],
    columns: [
      { key: 'topic', label: '主题' }, { key: 'message', label: '操作内容' },
      { key: 'messageType', label: '操作类型' }, { key: 'messageResult', label: '操作结果' },
      { key: 'displayName', label: '操作人' }, { key: 'executeTime', label: '执行时间' },
    ],
  },
  synchronizers: {
    title: '同步器日志', endpoint: '/historys/synchronizerHistory/fetch',
    search: [{ key: 'syncName', label: '同步器名称' }, { key: 'objectName', label: '对象名称' }],
    columns: [
      { key: 'syncName', label: '同步器名称' }, { key: 'objectName', label: '对象名称' },
      { key: 'syncAction', label: '同步动作' }, { key: 'syncResult', label: '同步结果' },
      { key: 'executeTime', label: '执行时间' },
    ],
  },
  connectors: {
    title: '连接器日志', endpoint: '/historys/connectorHistory/fetch',
    search: [{ key: 'conName', label: '连接器名称' }, { key: 'sourceName', label: '数据源名称' }],
    columns: [
      { key: 'conName', label: '连接器名称' }, { key: 'sourceName', label: '数据源名称' },
      { key: 'objectName', label: '对象名称' }, { key: 'action', label: '操作' },
      { key: 'message', label: '结果信息' }, { key: 'executeTime', label: '执行时间' },
    ],
  },
}

const route = useRoute()
const mode = computed<'portal' | 'admin'>(() => route.path.startsWith('/admin/') ? 'admin' : 'portal')
const auditKey = computed(() => String(route.params.audit || route.meta.audit || 'logins'))
const config = computed(() => configs[auditKey.value] || configs.logins)
const rows = ref<AuditRow[]>([])
const loading = ref(false)
const error = ref('')
const filters = reactive<Record<string, string>>({ startDate: '', endDate: '' })
const page = reactive({ current: 1, pageSize: 10, total: 0 })

function formatDate(value: string) {
  return value ? value.replace('T', ' ') + (value.length === 16 ? ':00' : '') : ''
}

function normalizePage(data: unknown) {
  if (Array.isArray(data)) return { rows: data as AuditRow[], total: data.length }
  const value = (data || {}) as Record<string, unknown>
  const resultRows = Array.isArray(value.rows) ? value.rows as AuditRow[] : []
  const totalValue = typeof value.records === 'number' ? value.records : typeof value.total === 'number' ? value.total : resultRows.length
  return { rows: resultRows, total: totalValue }
}

async function load() {
  loading.value = true
  error.value = ''
  const params: Record<string, unknown> = { pageNumber: page.current, pageSize: page.pageSize }
  config.value.search.forEach(item => { if (filters[item.key]) params[item.key] = filters[item.key] })
  if (filters.startDate) params.startDate = formatDate(filters.startDate)
  if (filters.endDate) params.endDate = formatDate(filters.endDate)
  try {
    const data = mode.value === 'admin'
      ? await adminGet<unknown>(config.value.endpoint, params)
      : await get<unknown>(config.value.endpoint, params)
    const result = normalizePage(data)
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : '审计数据加载失败'
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.keys(filters).forEach(key => { filters[key] = '' })
  page.current = 1
  load()
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  page.current = pagination.current || 1
  page.pageSize = pagination.pageSize || 10
  load()
}

function rowKey(record: AuditRow) {
  return String(record.id || record.sessionId || JSON.stringify(record))
}

watch([auditKey, mode], () => {
  page.current = 1
  load()
}, { immediate: true })
</script>

<template>
  <DefaultLayout :mode="mode">
    <div class="alain-default__content-title"><h1>{{ config.title }}</h1><a-button @click="load">刷新</a-button></div>
    <a-alert v-if="error" type="error" show-icon :message="error" closable @close="error = ''" />
    <a-card class="admin-page-card">
      <a-form layout="inline" @submit.prevent="load">
        <a-form-item v-for="item in config.search" :key="item.key" :label="item.label"><a-input v-model:value="filters[item.key]" allow-clear /></a-form-item>
        <a-form-item label="开始时间"><a-input v-model:value="filters.startDate" type="datetime-local" /></a-form-item>
        <a-form-item label="结束时间"><a-input v-model:value="filters.endDate" type="datetime-local" /></a-form-item>
        <a-form-item><a-space><a-button type="primary" html-type="submit">查询</a-button><a-button @click="reset">重置</a-button></a-space></a-form-item>
      </a-form>
    </a-card>
    <a-card class="admin-page-card">
      <a-table :data-source="rows" :loading="loading" :row-key="rowKey" :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true }" :scroll="{ x: 'max-content' }" @change="onTableChange">
        <a-table-column v-for="column in config.columns" :key="column.key" :title="column.label" :data-index="column.key" />
      </a-table>
    </a-card>
  </DefaultLayout>
</template>
