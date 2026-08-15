<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminPost, adminPut } from '../api'

interface Row {
  id?: string | number
  appName?: string
  resourceName?: string
  resourceType?: string
  resourceUrl?: string
  resourceAction?: string
  parentId?: string
  parentName?: string
  [key: string]: unknown
}

interface TreeNodeData {
  key: string
  title: string
  children?: TreeNodeData[]
}

const route = useRoute()
const apps = ref<Row[]>([])
const resources = ref<Row[]>([])
const resourceTree = ref<TreeNodeData[]>([])
const treeTitles = reactive<Record<string, string>>({})
const appId = ref('')
const selectedTreeKeys = ref<string[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const modalOpen = ref(false)
const editing = ref(false)
const searchName = ref('')
const form = reactive<Row>({})

function rows(data: unknown): Row[] {
  if (Array.isArray(data)) return data as Row[]
  const value = (data || {}) as Record<string, unknown>
  return Array.isArray(value.rows) ? value.rows as Row[] : []
}

function buildTree(data: unknown): TreeNodeData[] {
  const value = (data || {}) as Record<string, unknown>
  const source = Array.isArray(value.nodes) ? value.nodes as Array<Record<string, unknown>> : []
  const root = value.rootNode as Record<string, unknown> | undefined
  Object.keys(treeTitles).forEach(key => delete treeTitles[key])
  const byParent = new Map<string, Array<Record<string, unknown>>>()
  source.forEach(node => {
    const key = String(node.key || '')
    if (key) treeTitles[key] = String(node.title || key)
    const parent = String(node.parentKey || '')
    byParent.set(parent, [...(byParent.get(parent) || []), node])
  })
  const build = (node: Record<string, unknown>, visited: Set<string>): TreeNodeData => {
    const key = String(node.key || '')
    const nextVisited = new Set(visited).add(key)
    const children = (byParent.get(key) || [])
      .filter(child => String(child.key || '') !== key && !nextVisited.has(String(child.key || '')))
      .map(child => build(child, nextVisited))
    return { key, title: String(node.title || key), children: children.length ? children : undefined }
  }
  if (!root?.key) return []
  treeTitles[String(root.key)] = String(root.title || root.key)
  return [build(root, new Set())]
}

const selectedApp = computed(() => apps.value.find(item => String(item.id) === appId.value))

async function loadApps() {
  try {
    apps.value = rows(await adminGet('/apps/fetch', { resourceMgt: 'y', pageNumber: 1, pageSize: 1000 }))
    const queryAppId = String(route.query.appId || '')
    if (queryAppId && apps.value.some(item => String(item.id) === queryAppId)) appId.value = queryAppId
  } catch (err) {
    error.value = err instanceof Error ? err.message : '应用加载失败'
  }
}

async function loadResources() {
  resources.value = []
  resourceTree.value = []
  if (!appId.value) return
  loading.value = true
  error.value = ''
  try {
    const params = {
      appId: appId.value,
      appName: selectedApp.value?.appName,
      parentId: selectedTreeKeys.value[0] || '',
      resourceName: searchName.value,
      pageNumber: 1,
      pageSize: 1000,
    }
    const [listData, treeData] = await Promise.all([
      adminGet('/permissions/resources/fetch', params),
      adminGet('/permissions/resources/tree', { appId: appId.value, appName: selectedApp.value?.appName }),
    ])
    resources.value = rows(listData)
    resourceTree.value = buildTree(treeData)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '资源加载失败'
  } finally {
    loading.value = false
  }
}

function onTreeSelect(keys: Array<string | number>) {
  selectedTreeKeys.value = keys.map(String)
  loadResources()
}

async function openEditor(row?: Row) {
  Object.keys(form).forEach(key => delete form[key])
  editing.value = Boolean(row?.id)
  if (row?.id) {
    try { Object.assign(form, await adminGet<Row>(`/permissions/resources/get/${row.id}`)) } catch { Object.assign(form, row) }
  } else {
    const parentId = selectedTreeKeys.value[0] || appId.value
    Object.assign(form, {
      appId: appId.value,
      appName: selectedApp.value?.appName,
      parentId,
      parentName: treeTitles[parentId] || selectedApp.value?.appName,
      resourceType: 'MENU',
      status: 1,
      sortIndex: 1,
    })
  }
  modalOpen.value = true
}

async function save() {
  if (!form.resourceName || !form.resourceType || !form.appId || !form.parentId) {
    message.warning('请填写资源名称、类型并选择上级资源')
    return
  }
  saving.value = true
  try {
    if (editing.value) await adminPut('/permissions/resources/update', form)
    else await adminPost('/permissions/resources/add', form)
    message.success(editing.value ? '资源修改成功' : '资源新增成功')
    modalOpen.value = false
    await loadResources()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '资源保存失败')
  } finally {
    saving.value = false
  }
}

