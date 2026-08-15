<script setup lang="ts">
import { RouterView } from 'vue-router'
import { antdLocale } from './i18n'
</script>

<template>
  <a-config-provider :locale="antdLocale">
    <RouterView />
  </a-config-provider>
</template>
