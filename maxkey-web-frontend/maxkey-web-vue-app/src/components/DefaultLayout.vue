<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { auth } from '../auth'
import { adminMenus, portalMenus, type MenuItem } from '../menu'

const props = defineProps<{ mode: 'portal' | 'admin' }>()
const router = useRouter()
const collapsed = ref(false)
const opened = ref<string[]>([])
const menus = props.mode === 'admin' ? adminMenus : portalMenus

function toggle(item: MenuItem) {
  if (!item.children) return
  opened.value = opened.value.includes(item.key) ? opened.value.filter(key => key !== item.key) : [...opened.value, item.key]
}

function isOpen(item: MenuItem) {
  return opened.value.includes(item.key)
}

async function logout() {
  await auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="alain-default" :class="{ 'alain-default__collapsed': collapsed }">
    <header class="alain-default__header">
      <div class="alain-default__header-brand">
        <RouterLink to="/dashboard/home" class="alain-default__header-title">
          <img src="/assets/logo.png" alt="MaxKey" />
          <span><b>Max</b><strong>Key</strong> {{ mode === 'admin' ? '管理后台' : '统一认证' }}</span>
        </RouterLink>
      </div>
      <div class="alain-default__header-actions">
        <RouterLink v-if="mode === 'portal' && auth.isAdmin.value" class="header-link" to="/admin">管理后台</RouterLink>
        <RouterLink v-if="mode === 'admin'" class="header-link" to="/dashboard/home">返回门户</RouterLink>
        <span class="header-user">{{ auth.token.value?.displayName || auth.token.value?.username }}</span>
        <button class="header-button" @click="logout">退出</button>
      </div>
    </header>

    <aside class="alain-default__sidebar" :class="{ collapsed }">
      <button class="sidebar-toggle" type="button" @click="collapsed = !collapsed">{{ collapsed ? '»' : '«' }}</button>
      <nav class="sidebar-nav">
        <template v-for="item in menus" :key="item.key">
          <RouterLink v-if="item.path" class="sidebar-nav__item" active-class="selected" :to="item.path">
            <span class="sidebar-nav__icon">•</span><span v-if="!collapsed">{{ item.title }}</span>
          </RouterLink>
          <button v-else class="sidebar-nav__item sidebar-nav__group" type="button" @click="toggle(item)">
            <span class="sidebar-nav__icon">•</span><span v-if="!collapsed">{{ item.title }}</span><span v-if="!collapsed">{{ isOpen(item) ? '−' : '+' }}</span>
          </button>
          <div v-if="item.children && isOpen(item) && !collapsed" class="sidebar-nav__children">
            <RouterLink v-for="child in item.children" :key="child.key" class="sidebar-nav__item" active-class="selected" :to="child.path || '/portal'">{{ child.title }}</RouterLink>
          </div>
        </template>
      </nav>
    </aside>

    <main class="alain-default__content"><slot /></main>
    <footer class="alain-default__footer">MaxKey v4.2.0<br />Copyright {{ new Date().getFullYear() }} <a href="//www.maxkey.top" target="_blank">http://www.maxkey.top</a><br />Licensed under the Apache License, Version 2.0</footer>
  </div>
</template>
