<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 320px"
          @change="loadReport"
        />
        <el-button type="primary" :icon="Search" @click="loadReport">查询</el-button>
        <el-button type="primary" plain :icon="Download" @click="exportReport">导出报表</el-button>
      </div>

      <!-- 统计概览 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">应收总额</div>
            <div class="stat-value">¥{{ stats.totalAmount?.toFixed(2) || '0.00' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">已收总额</div>
            <div class="stat-value">¥{{ stats.totalPaid?.toFixed(2) || '0.00' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">未收总额</div>
            <div class="stat-value">¥{{ stats.totalRemaining?.toFixed(2) || '0.00' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">超期总额</div>
            <div class="stat-value">¥{{ stats.overdueAmount?.toFixed(2) || '0.00' }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 账龄分析图表 -->
      <div class="chart-container">
        <div ref="agingChartRef" class="aging-chart"></div>
      </div>

      <!-- 客户账龄明细表格 -->
      <div class="table-container">
        <h3>客户账龄明细</h3>
        <el-table v-loading="tableLoading" :data="customerAging" border stripe>
          <el-table-column prop="customerName" label="客户名称" min-width="150" />
          <el-table-column prop="totalAmount" label="应收总额" width="120" align="right">
            <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="totalPaid" label="已收金额" width="120" align="right">
            <template #default="{ row }">¥{{ row.totalPaid?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="totalRemaining" label="未收金额" width="120" align="right">
            <template #default="{ row }">¥{{ row.totalRemaining?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="未到期" width="120" align="right">
            <template #default="{ row }">¥{{ row.notDue?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="1-30天" width="100" align="right">
            <template #default="{ row }">¥{{ row.days1to30?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="31-60天" width="100" align="right">
            <template #default="{ row }">¥{{ row.days31to60?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="61-90天" width="100" align="right">
            <template #default="{ row }">¥{{ row.days61to90?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="90天以上" width="100" align="right">
            <template #default="{ row }">¥{{ row.daysOver90?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="超期天数" width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.maxOverdueDays > 0" class="overdue">
                {{ row.maxOverdueDays }}天
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="账龄分布" width="120" align="center">
            <template #default="{ row }">
              <el-progress
                :percentage="getAgingPercentage(row)"
                :color="getAgingColor(row)"
                :stroke-width="8"
                :show-text="false"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import { financeApi } from '@/api/finance'
import type { AgingAnalysisResponse } from '@/api/finance'

const dateRange = ref<[string, string]>([
  new Date().toISOString().slice(0, 10),
  new Date().toISOString().slice(0, 10)
])

const loading = ref(false)
const tableLoading = ref(false)
const stats = ref({
  totalAmount: 0,
  totalPaid: 0,
  totalRemaining: 0,
  overdueAmount: 0
})

const agingData = ref<AgingAnalysisResponse[]>([])
const customerAging = ref<any[]>([])

const agingChartRef = ref<HTMLDivElement>()

const chartInstance: any = ref(null)

async function loadReport() {
  loading.value = true
  try {
    // 加载账龄分析数据
    const agingDataResult = await financeApi.getAgingAnalysis()
    agingData.value = agingDataResult

    // 加载超期应收账款
    const overdueData = await financeApi.getOverdueReceivables()

    // 计算统计数据
    stats.value = {
      totalAmount: agingDataResult.reduce((sum, item) => sum + item.totalAmount, 0),
      totalPaid: agingDataResult.reduce((sum, item) => sum + item.totalPaid, 0),
      totalRemaining: agingDataResult.reduce((sum, item) => sum + item.totalRemaining, 0),
      overdueAmount: overdueData.reduce((sum, item) => sum + (item.remainingAmount || 0), 0)
    }

    // 生成客户账龄明细数据
    generateCustomerAgingData()

    // 渲染图表
    await nextTick()
    renderChart()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function generateCustomerAgingData() {
  // 这里应该从后端获取客户账龄明细数据
  // 现在使用模拟数据
  customerAging.value = [
    {
      customerName: '客户A',
      totalAmount: 100000,
      totalPaid: 60000,
      totalRemaining: 40000,
      notDue: 20000,
      days1to30: 10000,
      days31to60: 5000,
      days61to90: 3000,
      daysOver90: 2000,
      maxOverdueDays: 45
    },
    {
      customerName: '客户B',
      totalAmount: 80000,
      totalPaid: 40000,
      totalRemaining: 40000,
      notDue: 15000,
      days1to30: 8000,
      days31to60: 7000,
      days61to90: 5000,
      daysOver90: 5000,
      maxOverdueDays: 120
    }
  ]
}

function renderChart() {
  // 当前项目未引入图表组件，账龄数据通过下方明细表展示
}

function getAgingPercentage(row: any) {
  if (!row.totalRemaining) return 0
  return Math.round((row.daysOver90 / row.totalRemaining) * 100)
}

function getAgingColor(row: any) {
  const percentage = getAgingPercentage(row)
  if (percentage > 30) return '#f56c6c'
  if (percentage > 15) return '#e6a23c'
  return '#67c23a'
}

async function exportReport() {
  try {
    // 模拟导出功能
    ElMessage.success('报表导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  loadReport()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  color: white;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.stat-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.chart-container {
  margin-bottom: 30px;
  height: 400px;
}

.aging-chart {
  width: 100%;
  height: 100%;
}

.table-container {
  margin-top: 20px;
}

.table-container h3 {
  margin-bottom: 16px;
  color: #333;
}

.overdue {
  color: #f56c6c;
  font-weight: bold;
}
</style>