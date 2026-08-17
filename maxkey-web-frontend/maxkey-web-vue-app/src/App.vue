<script setup lang="ts">
import { RouterView } from 'vue-router'
import { antdLocale } from './i18n'

const themeConfig = {
  token: {
    colorPrimary: '#3f3ff3',
    colorInfo: '#3f3ff3',
    colorSuccess: '#19a974',
    colorWarning: '#d9931a',
    colorError: '#df4b55',
    colorText: '#17243a',
    colorTextSecondary: '#66758d',
    colorTextTertiary: '#94a0b2',
    colorBorder: '#dce3ee',
    colorBorderSecondary: '#e9edf4',
    colorBgLayout: '#f4f7fb',
    colorBgContainer: '#ffffff',
    borderRadius: 8,
    borderRadiusLG: 12,
    borderRadiusSM: 6,
    controlHeight: 36,
    controlHeightLG: 44,
    fontSize: 14,
    lineWidth: 1,
    boxShadowSecondary: '0 18px 48px rgba(22, 53, 102, 0.14)',
  },
  components: {
    Button: { fontWeight: 600, primaryShadow: 'none' },
    Card: { headerHeight: 52, bodyPadding: 20 },
    Table: { headerBg: '#f6f8fc', headerColor: '#45566f', headerSplitColor: '#e4e9f1', cellPaddingBlock: 13 },
    Menu: { itemBorderRadius: 8, itemHeight: 42, itemMarginInline: 10 },
    Modal: { titleFontSize: 18 },
    Drawer: { paddingLG: 24 },
    Tabs: { titleFontSize: 14, horizontalItemPadding: '12px 2px' },
  },
}
</script>

<template>
  <a-config-provider :locale="antdLocale" :theme="themeConfig">
    <RouterView />
  </a-config-provider>
</template>
