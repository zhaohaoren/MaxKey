<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminPost, adminPostFormData, adminPut } from '../api'
import { getAdminResource, type ResourceField } from '../adminResources'

interface Row {
  id?: string | number
  [key: string]: unknown
}

interface PageData {
  rows: Row[]
  total: number
}

interface TreeNodeData {
  key: string
  title: string
  children?: TreeNodeData[]
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
const userActionOpen = ref(false)
const userAction = ref<'password' | 'mfa'>('password')
const userActionForm = reactive<Record<string, unknown>>({})
const mappingOpen = ref(false)
const mappingEditorOpen = ref(false)
const mappingRows = ref<Row[]>([])
const mappingJobId = ref('')
const mappingForm = reactive<Record<string, unknown>>({})
const treeData = ref<TreeNodeData[]>([])
const treeLoading = ref(false)
const selectedTreeKeys = ref<string[]>([])
const treeTitles = reactive<Record<string, string>>({})
const importOpen = ref(false)
const importFile = ref<File>()
const importUpdateExist = ref('0')
const importing = ref(false)

const columns = computed(() => [
  ...resource.value.columns.map(column => ({ ...column, dataIndex: column.key, key: column.key })),
  { key: 'actions', label: '操作', title: '操作', fixed: 'right' as const, width: 240 },
])
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
  const total = typeof value.total === 'number' ? value.total : typeof value.records === 'number' ? value.records : rowsValue.length
  return { rows: rowsValue as Row[], total }
}

async function loadRows() {
  loading.value = true
  error.value = ''
  try {
    if (resource.value.singleton) {
      const detail = await adminGet<Row>(`${resource.value.base}/get`)
      rows.value = detail ? [detail] : []
      page.total = rows.value.length
      return
    }
    const params: Record<string, unknown> = { pageNumber: page.current, pageSize: page.pageSize }
    if (resource.value.tree && selectedTreeKeys.value[0]) {
      params[resource.value.treeFilterKey || 'parentId'] = selectedTreeKeys.value[0]
    }
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

function normalizeTree(data: unknown): TreeNodeData[] {
  const value = (data || {}) as Record<string, unknown>
  const sourceNodes = Array.isArray(value.nodes) ? value.nodes as Array<Record<string, unknown>> : []
  const root = value.rootNode as Record<string, unknown> | undefined
  Object.keys(treeTitles).forEach(key => delete treeTitles[key])

  const byParent = new Map<string, Array<Record<string, unknown>>>()
  sourceNodes.forEach(node => {
    const key = String(node.key || '')
    if (key) treeTitles[key] = String(node.title || key)
    const parentKey = String(node.parentKey || '')
    const children = byParent.get(parentKey) || []
    children.push(node)
    byParent.set(parentKey, children)
  })

  const build = (node: Record<string, unknown>, visited: Set<string>): TreeNodeData => {
    const key = String(node.key || '')
    const nextVisited = new Set(visited).add(key)
    const children = (byParent.get(key) || [])
      .filter(child => String(child.key || '') !== key && !nextVisited.has(String(child.key || '')))
      .map(child => build(child, nextVisited))
    return { key, title: String(node.title || key), children: children.length ? children : undefined }
  }

  if (root?.key) {
    treeTitles[String(root.key)] = String(root.title || root.key)
    return [build(root, new Set())]
  }
  const keys = new Set(sourceNodes.map(node => String(node.key || '')))
  return sourceNodes.filter(node => !keys.has(String(node.parentKey || ''))).map(node => build(node, new Set()))
}

async function loadTree() {
  if (!resource.value.tree) {
    treeData.value = []
    return
  }
  treeLoading.value = true
  try {
    treeData.value = normalizeTree(await adminGet('/orgs/tree'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : '组织树加载失败')
  } finally {
    treeLoading.value = false
  }
}

function onTreeSelect(keys: Array<string | number>) {
  selectedTreeKeys.value = keys.map(String)
  page.current = 1
  loadRows()
}

async function openEditor(row?: Row) {
  resetForm()
  if (!row && selectedTreeKeys.value[0]) {
    if (resource.value.key === 'orgs') form.parentId = selectedTreeKeys.value[0]
    if (resource.value.key === 'users') {
      form.departmentId = selectedTreeKeys.value[0]
      form.department = treeTitles[selectedTreeKeys.value[0]]
    }
  }
  editing.value = resource.value.singleton || Boolean(row?.id)
  if (resource.value.singleton) {
    try {
      Object.assign(form, await adminGet<Row>(`${resource.value.base}/get`))
    } catch {
      if (row) Object.assign(form, row)
    }
  } else if (row?.id) {
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
    if (editing.value || resource.value.singleton) await adminPut(`${resource.value.base}/update`, form)
    else await adminPost(`${resource.value.base}/add`, form)
    message.success(editing.value ? '修改成功' : '新增成功')
    modalOpen.value = false
    await loadRows()
    if (resource.value.tree) await loadTree()
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
        if (resource.value.tree) await loadTree()
      } catch (err) {
        message.error(err instanceof Error ? err.message : '删除失败')
      }
    },
  })
}

