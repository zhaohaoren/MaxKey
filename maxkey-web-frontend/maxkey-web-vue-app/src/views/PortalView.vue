<script setup lang="ts">
import {
  AppstoreOutlined,
  ArrowDownOutlined,
  ArrowRightOutlined,
  ArrowUpOutlined,
  CheckCircleFilled,
  DeleteOutlined,
  EditOutlined,
  FolderOutlined,
  MailOutlined,
  MoreOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  UserAddOutlined,
} from '@ant-design/icons-vue'
import { Modal, message } from 'ant-design-vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DefaultLayout from '../components/DefaultLayout.vue'
import AccountCredentialModal from '../components/AccountCredentialModal.vue'
import { get } from '../api'
import { auth } from '../auth'
import { LOGIN_WELCOME_KEY } from '../authRedirect'

interface AppItem {
  id?: string
  name?: string
  appName?: string
  iconBase64?: string
  loginUrl?: string
  protocol?: string
  inducer?: string
  category?: string
}

interface AppGroup {
  id: string
  name: string
  appIds: string[]
}

interface AppLayout {
  version: 1
  groups: AppGroup[]
  ungroupedIds: string[]
}

interface DisplayGroup extends AppGroup {
  isUngrouped: boolean
  items: AppItem[]
}

const UNGROUPED_ID = '__ungrouped__'
const LAYOUT_KEY_PREFIX = 'snowx_portal_app_layout:'

const { t } = useI18n({ useScope: 'global' })
const apps = ref<AppItem[]>([])
const loading = ref(true)
const error = ref('')
const selectedCategory = ref<string | undefined>('All')
const searchKeyword = ref('')
const credentialOpen = ref(false)
const credentialAppId = ref('')
const welcomeVisible = ref(false)
const appLayout = ref<AppLayout>({ version: 1, groups: [], ungroupedIds: [] })
const draggedAppId = ref('')
const dragOverGroupId = ref('')
const groupModalOpen = ref(false)
const groupModalMode = ref<'create' | 'rename'>('create')
const editingGroupId = ref('')
const groupName = ref('')
let suppressCardClickUntil = 0
let welcomeTimer: number | undefined
const categoryCodes = [
  'none', '1011', '1012', '1013', '1014', '1015', '1016', '1017',
  '1111', '1112', '1113', '1114', '1211', '1212', '1213', '1214',
  '1215', '1311', '1411', '1511', '1512', '1611', '1711', '1712',
  '1811', '1812', '1911', '1912',
]

const userName = computed(() => auth.token.value?.displayName || auth.token.value?.username || '')
const layoutOwner = computed(() => String(auth.token.value?.id || auth.token.value?.username || 'anonymous'))
const layoutStorageKey = computed(() => `${LAYOUT_KEY_PREFIX}${layoutOwner.value}`)
const enterpriseEmail = computed(() => auth.token.value?.email || '')
const avatar = computed(() => auth.token.value?.avatar || '')
const avatarFallback = computed(() => userName.value.trim().slice(0, 1).toUpperCase() || 'U')
const filteredApps = computed(() => {
  const keyword = searchKeyword.value.trim().toLocaleLowerCase()
  return apps.value.filter(item => {
    const categoryMatched = !selectedCategory.value || selectedCategory.value === 'All'
      || String(item.category || 'none') === selectedCategory.value
    const nameMatched = !keyword || displayName(item).toLocaleLowerCase().includes(keyword)
    return categoryMatched && nameMatched
  })
})
const filtersActive = computed(() => Boolean(searchKeyword.value.trim()) || Boolean(selectedCategory.value && selectedCategory.value !== 'All'))
const appMap = computed(() => new Map(apps.value.map(item => [appId(item), item])))
const displayGroups = computed<DisplayGroup[]>(() => {
  const visibleIds = new Set(filteredApps.value.map(appId))
  const toItems = (ids: string[]) => ids
    .filter(id => visibleIds.has(id))
    .map(id => appMap.value.get(id))
    .filter((item): item is AppItem => Boolean(item))

  return [
    ...appLayout.value.groups.map(group => ({ ...group, isUngrouped: false, items: toItems(group.appIds) })),
    {
      id: UNGROUPED_ID,
      name: t('ui.ungrouped'),
      appIds: appLayout.value.ungroupedIds,
      isUngrouped: true,
      items: toItems(appLayout.value.ungroupedIds),
    },
  ]
})

