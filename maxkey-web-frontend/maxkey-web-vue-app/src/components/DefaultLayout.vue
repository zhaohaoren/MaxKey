<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { RouterLink, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { auth } from '../auth'
import { currentLanguage, setLanguage, supportedLanguages, type SupportedLanguage } from '../i18n'
import { adminMenus, portalMenus, type MenuItem } from '../menu'

const props = defineProps<{ mode: 'portal' | 'admin' }>()
const router = useRouter()
const { t } = useI18n({ useScope: 'global' })
const collapsed = ref(false)
const opened = ref<string[]>([])
const fullscreen = ref(false)
const fullscreenEnabled = ref(document.fullscreenEnabled)
const menus = computed(() => props.mode === 'admin' ? adminMenus : portalMenus)

const displayName = computed(() => auth.token.value?.displayName || auth.token.value?.username || '')
const enterpriseEmail = computed(() => auth.token.value?.email || t('ui.noEmail'))
const avatar = computed(() => auth.token.value?.avatar || '')
const avatarFallback = computed(() => displayName.value.trim().slice(0, 1).toUpperCase() || 'U')

function toggle(item: MenuItem) {
  if (!item.children) return
  opened.value = opened.value.includes(item.key) ? opened.value.filter(key => key !== item.key) : [...opened.value, item.key]
}

function isOpen(item: MenuItem) {
  return opened.value.includes(item.key)
}

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
  <div class="alain-default" :class="{ 'alain-default__collapsed': collapsed }">
    <header class="alain-default__header">
      <div class="alain-default__header-brand">
        <RouterLink to="/dashboard/home" class="alain-default__header-title">
          <img src="/assets/logo.png" alt="MaxKey" />
          <span><b>Max</b><strong>Key</strong> {{ t(mode === 'admin' ? 'admin.mxk.title' : 'portal.mxk.title') }}</span>
        </RouterLink>
      </div>
      <div class="alain-default__header-actions">
        <RouterLink v-if="mode === 'portal' && auth.isAdmin.value" class="header-link" to="/admin">{{ t('portal.mxk.menu.mgt') }}</RouterLink>
        <RouterLink v-if="mode === 'admin'" class="header-link" to="/dashboard/home">{{ t('ui.returnPortal') }}</RouterLink>

        <button class="header-identity" type="button" :title="t('portal.mxk.menu.config.profile')" @click="openProfile">
          <a-avatar class="header-avatar" :src="avatar" :size="38">{{ avatarFallback }}</a-avatar>
          <span class="header-identity__text">
            <strong>{{ displayName }}</strong>
            <small>{{ enterpriseEmail }}</small>
          </span>
        </button>

        <a-dropdown trigger="click" placement="bottomRight">
          <button class="header-icon-button" type="button" :title="t('ui.legacy.portalSettings')" :aria-label="t('ui.legacy.portalSettings')">⚙</button>
          <template #overlay>
            <a-menu class="header-settings-menu" @click="handleSettingsMenu">
              <a-menu-item key="password">
                <span class="header-menu-icon">⌁</span>{{ t('portal.mxk.menu.config.password') }}
              </a-menu-item>
              <a-menu-item key="fullscreen" :disabled="!fullscreenEnabled">
                <span class="header-menu-icon">□</span>{{ t(fullscreen ? 'ui.legacy.fullscreenExit' : 'ui.legacy.fullscreen') }}
              </a-menu-item>
              <a-menu-item key="clear">
                <span class="header-menu-icon">⌫</span>{{ t('ui.legacy.clearStorage') }}
              </a-menu-item>
              <a-sub-menu key="language">
                <template #title><span class="header-menu-icon">◎</span>{{ t('ui.legacy.language') }}</template>
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

        <button class="header-icon-button" type="button" :title="t('ui.legacy.logout')" :aria-label="t('ui.legacy.logout')" @click="logout">↪</button>
      </div>
    </header>

    <aside class="alain-default__sidebar" :class="{ collapsed }">
      <button class="sidebar-toggle" type="button" @click="collapsed = !collapsed">{{ collapsed ? '»' : '«' }}</button>
      <nav class="sidebar-nav">
        <template v-for="item in menus" :key="item.key">
          <RouterLink v-if="item.path" class="sidebar-nav__item" active-class="selected" :to="item.path">
            <span class="sidebar-nav__icon">•</span><span v-if="!collapsed">{{ t(item.titleKey) }}</span>
          </RouterLink>
          <button v-else class="sidebar-nav__item sidebar-nav__group" type="button" @click="toggle(item)">
            <span class="sidebar-nav__icon">•</span><span v-if="!collapsed">{{ t(item.titleKey) }}</span><span v-if="!collapsed">{{ isOpen(item) ? '−' : '+' }}</span>
          </button>
          <div v-if="item.children && isOpen(item) && !collapsed" class="sidebar-nav__children">
            <RouterLink v-for="child in item.children" :key="child.key" class="sidebar-nav__item" active-class="selected" :to="child.path || '/portal'">{{ t(child.titleKey) }}</RouterLink>
          </div>
        </template>
      </nav>
    </aside>

    <main class="alain-default__content"><slot /></main>
    <footer class="alain-default__footer">MaxKey v4.2.0<br />Copyright {{ new Date().getFullYear() }} <a href="//www.maxkey.top" target="_blank">http://www.maxkey.top</a><br />Licensed under the Apache License, Version 2.0</footer>
  </div>
</template>