async function updateStatus(row: Row, status: number) {
  if (!row.id) return
  try {
    await adminGet(`${resource.value.base}/updateStatus`, { id: row.id, status })
    message.success(status === 1 ? '已启用' : status === 5 ? '已锁定' : '已停用')
    await loadRows()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '状态更新失败')
  }
}

async function testLdap() {
  saving.value = true
  try {
    await adminGet('/config/ldapcontext/test')
    message.success('LDAP 连接测试成功')
  } catch (err) {
    message.error(err instanceof Error ? err.message : 'LDAP 连接测试失败')
  } finally {
    saving.value = false
  }
}

function onImportFile(event: Event) {
  importFile.value = (event.target as HTMLInputElement).files?.[0]
}

async function importData() {
  if (!importFile.value) {
    message.warning('请选择 Excel 文件')
    return
  }
  importing.value = true
  try {
    const data = new FormData()
    data.append('excelFile', importFile.value)
    data.append('updateExist', importUpdateExist.value)
    await adminPostFormData(`${resource.value.base}/import`, data)
    message.success(`${resource.value.title}导入成功`)
    importOpen.value = false
    await loadRows()
    if (resource.value.tree) await loadTree()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '导入失败')
  } finally {
    importing.value = false
  }
}

function deleteRow(row: Row) {
  if (row.id === undefined || row.id === null) {
    message.warning('当前记录缺少可操作的标识')
    return
  }
  deleteRows([row.id])
}

async function openUserAction(row: Row, action: 'password' | 'mfa') {
  Object.keys(userActionForm).forEach(key => delete userActionForm[key])
  userAction.value = action
  Object.assign(userActionForm, { id: row.id, userId: row.id, username: row.username, displayName: row.displayName })
  if (action === 'mfa' && row.id) {
    try { Object.assign(userActionForm, await adminGet<Row>(`/users/get/${row.id}`)) } catch { /* 列表数据足以继续设置。 */ }
  }
  userActionOpen.value = true
}

async function generatePassword() {
  try {
    const password = await adminGet<string>('/users/randomPassword')
    userActionForm.password = password
    userActionForm.confirmPassword = password
  } catch (err) {
    message.error(err instanceof Error ? err.message : '随机密码生成失败')
  }
}

async function saveUserAction() {
  saving.value = true
  try {
    if (userAction.value === 'password') {
      if (!userActionForm.password || userActionForm.password !== userActionForm.confirmPassword) {
        message.warning('两次密码输入不一致')
        return
      }
      await adminPut('/users/changePassword', userActionForm)
    } else {
      await adminPut('/users/updateAuthnType', userActionForm)
    }
    message.success('用户安全设置保存成功')
    userActionOpen.value = false
  } catch (err) {
    message.error(err instanceof Error ? err.message : '用户安全设置保存失败')
  } finally {
    saving.value = false
  }
}

async function runSynchronizer(row: Row) {
  if (!row.id) return
  try { await adminGet('/config/synchronizers/synchr', { id: row.id }); message.success('同步任务已提交') } catch (err) { message.error(err instanceof Error ? err.message : '同步任务执行失败') }
}

