<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminPost } from '../api'

interface Row { id?: string; name?: string; groupName?: string; roleName?: string; username?: string; displayName?: string; memberName?: string; type?: string }

const route = useRoute()
const kind = computed<'group' | 'role'>(() => String(route.meta.assignment) === 'role' ? 'role' : 'group')
const username = computed(() => String(route.query.username || ''))
const userMode = computed(() => kind.value === 'group' && Boolean(username.value))
const parentBase = computed(() => kind.value === 'group' ? '/access/groups' : '/permissions/roles')
const memberBase = computed(() => kind.value === 'group' ? '/access/groupmembers' : '/permissions/rolemembers')
const parentKey = computed(() => kind.value === 'group' ? 'groupId' : 'roleId')
const parentNameKey = computed(() => kind.value === 'group' ? 'groupName' : 'roleName')
const title = computed(() => kind.value === 'group' ? '用户组成员' : '角色成员')
const parents = ref<Row[]>([])
const members = ref<Row[]>([])
const available = ref<Row[]>([])
const selectedParentId = ref('')
const selectedAvailableIds = ref<string[]>([])
const loading = ref(false)
const modalLoading = ref(false)
const modalOpen = ref(false)
const search = reactive({ parentName: '', username: '' })

function normalize(data: unknown) {
  if (Array.isArray(data)) return data as Row[]
  const value = (data || {}) as Record<string, unknown>
  return Array.isArray(value.rows) ? value.rows as Row[] : []
}

const selectedParent = computed(() => parents.value.find(item => String(item.id) === selectedParentId.value))

async function loadParents() {
  try {
    if (userMode.value) {
      parents.value = normalize(await adminGet<unknown>('/access/groupmembers/noMember', { username: username.value, groupName: search.parentName, pageNumber: 1, pageSize: 1000 }))
    } else {
      parents.value = normalize(await adminGet<unknown>(`${parentBase.value}/fetch`, { pageNumber: 1, pageSize: 1000 }))
    }
  } catch (err) { message.error(err instanceof Error ? err.message : `${title.value}加载失败`) }
}

async function loadMembers() {
  members.value = []
  if (!userMode.value && !selectedParentId.value) return
  loading.value = true
  try {
    const endpoint = kind.value === 'group' ? 'memberIn' : 'memberInRole'
    members.value = normalize(await adminGet<unknown>(userMode.value ? '/access/groupmembers/fetch' : `${memberBase.value}/${endpoint}`, userMode.value ? { username: username.value, pageNumber: 1, pageSize: 1000 } : { [parentKey.value]: selectedParentId.value, username: search.username, pageNumber: 1, pageSize: 1000 }))
  } catch (err) { message.error(err instanceof Error ? err.message : '成员加载失败') } finally { loading.value = false }
}

async function openAdd() {
  if (!userMode.value && !selectedParentId.value) { message.warning(`请先选择${kind.value === 'group' ? '用户组' : '角色'}`); return }
  modalOpen.value = true; modalLoading.value = true; selectedAvailableIds.value = []
  try {
    const endpoint = kind.value === 'group' ? 'memberNotIn' : 'memberNotInRole'
    available.value = normalize(await adminGet<unknown>(userMode.value ? '/access/groupmembers/noMember' : `${memberBase.value}/${endpoint}`, userMode.value ? { username: username.value, groupName: search.parentName, pageNumber: 1, pageSize: 1000 } : { [parentKey.value]: selectedParentId.value, pageNumber: 1, pageSize: 1000 }))
  } catch (err) { message.error(err instanceof Error ? err.message : '可选用户加载失败') } finally { modalLoading.value = false }
}

