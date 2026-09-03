<template>
  <div class="page"><el-card shadow="never">
    <div class="toolbar">
      <el-select v-model="query.warehouseId" clearable placeholder="仓库" style="width:180px"><el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id!" /></el-select>
      <el-input v-model="query.docType" clearable placeholder="单据类型" style="width:160px" />
      <el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="bizDate" label="业务日期" width="120" />
      <el-table-column prop="docType" label="单据类型" width="140" />
      <el-table-column prop="docNo" label="单据号" width="170" />
      <el-table-column prop="warehouseName" label="仓库" width="140" />
      <el-table-column prop="productName" label="商品" min-width="180" />
      <el-table-column prop="direction" label="方向" width="80"><template #default="{row}">{{ row.direction === 1 ? '入库' : '出库' }}</template></el-table-column>
      <el-table-column prop="quantity" label="数量" width="100" align="right" />
      <el-table-column prop="unitCost" label="单位成本" width="110" align="right" />
      <el-table-column prop="balanceQuantity" label="结存数量" width="110" align="right" />
    </el-table>
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" class="pager" @change="load" />
  </el-card></div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { inventoryQueryApi, type LedgerRow } from '@/api/inventory-query'
import { warehouseApi } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
const loading = ref(false), rows = ref<LedgerRow[]>([]), total = ref(0), warehouses = ref<Warehouse[]>([]), dates = ref<string[]>([])
const query = reactive<{warehouseId?: number; docType?: string; startDate?: string; endDate?: string; page: number; size: number}>({ page: 1, size: 20 })
async function load() { loading.value = true; try { query.startDate = dates.value?.[0]; query.endDate = dates.value?.[1]; const data = await inventoryQueryApi.ledgers(query); rows.value = data.records; total.value = data.total } finally { loading.value = false } }
onMounted(async () => { warehouses.value = await warehouseApi.listAll(); await load() })
</script>
<style scoped>.toolbar{display:flex;gap:8px;margin-bottom:16px;align-items:center}.pager{margin-top:16px;justify-content:flex-end}</style>