async function loadApps() {
  loading.value = true
  error.value = ''
  try {
    const data = await get<AppItem[]>('/appList')
    apps.value = Array.isArray(data) ? data : (((data as unknown as { rows?: AppItem[] })?.rows || []) as AppItem[])
    normalizeLayout()
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('ui.appLoadError')
  } finally {
    loading.value = false
  }
}

function appId(item: AppItem) {
  return String(item.id || item.appName || item.name || '')
}

function normalizeLayout() {
  const availableIds = apps.value.map(appId).filter(Boolean)
  const availableSet = new Set(availableIds)
  const used = new Set<string>()
  let saved: Partial<AppLayout> = {}
  try {
    saved = JSON.parse(localStorage.getItem(layoutStorageKey.value) || '{}') as Partial<AppLayout>
  } catch {
    saved = {}
  }

  const cleanIds = (ids: unknown) => Array.isArray(ids)
    ? ids.map(String).filter(id => availableSet.has(id) && !used.has(id) && Boolean(used.add(id)))
    : []
  const groups = Array.isArray(saved.groups)
    ? saved.groups.filter(group => group && typeof group.id === 'string' && typeof group.name === 'string').map(group => ({
      id: group.id,
      name: group.name.trim() || t('ui.unnamedGroup'),
      appIds: cleanIds(group.appIds),
    }))
    : []
  const ungroupedIds = cleanIds(saved.ungroupedIds)
  availableIds.forEach(id => {
    if (!used.has(id)) {
      used.add(id)
      ungroupedIds.push(id)
    }
  })
  appLayout.value = { version: 1, groups, ungroupedIds }
  saveLayout()
}

function saveLayout() {
  localStorage.setItem(layoutStorageKey.value, JSON.stringify(appLayout.value))
}

function groupIds(groupId: string) {
  if (groupId === UNGROUPED_ID) return appLayout.value.ungroupedIds
  return appLayout.value.groups.find(group => group.id === groupId)?.appIds
}

function findAppGroupId(id: string) {
  return appLayout.value.groups.find(group => group.appIds.includes(id))?.id || UNGROUPED_ID
}

function moveApp(id: string, targetGroupId: string, targetIndex?: number) {
  const sourceIds = groupIds(findAppGroupId(id))
  const targetIds = groupIds(targetGroupId)
  if (!sourceIds || !targetIds) return
  const sourceIndex = sourceIds.indexOf(id)
  if (sourceIndex < 0) return
  sourceIds.splice(sourceIndex, 1)
  const insertAt = targetIndex === undefined ? targetIds.length : Math.max(0, Math.min(targetIndex, targetIds.length))
  targetIds.splice(insertAt, 0, id)
  saveLayout()
}

function moveBefore(id: string, targetId: string, targetGroupId: string) {
  if (id === targetId) return
  const sourceIds = groupIds(findAppGroupId(id))
  const targetIds = groupIds(targetGroupId)
  if (!sourceIds || !targetIds) return
  const sourceIndex = sourceIds.indexOf(id)
  if (sourceIndex < 0) return
  sourceIds.splice(sourceIndex, 1)
  const targetIndex = targetIds.indexOf(targetId)
  targetIds.splice(targetIndex < 0 ? targetIds.length : targetIndex, 0, id)
  saveLayout()
}

function moveRelative(id: string, offset: -1 | 1) {
  const ids = groupIds(findAppGroupId(id))
  if (!ids) return
  const index = ids.indexOf(id)
  const target = index + offset
  if (index < 0 || target < 0 || target >= ids.length) return
  ids.splice(index, 1)
  ids.splice(target, 0, id)
  saveLayout()
}

function canMoveRelative(id: string, offset: -1 | 1) {
  const ids = groupIds(findAppGroupId(id)) || []
  const index = ids.indexOf(id)
  return index >= 0 && index + offset >= 0 && index + offset < ids.length
}

function handleDragStart(event: DragEvent, id: string) {
  if (filtersActive.value) {
    event.preventDefault()
    return
  }
  draggedAppId.value = id
  event.dataTransfer?.setData('text/plain', id)
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}

function handleDragEnd() {
  suppressCardClickUntil = Date.now() + 250
  draggedAppId.value = ''
  dragOverGroupId.value = ''
}

