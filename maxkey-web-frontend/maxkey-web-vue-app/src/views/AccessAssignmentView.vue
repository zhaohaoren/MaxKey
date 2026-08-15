<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminGetLegacyPage, adminPost } from '../api'

interface Row {
  id?: string | number
  groupName?: string
  appName?: string
  category?: string
  protocol?: string
  iconBase64?: string
}

const groups = ref<Row[]>([])
const assignedApps = ref<Row[]>([])
const availableApps = ref<Row[]>([])
const selectedGroupId = ref('')
const selectedAssignedIds = ref<Array<string | number>>([])
const selectedAvailableIds = ref<Array<string | number>>([])
const loading = ref(false)
const modalLoading = ref(false)
const addOpen = ref(false)
const search = reactive({ groupName: '', appName: '', availableAppName: '' })

function normalize(data: unknown): Row[] {
  if (Array.isArray(data)) return data as Row[]
  const value = (data || {}) as Record<string, unknown>
  return Array.isArray(value.rows) ? value.rows as Row[] : Array.isArray(value.records) ? value.records as Row[] : []
}

const selectedGroup = computed(() => groups.value.find(item => String(item.id) === selectedGroupId.value))
const filteredGroups = computed(() => groups.value.filter(item => !search.groupName || String(item.groupName || '').includes(search.groupName)))

async function loadGroups() {
  try {
    groups.value = normalize(await adminGet<unknown>('/access/groups/fetch', { pageNumber: 1, pageSize: 1000 }))
    if (!selectedGroupId.value && groups.value[0]?.id != null) selectedGroupId.value = String(groups.value[0].id)
  } catch (err) {
    message.error(err instanceof Error ? err.message : '用户组加载失败')
  }
}

async function loadAssignedApps() {
  assignedApps.value = []
  selectedAssignedIds.value = []
  if (!selectedGroupId.value) return
  loading.value = true
  try {
    assignedApps.value = normalize(await adminGetLegacyPage<unknown>('/access/access/appsInGroup', {
      groupId: selectedGroupId.value,
      appName: search.appName,
      pageNumber: 1,
      pageSize: 1000,
    }))
  } catch (err) {
    message.error(err instanceof Error ? err.message : '已授权应用加载失败')
  } finally {
    loading.value = false
  }
}

async function loadAvailableApps() {
  modalLoading.value = true
  selectedAvailableIds.value = []
  try {
    availableApps.value = normalize(await adminGetLegacyPage<unknown>('/access/access/appsNotInGroup', {
      groupId: selectedGroupId.value,
      appName: search.availableAppName,
      pageNumber: 1,
      pageSize: 1000,
    }))
  } catch (err) {
    availableApps.value = []
    message.error(err instanceof Error ? err.message : '可授权应用加载失败')
  } finally {
    modalLoading.value = false
  }
}

async function openAdd() {
  if (!selectedGroupId.value) {
    message.warning('请先选择用户组')
    return
  }
  search.availableAppName = ''
  addOpen.value = true
  await loadAvailableApps()
}

async function addApps() {
  if (!selectedAvailableIds.value.length) {
    message.warning('请选择要授权的应用')
    return
  }
  modalLoading.value = true
  try {
    await adminPost('/access/access/add', {
      groupId: selectedGroupId.value,
      groupName: selectedGroup.value?.groupName,
      appId: selectedAvailableIds.value.join(','),
    })
    message.success('应用授权成功')
    addOpen.value = false
    await loadAssignedApps()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '应用授权失败')
  } finally {
    modalLoading.value = false
  }
}

function removeApps(ids: Array<string | number>) {
  if (!ids.length) return
  Modal.confirm({
    title: `确认移除 ${ids.length} 项应用授权？`,
    async onOk() {
      try {
        await adminDelete('/access/access/delete', { ids: ids.join(',') })
        message.success('应用授权已移除')
        await loadAssignedApps()
      } catch (err) {
        message.error(err instanceof Error ? err.message : '移除授权失败')
      }
    },
  })
}

watch(selectedGroupId, loadAssignedApps)
onMounted(loadGroups)
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title">
      <div><h1>访问控制</h1><small>按用户组维护可访问的应用</small></div>
      <a-space>
        <a-button @click="loadAssignedApps">刷新</a-button>
        <a-button type="primary" :disabled="!selectedGroupId" @click="openAdd">添加应用</a-button>
        <a-button danger :disabled="!selectedAssignedIds.length" @click="removeApps(selectedAssignedIds)">批量移除</a-button>
      </a-space>
    </div>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="7">
        <a-card title="用户组" class="admin-page-card">
          <a-input v-model:value="search.groupName" allow-clear placeholder="筛选用户组名称" />
          <a-list :data-source="filteredGroups" size="small">
            <template #renderItem="{ item }">
              <a-list-item class="assignment-parent" :class="{ selected: String(item.id) === selectedGroupId }" @click="selectedGroupId = String(item.id)">
                {{ item.groupName }}
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="17">
        <a-card :title="selectedGroup ? `${selectedGroup.groupName} 的应用授权` : '应用授权'" class="admin-page-card">
          <a-form layout="inline" class="feature-actions" @submit.prevent="loadAssignedApps">
            <a-form-item label="应用名称"><a-input v-model:value="search.appName" allow-clear /></a-form-item>
            <a-button type="primary" html-type="submit" :disabled="!selectedGroupId">查询</a-button>
          </a-form>
          <a-table
            :data-source="assignedApps"
            :loading="loading"
            row-key="id"
            :row-selection="{ selectedRowKeys: selectedAssignedIds, onChange: (keys: Array<string | number>) => selectedAssignedIds = keys }"
            :pagination="{ pageSize: 10 }"
          >
            <a-table-column title="应用名称" data-index="appName" />
            <a-table-column title="分类" data-index="category" />
            <a-table-column title="协议" data-index="protocol" />
            <a-table-column title="操作"><template #default="{ record }"><a-button danger type="link" @click="removeApps([record.id])">移除</a-button></template></a-table-column>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-modal v-model:open="addOpen" title="添加可访问应用" width="780px" :confirm-loading="modalLoading" @ok="addApps">
      <a-form layout="inline" class="feature-actions" @submit.prevent="loadAvailableApps">
        <a-form-item label="应用名称"><a-input v-model:value="search.availableAppName" allow-clear /></a-form-item>
        <a-button type="primary" html-type="submit">查询</a-button>
      </a-form>
      <a-table
        :data-source="availableApps"
        :loading="modalLoading"
        row-key="id"
        :row-selection="{ selectedRowKeys: selectedAvailableIds, onChange: (keys: Array<string | number>) => selectedAvailableIds = keys }"
        :pagination="{ pageSize: 10 }"
      >
        <a-table-column title="应用名称" data-index="appName" />
        <a-table-column title="分类" data-index="category" />
        <a-table-column title="协议" data-index="protocol" />
        <a-table-column title="状态" data-index="status" />
      </a-table>
    </a-modal>
  </DefaultLayout>
</template>
