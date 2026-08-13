<script setup>
import { onMounted, ref } from 'vue'

const user = ref(null)
const loading = ref(true)
const error = ref('')

async function loadUser() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetch('/api/me', { credentials: 'include' })
    if (response.status === 401) {
      user.value = null
      return
    }
    if (!response.ok) {
      throw new Error(`请求失败：${response.status}`)
    }
    user.value = await response.json()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function login() {
  window.location.href = '/oauth2/authorization/maxkey'
}

function logout() {
  window.location.href = '/api/logout'
}

onMounted(loadUser)
</script>

<template>
  <main class="page-shell">
    <section class="hero">
      <p class="eyebrow">MAXKEY · OIDC INTEGRATION</p>
      <h1>Vue 应用的<br /><em>统一身份登录</em></h1>
      <p class="intro">
        前端只负责展示，Spring Boot 负责 OIDC 授权码回调和会话保存。
        `client_secret` 不会进入浏览器。
      </p>
      <div class="actions">
        <button v-if="!user" class="primary-button" @click="login">使用 MaxKey 登录</button>
        <button v-else class="secondary-button" @click="logout">退出登录</button>
        <button class="text-button" @click="loadUser">刷新登录状态</button>
      </div>
    </section>

    <section class="status-card" :class="{ authenticated: user }">
      <div class="card-heading">
        <span class="status-dot"></span>
        <span>{{ loading ? '正在检查会话' : user ? 'OIDC 登录成功' : '当前未登录' }}</span>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-else-if="loading" class="muted">正在请求 Spring Boot 的 `/api/me`...</p>
      <div v-else-if="user" class="user-content">
        <div class="identity">
          <span class="avatar">{{ (user.claims.name || user.claims.preferred_username || '?').slice(0, 1) }}</span>
          <div>
            <strong>{{ user.claims.name || user.claims.preferred_username || user.subject }}</strong>
            <small>subject: {{ user.subject }}</small>
          </div>
        </div>
        <h2>OIDC Claims</h2>
        <pre>{{ JSON.stringify(user.claims, null, 2) }}</pre>
      </div>
      <p v-else class="muted">点击上面的登录按钮，浏览器将跳转到 MaxKey 登录页。</p>
    </section>
  </main>
</template>
