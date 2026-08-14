<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminPost, adminPut } from '../api'
import { getAdminResource, type ResourceField } from '../adminResources'

interface Row {
  id?: string | number
  [key: string]: unknown
}

interface PageData {
  rows: Row[]
  total: number
}

const route = useRoute()
const resourceKey = computed(() => {
  const value = String(route.params.resource || 'users')
  return ({ session: 'sessions', audit: 'historys', synchronizer: 'synchronizers' } as Record<string, string>)[value] || value
})
const resource = computed(() => getAdminResource(resourceKey.value))
const rows = ref<Row[]>([])
const selectedRowKeys = ref<Array<string | number>>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const page = reactive({ current: 1, pageSize: 10, total: 0 })
const search = reactive<Record<string, unknown>>({})
const form = reactive<Record<string, unknown>>({})
const modalOpen = ref(false)
const editing = ref(false)

const columns = computed(() => resource.value.columns.map(column => ({ ...column, dataIndex: column.key, key: column.key })))
const hasEditor = computed(() => resource.value.fields.length > 0)

function displayValue(row: Row, key: string) {
  const value = row[key]
  if (value === null || value === undefined || value === '') return '—'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function normalizePage(data: unknown): PageData {
  if (Array.isArray(data)) return { rows: data as Row[], total: data.length }
  const value = (data || {}) as Record<string, unknown>
  const rowsValue = Array.isArray(value.rows) ? value.rows : Array.isArray(value.records) ? value.records : []
  return { rows: rowsValue as Row[], total: Number(value.total || value.records || rowsValue.length) }
}

async function loadRows() {
  loading.value = true
  error.value = ''
  try {
    const params: Record<string, unknown> = { pageNumber: page.current, pageSize: page.pageSize }
    resource.value.searchFields?.forEach(key => {
      if (search[key] !== undefined && search[key] !== '') params[key] = search[key]
    })
    const data = await adminGet<unknown>(`${resource.value.fetchPath || `${resource.value.base}/fetch`}`, params)
    const result = normalizePage(data)
    rows.value = result.rows
    page.total = result.total
    selectedRowKeys.value = []
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : '管理数据加载失败'
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  Object.keys(search).forEach(key => delete search[key])
  page.current = 1
  loadRows()
}

function resetForm() {
  Object.keys(form).forEach(key => delete form[key])
  resource.value.fields.forEach(field => {
    if (field.type === 'switch') form[field.key] = false
  })
}

async function openEditor(row?: Row) {
  resetForm()
  editing.value = Boolean(row?.id)
  if (row?.id) {
    try {
      const detail = await adminGet<Row>(`${resource.value.base}/get/${row.id}`)
      Object.assign(form, detail || row)
    } catch {
      Object.assign(form, row)
    }
  }
  modalOpen.value = true
}

function validateFields() {
  const missing = resource.value.fields.find(field => field.required && (form[field.key] === undefined || form[field.key] === ''))
  if (missing) {
    message.warning(`请填写${missing.label}`)
    return false
  }
  return true
}

async function save() {
  if (!validateFields()) return
  saving.value = true
  try {
    if (editing.value) await adminPut(`${resource.value.base}/update`, form)
    else await adminPost(`${resource.value.base}/add`, form)
    message.success(editing.value ? '修改成功' : '新增成功')
    modalOpen.value = false
    await loadRows()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function deleteRows(ids: Array<string | number>) {
  if (!ids.length) return
  Modal.confirm({
    title: '确认删除选中数据？',
    content: '删除操作会立即提交到服务端。',
    async onOk() {
      try {
        await adminDelete(resource.value.deletePath || `${resource.value.base}/delete`, { ids: ids.join(',') })
        message.success('删除成功')
        await loadRows()
      } catch (err) {
        message.error(err instanceof Error ? err.message : '删除失败')
      }
    },
  })
}

function onPageChange(current: number, pageSize: number) {
  page.current = current
  page.pageSize = pageSize
  loadRows()
}

function rowKey(row: Row) {
  return String(row.id || JSON.stringify(row))
}

function onSelectionChange(keys: Array<string | number>) {
  selectedRowKeys.value = keys
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  onPageChange(pagination.current || 1, pagination.pageSize || 10)
}

function fieldComponent(field: ResourceField) {
  if (field.type === 'textarea') return 'a-textarea'
  if (field.type === 'number') return 'a-input-number'
  if (field.type === 'select') return 'a-select'
  if (field.type === 'switch') return 'a-switch'
  return 'a-input'
}

watch(resourceKey, () => {
  page.current = 1
  resetSearch()
  loadRows()
}, { immediate: true })
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title">
      <div><h1>{{ resource.title }}</h1><small>功能接口已接入统一 Vue 应用</small></div>
      <a-space>
        <a-button @click="loadRows">刷新</a-button>
        <a-button v-if="hasEditor" type="primary" @click="openEditor()">新增</a-button>
        <a-button v-if="selectedRowKeys.length" danger @click="deleteRows(selectedRowKeys)">批量删除</a-button>
      </a-space>
    </div>

    <a-alert v-if="error" type="error" show-icon :message="error" closable @close="error = ''" />
    <a-card class="admin-page-card">
      <a-form layout="inline" @submit.prevent="loadRows">
        <a-form-item v-for="key in resource.searchFields" :key="key" :label="key">
          <a-input v-model:value="search[key]" allow-clear @press-enter="loadRows" />
        </a-form-item>
        <a-form-item>
          <a-space><a-button type="primary" html-type="submit">查询</a-button><a-button @click="resetSearch">重置</a-button></a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :title="`${resource.title}列表`" class="admin-page-card">
      <a-table
        :data-source="rows"
        :columns="columns"
        :loading="loading"
        :row-key="rowKey"
        :row-selection="{ selectedRowKeys, onChange: onSelectionChange }"
        :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="String(record.status) === '1' || record.status === '正常' ? 'green' : 'default'">{{ displayValue(record, column.key) }}</a-tag>
          </template>
          <template v-else>{{ displayValue(record, column.key) }}</template>
        </template>
        <template #emptyText><a-empty description="暂无数据" /></template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="editing ? `编辑${resource.title}` : `新增${resource.title}`" :confirm-loading="saving" width="720px" @ok="save">
      <a-form layout="vertical">
        <a-form-item v-for="field in resource.fields" :key="field.key" :label="field.label" :required="field.required">
          <component :is="fieldComponent(field)" v-model:value="form[field.key]" :placeholder="`请输入${field.label}`" :style="{ width: '100%' }">
            <template v-if="field.type === 'select'">
              <a-select-option v-for="option in field.options" :key="String(option.value)" :value="option.value">{{ option.label }}</a-select-option>
            </template>
          </component>
        </a-form-item>
      </a-form>
    </a-modal>
  </DefaultLayout>
</template>