function handleDropIntoGroup(event: DragEvent, groupId: string) {
  event.preventDefault()
  event.stopPropagation()
  const id = draggedAppId.value || event.dataTransfer?.getData('text/plain') || ''
  if (id && !filtersActive.value) moveApp(id, groupId)
  handleDragEnd()
}

function handleDropBefore(event: DragEvent, targetId: string, groupId: string) {
  event.preventDefault()
  event.stopPropagation()
  const id = draggedAppId.value || event.dataTransfer?.getData('text/plain') || ''
  if (id && !filtersActive.value) moveBefore(id, targetId, groupId)
  handleDragEnd()
}

function openCreateGroup() {
  groupModalMode.value = 'create'
  editingGroupId.value = ''
  groupName.value = ''
  groupModalOpen.value = true
}

function openRenameGroup(group: AppGroup) {
  groupModalMode.value = 'rename'
  editingGroupId.value = group.id
  groupName.value = group.name
  groupModalOpen.value = true
}

function submitGroup() {
  const name = groupName.value.trim()
  if (!name) {
    message.warning(t('ui.groupNameRequired'))
    return
  }
  if (groupModalMode.value === 'create') {
    const id = globalThis.crypto?.randomUUID?.() || `group-${Date.now()}`
    appLayout.value.groups.push({ id, name, appIds: [] })
  } else {
    const group = appLayout.value.groups.find(item => item.id === editingGroupId.value)
    if (group) group.name = name
  }
  saveLayout()
  groupModalOpen.value = false
}

function deleteGroup(group: AppGroup) {
  Modal.confirm({
    title: t('ui.deleteGroup'),
    content: t('ui.deleteGroupConfirm', { name: group.name }),
    okText: t('ui.delete'),
    okType: 'danger',
    cancelText: t('ui.cancel'),
    onOk: () => {
      const index = appLayout.value.groups.findIndex(item => item.id === group.id)
      if (index < 0) return
      const [removed] = appLayout.value.groups.splice(index, 1)
      appLayout.value.ungroupedIds.push(...removed.appIds)
      saveLayout()
    },
  })
}

function openApp(item: AppItem) {
  if (Date.now() < suppressCardClickUntil) return
  if ((item.protocol === 'Basic' || item.inducer === 'SP') && item.loginUrl) {
    window.open(item.loginUrl, '_blank', 'noopener')
    return
  }
  if (item.id) window.open(`/sign/authz/${encodeURIComponent(item.id)}`, '_blank', 'noopener')
}

function configureAccount(item: AppItem) {
  if (!item.id) return
  credentialAppId.value = item.id
  credentialOpen.value = true
}

function displayName(item: AppItem) {
  return item.appName || item.name || t('ui.unnamedApp')
}

function appInitial(item: AppItem) {
  return displayName(item).trim().slice(0, 1).toLocaleUpperCase() || 'A'
}

function handleIconError(event: Event) {
  const image = event.currentTarget as HTMLImageElement
  image.hidden = true
}

function categoryName(item: AppItem) {
  const code = String(item.category || 'none')
  return t(`portal.mxk.apps.category.${code}`)
}

onMounted(() => {
  loadApps()
  if (sessionStorage.getItem(LOGIN_WELCOME_KEY) === '1') {
    sessionStorage.removeItem(LOGIN_WELCOME_KEY)
    welcomeVisible.value = true
    welcomeTimer = window.setTimeout(() => {
      welcomeVisible.value = false
    }, 4000)
  }
})

onBeforeUnmount(() => {
  if (welcomeTimer) window.clearTimeout(welcomeTimer)
})
</script>

