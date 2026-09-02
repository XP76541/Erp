<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.supplierId" clearable filterable placeholder="供应商" style="width: 180px">
          <el-option v-for="supplier in suppliers" :key="supplier.id" :label="supplier.name" :value="supplier.id!" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px">
          <el-option label="未结算" value="UNSETTLED" />
          <el-option label="部分结算" value="PARTIAL" />
          <el-option label="已结算" value="SETTLED" />
        </el-select>
        <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="业务开始" end-placeholder="业务结束" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="docNo" label="来源单号" width="170" />
        <el-table-column prop="supplierName" label="供应商" min-width="150" />
        <el-table-column prop="bizDate" label="业务日期" width="115" align="center" />
        <el-table-column prop="dueDate" label="到期日" width="115" align="center" />
        <el-table-column label="应付金额" width="120" align="right"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="已付金额" width="120" align="right"><template #default="{ row }">¥{{ money(row.paidAmount) }}</template></el-table-column>
        <el-table-column label="余额" width="120" align="right"><template #default="{ row }">¥{{ money(row.remainingAmount) }}</template></el-table-column>
        <el-table-column prop="agingBucket" label="账龄" width="100" align="center" />
        <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" class="pagination" @change="load" />
    </el-card>

    <el-card shadow="never" class="aging-card">
      <template #header>应付账龄汇总</template>
      <el-row :gutter="12">
        <el-col v-for="item in aging" :key="item.bucket" :span="24 / aging.length">
          <div class="stat"><div>{{ item.bucket }} ({{ item.count }})</div><strong>¥{{ money(item.remainingAmount) }}</strong></div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { payableApi, type PayableAgingResponse, type PayableListResponse } from '@/api/payable'
import { supplierApi, type Supplier } from '@/api/supplier'

const loading = ref(false)
const records = ref<PayableListResponse[]>([])
const aging = ref<PayableAgingResponse[]>([])
const suppliers = ref<Supplier[]>([])
const total = ref(0)
const query = reactive({ supplierId: undefined as number | undefined, status: '', dateRange: [] as string[], page: 1, size: 10 })

const money = (value: number) => (value ?? 0).toFixed(2)
const statusText = (status: string) => ({ UNSETTLED: '未结算', PARTIAL: '部分结算', SETTLED: '已结算' }[status] || status)
const statusType = (status: string) => status === 'SETTLED' ? 'success' : status === 'PARTIAL' ? 'warning' : 'danger'

async function load() {
  loading.value = true
  try {
    const data = await payableApi.list({ supplierId: query.supplierId, status: query.status || undefined, startDate: query.dateRange[0], endDate: query.dateRange[1], page: query.page, size: query.size })
    records.value = data.records
    total.value = data.total
    aging.value = await payableApi.aging(query.supplierId)
  } finally {
    loading.value = false
  }
}
function search() { query.page = 1; load() }
onMounted(async () => {
  suppliers.value = (await supplierApi.page({ page: 1, size: 500 })).records
  await load()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.aging-card { margin-top: 20px; }
.stat { padding: 16px; text-align: center; background: #f8f9fa; border-radius: 6px; color: #606266; }
.stat strong { display: block; margin-top: 8px; font-size: 20px; color: #303133; }
</style>
