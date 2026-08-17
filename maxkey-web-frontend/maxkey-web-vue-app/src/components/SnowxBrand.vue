<script setup lang="ts">
withDefaults(defineProps<{
  compact?: boolean
  subtitle?: string
  inverse?: boolean
}>(), {
  compact: false,
  subtitle: '',
  inverse: false,
})
</script>

<template>
  <span class="snowx-brand" :class="{ 'snowx-brand--compact': compact, 'snowx-brand--inverse': inverse }">
    <img
      class="snowx-brand__logo"
      :src="inverse ? '/assets/brand/snowx-logo-white.png' : '/assets/brand/snowx-logo-black.png'"
      alt="SnowX"
    />
    <span v-if="!compact" class="snowx-brand__copy">
      <small v-if="subtitle">{{ subtitle }}</small>
    </span>
  </span>
</template>
