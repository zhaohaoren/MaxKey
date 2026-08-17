<script setup lang="ts">
import * as echarts from 'echarts'
import { ApartmentOutlined, AppstoreOutlined, TeamOutlined, UserAddOutlined } from '@ant-design/icons-vue'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch, type Component } from 'vue'
import { useI18n } from 'vue-i18n'
import chinaMap from '../assets/maps/china.json'
import worldMap from '../assets/maps/world.zh.json'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminGet } from '../api'

interface ReportItem {
  reportstring?: string
  reportcount?: number | string
  appname?: string
}

type MapMode = 'china' | 'world'

const { t } = useI18n({ useScope: 'global' })
const loading = ref(true)
const error = ref('')
const data = reactive<Record<string, unknown>>({})
const dayChartRef = ref<HTMLElement>()
const monthChartRef = ref<HTMLElement>()
const mapChartRef = ref<HTMLElement>()
const mapType = ref<MapMode>('china')
const sparkData = [7, 5, 4, 2, 4, 7, 5, 6, 5, 9, 6, 3, 1, 5, 3, 6, 5]
const metricIcons: Record<string, Component> = {
  users: UserAddOutlined,
  depts: ApartmentOutlined,
  groups: TeamOutlined,
  apps: AppstoreOutlined,
}
let dayChart: echarts.ECharts | undefined
let monthChart: echarts.ECharts | undefined
let mapChart: echarts.ECharts | undefined

echarts.registerMap('maxkey-china', chinaMap as never)
echarts.registerMap('maxkey-world', worldMap as never)

const cards = computed(() => [
  {
    key: 'users',
    value: `${number('newUsers')}/${number('totalUsers')}`,
    label: `${t('admin.mxk.home.newUsers')}/${t('admin.mxk.home.totalUsers')}`,
    tone: 'primary',
  },
  { key: 'depts', value: number('totalDepts'), label: t('admin.mxk.home.totalDepts'), tone: 'orange' },
  { key: 'groups', value: number('totalGroups'), label: t('admin.mxk.home.totalGroups'), tone: 'success' },
  { key: 'apps', value: number('totalApps'), label: t('admin.mxk.home.totalApps'), tone: 'magenta' },
])

const activeMapRows = computed(() => topList(mapType.value === 'china' ? 'reportProvince' : 'reportCountry'))

function number(key: string) {
  return Number(data[key] || 0)
}

function list(key: string): ReportItem[] {
  return Array.isArray(data[key]) ? data[key] as ReportItem[] : []
}

function topList(key: string) {
  return list(key).slice(0, 10)
}

function count(item: ReportItem) {
  return Number(item.reportcount || 0)
}

function chartOption(items: ReportItem[], color: string): echarts.EChartsOption {
  return {
    animationDuration: 500,
    color: [color],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 24, right: 20, bottom: 42, left: 54 },
    xAxis: {
      type: 'category',
      data: items.map(item => item.reportstring || ''),
      axisTick: { alignWithLabel: true },
      axisLabel: { color: '#8c8c8c', hideOverlap: true },
      axisLine: { lineStyle: { color: '#d9d9d9' } },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#8c8c8c' },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [{
      type: 'bar',
      data: items.map(count),
      barMaxWidth: 36,
      itemStyle: { borderRadius: [3, 3, 0, 0] },
    }],
  }
}

function mapOption(): echarts.EChartsOption {
  const isChina = mapType.value === 'china'
  const items = list(isChina ? 'reportProvince' : 'reportCountry')
  const max = Math.max(1, ...items.map(count))
  return {
    animationDuration: 500,
    tooltip: {
      trigger: 'item',
      formatter: params => {
        const item = params as { name?: string; value?: number | string }
        return `${item.name || '-'}<br/>${t('admin.mxk.home.accessPV')}: ${Number(item.value || 0)}`
      },
    },
    visualMap: {
      type: 'continuous',
      min: 0,
      max,
      left: 12,
      bottom: 18,
      calculable: true,
      text: [String(max), '0'],
      inRange: { color: ['#f0f8ff', '#85daef', '#5475f5', '#4b0082', '#dc143c'] },
      textStyle: { color: '#8c8c8c' },
    },
    series: [{
      name: t('admin.mxk.home.accessPV'),
      type: 'map',
      map: isChina ? 'maxkey-china' : 'maxkey-world',
      roam: true,
      scaleLimit: { min: 0.8, max: 8 },
      label: { show: isChina, color: '#595959', fontSize: 10 },
      emphasis: { label: { show: true }, itemStyle: { areaColor: '#ffd666' } },
      itemStyle: { areaColor: '#f5f5f5', borderColor: '#fff', borderWidth: 0.8 },
      data: items.map(item => ({ name: item.reportstring || 'Other', value: count(item) })),
    }],
  }
}

