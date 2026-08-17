<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DefaultLayout from '../components/DefaultLayout.vue'
import { get, put } from '../api'

const { t } = useI18n({ useScope: 'global' })
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const form = reactive<Record<string, unknown>>({ otpCode: '' })
const formattedSecret = computed(() => String(form.sharedSecret || '').match(/.{1,4}/g)?.join(' ') || '')

async function load() {
  loading.value = true
  error.value = ''
  try {
    Object.assign(form, await get<Record<string, unknown>>('/config/timebased/view'), { otpCode: '' })
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('ui.timebasedLoadError')
  } finally {
    loading.value = false
  }
}

async function generate() {
  saving.value = true
  try {
    Object.assign(form, await get<Record<string, unknown>>('/config/timebased/generate'), { otpCode: '' })
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('ui.timebasedGenerateError'))
  } finally {
    saving.value = false
  }
}

async function save() {
  if (!String(form.sharedSecret || '')) return void message.warning(t('ui.generateSecretFirst'))
  if (!String(form.otpCode || '').trim()) return void message.warning(t('ui.otpRequired'))
  saving.value = true
  try {
    await put('/config/timebased/update', form)
    message.success(t('portal.mxk.alert.operate.success'))
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('portal.mxk.alert.operate.error'))
  } finally {
    saving.value = false
  }
}

async function verify() {
  if (!String(form.otpCode || '').trim()) return void message.warning(t('ui.otpRequired'))
  try {
    await get('/config/timebased/verify', { otpCode: form.otpCode })
    message.success(t('ui.otpVerifySuccess'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('ui.otpVerifyError'))
  }
}

onMounted(() => { void load() })
</script>

<template>
  <DefaultLayout mode="portal">
    <div class="alain-default__content-title"><h1>{{ t('portal.mxk.menu.config.timebased') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card>
      <a-spin :spinning="loading">
        <a-row :gutter="24" class="timebased-content">
          <a-col :xs="24" :md="10" class="timebased-qr"><img v-if="form.qrCode" :src="String(form.qrCode)" :alt="t('portal.mxk.timebased.rqCode')" /><a-empty v-else :description="t('ui.noOtpSecret')" /></a-col>
          <a-col :xs="24" :md="14">
            <a-form :label-col="{ span: 7 }" :wrapper-col="{ span: 15 }">
              <a-form-item :label="t('portal.mxk.timebased.displayName')"><a-input v-model:value="form.displayName" disabled /></a-form-item>
              <a-form-item :label="t('portal.mxk.timebased.username')"><a-input v-model:value="form.username" disabled /></a-form-item>
              <a-form-item :label="t('portal.mxk.timebased.digits')"><a-input v-model:value="form.digits" disabled /></a-form-item>
              <a-form-item :label="t('portal.mxk.timebased.period')"><a-input v-model:value="form.period" disabled /></a-form-item>
              <a-form-item :label="t('portal.mxk.timebased.sharedSecret')"><a-input :value="formattedSecret" disabled /></a-form-item>
              <a-form-item :label="t('portal.mxk.timebased.one-timePassword')"><a-input v-model:value="form.otpCode" :placeholder="t('ui.otpPlaceholder')" autocomplete="one-time-code" /></a-form-item>
            </a-form>
            <div class="portal-form-actions"><a-space wrap><a-button @click="generate">{{ t('portal.mxk.text.generate') }}</a-button><a-button @click="verify">{{ t('portal.mxk.text.verify') }}</a-button><a-button type="primary" :loading="saving" @click="save">{{ t('portal.mxk.text.save') }}</a-button></a-space></div>
          </a-col>
        </a-row>
      </a-spin>
    </a-card>
  </DefaultLayout>
</template>

<style scoped>
.timebased-content { width: 100%; }
.timebased-qr { display: flex; min-height: 320px; align-items: flex-start; justify-content: center; text-align: center; }
.timebased-qr img { width: min(300px, 100%); height: auto; }
</style>