async function loadMappings() {
  try {
    const data = await adminGet<unknown>(`/config/synchronizers/mapping-list/${mappingJobId.value}`)
    mappingRows.value = Array.isArray(data) ? data as Row[] : []
  } catch (err) { message.error(err instanceof Error ? err.message : '字段映射加载失败') }
}

async function openMappings(row: Row) {
  if (!row.id) return
  mappingJobId.value = String(row.id)
  mappingOpen.value = true
  await loadMappings()
}

async function editMapping(row?: Row) {
  Object.keys(mappingForm).forEach(key => delete mappingForm[key])
  if (row?.id) {
    try { Object.assign(mappingForm, await adminGet<Row>(`/config/synchronizers/mapping-get/${row.id}`)) } catch { Object.assign(mappingForm, row) }
  } else {
    Object.assign(mappingForm, { jobId: mappingJobId.value, objectType: '1', status: 1 })
  }
  mappingEditorOpen.value = true
}

async function saveMapping() {
  if (!mappingForm.sourceField || !mappingForm.targetField) { message.warning('请填写源字段和目标字段'); return }
  saving.value = true
  try {
    mappingForm.jobId = mappingJobId.value
    if (mappingForm.id) await adminPut('/config/synchronizers/mapping-update', mappingForm)
    else await adminPost('/config/synchronizers/mapping-add', mappingForm)
    message.success('字段映射保存成功'); mappingEditorOpen.value = false; await loadMappings()
  } catch (err) { message.error(err instanceof Error ? err.message : '字段映射保存失败') } finally { saving.value = false }
}