function renderCharts() {
  if (dayChartRef.value) {
    dayChart ||= echarts.init(dayChartRef.value)
    dayChart.setOption(chartOption(list('reportDayHour'), '#3f3ff3'), true)
  }
  if (monthChartRef.value) {
    monthChart ||= echarts.init(monthChartRef.value)
    monthChart.setOption(chartOption(list('reportMonth'), '#3f3ff3'), true)
  }
  if (mapChartRef.value) {
    mapChart ||= echarts.init(mapChartRef.value)
    mapChart.setOption(mapOption(), true)
  }
}

function resizeCharts() {
  dayChart?.resize()
  monthChart?.resize()
  mapChart?.resize()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    Object.assign(data, await adminGet<Record<string, unknown>>('/dashboard'))
    await nextTick()
    renderCharts()
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('ui.dashboardLoadError')
  } finally {
    loading.value = false
  }
}

watch(mapType, async () => {
  await nextTick()
  mapChart?.setOption(mapOption(), true)
  mapChart?.resize()
})

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  void load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  dayChart?.dispose()
  monthChart?.dispose()
  mapChart?.dispose()
})
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title">
      <h1>{{ t('admin.mxk.menu.home') }}</h1>
      <a-button :loading="loading" @click="load">{{ t('ui.refresh') }}</a-button>
    </div>

    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-spin :spinning="loading">
      <section class="dashboard-metrics">
        <article v-for="card in cards" :key="card.key" class="metric-card" :class="`metric-card--${card.tone}`">
          <span class="metric-card__icon"><component :is="metricIcons[card.key]" /></span>
          <div class="metric-card__copy">
            <strong>{{ card.value }}</strong>
            <span>{{ card.label }}</span>
          </div>
          <div class="metric-spark" aria-hidden="true">
            <i v-for="(value, index) in sparkData" :key="index" :style="{ height: `${value * 3 + 4}px` }" />
          </div>
        </article>
      </section>

      <a-card :bordered="false" class="dashboard-card">
        <template #title>
          <div class="chart-title">
            <span>{{ t('admin.mxk.home.dayAccessCount') }}</span>
            <span class="chart-tag chart-tag--blue">◷ {{ t('admin.mxk.home.accessCount') }} <b>{{ number('dayCount') }}</b></span>
            <span class="chart-tag chart-tag--green">ⓘ {{ t('admin.mxk.home.onlineUsers') }} <b>{{ number('onlineUsers') }}</b></span>
          </div>
        </template>
        <div ref="dayChartRef" class="access-chart" />
      </a-card>

      <a-card :bordered="false" class="dashboard-card">
        <template #title>
          <div class="chart-title">
            <span>{{ t('admin.mxk.home.monthAccessCount') }}</span>
            <span class="chart-tag chart-tag--blue">◷ {{ t('admin.mxk.home.accessCount') }} <b>{{ number('monthCount') }}</b></span>
            <span class="chart-tag chart-tag--green">ⓘ {{ t('admin.mxk.home.activeUsers') }} <b>{{ number('activeUsers') }}</b></span>
          </div>
        </template>
        <div ref="monthChartRef" class="access-chart" />
      </a-card>

      <a-card :bordered="false" class="dashboard-card dashboard-map-card">
        <template #title>
          <div class="map-card-title">
            <span>{{ t('admin.mxk.home.monthProvinceAccessCount') }}</span>
            <a-radio-group v-model:value="mapType" button-style="solid" size="small">
              <a-radio-button value="china">{{ t('admin.mxk.home.maptype.china') }}</a-radio-button>
              <a-radio-button value="world">{{ t('admin.mxk.home.maptype.world') }}</a-radio-button>
            </a-radio-group>
          </div>
        </template>
        <div class="map-and-ranking">
          <div ref="mapChartRef" class="map-chart" />
          <a-table :data-source="activeMapRows" :pagination="false" size="small" row-key="reportstring" class="ranking-table">
            <a-table-column :title="t('admin.mxk.home.number')" width="72" align="center">
              <template #default="{ index }">{{ index + 1 }}</template>
            </a-table-column>
            <a-table-column
              :title="t(mapType === 'china' ? 'admin.mxk.home.province' : 'admin.mxk.home.country')"
              data-index="reportstring"
              align="center"
            />
            <a-table-column :title="t('admin.mxk.home.accessPV')" data-index="reportcount" align="center" />
          </a-table>
        </div>
      </a-card>

      <div class="dashboard-tables">
        <a-card :title="t('admin.mxk.home.monthAppCount')" :bordered="false">
          <a-table :data-source="list('reportApp')" :pagination="false" size="small" row-key="appname">
            <a-table-column :title="t('admin.mxk.home.appName')" data-index="appname" />
            <a-table-column :title="t('admin.mxk.home.accessCount')" data-index="reportcount" />
          </a-table>
        </a-card>
        <a-card :title="t('admin.mxk.home.monthBrowserCount')" :bordered="false">
          <a-table :data-source="list('reportBrowser')" :pagination="false" size="small" row-key="reportstring">
            <a-table-column :title="t('admin.mxk.home.browser')" data-index="reportstring" />
            <a-table-column :title="t('admin.mxk.home.accessCount')" data-index="reportcount" />
          </a-table>
        </a-card>
      </div>
    </a-spin>
  </DefaultLayout>