<template>
  <DefaultLayout mode="portal">
    <div class="portal-home">
      <a-alert v-if="error" type="error" show-icon :message="error" closable />

      <Transition name="portal-welcome">
        <section v-if="welcomeVisible" class="portal-hero">
          <div class="portal-hero__copy">
            <h1>{{ t('ui.portalWelcome', { name: userName }) }}</h1>
            <p>{{ t('ui.portalHeroDescription') }}</p>
          </div>
          <div class="portal-hero__identity">
            <a-avatar :src="avatar" :size="48">{{ avatarFallback }}</a-avatar>
            <div>
              <strong>{{ userName }}</strong>
              <span>{{ enterpriseEmail || t('ui.noEmail') }}</span>
              <small><CheckCircleFilled />{{ t('ui.portalIdentityVerified') }}</small>
            </div>
          </div>
        </section>
      </Transition>

      <section class="portal-status-grid" aria-label="Identity overview">
        <article class="portal-status-card">
          <span class="portal-status-card__icon"><AppstoreOutlined /></span>
          <div><small>{{ t('ui.portalAvailableApps') }}</small><strong>{{ apps.length }}</strong></div>
        </article>
        <article class="portal-status-card">
          <span class="portal-status-card__icon"><SafetyCertificateOutlined /></span>
          <div><small>{{ t('ui.portalAccountStatus') }}</small><strong>{{ t('ui.portalActive') }}</strong></div>
        </article>
        <article class="portal-status-card">
          <span class="portal-status-card__icon"><MailOutlined /></span>
          <div><small>{{ t('ui.portalEmailStatus') }}</small><strong>{{ enterpriseEmail ? t('ui.portalBound') : t('ui.portalUnbound') }}</strong></div>
        </article>
      </section>

      <section class="portal-app-section">
        <header class="portal-app-section__header">
          <div>
            <h2>{{ t('ui.portalApplications') }}</h2>
            <p>{{ t('ui.portalApplicationsHint', { count: filteredApps.length }) }}</p>
          </div>
          <div class="portal-app-filters">
            <a-button type="primary" @click="openCreateGroup"><PlusOutlined />{{ t('ui.createGroup') }}</a-button>
            <a-input v-model:value="searchKeyword" allow-clear :placeholder="t('ui.portalSearchPlaceholder')">
              <template #prefix><SearchOutlined /></template>
            </a-input>
            <a-select v-model:value="selectedCategory" show-search allow-clear :placeholder="t('ui.appCategory')">
              <a-select-option value="All">{{ t('ui.allCategories') }}</a-select-option>
              <a-select-option v-for="code in categoryCodes" :key="code" :value="code">{{ t(`portal.mxk.apps.category.${code}`) }}</a-select-option>
            </a-select>
          </div>
        </header>

        <div v-if="filtersActive" class="portal-filter-notice">{{ t('ui.clearFiltersToSort') }}</div>

        <div v-if="loading" class="portal-loading-grid">
          <a-skeleton v-for="index in 6" :key="index" active :paragraph="{ rows: 2 }" />
        </div>
        <div v-else-if="apps.length" class="portal-groups">
          <section
            v-for="group in displayGroups"
            :key="group.id"
            class="portal-group"
            :class="{ 'portal-group--drag-over': dragOverGroupId === group.id && !filtersActive }"
            @dragenter.prevent="dragOverGroupId = group.id"
            @dragover.prevent
            @dragleave.self="dragOverGroupId = ''"
            @drop="handleDropIntoGroup($event, group.id)"
          >
            <header class="portal-group__header">
              <div class="portal-group__title">
                <FolderOutlined />
                <strong>{{ group.name }}</strong>
                <span>{{ group.items.length }}</span>
              </div>
              <div v-if="!group.isUngrouped" class="portal-group__actions">
                <button type="button" :title="t('ui.renameGroup')" @click="openRenameGroup(group)"><EditOutlined /></button>
                <button type="button" :title="t('ui.deleteGroup')" @click="deleteGroup(group)"><DeleteOutlined /></button>
              </div>
            </header>

            <div v-if="group.items.length" class="portal-app-grid">
              <article
                v-for="item in group.items"
                :key="appId(item)"
                class="portal-app-card"
                :class="{ 'portal-app-card--dragging': draggedAppId === appId(item) }"
                role="button"
                tabindex="0"
                :draggable="!filtersActive"
                @dragstart="handleDragStart($event, appId(item))"
                @dragend="handleDragEnd"
                @dragover.prevent.stop
                @drop="handleDropBefore($event, appId(item), group.id)"
                @click="openApp(item)"
                @keydown.enter="openApp(item)"
                @keydown.space.prevent="openApp(item)"
              >
                <a-dropdown :trigger="['click']">
                  <button type="button" class="portal-card-menu-button" :title="t('ui.appActions')" @click.stop><MoreOutlined /></button>
                  <template #overlay>
                    <div class="portal-card-menu" @click.stop>
                      <button type="button" :disabled="!canMoveRelative(appId(item), -1)" @click="moveRelative(appId(item), -1)"><ArrowUpOutlined />{{ t('ui.moveUp') }}</button>
                      <button type="button" :disabled="!canMoveRelative(appId(item), 1)" @click="moveRelative(appId(item), 1)"><ArrowDownOutlined />{{ t('ui.moveDown') }}</button>
                      <button v-if="item.protocol === 'Form_Based'" type="button" @click="configureAccount(item)"><UserAddOutlined />{{ t('ui.configureAccount') }}</button>
                      <div class="portal-card-menu__label">{{ t('ui.moveToGroup') }}</div>
                      <button
                        v-for="targetGroup in displayGroups"
                        :key="targetGroup.id"
                        type="button"
                        :disabled="findAppGroupId(appId(item)) === targetGroup.id"
                        @click="moveApp(appId(item), targetGroup.id)"
                      ><FolderOutlined />{{ targetGroup.name }}</button>
                    </div>
                  </template>
                </a-dropdown>
                <span class="portal-app-card__icon-wrap">
                  <span class="portal-app-card__initial">{{ appInitial(item) }}</span>
                  <img v-if="item.iconBase64" class="portal-app-card__icon" :src="item.iconBase64" alt="" @error="handleIconError" />
                </span>
                <span class="portal-app-card__content">
                  <strong class="portal-app-card__name" :title="displayName(item)">{{ displayName(item) }}</strong>
                  <small>{{ categoryName(item) }} · {{ item.protocol || 'SSO' }}</small>
                </span>
                <ArrowRightOutlined class="portal-app-card__arrow" />
              </article>
            </div>
            <div v-else class="portal-group__empty"><FolderOutlined />{{ filtersActive ? t('ui.noMatchingAppsInGroup') : t('ui.dropAppsHere') }}</div>
          </section>
        </div>
        <a-empty v-else :description="t('ui.noApps')" />
      </section>
    </div>

    <AccountCredentialModal v-model:open="credentialOpen" :app-id="credentialAppId" />
    <a-modal
      v-model:open="groupModalOpen"
      :title="groupModalMode === 'create' ? t('ui.createGroup') : t('ui.renameGroup')"
      :ok-text="t('ui.confirm')"
      :cancel-text="t('ui.cancel')"
      @ok="submitGroup"
    >
      <a-form layout="vertical" @submit.prevent="submitGroup">
        <a-form-item :label="t('ui.groupName')" required>
          <a-input v-model:value="groupName" :placeholder="t('ui.groupNamePlaceholder')" :maxlength="30" autofocus @press-enter="submitGroup" />
        </a-form-item>
      </a-form>
    </a-modal>
  </DefaultLayout>