function remove(row: Row) {
  if (!row.id) return
  Modal.confirm({
    title: `确认删除资源 ${row.resourceName || ''}？`,
    async onOk() {
      try {
        await adminDelete('/permissions/resources/delete', { ids: row.id })
        message.success('资源已删除')
        await loadResources()
      } catch (err) {
        message.error(err instanceof Error ? err.message : '资源删除失败')
      }
    },
  })
}

watch(appId, () => { selectedTreeKeys.value = []; loadResources() })
onMounted(loadApps)
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title">
      <div><h1>资源管理</h1><small>按应用维护层级资源</small></div>
      <a-space><a-button @click="loadResources">刷新</a-button><a-button type="primary" :disabled="!appId" @click="openEditor()">新增资源</a-button></a-space>
    </div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card class="admin-page-card">
      <a-form layout="inline" @submit.prevent="loadResources">
        <a-form-item label="应用" required><a-select v-model:value="appId" show-search option-filter-prop="label" placeholder="请选择应用" style="width: 260px"><a-select-option v-for="item in apps" :key="String(item.id)" :value="String(item.id)" :label="item.appName">{{ item.appName }}</a-select-option></a-select></a-form-item>
        <a-form-item label="资源名称"><a-input v-model:value="searchName" allow-clear /></a-form-item>
        <a-button type="primary" html-type="submit" :disabled="!appId">查询</a-button>
      </a-form>
    </a-card>
    <a-row :gutter="16">
      <a-col :xs="24" :lg="6"><a-card title="资源树" class="admin-page-card"><a-tree block-node default-expand-all :tree-data="resourceTree" :selected-keys="selectedTreeKeys" @select="onTreeSelect" /></a-card></a-col>
      <a-col :xs="24" :lg="18"><a-card :title="selectedApp ? `${selectedApp.appName} 的资源` : '资源列表'" class="admin-page-card"><a-table :data-source="resources" :loading="loading" row-key="id" :pagination="{ pageSize: 10 }"><a-table-column title="资源名称" data-index="resourceName" /><a-table-column title="类型" data-index="resourceType" /><a-table-column title="地址" data-index="resourceUrl" /><a-table-column title="动作" data-index="resourceAction" /><a-table-column title="操作"><template #default="{ record }"><a-button type="link" @click="openEditor(record)">编辑</a-button><a-button danger type="link" @click="remove(record)">删除</a-button></template></a-table-column></a-table></a-card></a-col>
    </a-row>

    <a-modal v-model:open="modalOpen" :title="editing ? '编辑资源' : '新增资源'" width="760px" :confirm-loading="saving" @ok="save">
      <a-form layout="vertical"><a-row :gutter="16">
        <a-col :xs="24" :md="12"><a-form-item label="资源名称" required><a-input v-model:value="form.resourceName" /></a-form-item></a-col>
        <a-col :xs="24" :md="12"><a-form-item label="资源类型" required><a-select v-model:value="form.resourceType"><a-select-option v-for="type in ['MENU', 'PAGE', 'MODULE', 'ELEMENT', 'BUTTON', 'FILE', 'DATA', 'OTHER']" :key="type" :value="type">{{ type }}</a-select-option></a-select></a-form-item></a-col>
        <a-col :xs="24" :md="12"><a-form-item label="上级资源标识" required><a-input v-model:value="form.parentId" /></a-form-item></a-col>
        <a-col :xs="24" :md="12"><a-form-item label="上级资源名称"><a-input v-model:value="form.parentName" /></a-form-item></a-col>
        <a-col :xs="24" :md="12"><a-form-item label="资源地址"><a-input v-model:value="form.resourceUrl" /></a-form-item></a-col>
        <a-col :xs="24" :md="12"><a-form-item label="资源动作"><a-input v-model:value="form.resourceAction" /></a-form-item></a-col>
        <a-col :xs="24" :md="12"><a-form-item label="权限表达式"><a-input v-model:value="form.permission" /></a-form-item></a-col>
        <a-col :xs="24" :md="12"><a-form-item label="资源图标"><a-input v-model:value="form.resourceIcon" /></a-form-item></a-col>
        <a-col :xs="24" :md="12"><a-form-item label="资源样式"><a-input v-model:value="form.resourceStyle" /></a-form-item></a-col>
        <a-col :xs="24" :md="6"><a-form-item label="排序"><a-input-number v-model:value="form.sortIndex" style="width: 100%" /></a-form-item></a-col>
        <a-col :xs="24" :md="6"><a-form-item label="状态"><a-select v-model:value="form.status"><a-select-option :value="1">启用</a-select-option><a-select-option :value="0">停用</a-select-option></a-select></a-form-item></a-col>
      </a-row></a-form>
    </a-modal>
  </DefaultLayout>
</template>
