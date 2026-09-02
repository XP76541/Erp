<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="退货单号" clearable style="width: 180px" @keyup.enter="search" />
        <el-select v-model="query.supplierId" clearable filterable placeholder="供应商" style="width: 180px">
          <el-option v-for="supplier in suppliers" :key="supplier.id" :label="supplier.name" :value="supplier.id!" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px">
          <el-option label="草稿" value="DRAFT" /><el-option label="已审核" value="AUDITED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="docNo" label="退货单号" width="170" />
        <el-table-column label="供应商" min-width="150"><template #default="{ row }">{{ supplierName(row.supplierId) }}</template></el-table-column>
        <el-table-column prop="bizDate" label="业务日期" width="115" />
        <el-table-column label="退货金额" width="120" align="right"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column>
        <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="row.status === 'AUDITED' ? 'success' : 'warning'">{{ row.status === 'AUDITED' ? '已审核' : '草稿' }}</el-tag></template></el-table-column>
        <el-table-column prop="auditAt" label="审核时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right"><template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="audit(row)">审核</el-button>
        </template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" class="pagination" @change="load" />
    </el-card>

    <el-dialog v-model="detailVisible" title="采购退货详情" width="760px">
      <template v-if="detail">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="退货单号">{{ detail.doc.docNo }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ supplierName(detail.doc.supplierId) }}</el-descriptions-item>
          <el-descriptions-item label="业务日期">{{ detail.doc.bizDate }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.doc.status === 'AUDITED' ? '已审核' : '草稿' }}</el-descriptions-item>
          <el-descriptions-item label="原因" :span="2">{{ detail.doc.reason || '—' }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" border size="small" class="detail-table">
          <el-table-column prop="lineNo" label="行号" width="70" />
          <el-table-column prop="inboundItemId" label="原入库明细" width="120" />
          <el-table-column prop="productId" label="商品ID" width="100" />
          <el-table-column prop="qty" label="数量" width="100" align="right" />
          <el-table-column prop="unitCost" label="原入库成本" width="120" align="right" />
          <el-table-column prop="amount" label="金额" width="110" align="right" />
          <el-table-column prop="note" label="备注" min-width="140" />
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { purchaseReturnApi, type PurchaseReturnDetailResponse, type PurchaseReturnListResponse } from '@/api/purchaseReturn'
import { supplierApi, type Supplier } from '@/api/supplier'

const loading = ref(false)
const records = ref<PurchaseReturnListResponse[]>([])
const total = ref(0)
const suppliers = ref<Supplier[]>([])
const detailVisible = ref(false)
const detail = ref<PurchaseReturnDetailResponse>()
const query = reactive({ page: 1, size: 10, keyword: '', status: '', supplierId: undefined as number | undefined })
const supplierMap = computed(() => new Map(suppliers.value.map((item) => [item.id!, item.name])))
const supplierName = (id?: number) => id ? supplierMap.value.get(id) || '—' : '—'
const money = (value?: number) => (value ?? 0).toFixed(2)

async function load() {
  loading.value = true
  try {
    const data = await purchaseReturnApi.list({ ...query, status: query.status || undefined })
    records.value = data.records; total.value = data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; load() }
async function showDetail(id: number) { detail.value = await purchaseReturnApi.detail(id); detailVisible.value = true }
async function audit(row: PurchaseReturnListResponse) {
  try { await ElMessageBox.confirm(`审核后将扣减库存并生成红字应付,确认审核 ${row.docNo}?`, '审核确认', { type: 'warning' }) } catch { return }
  await purchaseReturnApi.audit(row.id); ElMessage.success('审核成功'); await load()
}
onMounted(async () => { suppliers.value = (await supplierApi.page({ page: 1, size: 500 })).records; await load() })
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.detail-table { margin-top: 16px; }
</style>
