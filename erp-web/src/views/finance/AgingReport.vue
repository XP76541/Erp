<template>
  <div class="page"><el-card shadow="never">
    <div class="toolbar"><el-date-picker v-model="cutoffDate" type="date" value-format="YYYY-MM-DD" placeholder="截止日期" /><el-button type="primary" :icon="Search" :loading="loading" @click="loadReport">查询</el-button><el-button plain :icon="Download" :disabled="!customerAging.length" @click="exportReport">导出明细</el-button></div>
    <el-row :gutter="16" class="stats-row"><el-col v-for="item in statCards" :key="item.label" :span="6"><el-statistic :title="item.label" :value="item.value" :precision="2" prefix="¥" /></el-col></el-row>
    <el-alert v-if="agingData.length === 0 && !loading" title="暂无账龄数据" type="info" :closable="false" />
    <el-table v-else v-loading="loading" :data="customerAging" border stripe><el-table-column prop="customerName" label="客户名称" min-width="150" /><el-table-column prop="totalAmount" label="应收总额" width="130" align="right"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column><el-table-column prop="totalPaid" label="已收金额" width="130" align="right"><template #default="{ row }">¥{{ money(row.totalPaid) }}</template></el-table-column><el-table-column prop="totalRemaining" label="未收金额" width="130" align="right"><template #default="{ row }">¥{{ money(row.totalRemaining) }}</template></el-table-column><el-table-column label="未到期" width="110" align="right"><template #default="{ row }">¥{{ money(row.notDue) }}</template></el-table-column><el-table-column label="1-30天" width="110" align="right"><template #default="{ row }">¥{{ money(row.days1to30) }}</template></el-table-column><el-table-column label="31-60天" width="110" align="right"><template #default="{ row }">¥{{ money(row.days31to60) }}</template></el-table-column><el-table-column label="61-90天" width="110" align="right"><template #default="{ row }">¥{{ money(row.days61to90) }}</template></el-table-column><el-table-column label="90天以上" width="110" align="right"><template #default="{ row }">¥{{ money(row.daysOver90) }}</template></el-table-column><el-table-column label="最大逾期" width="100" align="center"><template #default="{ row }">{{ row.maxOverdueDays > 0 ? `${row.maxOverdueDays}天` : '-' }}</template></el-table-column></el-table>
  </el-card></div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search } from '@element-plus/icons-vue'
import { financeApi } from '@/api/finance'
import type { AgingAnalysisResponse, ReceivableListResponse } from '@/api/finance'
interface CustomerAging { customerId: number; customerName: string; totalAmount: number; totalPaid: number; totalRemaining: number; notDue: number; days1to30: number; days31to60: number; days61to90: number; daysOver90: number; maxOverdueDays: number }
const loading = ref(false); const cutoffDate = ref(new Date().toISOString().slice(0, 10)); const agingData = ref<AgingAnalysisResponse[]>([]); const customerAging = ref<CustomerAging[]>([])
const stats = computed(() => agingData.value.reduce((a, row) => ({ total: a.total + row.totalAmount, paid: a.paid + row.totalPaid, remaining: a.remaining + row.totalRemaining, overdue: a.overdue + (row.agingBucket === '未到期' ? 0 : row.totalRemaining) }), { total: 0, paid: 0, remaining: 0, overdue: 0 }))
const statCards = computed(() => [{ label: '应收总额', value: stats.value.total }, { label: '已收总额', value: stats.value.paid }, { label: '未收总额', value: stats.value.remaining }, { label: '逾期总额', value: stats.value.overdue }])
const money = (value?: number) => Number(value || 0).toFixed(2)
async function loadReport() {
  loading.value = true
  try {
    const [analysis, result] = await Promise.all([
      financeApi.getAgingAnalysis(cutoffDate.value),
      financeApi.getReceivables({ page: 1, size: 500, endDate: cutoffDate.value }),
    ])
    agingData.value = analysis || []
    customerAging.value = groupByCustomer(result?.records || [])
  } catch {
    agingData.value = []
    customerAging.value = []
  } finally {
    loading.value = false
  }
}
function groupByCustomer(rows: ReceivableListResponse[]) { const grouped = new Map<number, CustomerAging>(); rows.forEach((row) => { const id = row.customerId; const name = row.customerName || `客户#${id}`; const item = grouped.get(id) || { customerId: id, customerName: name, totalAmount: 0, totalPaid: 0, totalRemaining: 0, notDue: 0, days1to30: 0, days31to60: 0, days61to90: 0, daysOver90: 0, maxOverdueDays: 0 }; item.totalAmount += row.amount || 0; item.totalPaid += row.paidAmount || 0; item.totalRemaining += row.remainingAmount || 0; item.maxOverdueDays = Math.max(item.maxOverdueDays, row.daysOverdue || 0); const bucket = row.agingBucket || '未到期'; if (bucket === '未到期') item.notDue += row.remainingAmount || 0; else if (bucket === '1-30天') item.days1to30 += row.remainingAmount || 0; else if (bucket === '31-60天') item.days31to60 += row.remainingAmount || 0; else if (bucket === '61-90天') item.days61to90 += row.remainingAmount || 0; else item.daysOver90 += row.remainingAmount || 0; grouped.set(id, item) }); return Array.from(grouped.values()) }
function exportReport() { const header = '客户名称,应收总额,已收金额,未收金额,未到期,1-30天,31-60天,61-90天,90天以上,最大逾期天数'; const lines = customerAging.value.map(r => [r.customerName, r.totalAmount, r.totalPaid, r.totalRemaining, r.notDue, r.days1to30, r.days31to60, r.days61to90, r.daysOver90, r.maxOverdueDays].join(',')); const blob = new Blob([`﻿${header}\n${lines.join('\n')}`], { type: 'text/csv;charset=utf-8' }); const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = `应收账龄_${new Date().toISOString().slice(0, 10)}.csv`; link.click(); link.remove(); URL.revokeObjectURL(url); ElMessage.success('明细导出成功') }
onMounted(loadReport)
</script>
<style scoped>.toolbar { display:flex; gap:8px; margin-bottom:20px; }.stats-row { margin-bottom:20px; }</style>
