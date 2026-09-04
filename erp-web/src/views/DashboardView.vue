<template>
  <div class="dashboard">
    <section class="hero">
      <div>
        <p class="eyebrow">业务总览 · {{ dateLabel }}</p>
        <h2>{{ greeting }}，{{ userStore.realName || '同事' }}</h2>
        <p class="hero-desc">这里是今天的业务进度。</p>
      </div>
      <div class="hero-actions">
        <span class="updated-at">数据日期：{{ dateLabel }}</span>
        <el-button type="primary" :loading="refreshing" :icon="Refresh" @click="loadDashboard">
          刷新数据
        </el-button>
      </div>
    </section>

    <el-alert
      v-if="!userStore.rolesLoaded"
      title="正在确认当前用户权限…"
      type="info"
      :closable="false"
      show-icon
      class="permission-alert"
    />

    <section class="kpi-grid" aria-label="关键指标">
      <article v-for="card in kpiCards" :key="card.label" class="kpi-card" :class="`kpi-${card.tone}`">
        <div class="kpi-icon"><el-icon><component :is="card.icon" /></el-icon></div>
        <div class="kpi-content">
          <span class="kpi-label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.hint }}</small>
        </div>
      </article>
    </section>

    <el-row :gutter="18" class="content-row">
      <el-col :xs="24" :lg="15">
        <el-card shadow="never" class="panel sales-panel">
          <template #header>
            <div class="panel-header">
              <div><h3>销售概览</h3><span>{{ rangeLabel }}</span></div>
              <el-button link type="primary" @click="go('/finance/reports')">查看报表</el-button>
            </div>
          </template>
          <div v-loading="widgets.sales.loading" class="sales-content">
            <el-alert v-if="widgets.sales.error" :title="widgets.sales.error" type="warning" show-icon :closable="false">
              <template #default><el-button link type="primary" @click="loadSales">重试</el-button></template>
            </el-alert>
            <el-empty v-else-if="!salesRows.length" description="当前日期范围暂无销售数据" :image-size="72" />
            <template v-else>
              <div class="sales-summary">
                <div><span>订单总数</span><b>{{ salesTotals.orders }}</b></div>
                <div><span>销售金额</span><b>¥{{ money(salesTotals.amount) }}</b></div>
                <div><span>已出库金额</span><b>¥{{ money(salesTotals.shipped) }}</b></div>
              </div>
              <div class="trend-list" aria-label="每日销售趋势">
                <div v-for="row in salesRows" :key="row.reportDate" class="trend-row">
                  <span class="trend-date">{{ shortDate(row.reportDate) }}</span>
                  <div class="trend-track"><i :style="{ width: `${barWidth(row.totalAmount)}%` }" /></div>
                  <span class="trend-value">¥{{ money(row.totalAmount) }}</span>
                  <span class="trend-orders">{{ row.totalOrders }} 单</span>
                </div>
              </div>
            </template>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="9">
        <el-card shadow="never" class="panel warning-panel">
          <template #header>
            <div class="panel-header">
              <div><h3>库存预警</h3><span>需要关注的库存状态</span></div>
              <el-button link type="primary" @click="go('/inventory/stocks')">库存管理</el-button>
            </div>
          </template>
          <div v-loading="widgets.warnings.loading" class="warning-content">
            <el-alert v-if="widgets.warnings.error" :title="widgets.warnings.error" type="warning" show-icon :closable="false">
              <template #default><el-button link type="primary" @click="loadWarnings">重试</el-button></template>
            </el-alert>
            <el-empty v-else-if="!warningStats" description="暂无预警数据" :image-size="72" />
            <template v-else>
              <div class="warning-total"><strong>{{ warningTotal }}</strong><span>项待处理预警</span></div>
              <div class="warning-grid">
                <div v-for="item in warningItems" :key="item.label" class="warning-item">
                  <span class="dot" :class="item.tone" />
                  <span>{{ item.label }}</span><b>{{ item.value }}</b>
                </div>
              </div>
              <div v-if="activeWarnings.length" class="active-warning-list">
                <div v-for="warning in activeWarnings.slice(0, 3)" :key="warning.id">
                  <span>{{ warning.productName || '未知商品' }}</span>
                  <el-tag size="small" :type="warningTagType(warning.warningType)">{{ warningTypeLabel(warning.warningType) }}</el-tag>
                </div>
              </div>
            </template>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18" class="content-row">
      <el-col v-if="canViewFinance" :xs="24" :lg="14">
        <el-card shadow="never" class="panel finance-panel">
          <template #header>
            <div class="panel-header">
              <div><h3>经营摘要</h3><span>按所选期间统计</span></div>
              <el-button link type="primary" @click="go('/finance/reports')">查看详情</el-button>
            </div>
          </template>
          <div v-loading="widgets.finance.loading" class="finance-content">
            <el-alert v-if="widgets.finance.error" :title="widgets.finance.error" type="warning" show-icon :closable="false">
              <template #default><el-button link type="primary" @click="loadFinance">重试</el-button></template>
            </el-alert>
            <el-empty v-else-if="!finance" description="暂无经营摘要" :image-size="72" />
            <div v-else class="finance-grid">
              <div><span>销售额</span><strong>¥{{ money(finance.totalSales) }}</strong></div>
              <div><span>采购额</span><strong>¥{{ money(finance.totalPurchases) }}</strong></div>
              <div><span>应收余额</span><strong>¥{{ money(finance.totalReceivables) }}</strong></div>
              <div><span>应付余额</span><strong>¥{{ money(finance.totalPayables) }}</strong></div>
              <div><span>库存总值</span><strong>{{ canShowCost ? `¥${money(finance.totalInventory)}` : '不可用' }}</strong></div>
              <div><span>净利润</span><strong>{{ canShowCost && finance.netProfit != null ? `¥${money(finance.netProfit)}` : '不可用' }}</strong></div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col v-if="canViewFinance" :xs="24" :lg="10">
        <el-card shadow="never" class="panel aging-panel">
          <template #header>
            <div class="panel-header">
              <div><h3>应收账龄</h3><span>截至 {{ today }}</span></div>
              <el-button link type="primary" @click="go('/finance/aging-report')">账龄分析</el-button>
            </div>
          </template>
          <div v-loading="widgets.aging.loading" class="aging-content">
            <el-alert v-if="widgets.aging.error" :title="widgets.aging.error" type="warning" show-icon :closable="false">
              <template #default><el-button link type="primary" @click="loadAging">重试</el-button></template>
            </el-alert>
            <el-empty v-else-if="!agingRows.length" description="暂无未收款账龄数据" :image-size="72" />
            <div v-else class="aging-list">
              <div v-for="row in agingRows" :key="row.agingBucket" class="aging-row">
                <span>{{ row.agingBucket }}</span><div class="aging-track"><i :style="{ width: `${agingWidth(row.totalRemaining)}%` }" /></div><b>¥{{ money(row.totalRemaining) }}</b>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col v-if="!canViewFinance" :xs="24" :lg="14">
        <el-card shadow="never" class="panel tips-panel">
          <template #header><div class="panel-header"><div><h3>工作提醒</h3><span>根据当前角色展示可用操作</span></div></div></template>
          <div class="tips-content"><el-icon><InfoFilled /></el-icon><p>仪表盘只展示当前账号有权访问的数据。成本、库存金额和利润等敏感信息不会向当前角色展示。</p></div>
        </el-card>
      </el-col>
    </el-row>

    <section class="quick-section">
      <div class="section-title"><h3>快捷入口</h3><span>快速进入常用业务</span></div>
      <div class="quick-grid">
        <button v-for="item in quickLinks" :key="item.path" type="button" class="quick-link" @click="go(item.path)">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span><small>{{ item.description }}</small><el-icon class="arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight, Box, Coin, DataAnalysis, Document, InfoFilled, Refresh, ShoppingCart, SoldOut, Warning,
} from '@element-plus/icons-vue'
import { financeApi, type AgingAnalysisResponse } from '@/api/finance'
import { financeSummaryApi, salesDailyReportApi } from '@/api/report'
import type { FinanceSummaryResponse, SalesDailyReportResponse } from '@/api/report'
import { inventoryWarningApi } from '@/api/inventory'
import type { InventoryWarningStats, InventoryWarning } from '@/api/inventory'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const today = localDate()
const dateRange = ref<[string, string]>([today, today])
const refreshing = ref(false)
const requestVersion = ref(0)
const salesRows = ref<SalesDailyReportResponse[]>([])
const finance = ref<FinanceSummaryResponse>()
const agingRows = ref<AgingAnalysisResponse[]>([])
const warningStats = ref<InventoryWarningStats>()
const activeWarnings = ref<InventoryWarning[]>([])

