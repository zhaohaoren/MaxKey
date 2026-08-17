<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DefaultLayout from '../components/DefaultLayout.vue'
import { get, put } from '../api'

const { t } = useI18n({ useScope: 'global' })
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const form = reactive<Record<string, unknown>>({ authnType: 0 })

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await get<Record<string, unknown>>('/users/profile/get')
    Object.assign(form, data, { authnType: Number(data.authnType || 0) })
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('ui.profileLoadError')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await put('/users/profile/updateAuthnType', form)
    message.success(t('portal.mxk.alert.operate.success'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('portal.mxk.alert.operate.error'))
  } finally {
    saving.value = false
  }
}

onMounted(() => { void load() })
</script>

<template>
  <DefaultLayout mode="portal">
    <div class="alain-default__content-title"><h1>{{ t('portal.mxk.menu.config.mfa') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card>
      <a-spin :spinning="loading">
        <a-form class="portal-single-form" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" @submit.prevent="save">
          <a-form-item :label="t('portal.mxk.password.displayName')"><a-input v-model:value="form.displayName" disabled /></a-form-item>
          <a-form-item :label="t('portal.mxk.password.username')"><a-input v-model:value="form.username" disabled /></a-form-item>
          <a-form-item :label="t('portal.mxk.users.mobile')"><a-input v-model:value="form.mobile" disabled /></a-form-item>
          <a-form-item :label="t('portal.mxk.users.email')"><a-input v-model:value="form.email" disabled /></a-form-item>
          <a-form-item :label="t('ui.userAuthnType')">
            <a-radio-group v-model:value="form.authnType" button-style="solid">
              <a-radio-button v-for="value in [0, 1, 2, 3]" :key="value" :value="value">{{ t(`portal.mxk.users.authnType.${value}`) }}</a-radio-button>
            </a-radio-group>
          </a-form-item>
          <div class="portal-form-actions"><a-button type="primary" html-type="submit" :loading="saving">{{ t('portal.mxk.text.save') }}</a-button></div>
        </a-form>
      </a-spin>
    </a-card>
  </DefaultLayout>
</template>

<style scoped>
.portal-single-form { width: 100%; max-width: 980px; margin: 0 auto; }
</style>
