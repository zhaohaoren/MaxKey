<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminGet, adminPut } from '../api'

interface OptionRow {
  id?: string
  appName?: string
  name?: string
  roleName?: string
  resourceName?: string
  resourceId?: string
}

const apps = ref<OptionRow[]>([])
const roles = ref<OptionRow[]>([])
const resources = ref<OptionRow[]>([])
const appId = ref('')
const roleId = ref('')
const selectedResourceIds = ref<string[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const ready = computed(() => Boolean(appId.value && roleId.value))

function rows(data: unknown): OptionRow[] {
  if (Array.isArray(data)) return data as OptionRow[]
  const value = (data || {}) as Record<string, unknown>
  return Array.isArray(value.rows) ? value.rows as OptionRow[] : []
}

async function loadOptions() {
  loading.value = true
  try {
    const [appData, roleData] = await Promise.all([
      adminGet<unknown>('/apps/fetch', { pageNumber: 1, pageSize: 1000 }),
      adminGet<unknown>('/permissions/roles/fetch', { pageNumber: 1, pageSize: 1000 }),
    ])
    apps.value = rows(appData)
    roles.value = rows(roleData)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '应用和角色加载失败'
  } finally {
    loading.value = false
  }
}

async function loadAssignments() {
  selectedResourceIds.value = []
  resources.value = []
  if (!ready.value) return
  loading.value = true
  error.value = ''
  try {
    const [resourceData, assigned] = await Promise.all([
      adminGet<unknown>('/permissions/resources/fetch', { appId: appId.value, pageNumber: 1, pageSize: 1000 }),
      adminGet<OptionRow[]>('/permissions/permissionRole/get', { appId: appId.value, roleId: roleId.value }),
    ])
    resources.value = rows(resourceData)
    selectedResourceIds.value = (Array.isArray(assigned) ? assigned : []).map(item => String(item.resourceId || item.id || '')).filter(Boolean)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '角色权限加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!ready.value) return
  saving.value = true
  try {
    await adminPut('/permissions/permissionRole/update', { appId: appId.value, roleId: roleId.value, resourceId: selectedResourceIds.value.join(',') })
    message.success('角色权限保存成功')
    await loadAssignments()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '角色权限保存失败')
  } finally {
    saving.value = false
  }
}

watch([appId, roleId], loadAssignments)
onMounted(loadOptions)
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><div><h1>角色权限关联</h1><small>按应用和角色分配可访问资源</small></div><a-button @click="loadAssignments">刷新</a-button></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card class="admin-page-card">
      <a-spin :spinning="loading">
        <a-form layout="vertical">
          <a-row :gutter="16">
            <a-col :xs="24" :md="12"><a-form-item label="应用" required><a-select v-model:value="appId" show-search option-filter-prop="label" placeholder="请选择应用"><a-select-option v-for="item in apps" :key="String(item.id)" :value="String(item.id)" :label="item.appName || item.name">{{ item.appName || item.name }}</a-select-option></a-select></a-form-item></a-col>
            <a-col :xs="24" :md="12"><a-form-item label="角色" required><a-select v-model:value="roleId" show-search option-filter-prop="label" placeholder="请选择角色"><a-select-option v-for="item in roles" :key="String(item.id)" :value="String(item.id)" :label="item.roleName || item.name">{{ item.roleName || item.name }}</a-select-option></a-select></a-form-item></a-col>
          </a-row>
          <a-form-item label="授权资源"><a-select v-model:value="selectedResourceIds" mode="multiple" show-search option-filter-prop="label" :disabled="!ready" placeholder="请选择资源"><a-select-option v-for="item in resources" :key="String(item.id)" :value="String(item.id)" :label="item.name || item.resourceName">{{ item.name || item.resourceName || item.id }}</a-select-option></a-select></a-form-item>
          <a-button type="primary" :disabled="!ready" :loading="saving" @click="save">保存权限</a-button>
        </a-form>
      </a-spin>
    </a-card>
  </DefaultLayout>
</template>