const widgets = reactive({
  sales: { loading: false, error: '' }, finance: { loading: false, error: '' },
  aging: { loading: false, error: '' }, warnings: { loading: false, error: '' },
})

const roles = computed(() => userStore.roleCodes)
const canViewFinance = computed(() => roles.value.some((role) => ['ADMIN', 'BOSS', 'FINANCE'].includes(role)))
const canViewCost = computed(() => canViewFinance.value && finance.value?.costDataAvailable !== false)
const canViewSales = computed(() => roles.value.some((role) => ['ADMIN', 'BOSS', 'FINANCE', 'SALES'].includes(role)))
const canViewWarnings = computed(() => roles.value.some((role) => ['ADMIN', 'BOSS', 'FINANCE', 'WAREHOUSE'].includes(role)))
const dateLabel = computed(() => today === localDate() ? `今日 ${today}` : today)
const rangeLabel = computed(() => dateRange.value[0] === dateRange.value[1] ? dateRange.value[0] : `${dateRange.value[0]} 至 ${dateRange.value[1]}`)
const greeting = computed(() => { const hour = new Date().getHours(); return hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好' })

const salesTotals = computed(() => salesRows.value.reduce((total, row) => ({
  orders: total.orders + Number(row.totalOrders || 0), amount: total.amount + Number(row.totalAmount || 0), shipped: total.shipped + Number(row.shippedAmount || 0),
}), { orders: 0, amount: 0, shipped: 0 }))
const maxSales = computed(() => Math.max(...salesRows.value.map((row) => Number(row.totalAmount || 0)), 1))
const maxAging = computed(() => Math.max(...agingRows.value.map((row) => Number(row.totalRemaining || 0)), 1))
const warningTotal = computed(() => warningStats.value ? Number(warningStats.value.stockOutCount || 0) + Number(warningStats.value.stockOverCount || 0) + Number(warningStats.value.expiringCount || 0) + Number(warningStats.value.spoiledCount || 0) : 0)

const kpiCards = computed(() => {
  const cards = [
    { label: '销售金额', value: `¥${money(salesTotals.value.amount)}`, hint: `${salesTotals.value.orders} 笔订单`, tone: 'blue', icon: SoldOut },
    { label: '已出库金额', value: `¥${money(salesTotals.value.shipped)}`, hint: '销售履约进度', tone: 'green', icon: Box },
  ]
  if (canViewFinance.value) {
    cards.push({ label: '应收余额', value: finance.value ? `¥${money(finance.value.totalReceivables)}` : '—', hint: '待回款金额', tone: 'orange', icon: Coin })
    cards.push({ label: '应付余额', value: finance.value ? `¥${money(finance.value.totalPayables)}` : '—', hint: '待付款金额', tone: 'purple', icon: ShoppingCart })
  } else if (canViewWarnings.value) {
    cards.push({ label: '库存预警', value: String(warningTotal.value), hint: '项待处理', tone: 'orange', icon: Warning })
    cards.push({ label: '销售订单', value: String(salesTotals.value.orders), hint: '所选期间订单数', tone: 'purple', icon: Document })
  }
  return cards
})

const warningItems = computed(() => warningStats.value ? [
  { label: '缺货', value: warningStats.value.stockOutCount || 0, tone: 'danger' },
  { label: '超量', value: warningStats.value.stockOverCount || 0, tone: 'warning' },
  { label: '临期', value: warningStats.value.expiringCount || 0, tone: 'info' },
  { label: '呆滞', value: warningStats.value.spoiledCount || 0, tone: 'muted' },
] : [])

const quickLinks = computed(() => {
  const links = []
  if (roles.value.some((role) => ['ADMIN', 'SALES'].includes(role))) {
    links.push({ path: '/sales/orders', label: '销售订单', description: '管理销售订单', icon: SoldOut })
    links.push({ path: '/sales/outbounds', label: '销售出库', description: '处理发货业务', icon: Box })
  }
  if (roles.value.some((role) => ['ADMIN', 'WAREHOUSE'].includes(role))) {
    links.push({ path: '/inventory/stocks', label: '即时库存', description: '查看库存数量', icon: Box })
    links.push({ path: '/inventory/ledgers', label: '出入库流水', description: '追踪库存变动', icon: DataAnalysis })
  }
  if (canViewFinance.value) {
    links.push({ path: '/finance/receivables', label: '应收核销', description: '登记客户收款', icon: Coin })
    links.push({ path: '/finance/reports', label: '经营报表', description: '查看经营分析', icon: DataAnalysis })
  }
  return links
})

function localDate(date = new Date()) {
  const year = date.getFullYear(); const month = String(date.getMonth() + 1).padStart(2, '0'); const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
function money(value: number | null | undefined) { return value == null || Number.isNaN(Number(value)) ? '—' : Number(value).toFixed(2) }
function shortDate(value: string) { return value?.slice(5) || '—' }
function barWidth(value: number | null | undefined) { return Math.max(Number(value || 0) / maxSales.value * 100, Number(value || 0) > 0 ? 4 : 0) }
function agingWidth(value: number | null | undefined) { return Math.max(Number(value || 0) / maxAging.value * 100, Number(value || 0) > 0 ? 4 : 0) }
function warningTypeLabel(type: string) { return ({ STOCK_OUT: '缺货', STOCK_OVER: '超量', EXPIRING: '临期', SPOILED: '呆滞' } as Record<string, string>)[type] || type || '预警' }
function warningTagType(type: string) { return ({ STOCK_OUT: 'danger', STOCK_OVER: 'warning', EXPIRING: 'info', SPOILED: '' } as Record<string, string>)[type] || 'info' }
function go(path: string) { router.push(path) }

async function runWidget<T>(key: keyof typeof widgets, loader: () => Promise<T>, target: (value: T) => void) {
  const version = requestVersion.value; widgets[key].loading = true; widgets[key].error = ''
  try { const result = await loader(); if (version === requestVersion.value) target(result) }
  catch (error) { if (version === requestVersion.value) widgets[key].error = error instanceof Error ? error.message : '加载失败，请重试' }
  finally { if (version === requestVersion.value) widgets[key].loading = false }
}
async function loadSales() { if (!canViewSales.value) return; await runWidget('sales', () => salesDailyReportApi.get({ startDate: dateRange.value[0], endDate: dateRange.value[1] }), (data) => { salesRows.value = Array.isArray(data) ? data : [] }) }
async function loadFinance() { if (!canViewFinance.value) return; await runWidget('finance', () => financeSummaryApi.get({ startDate: dateRange.value[0], endDate: dateRange.value[1] }), (data) => { finance.value = data }) }
async function loadAging() { if (!canViewFinance.value) return; await runWidget('aging', () => financeApi.getAgingAnalysis(today), (data) => { agingRows.value = Array.isArray(data) ? data : [] }) }
async function loadWarnings() {
  if (!canViewWarnings.value) return
  const version = requestVersion.value; widgets.warnings.loading = true; widgets.warnings.error = ''
  try {
    const [stats, active] = await Promise.all([inventoryWarningApi.getWarningStats(), inventoryWarningApi.getActiveWarnings()])
    if (version === requestVersion.value) { warningStats.value = stats; activeWarnings.value = Array.isArray(active) ? active : [] }
  } catch (error) { if (version === requestVersion.value) widgets.warnings.error = error instanceof Error ? error.message : '加载失败，请重试' }
  finally { if (version === requestVersion.value) widgets.warnings.loading = false }
}
async function loadDashboard() {
  if (dateRange.value[0] > dateRange.value[1]) { ElMessage.warning('开始日期不能晚于结束日期'); return }
  requestVersion.value += 1; refreshing.value = true
  try { await Promise.all([loadSales(), loadFinance(), loadAging(), loadWarnings()]) } finally { refreshing.value = false }
}
onMounted(loadDashboard)
</script>

<style scoped>
.dashboard { max-width: 1480px; margin: 0 auto; color: #24313d; }
.hero { display: flex; justify-content: space-between; align-items: center; gap: 24px; padding: 24px 26px; margin-bottom: 18px; border: 1px solid #dce8eb; border-radius: 9px; color: #eaf4f5; background: #28596b; box-shadow: none; }
.eyebrow { margin: 0 0 8px; color: #b9d5da; font-size: 12px; letter-spacing: .5px; }
.hero h2 { margin: 0; font-size: 24px; font-weight: 600; }
.hero-desc { margin: 8px 0 0; color: #c4dadd; font-size: 14px; }
.hero-actions { display: flex; align-items: center; gap: 18px; white-space: nowrap; }
.updated-at { color: #c0d9dd; font-size: 13px; }
.permission-alert { margin-bottom: 18px; }
.kpi-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-bottom: 18px; }
.kpi-card { display: flex; align-items: center; gap: 14px; min-height: 102px; padding: 18px; border: 1px solid var(--erp-border); border-radius: 8px; background: #fff; box-shadow: none; }
.kpi-icon { display: grid; place-items: center; width: 38px; height: 38px; border-radius: 7px; font-size: 19px; }
.kpi-blue .kpi-icon { color: #2f6f8f; background: #e7f1f3; }.kpi-green .kpi-icon { color: #3b806d; background: #e8f3ef; }.kpi-orange .kpi-icon { color: #ae762e; background: #f8f0e3; }.kpi-purple .kpi-icon { color: #706685; background: #efedf3; }
.kpi-content { min-width: 0; }.kpi-label, .kpi-content small { display: block; color: #7b8792; font-size: 12px; }.kpi-content strong { display: block; margin: 6px 0 4px; color: #263640; font-size: 21px; font-weight: 650; }.kpi-content small { color: #a0aab1; }
.content-row { margin-bottom: 18px; }.panel { height: 100%; border: 1px solid var(--erp-border); border-radius: 8px; box-shadow: none; }.panel :deep(.el-card__header) { padding: 16px 18px; border-bottom: 1px solid #eef1f3; }.panel :deep(.el-card__body) { padding: 18px; }.panel-header { display: flex; align-items: center; justify-content: space-between; gap: 10px; }.panel-header h3, .section-title h3 { margin: 0 0 4px; color: #2c3d48; font-size: 15px; }.panel-header span, .section-title span { color: #8d99a2; font-size: 12px; }
.sales-content, .warning-content, .finance-content, .aging-content { min-height: 235px; }.sales-summary { display: flex; gap: 32px; padding-bottom: 16px; border-bottom: 1px solid #eef1f3; }.sales-summary div { display: flex; flex-direction: column; gap: 5px; }.sales-summary span, .finance-grid span { color: #7f8b94; font-size: 12px; }.sales-summary b { color: #2b3e4b; font-size: 17px; }.trend-list { margin-top: 15px; }.trend-row { display: grid; grid-template-columns: 55px 1fr 90px 45px; align-items: center; gap: 10px; min-height: 34px; font-size: 12px; }.trend-date, .trend-orders { color: #8a969e; }.trend-value { color: #526570; text-align: right; }.trend-track, .aging-track { height: 6px; overflow: hidden; border-radius: 3px; background: #edf1f3; }.trend-track i, .aging-track i { display: block; height: 100%; border-radius: inherit; background: #5d9aae; transition: width .3s ease; }
.warning-total { display: flex; align-items: baseline; gap: 9px; padding-bottom: 17px; }.warning-total strong { color: #ad762d; font-size: 30px; }.warning-total span { color: #818d96; font-size: 13px; }.warning-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }.warning-item { display: flex; align-items: center; gap: 7px; color: #66757e; font-size: 13px; }.warning-item b { margin-left: auto; color: #31434f; }.dot { width: 7px; height: 7px; border-radius: 50%; }.dot.danger { background: #c85d62; }.dot.warning { background: #c8923a; }.dot.info { background: #6097a9; }.dot.muted { background: #9ba6ab; }.active-warning-list { margin-top: 18px; padding-top: 9px; border-top: 1px solid #eef1f3; }.active-warning-list > div { display: flex; justify-content: space-between; align-items: center; padding: 7px 0; color: #5d6c75; font-size: 13px; }
.finance-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 19px 15px; padding-top: 4px; }.finance-grid div { display: flex; flex-direction: column; gap: 6px; }.finance-grid strong { color: #30434f; font-size: 16px; }.aging-list { padding-top: 4px; }.aging-row { display: grid; grid-template-columns: 70px 1fr 90px; align-items: center; gap: 10px; min-height: 36px; color: #687680; font-size: 12px; }.aging-row b { color: #52636e; text-align: right; font-weight: 500; }.aging-track i { background: #c8923a; }.tips-content { display: flex; align-items: center; gap: 15px; min-height: 235px; color: #77858d; }.tips-content .el-icon { color: #5d9aae; font-size: 24px; }.tips-content p { margin: 0; line-height: 1.8; font-size: 13px; }
.quick-section { padding: 4px 0 20px; }.section-title { display: flex; align-items: baseline; gap: 10px; margin: 5px 0 13px; }.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 11px; }.quick-link { position: relative; display: flex; flex-direction: column; align-items: flex-start; gap: 6px; min-height: 98px; padding: 16px; border: 1px solid var(--erp-border); border-radius: 8px; color: #354852; background: #fff; text-align: left; cursor: pointer; transition: border-color .2s, background .2s; }.quick-link:hover, .quick-link:focus-visible { border-color: #9ab8c5; background: #fbfdfd; transform: none; }.quick-link > .el-icon { color: #3e7d93; font-size: 20px; }.quick-link span { font-size: 14px; font-weight: 600; }.quick-link small { color: #8f9ba2; font-size: 12px; }.quick-link .arrow { position: absolute; right: 14px; top: 17px; color: #b2bec3; font-size: 14px; }
@media (max-width: 900px) { .hero { align-items: flex-start; flex-direction: column; padding: 22px; }.hero-actions { width: 100%; justify-content: space-between; }.kpi-grid { grid-template-columns: repeat(2, 1fr); }.quick-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 520px) { .kpi-grid, .quick-grid { grid-template-columns: 1fr; }.hero h2 { font-size: 22px; }.sales-summary { gap: 15px; flex-wrap: wrap; }.trend-row { grid-template-columns: 48px 1fr 78px; }.trend-orders { display: none; }.finance-grid { grid-template-columns: repeat(2, 1fr); }.hero-actions { align-items: flex-start; flex-direction: column; gap: 10px; } }
</style>