async function addMembers() {
  const selected = available.value.filter(item => item.id && selectedAvailableIds.value.includes(String(item.id)))
  if (!selected.length) { message.warning('请选择要添加的用户'); return }
  modalLoading.value = true
  try {
    if (userMode.value) {
      await adminPost('/access/groupmembers/addMember2Groups', { username: username.value, groupId: selected.map(item => item.id).join(','), groupName: selected.map(item => item.groupName || item.name || '').join(',') })
    } else {
      await adminPost(`${memberBase.value}/add`, {
        type: 'USER', [parentKey.value]: selectedParentId.value, [parentNameKey.value]: selectedParent.value?.name || selectedParent.value?.[parentNameKey.value],
        memberId: selected.map(item => item.id).join(','), memberName: selected.map(item => item.username || item.displayName || item.memberName || '').join(','),
      })
    }
    message.success('成员添加成功'); modalOpen.value = false; await loadMembers()
  } catch (err) { message.error(err instanceof Error ? err.message : '成员添加失败') } finally { modalLoading.value = false }
}

function removeMember(row: Row) {
  if (!row.id) return
  Modal.confirm({ title: '确认移除该成员？', async onOk() { try { await adminDelete(`${memberBase.value}/delete`, { ids: row.id }); message.success('成员已移除'); await loadMembers() } catch (err) { message.error(err instanceof Error ? err.message : '移除失败') } } })
}

watch([kind, selectedParentId, username], () => { loadParents(); loadMembers() })
onMounted(() => { loadParents(); loadMembers() })
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><div><h1>{{ userMode ? '用户所属用户组' : title }}</h1><small>{{ userMode ? `维护 ${username} 的用户组` : `选择${kind === 'group' ? '用户组' : '角色'}后维护成员` }}</small></div><a-space><a-button @click="userMode ? loadMembers() : loadMembers()">刷新</a-button><a-button type="primary" :disabled="!userMode && !selectedParentId" @click="openAdd">{{ userMode ? '添加用户组' : '添加成员' }}</a-button></a-space></div>
    <a-row :gutter="16"><a-col v-if="!userMode" :xs="24" :lg="7"><a-card :title="kind === 'group' ? '用户组' : '角色'"><a-input v-model:value="search.parentName" allow-clear placeholder="筛选名称" /><a-list :data-source="parents.filter(item => !search.parentName || String(item.name || item[parentNameKey] || '').includes(search.parentName))" size="small"><template #renderItem="{ item }"><a-list-item class="assignment-parent" :class="{ selected: String(item.id) === selectedParentId }" @click="selectedParentId = String(item.id)">{{ item.name || item[parentNameKey] }}</a-list-item></template></a-list></a-card></a-col>
      <a-col :xs="24" :lg="userMode ? 24 : 17"><a-card :title="userMode ? `${username} 的用户组` : (selectedParent ? `${selectedParent.name || selectedParent[parentNameKey]} 的成员` : '成员列表')"><a-form v-if="!userMode" layout="inline" @submit.prevent="loadMembers"><a-form-item label="用户名"><a-input v-model:value="search.username" allow-clear /></a-form-item><a-button type="primary" html-type="submit" :disabled="!selectedParentId">查询</a-button></a-form><a-table :data-source="members" :loading="loading" row-key="id" :pagination="false"><a-table-column v-if="userMode" title="用户组" data-index="groupName" /><a-table-column v-if="!userMode" title="用户名" data-index="username" /><a-table-column v-if="!userMode" title="显示名称"><template #default="{ record }">{{ record.displayName || record.memberName }}</template></a-table-column><a-table-column title="类型" data-index="type" /><a-table-column title="操作"><template #default="{ record }"><a-button danger type="link" @click="removeMember(record)">移除</a-button></template></a-table-column></a-table></a-card></a-col></a-row>
    <a-modal v-model:open="modalOpen" title="添加成员" width="760px" :confirm-loading="modalLoading" @ok="addMembers"><a-table :data-source="available" :loading="modalLoading" row-key="id" :row-selection="{ selectedRowKeys: selectedAvailableIds, onChange: (keys: string[]) => selectedAvailableIds = keys.map(String) }" :pagination="{ pageSize: 10 }"><a-table-column title="用户名" data-index="username" /><a-table-column title="显示名称" data-index="displayName" /><a-table-column title="邮箱" data-index="email" /></a-table></a-modal>
  </DefaultLayout>
</template>