</template>

<style scoped>
.portal-home { width: 100%; }
.portal-welcome-enter-active, .portal-welcome-leave-active { transition: opacity .35s ease, transform .35s ease; }
.portal-welcome-enter-from, .portal-welcome-leave-to { opacity: 0; transform: translateY(-10px); }
.portal-hero { position: relative; display: flex; min-height: 110px; box-sizing: border-box; overflow: hidden; align-items: center; justify-content: space-between; gap: 28px; padding: 13px 28px; border-radius: 17px; color: #fff; background: linear-gradient(132deg, #3030db 0%, #3f3ff3 48%, #5269f6 100%); box-shadow: 0 12px 30px rgba(47, 52, 190, .15); }
.portal-hero::before, .portal-hero::after { position: absolute; border: 1px solid rgba(255, 255, 255, .12); border-radius: 50%; content: ""; }
.portal-hero::before { top: -158px; right: 20%; width: 360px; height: 360px; }
.portal-hero::after { right: -90px; bottom: -210px; width: 410px; height: 410px; background: radial-gradient(circle, rgba(255, 255, 255, .08), transparent 66%); }
.portal-hero__copy, .portal-hero__identity { position: relative; z-index: 1; }
.portal-hero h1 { margin: 0; color: #fff; font-size: clamp(22px, 2.1vw, 29px); font-weight: 620; letter-spacing: -.02em; }
.portal-hero p { margin: 7px 0 0; color: rgba(255, 255, 255, .7); font-size: 13px; }
.portal-hero__identity { display: flex; min-width: 250px; align-items: center; gap: 11px; padding: 8px 12px; border: 1px solid rgba(255, 255, 255, .16); border-radius: 13px; background: rgba(255, 255, 255, .1); backdrop-filter: blur(12px); }
.portal-hero__identity :deep(.ant-avatar) { flex: 0 0 auto; color: #3f3ff3; background: #fff; font-weight: 700; }
.portal-hero__identity > div { display: flex; min-width: 0; flex-direction: column; }
.portal-hero__identity strong, .portal-hero__identity span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.portal-hero__identity strong { color: #fff; font-size: 15px; }
.portal-hero__identity span { max-width: 210px; margin-top: 3px; color: rgba(255, 255, 255, .66); font-size: 12px; }
.portal-hero__identity small { display: inline-flex; align-items: center; gap: 5px; margin-top: 5px; color: #b9f3d0; font-size: 11px; }

.portal-status-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin: 0 0 18px; }
.portal-hero + .portal-status-grid { margin-top: 18px; }
.portal-status-card { display: flex; min-width: 0; align-items: center; gap: 14px; padding: 18px 20px; border: 1px solid #e8ebf3; border-radius: 16px; background: #fff; box-shadow: 0 8px 24px rgba(39, 51, 89, .045); }
.portal-status-card__icon { display: grid; width: 42px; height: 42px; flex: 0 0 42px; border-radius: 12px; color: #3f3ff3; background: #efefff; font-size: 19px; place-items: center; }
.portal-status-card > div { display: flex; min-width: 0; flex-direction: column; }
.portal-status-card small { color: #8993a5; font-size: 12px; }
.portal-status-card strong { overflow: hidden; margin-top: 4px; color: #192338; font-size: 17px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }

.portal-app-section { min-height: 360px; padding: 26px; border: 1px solid #e8ebf3; border-radius: 20px; background: #fff; box-shadow: 0 10px 30px rgba(39, 51, 89, .045); }
.portal-app-section__header { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.portal-app-section__header h2 { margin: 0; color: #172033; font-size: 20px; font-weight: 650; }
.portal-app-section__header p { margin: 5px 0 0; color: #8a94a6; font-size: 12px; }
.portal-app-filters { display: flex; align-items: center; gap: 10px; }
.portal-app-filters :deep(.ant-btn) { height: 40px; border-radius: 10px; box-shadow: none; }
.portal-app-filters :deep(.ant-input-affix-wrapper) { width: 230px; height: 40px; border-radius: 10px; }
.portal-app-filters :deep(.ant-select) { width: 190px; }
.portal-app-filters :deep(.ant-select-selector) { height: 40px !important; align-items: center; border-radius: 10px !important; }
.portal-filter-notice { margin: -10px 0 18px; padding: 9px 12px; border-radius: 9px; color: #536079; background: #f4f6fb; font-size: 12px; }
.portal-groups { display: flex; flex-direction: column; gap: 18px; }
.portal-group { padding: 16px; border: 1px solid #e8ebf3; border-radius: 16px; background: #fafbfe; transition: border-color .18s, background .18s, box-shadow .18s; }
.portal-group--drag-over { border-color: #5369f5; background: #f5f6ff; box-shadow: inset 0 0 0 1px rgba(63, 63, 243, .12); }
.portal-group__header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 13px; }
.portal-group__title { display: flex; min-width: 0; align-items: center; gap: 8px; color: #303b51; }
.portal-group__title > :first-child { color: #596cf4; }
.portal-group__title strong { overflow: hidden; font-size: 14px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.portal-group__title span { display: inline-grid; min-width: 21px; height: 21px; padding: 0 5px; box-sizing: border-box; border-radius: 10px; color: #788398; background: #edf0f6; font-size: 11px; place-items: center; }
.portal-group__actions { display: flex; gap: 4px; }
.portal-group__actions button { display: grid; width: 30px; height: 30px; padding: 0; border: 0; border-radius: 8px; color: #7f899b; background: transparent; cursor: pointer; place-items: center; }
.portal-group__actions button:hover { color: #3f3ff3; background: #ededff; }
.portal-group__actions button:last-child:hover { color: #d4380d; background: #fff0eb; }
.portal-group__empty { display: flex; min-height: 78px; align-items: center; justify-content: center; gap: 8px; border: 1px dashed #d9deea; border-radius: 12px; color: #98a1b2; background: rgba(255, 255, 255, .58); font-size: 12px; }
.portal-app-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 14px; }
.portal-app-card { position: relative; display: grid; min-width: 0; min-height: 150px; box-sizing: border-box; overflow: hidden; grid-template-columns: 68px minmax(0, 1fr) 12px; align-items: center; gap: 18px; padding: 24px; border: 1px solid #e8ebf3; border-radius: 18px; background: #fff; cursor: pointer; outline: none; transition: border-color .18s, box-shadow .18s, transform .18s; }
.portal-app-card:hover, .portal-app-card:focus-visible { border-color: rgba(63, 63, 243, .4); box-shadow: 0 12px 28px rgba(50, 55, 150, .1); transform: translateY(-2px); }
.portal-app-card[draggable="true"] { cursor: grab; }
.portal-app-card[draggable="true"]:active { cursor: grabbing; }
.portal-app-card--dragging { opacity: .38; transform: scale(.98); }
.portal-app-card__icon-wrap { position: relative; display: grid; width: 68px; height: 68px; overflow: hidden; contain: paint; border: 1px solid #e6e8f5; border-radius: 16px; color: #3f3ff3; background: linear-gradient(145deg, #f2f2ff, #e9eaff); place-items: center; }
.portal-app-card__initial { font-size: 25px; font-weight: 700; line-height: 1; }
.portal-app-card__icon { position: absolute; inset: 0; width: 100%; height: 100%; padding: 8px; box-sizing: border-box; border: 0; border-radius: 13px; background: #fff; object-fit: contain; }
.portal-app-card__content { display: flex; min-width: 0; max-width: 100%; overflow: hidden; flex-direction: column; }
.portal-app-card__name { overflow: hidden; color: #202a3d; font-size: 14px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.portal-app-card__content small { overflow: hidden; margin-top: 7px; color: #929bab; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.portal-app-card__arrow { color: #aab1bf; font-size: 12px; }
.portal-card-menu-button { position: absolute; z-index: 2; top: 8px; right: 8px; display: grid; width: 30px; height: 30px; padding: 0; border: 0; border-radius: 8px; color: #8a94a7; background: transparent; cursor: pointer; place-items: center; }
.portal-card-menu-button:hover { color: #3f3ff3; background: #f0f0ff; }
.portal-card-menu { display: flex; min-width: 176px; max-height: 360px; overflow-y: auto; flex-direction: column; padding: 6px; border: 1px solid #eaedf4; border-radius: 11px; background: #fff; box-shadow: 0 12px 32px rgba(30, 39, 75, .16); }
.portal-card-menu button { display: flex; min-height: 34px; align-items: center; gap: 9px; padding: 7px 10px; border: 0; border-radius: 7px; color: #344057; background: transparent; cursor: pointer; font-size: 12px; text-align: left; }
.portal-card-menu button:hover:not(:disabled) { color: #3f3ff3; background: #f2f2ff; }
.portal-card-menu button:disabled { color: #b8bfcb; cursor: default; }
.portal-card-menu__label { margin: 5px 4px 3px; padding-top: 7px; border-top: 1px solid #eef0f5; color: #9aa3b2; font-size: 10px; }
.portal-loading-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.portal-loading-grid :deep(.ant-skeleton) { padding: 18px; border: 1px solid #eef0f5; border-radius: 14px; }

@media (max-width: 700px) {
  .portal-hero { align-items: flex-start; flex-direction: column; }
  .portal-hero__identity { width: 100%; box-sizing: border-box; }
  .portal-status-grid { grid-template-columns: 1fr; }
  .portal-app-section__header { align-items: flex-start; flex-direction: column; }
  .portal-app-filters { width: 100%; }
  .portal-app-filters :deep(.ant-input-affix-wrapper), .portal-app-filters :deep(.ant-select) { flex: 1; width: auto; }
  .portal-loading-grid { grid-template-columns: 1fr; }
}

@media (max-width: 560px) {
  .portal-hero { min-height: 0; padding: 26px 22px; border-radius: 18px; }
  .portal-hero__identity { min-width: 0; }
  .portal-app-section { padding: 20px 16px; }
  .portal-app-filters { align-items: stretch; flex-direction: column; }
  .portal-app-filters :deep(.ant-input-affix-wrapper), .portal-app-filters :deep(.ant-select) { width: 100%; }
  .portal-group { padding: 12px; }
  .portal-app-grid { grid-template-columns: 1fr; }
}
</style>
