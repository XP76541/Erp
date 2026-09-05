<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
        <el-date-picker v-if="activeTab === 'inventory'" v-model="inventoryDate" type="date" value-format="YYYY-MM-DD" placeholder="库存日期" />
        <el-button type="primary" :icon="Search" :loading="loading" @click="loadActive">查询</el-button>
        <el-button plain :icon="Download" :loading="exporting" @click="downloadActive">导出当前报表</el-button>
      </div>
      <el-tabs v-model="activeTab" @tab-change="loadActive">
        <el-tab-pane label="销售日报" name="sales">
          <el-row :gutter="16" class="stats"><el-col :span="8"><el-statistic title="订单数" :value="salesTotals.orders" /></el-col><el-col :span="8"><el-statistic title="销售金额" :value="salesTotals.amount" :precision="2" prefix="¥" /></el-col><el-col :span="8"><el-statistic title="已出库金额" :value="salesTotals.shipped" :precision="2" prefix="¥" /></el-col></el-row>
          <el-table v-loading="loading" :data="salesRows" border stripe><el-table-column prop="reportDate" label="日期" width="130" /><el-table-column prop="totalOrders" label="订单数" width="110" /><el-table-column prop="totalAmount" label="销售金额" width="150"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column><el-table-column prop="shippedAmount" label="已出库金额" width="150"><template #default="{ row }">¥{{ money(row.shippedAmount) }}</template></el-table-column><el-table-column label="明细" min-width="260"><template #default="{ row }">{{ row.orders?.length || 0 }} 笔订单</template></el-table-column></el-table>
        </el-tab-pane>
        <el-tab-pane label="进销存汇总" name="inventory">
          <el-row :gutter="16" class="stats"><el-col :span="12"><el-statistic title="商品数" :value="inventory?.totalProducts || 0" /></el-col><el-col :span="12"><el-statistic title="库存总值" :value="inventory?.totalValue || 0" :precision="2" prefix="¥" /></el-col></el-row>
          <el-table v-loading="loading" :data="inventory?.products || []" border stripe><el-table-column prop="productName" label="商品" min-width="180" /><el-table-column prop="productSpec" label="规格" min-width="130" /><el-table-column prop="warehouseName" label="仓库" min-width="130" /><el-table-column prop="quantity" label="数量" width="110" /><el-table-column prop="unitCost" label="单位成本" width="130"><template #default="{ row }">¥{{ money(row.unitCost) }}</template></el-table-column><el-table-column prop="totalValue" label="库存金额" width="140"><template #default="{ row }">¥{{ money(row.totalValue) }}</template></el-table-column></el-table>
        </el-tab-pane>
        <el-tab-pane label="财务汇总" name="finance">
          <el-row :gutter="16" class="stats"><el-col v-for="item in financeCards" :key="item.label" :span="8"><el-statistic :title="item.label" :value="item.value" :precision="2" prefix="¥" /></el-col></el-row>
          <el-descriptions v-if="finance" :column="2" border><el-descriptions-item label="报表日期">{{ finance.reportDate }}</el-descriptions-item><el-descriptions-item label="净利润">¥{{ money(finance.netProfit) }}</el-descriptions-item><el-descriptions-item label="应收账款">¥{{ money(finance.totalReceivables) }}</el-descriptions-item><el-descriptions-item label="应付账款">¥{{ money(finance.totalPayables) }}</el-descriptions-item></el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search } from '@element-plus/icons-vue'
import { financeSummaryApi, inventorySummaryApi, salesDailyReportApi } from '@/api/report'
import type { FinanceSummaryResponse, InventorySummaryResponse, SalesDailyReportResponse } from '@/api/report'

const today = new Date().toISOString().slice(0, 10)
const dateRange = ref<[string, string]>([today, today])
const inventoryDate = ref(today)
const activeTab = ref('sales')
const loading = ref(false)
const exporting = ref(false)
const salesRows = ref<SalesDailyReportResponse[]>([])
const inventory = ref<InventorySummaryResponse>()
const finance = ref<FinanceSummaryResponse>()
const salesTotals = computed(() => salesRows.value.reduce((a, r) => ({ orders: a.orders + (r.totalOrders || 0), amount: a.amount + (r.totalAmount || 0), shipped: a.shipped + (r.shippedAmount || 0) }), { orders: 0, amount: 0, shipped: 0 }))
const financeCards = computed(() => [{ label: '销售额', value: finance.value?.totalSales || 0 }, { label: '采购额', value: finance.value?.totalPurchases || 0 }, { label: '库存总值', value: finance.value?.totalInventory || 0 }, { label: '净利润', value: finance.value?.netProfit || 0 }])
const rangeParams = () => ({ startDate: dateRange.value?.[0], endDate: dateRange.value?.[1] })
const money = (value?: number) => Number(value || 0).toFixed(2)

async function loadActive() {
  loading.value = true
  try {
    if (activeTab.value === 'sales') salesRows.value = await salesDailyReportApi.get(rangeParams())
    else if (activeTab.value === 'inventory') inventory.value = await inventorySummaryApi.get({ date: inventoryDate.value })
    else finance.value = await financeSummaryApi.get(rangeParams())
  } finally { loading.value = false }
}
async function downloadActive() {
  exporting.value = true
  try {
    const response = activeTab.value === 'sales' ? await salesDailyReportApi.export(rangeParams()) : activeTab.value === 'inventory' ? await inventorySummaryApi.export({ date: inventoryDate.value }) : await financeSummaryApi.export(rangeParams())
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a'); link.href = url; link.download = `${activeTab.value}-report.xlsx`; link.click(); link.remove(); URL.revokeObjectURL(url)
    ElMessage.success('报表导出成功')
  } finally { exporting.value = false }
}
onMounted(loadActive)
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; margin-bottom: 18px; flex-wrap: wrap; }
.stats { margin: 4px 0 20px; }
</style>
