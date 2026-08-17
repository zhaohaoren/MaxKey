<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { get, put } from '../api'

const props = defineProps<{ open: boolean; appId: string }>()
const emit = defineEmits<{ 'update:open': [value: boolean] }>()
const { t } = useI18n({ useScope: 'global' })
const loading = ref(false)
const saving = ref(false)
const form = reactive<Record<string, unknown>>({})

async function initialize() {
  Object.keys(form).forEach(key => delete form[key])
  if (!props.appId) return
  loading.value = true
  try {
    Object.assign(form, await get<Record<string, unknown>>(`/authz/credential/get/${encodeURIComponent(props.appId)}`))
    form.confirmPassword = form.relatedPassword || ''
  } catch (error) {
    message.error(error instanceof Error ? error.message : t('ui.credentialLoadError'))
    emit('update:open', false)
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!String(form.relatedUsername || '').trim() || !String(form.relatedPassword || '')) {
    message.warning(t('ui.credentialRequired'))
    return
  }
  if (form.relatedPassword !== form.confirmPassword) {
    message.warning(t('ui.passwordMismatch'))
    return
  }
  saving.value = true
  try {
    await put('/authz/credential/update', form)
    message.success(t('portal.mxk.alert.operate.success'))
  } catch (error) {
    message.error(error instanceof Error ? error.message : t('portal.mxk.alert.operate.error'))
  } finally {
    saving.value = false
  }
}

watch(() => [props.open, props.appId] as const, ([open]) => { if (open) void initialize() })
</script>

<template>
  <a-modal
    :open="open"
    :title="t('ui.configureAccount')"
    width="550px"
    :confirm-loading="saving"
    :ok-text="t('portal.mxk.text.submit')"
    :cancel-text="t('portal.mxk.text.close')"
    @ok="save"
    @cancel="emit('update:open', false)"
  >
    <a-spin :spinning="loading">
      <a-form :label-col="{ span: 7 }" :wrapper-col="{ span: 15 }">
        <a-form-item :label="t('portal.mxk.password.displayName')"><a-input v-model:value="form.displayName" disabled /></a-form-item>
        <a-form-item :label="t('portal.mxk.accounts.username')"><a-input v-model:value="form.username" disabled /></a-form-item>
        <a-form-item :label="t('portal.mxk.accounts.appName')"><a-input v-model:value="form.appName" disabled /></a-form-item>
        <a-form-item required :label="t('portal.mxk.accounts.relatedUsername')"><a-input v-model:value="form.relatedUsername" /></a-form-item>
        <a-form-item required :label="t('portal.mxk.accounts.relatedPassword')"><a-input-password v-model:value="form.relatedPassword" /></a-form-item>
        <a-form-item required :label="t('portal.mxk.password.confirmPassword')"><a-input-password v-model:value="form.confirmPassword" /></a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>
