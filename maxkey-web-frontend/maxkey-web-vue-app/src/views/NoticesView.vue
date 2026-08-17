<script setup lang="ts">
import DefaultLayout from '../components/DefaultLayout.vue'
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title">
      <div><h1>通知公告</h1><small>原管理端保留的通知公告入口</small></div>
    </div>
    <a-card class="admin-page-card">
      <a-empty description="通知公告功能暂未配置内容" />
    </a-card>
  </DefaultLayout>
</template>
