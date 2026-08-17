<script setup lang="ts">
import {
  ApiOutlined,
  AppstoreOutlined,
  AuditOutlined,
  CheckSquareOutlined,
  ClusterOutlined,
  CommentOutlined,
  ContactsOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  DownOutlined,
  EyeOutlined,
  ExpandOutlined,
  FileProtectOutlined,
  GlobalOutlined,
  HistoryOutlined,
  HomeOutlined,
  IdcardOutlined,
  LockOutlined,
  LogoutOutlined,
  MailOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PartitionOutlined,
  ProjectOutlined,
  RadarChartOutlined,
  ReadOutlined,
  SafetyCertificateOutlined,
  SendOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons-vue'
import { computed, onBeforeUnmount, onMounted, ref, watch, type Component } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { auth } from '../auth'
import { currentLanguage, setLanguage, supportedLanguages, type SupportedLanguage } from '../i18n'
import { adminMenus, portalMenus, type MenuIcon, type MenuItem } from '../menu'

const props = defineProps<{ mode: 'portal' | 'admin' }>()
const router = useRouter()
const route = useRoute()
const { t } = useI18n({ useScope: 'global' })
const collapsed = ref(false)
const opened = ref<string[]>([])
const fullscreen = ref(false)
const fullscreenEnabled = ref(document.fullscreenEnabled)
const menus = computed(() => props.mode === 'admin' ? adminMenus : portalMenus)
const menuIcons: Record<MenuIcon, Component> = {
  api: ApiOutlined,
  appstore: AppstoreOutlined,
  audit: AuditOutlined,
  'check-square': CheckSquareOutlined,
  cluster: ClusterOutlined,
  comment: CommentOutlined,
  contacts: ContactsOutlined,
  database: DatabaseOutlined,
  eye: EyeOutlined,
  'file-protect': FileProtectOutlined,
  history: HistoryOutlined,
  home: HomeOutlined,
  idcard: IdcardOutlined,
  mail: MailOutlined,
  partition: PartitionOutlined,
  project: ProjectOutlined,
  radar: RadarChartOutlined,
  read: ReadOutlined,
  safety: SafetyCertificateOutlined,
  send: SendOutlined,
  setting: SettingOutlined,
  team: TeamOutlined,
  user: UserOutlined,
}

const displayName = computed(() => auth.token.value?.displayName || auth.token.value?.username || '')
const enterpriseEmail = computed(() => auth.token.value?.email || t('ui.noEmail'))
const avatar = computed(() => auth.token.value?.avatar || '')
const avatarFallback = computed(() => displayName.value.trim().slice(0, 1).toUpperCase() || 'U')

function toggle(item: MenuItem) {
  if (!item.children) return
  opened.value = opened.value.includes(item.key) ? [] : [item.key]
}

function isOpen(item: MenuItem) {
  return opened.value.includes(item.key)
}

function icon(item: MenuItem) {
  return item.icon ? menuIcons[item.icon] : undefined
}

function isPathActive(path?: string) {
  return Boolean(path && (route.path === path || route.path.startsWith(`${path}/`)))
}

function isGroupActive(item: MenuItem) {
  return Boolean(item.children?.some(child => isPathActive(child.path)))
}

watch(() => route.path, () => {
  const activeGroup = menus.value.find(isGroupActive)
  opened.value = activeGroup ? [activeGroup.key] : []
}, { immediate: true })

function openProfile() {
  void router.push('/config/profile')
}

function changePassword() {
  void router.push('/config/password')
}

async function toggleFullscreen() {
  if (!fullscreenEnabled.value) return
  if (document.fullscreenElement) await document.exitFullscreen()
  else await document.documentElement.requestFullscreen()
}

function syncFullscreen() {
  fullscreen.value = Boolean(document.fullscreenElement)
}

function clearStorage() {
  Modal.confirm({
    title: t('ui.clearTitle'),
    onOk() {
      localStorage.clear()
      message.success(t('ui.clearSuccess'))
    },
  })
}

function handleSettingsMenu({ key }: { key: string | number }) {
  const action = String(key)
  if (action === 'password') changePassword()
  else if (action === 'fullscreen') void toggleFullscreen()
  else if (action === 'clear') clearStorage()
  else if (action.startsWith('language:')) {
    void setLanguage(action.slice(9) as SupportedLanguage).catch(() => message.error(t('ui.languageLoadError')))
  }
}

async function logout() {
  await auth.logout()
  await router.replace('/login')
}

onMounted(() => {
  fullscreenEnabled.value = document.fullscreenEnabled
  syncFullscreen()
  document.addEventListener('fullscreenchange', syncFullscreen)
})

onBeforeUnmount(() => document.removeEventListener('fullscreenchange', syncFullscreen))
</script>

<template>
  <div class="alain-default" :class="{ 'alain-default__collapsed': collapsed, 'alain-default--portal': mode === 'portal', 'alain-default--admin': mode === 'admin' }">
    <header class="alain-default__header">
      <div class="alain-default__header-brand">
        <RouterLink :to="mode === 'admin' ? '/admin' : '/dashboard/home'" class="alain-default__header-title">
          <img class="snowx-shell-logo" src="/assets/brand/snowx-logo-black.png" alt="SnowX" />
          <span class="snowx-shell-divider" />
          <span class="snowx-shell-title">{{ t(mode === 'admin' ? 'ui.adminCenter' : 'ui.portalCenter') }}</span>
        </RouterLink>
      </div>
      <div class="alain-default__header-actions">
        <button class="header-identity" type="button" :title="t('portal.mxk.menu.config.profile')" @click="openProfile">
          <a-avatar class="header-avatar" :src="avatar" :size="38">{{ avatarFallback }}</a-avatar>
          <span class="header-identity__text">
            <strong>{{ displayName }}</strong>
            <small>{{ enterpriseEmail }}</small>
          </span>
        </button>

        <a-dropdown trigger="click" placement="bottomRight">
          <button class="header-icon-button" type="button" :title="t('ui.legacy.portalSettings')" :aria-label="t('ui.legacy.portalSettings')"><SettingOutlined /></button>
          <template #overlay>
            <a-menu class="header-settings-menu" @click="handleSettingsMenu">
              <a-menu-item key="password">
                <span class="header-menu-icon"><LockOutlined /></span>{{ t('portal.mxk.menu.config.password') }}
              </a-menu-item>
              <a-menu-item key="fullscreen" :disabled="!fullscreenEnabled">
                <span class="header-menu-icon"><ExpandOutlined /></span>{{ t(fullscreen ? 'ui.legacy.fullscreenExit' : 'ui.legacy.fullscreen') }}
              </a-menu-item>
              <a-menu-item key="clear">
                <span class="header-menu-icon"><DeleteOutlined /></span>{{ t('ui.legacy.clearStorage') }}
              </a-menu-item>
              <a-sub-menu key="language">
                <template #title><span class="header-menu-icon"><GlobalOutlined /></span>{{ t('ui.legacy.language') }}</template>
                <a-menu-item
                  v-for="item in supportedLanguages"
                  :key="`language:${item.code}`"
                  :class="{ 'header-language-selected': item.code === currentLanguage }"
                >
                  <span class="header-language-abbr">{{ item.abbr }}</span>{{ item.name }}
                </a-menu-item>
              </a-sub-menu>
            </a-menu>
          </template>
        </a-dropdown>

        <button class="header-icon-button" type="button" :title="t('ui.legacy.logout')" :aria-label="t('ui.legacy.logout')" @click="logout"><LogoutOutlined /></button>
      </div>
    </header>

    <nav v-if="mode === 'portal'" class="portal-topnav" aria-label="Portal navigation">
      <template v-for="item in menus" :key="item.key">
        <RouterLink v-if="item.path" class="portal-topnav__item" active-class="selected" :to="item.path">
          <component :is="icon(item)" v-if="icon(item)" />
          <span>{{ t(item.titleKey) }}</span>
        </RouterLink>
        <a-dropdown v-else trigger="click" placement="bottomLeft">
          <button class="portal-topnav__item" :class="{ selected: isGroupActive(item) }" type="button">
            <component :is="icon(item)" v-if="icon(item)" />
            <span>{{ t(item.titleKey) }}</span>
            <DownOutlined class="portal-topnav__arrow" />
          </button>
          <template #overlay>
            <div class="portal-topnav-panel">
              <RouterLink v-for="child in item.children" :key="child.key" class="portal-topnav-panel__item" :to="child.path || '/dashboard/home'">
                <span class="portal-topnav-panel__icon"><component :is="icon(child)" v-if="icon(child)" /></span>
                <span>{{ t(child.titleKey) }}</span>
              </RouterLink>
            </div>
          </template>
        </a-dropdown>
      </template>
      <RouterLink v-if="auth.isAdmin.value" class="portal-topnav__item portal-topnav__admin" to="/admin">
        <UserSwitchOutlined />
        <span>{{ t('ui.adminPortalEntry') }}</span>
      </RouterLink>
    </nav>

    <aside v-else class="alain-default__sidebar" :class="{ collapsed }">
      <div class="admin-sidebar-head">
        <span v-if="!collapsed">{{ t('ui.adminNavigation') }}</span>
        <button class="sidebar-toggle" type="button" @click="collapsed = !collapsed"><MenuUnfoldOutlined v-if="collapsed" /><MenuFoldOutlined v-else /></button>
      </div>
      <nav class="sidebar-nav">
        <template v-for="item in menus" :key="item.key">
          <RouterLink v-if="item.path" class="sidebar-nav__item" active-class="selected" :to="item.path">
            <component :is="icon(item)" v-if="icon(item)" class="sidebar-nav__icon" />
            <span v-else class="sidebar-nav__icon">•</span>
            <span v-if="!collapsed">{{ t(item.titleKey) }}</span>
          </RouterLink>
          <button v-else class="sidebar-nav__item sidebar-nav__group" :class="{ selected: isGroupActive(item) }" type="button" @click="toggle(item)">
            <component :is="icon(item)" v-if="icon(item)" class="sidebar-nav__icon" />
            <span v-else class="sidebar-nav__icon">•</span>
            <span v-if="!collapsed">{{ t(item.titleKey) }}</span><DownOutlined v-if="!collapsed" class="sidebar-nav__chevron" :class="{ open: isOpen(item) }" />
          </button>
          <div v-if="item.children && isOpen(item) && !collapsed" class="sidebar-nav__children">
            <RouterLink v-for="child in item.children" :key="child.key" class="sidebar-nav__item" active-class="selected" :to="child.path || '/portal'">
              <component :is="icon(child)" v-if="icon(child)" class="sidebar-nav__icon" />
              <span>{{ t(child.titleKey) }}</span>
            </RouterLink>
          </div>
        </template>
      </nav>
      <RouterLink class="admin-sidebar-portal" to="/dashboard/home" :title="t('ui.returnPortal')">
        <AppstoreOutlined />
        <span v-if="!collapsed">{{ t('ui.returnPortal') }}</span>
      </RouterLink>
    </aside>

    <main class="alain-default__content"><slot /></main>
    <footer class="alain-default__footer">Copyright © {{ new Date().getFullYear() }} SnowX · Identity and Access Management System</footer>
  </div>
</template>
