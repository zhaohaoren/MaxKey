<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminGet, adminPut } from '../api'

interface Row {
  id?: string | number
  appName?: string
  groupName?: string
  resourceId?: string
}

interface TreeNodeData {
  key: string
  title: string
  children?: TreeNodeData[]
}

const apps = ref<Row[]>([])
const route = useRoute()
const groups = ref<Row[]>([])
const appId = ref('')
const groupId = ref('')
const resourceTree = ref<TreeNodeData[]>([])
const checkedResourceIds = ref<string[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')

function rows(data: unknown): Row[] {
  if (Array.isArray(data)) return data as Row[]
  const value = (data || {}) as Record<string, unknown>
  return Array.isArray(value.rows) ? value.rows as Row[] : []
}

function buildTree(data: unknown): TreeNodeData[] {
  const value = (data || {}) as Record<string, unknown>
  const source = Array.isArray(value.nodes) ? value.nodes as Array<Record<string, unknown>> : []
  const root = value.rootNode as Record<string, unknown> | undefined
  const byParent = new Map<string, Array<Record<string, unknown>>>()
  source.forEach(node => {
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
  return root?.key ? [build(root, new Set())] : []
}

const selectedApp = computed(() => apps.value.find(item => String(item.id) === appId.value))

async function loadOptions() {
  loading.value = true
  try {
    const [appData, groupData] = await Promise.all([
      adminGet<unknown>('/apps/fetch', { resourceMgt: 'y', pageNumber: 1, pageSize: 1000 }),
      adminGet<unknown>('/access/groups/fetch', { pageNumber: 1, pageSize: 1000 }),
    ])
    apps.value = rows(appData)
    groups.value = rows(groupData)
    const queryAppId = String(route.query.appId || '')
    if (queryAppId && apps.value.some(item => String(item.id) === queryAppId)) appId.value = queryAppId
  } catch (err) {
    error.value = err instanceof Error ? err.message : '应用和用户组加载失败'
  } finally {
    loading.value = false
  }
}

async function loadPermission() {
  resourceTree.value = []
  checkedResourceIds.value = []
  if (!appId.value) return
  loading.value = true
  error.value = ''
  try {
    const [treeData, assigned] = await Promise.all([
      adminGet('/permissions/resources/tree', { appId: appId.value, appName: selectedApp.value?.appName }),
      groupId.value ? adminGet<Row[]>('/permissions/permission/get', { appId: appId.value, groupId: groupId.value }) : Promise.resolve([]),
    ])
    resourceTree.value = buildTree(treeData)
    checkedResourceIds.value = rows(assigned).map(item => String(item.resourceId || '')).filter(Boolean)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '权限数据加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!appId.value || !groupId.value) {
    message.warning('请选择应用和用户组')
    return
  }
  saving.value = true
  try {
    await adminPut('/permissions/permission/update', {
      appId: appId.value,
      groupId: groupId.value,
      resourceId: checkedResourceIds.value.join(','),
    })
    message.success('用户组权限保存成功')
    await loadPermission()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '权限保存失败')
  } finally {
    saving.value = false
  }
}

watch([appId, groupId], loadPermission)
onMounted(loadOptions)
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title">
      <div><h1>用户组权限</h1><small>按应用为用户组分配资源访问权限</small></div>
      <a-space><a-button @click="loadPermission">刷新</a-button><a-button type="primary" :disabled="!appId || !groupId" :loading="saving" @click="save">保存权限</a-button></a-space>
    </div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card class="admin-page-card">
      <a-spin :spinning="loading">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="应用" required>
              <a-select v-model:value="appId" show-search option-filter-prop="label" placeholder="请选择应用">
                <a-select-option v-for="item in apps" :key="String(item.id)" :value="String(item.id)" :label="item.appName">{{ item.appName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="用户组" required>
              <a-select v-model:value="groupId" show-search option-filter-prop="label" placeholder="请选择用户组">
                <a-select-option v-for="item in groups" :key="String(item.id)" :value="String(item.id)" :label="item.groupName">{{ item.groupName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider>资源权限</a-divider>
        <a-empty v-if="!appId" description="请先选择应用" />
        <a-tree v-else v-model:checked-keys="checkedResourceIds" checkable default-expand-all :tree-data="resourceTree" />
      </a-spin>
    </a-card>
  </DefaultLayout>
</template>