function deleteMapping(row: Row) {
  if (!row.id) return
  Modal.confirm({ title: '确认删除字段映射？', async onOk() { try { await adminGet(`/config/synchronizers/mapping-delete/${row.id}`); message.success('字段映射已删除'); await loadMappings() } catch (err) { message.error(err instanceof Error ? err.message : '删除失败') } } })
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

watch(resourceKey, async () => {
  page.current = 1
  Object.keys(search).forEach(key => delete search[key])
  selectedTreeKeys.value = []
  await loadTree()
  await loadRows()
}, { immediate: true })
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title">
      <div><h1>{{ resource.title }}</h1><small>功能接口已接入统一 Vue 应用</small></div>
      <a-space>
        <a-button @click="loadRows">刷新</a-button>
        <a-button v-if="resource.importable" @click="importFile = undefined; importOpen = true">导入</a-button>
        <a-button v-if="resource.key === 'ldapcontext'" :loading="saving" @click="testLdap">测试连接</a-button>
        <a-button v-if="hasEditor && !resource.singleton" type="primary" @click="openEditor()">新增</a-button>
        <a-button v-if="selectedRowKeys.length && !resource.singleton" danger @click="deleteRows(selectedRowKeys)">批量删除</a-button>
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

    <a-row :gutter="16">
      <a-col v-if="resource.tree" :xs="24" :lg="6">
        <a-card title="组织树" class="admin-page-card">
          <a-spin :spinning="treeLoading">
            <a-tree
              block-node
              default-expand-all
              :tree-data="treeData"
              :selected-keys="selectedTreeKeys"
              @select="onTreeSelect"
            />
          </a-spin>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="resource.tree ? 18 : 24">
        <a-card :title="`${resource.title}列表`" class="admin-page-card">
          <a-table
            :data-source="rows"
            :columns="columns"
            :loading="loading"
            :row-key="rowKey"
            :row-selection="resource.singleton ? undefined : { selectedRowKeys, onChange: onSelectionChange }"
            :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="String(record.status) === '1' || record.status === '正常' ? 'green' : 'default'">{{ displayValue(record, column.key) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-space wrap>
                  <a-button v-if="hasEditor" type="link" size="small" @click="openEditor(record)">编辑</a-button>
                  <a-button v-if="resource.key === 'users'" type="link" size="small" @click="openUserAction(record, 'password')">密码</a-button>
                  <a-button v-if="resource.key === 'users'" type="link" size="small" @click="openUserAction(record, 'mfa')">MFA</a-button>
                  <a-button v-if="['users', 'accounts'].includes(resource.key) && String(record.status) !== '1'" type="link" size="small" @click="updateStatus(record, 1)">启用</a-button>
                  <a-button v-if="['users', 'accounts'].includes(resource.key) && String(record.status) === '1'" type="link" size="small" @click="updateStatus(record, 4)">停用</a-button>
                  <a-button v-if="resource.key === 'users' && String(record.status) === '1'" type="link" size="small" @click="updateStatus(record, 5)">锁定</a-button>
                  <a-button v-if="resource.key === 'synchronizers'" type="link" size="small" @click="runSynchronizer(record)">执行</a-button>
                  <a-button v-if="resource.key === 'synchronizers'" type="link" size="small" @click="openMappings(record)">字段映射</a-button>
                  <a-button v-if="!resource.singleton" danger type="link" size="small" @click="deleteRow(record)">{{ resource.key === 'sessions' ? '终止' : '删除' }}</a-button>
                </a-space>
              </template>
              <template v-else>{{ displayValue(record, column.key) }}</template>
            </template>
            <template #emptyText><a-empty description="暂无数据" /></template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

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

    <a-modal v-model:open="userActionOpen" :title="userAction === 'password' ? '重置用户密码' : '设置用户二次认证'" :confirm-loading="saving" @ok="saveUserAction">
      <a-form layout="vertical">
        <a-form-item label="用户"><a-input :value="String(userActionForm.displayName || userActionForm.username || '')" disabled /></a-form-item>
        <template v-if="userAction === 'password'">
          <a-form-item label="新密码" required><a-input-password v-model:value="userActionForm.password" /></a-form-item>
          <a-form-item label="确认密码" required><a-input-password v-model:value="userActionForm.confirmPassword" /></a-form-item>
          <a-button @click="generatePassword">生成随机密码</a-button>
        </template>
        <a-form-item v-else label="认证方式"><a-radio-group v-model:value="userActionForm.authnType" button-style="solid"><a-radio-button value="0">关闭</a-radio-button><a-radio-button value="1">短信</a-radio-button><a-radio-button value="2">动态口令</a-radio-button><a-radio-button value="3">邮件</a-radio-button></a-radio-group></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="importOpen" :title="`导入${resource.title}`" :confirm-loading="importing" @ok="importData">
      <a-alert type="info" show-icon message="支持 .xls 和 .xlsx；数据列需使用 MaxKey 导入模板格式。" class="feature-actions" />
      <a-form layout="vertical">
        <a-form-item label="Excel 文件" required><input type="file" accept=".xls,.xlsx" @change="onImportFile" /></a-form-item>
        <a-form-item label="已存在数据"><a-radio-group v-model:value="importUpdateExist"><a-radio value="0">跳过</a-radio><a-radio value="1">更新</a-radio></a-radio-group></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="mappingOpen" title="同步字段映射" width="820px" :footer="null">
      <a-button type="primary" class="feature-actions" @click="editMapping()">新增映射</a-button>
      <a-table :data-source="mappingRows" row-key="id" :pagination="false"><a-table-column title="源字段" data-index="sourceField" /><a-table-column title="目标字段" data-index="targetField" /><a-table-column title="对象类型"><template #default="{ record }">{{ String(record.objectType) === '1' ? '用户' : '组织' }}</template></a-table-column><a-table-column title="说明" data-index="description" /><a-table-column title="操作"><template #default="{ record }"><a-button type="link" @click="editMapping(record)">编辑</a-button><a-button danger type="link" @click="deleteMapping(record)">删除</a-button></template></a-table-column></a-table>
    </a-modal>
    <a-modal v-model:open="mappingEditorOpen" :title="mappingForm.id ? '编辑字段映射' : '新增字段映射'" :confirm-loading="saving" @ok="saveMapping"><a-form layout="vertical"><a-form-item label="源字段" required><a-input v-model:value="mappingForm.sourceField" /></a-form-item><a-form-item label="目标字段" required><a-input v-model:value="mappingForm.targetField" /></a-form-item><a-form-item label="对象类型"><a-select v-model:value="mappingForm.objectType"><a-select-option value="1">用户</a-select-option><a-select-option value="2">组织</a-select-option></a-select></a-form-item><a-form-item label="说明"><a-textarea v-model:value="mappingForm.description" /></a-form-item></a-form></a-modal>
  </DefaultLayout>
</template>