</template>

<style scoped>
.dashboard-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 20px;
}

.metric-card {
  display: flex;
  min-width: 0;
  min-height: 112px;
  box-sizing: border-box;
  align-items: center;
  gap: 14px;
  overflow: hidden;
  padding: 20px;
  border: 1px solid #e7ebf3;
  border-radius: 16px;
  color: #3f3ff3;
  background: #fff;
  box-shadow: 0 8px 24px rgba(35, 47, 88, .045);
}

.metric-card--primary { --metric-color: #3f3ff3; --metric-bg: #eeeeff; }
.metric-card--orange { --metric-color: #d78318; --metric-bg: #fff4df; }
.metric-card--success { --metric-color: #198b67; --metric-bg: #e8f8f1; }
.metric-card--magenta { --metric-color: #d34879; --metric-bg: #ffedf4; }

.metric-card__icon {
  display: grid;
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  border-radius: 13px;
  color: var(--metric-color);
  background: var(--metric-bg);
  font-size: 20px;
  place-items: center;
}

.metric-card__copy {
  min-width: 0;
  flex: 1;
}

.metric-card__copy strong {
  display: block;
  margin-bottom: 5px;
  color: #192338;
  font-size: 25px;
  font-weight: 650;
  line-height: 1.2;
}

.metric-card__copy span {
  display: block;
  overflow: hidden;
  color: #8993a5;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-spark {
  display: flex;
  height: 42px;
  flex: 0 0 31%;
  align-items: flex-end;
  justify-content: center;
  gap: 3px;
  padding: 0 0 3px;
}

.metric-spark i {
  width: 3px;
  max-height: 36px;
  border-radius: 2px 2px 0 0;
  background: color-mix(in srgb, var(--metric-color) 52%, transparent);
}

.dashboard-card { margin-bottom: 20px; }
.access-chart { width: 100%; height: 275px; }

.chart-title,
.map-card-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-weight: 500;
}

.chart-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 1px 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 400;
  line-height: 22px;
}

.chart-tag b { margin-left: 2px; }
.chart-tag--blue { border-color: #91caff; color: #0958d9; background: #e6f4ff; }
.chart-tag--green { border-color: #b7eb8f; color: #389e0d; background: #f6ffed; }

.map-card-title { justify-content: space-between; }
.map-and-ranking { display: grid; grid-template-columns: minmax(0, 2fr) minmax(360px, 1fr); gap: 24px; align-items: start; }
.map-chart { width: 100%; height: 500px; }
.ranking-table { min-width: 0; }
.dashboard-tables { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }

@media (max-width: 1100px) {
  .dashboard-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .map-and-ranking { grid-template-columns: 1fr; }
  .map-chart { height: 420px; }
}

@media (max-width: 720px) {
  .dashboard-metrics,
  .dashboard-tables { grid-template-columns: 1fr; }
  .map-chart { height: 340px; }
  .map-and-ranking { gap: 12px; }
  .chart-title { align-items: flex-start; flex-direction: column; }
}
</style>
