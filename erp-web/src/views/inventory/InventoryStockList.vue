<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.warehouseId" clearable placeholder="仓库" style="width: 180px" @change="load">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id!" />
        </el-select>
        <el-select v-model="query.productId" clearable filterable placeholder="商品" style="width: 220px" @change="load">
          <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id!" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column prop="productCode" label="商品编码" width="140" />
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column prop="productSpec" label="规格" width="140" />
        <el-table-column prop="quantity" label="库存数量" width="120" align="right" />
        <el-table-column prop="unitCost" label="单位成本" width="120" align="right"><template #default="{ row }">¥{{ row.unitCost?.toFixed(4) }}</template></el-table-column>
        <el-table-column prop="totalValue" label="库存金额" width="130" align="right"><template #default="{ row }">¥{{ row.totalValue?.toFixed(2) }}</template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[20, 50, 100]" layout="total, sizes, prev, pager, next" class="pagination" @change="load" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { inventoryQueryApi, type StockRow } from '@/api/inventory-query'
import { warehouseApi } from '@/api/warehouse'
import { productApi } from '@/api/product'
import type { Warehouse } from '@/api/warehouse'
import type { Product } from '@/api/product'
const loading = ref(false), rows = ref<StockRow[]>([]), total = ref(0)
const warehouses = ref<Warehouse[]>([]), products = ref<Product[]>([])
const query = reactive({ warehouseId: undefined as number | undefined, productId: undefined as number | undefined, page: 1, size: 20 })
async function load() { loading.value = true; try { const data = await inventoryQueryApi.stocks(query); rows.value = data.records; total.value = data.total } finally { loading.value = false } }
onMounted(async () => { const [w, p] = await Promise.all([warehouseApi.listAll(), productApi.page({ page: 1, size: 500 })]); warehouses.value = w; products.value = p.records; await load() })
</script>
<style scoped>.toolbar{display:flex;gap:8px;margin-bottom:16px}.pagination{margin-top:16px;justify-content:flex-end}</style>
