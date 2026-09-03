<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="调拨单号" clearable style="width: 180px" @keyup.enter="search" />
        <el-select v-model="query.warehouseId" clearable placeholder="仓库" style="width: 180px" @change="search">
          <el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.name" :value="warehouse.id!" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增调拨</el-button>
      </div>
      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="docNo" label="调拨单号" width="170" />
        <el-table-column prop="fromWarehouseName" label="调出仓" width="140" />
        <el-table-column prop="toWarehouseName" label="调入仓" width="140" />
        <el-table-column prop="bizDate" label="业务日期" width="120" />
        <el-table-column label="金额" width="120" align="right"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column>
        <el-table-column label="状态" width="100" align="center"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="175" />
        <el-table-column label="操作" width="260" fixed="right"><template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="audit(row)">审核</el-button>
          <el-button v-if="row.status === 'AUDITED'" link type="success" @click="complete(row)">完成</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="cancel(row)">取消</el-button>
        </template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" class="pagination" @change="load" />
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增库存调拨" width="820px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="调出仓" prop="fromWarehouseId"><el-select v-model="form.fromWarehouseId" style="width:100%"><el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.name" :value="warehouse.id!" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="调入仓" prop="toWarehouseId"><el-select v-model="form.toWarehouseId" style="width:100%"><el-option v-for="warehouse in warehouses" :key="warehouse.id" :label="warehouse.name" :value="warehouse.id!" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="业务日期"><el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-table :data="form.items" border size="small">
          <el-table-column label="商品" min-width="220"><template #default="{ $index }"><el-select v-model="form.items[$index].productId" filterable placeholder="选择商品" style="width:100%"><el-option v-for="product in products" :key="product.id" :label="product.name + (product.spec ? ` / ${product.spec}` : '')" :value="product.id!" /></el-select></template></el-table-column>
          <el-table-column label="数量" width="130"><template #default="{ $index }"><el-input-number v-model="form.items[$index].qty" :min="0.0001" :precision="4" :controls="false" style="width:100%" /></template></el-table-column>
          <el-table-column label="单价" width="130"><template #default="{ $index }"><el-input-number v-model="form.items[$index].price" :min="0" :precision="2" :controls="false" style="width:100%" /></template></el-table-column>
          <el-table-column label="金额" width="110" align="right"><template #default="{ $index }">{{ money((form.items[$index].qty || 0) * (form.items[$index].price || 0)) }}</template></el-table-column>
          <el-table-column label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="form.items.splice($index, 1)">删除</el-button></template></el-table-column>
        </el-table>
        <el-button plain size="small" :icon="Plus" class="add-line" @click="addItem">添加一行</el-button>
        <el-form-item label="备注" class="remark"><el-input v-model="form.remark" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="库存调拨详情" width="820px">
      <template v-if="detail">
        <el-descriptions :column="3" border><el-descriptions-item label="调拨单号">{{ detail.docNo }}</el-descriptions-item><el-descriptions-item label="调出仓">{{ detail.fromWarehouseName || warehouseName(detail.fromWarehouseId) }}</el-descriptions-item><el-descriptions-item label="调入仓">{{ detail.toWarehouseName || warehouseName(detail.toWarehouseId) }}</el-descriptions-item><el-descriptions-item label="业务日期">{{ detail.bizDate }}</el-descriptions-item><el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item><el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item></el-descriptions>
        <el-table :data="detail.items" border size="small" class="detail-table"><el-table-column prop="productName" label="商品" min-width="180" /><el-table-column prop="qty" label="数量" width="110" align="right" /><el-table-column prop="price" label="单价" width="110" align="right" /><el-table-column prop="amount" label="金额" width="110" align="right" /><el-table-column prop="note" label="备注" min-width="140" /></el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { inventoryTransferApi } from '@/api/inventory'
import { warehouseApi, type Warehouse } from '@/api/warehouse'
import { productApi, type Product } from '@/api/product'

type Transfer = { id: number; docNo: string; fromWarehouseId: number; toWarehouseId: number; bizDate: string; status: string; totalAmount: number; fromWarehouseName?: string; toWarehouseName?: string; createdAt: string }
type TransferDetail = Transfer & { items: Array<{ productName?: string; qty: number; price: number; amount: number; note?: string }>; remark?: string }
const statusOptions = [{ value: 'DRAFT', label: '草稿' }, { value: 'AUDITED', label: '已审核' }, { value: 'COMPLETED', label: '已完成' }, { value: 'CANCELLED', label: '已取消' }]
const loading = ref(false), saving = ref(false), records = ref<Transfer[]>([]), total = ref(0), warehouses = ref<Warehouse[]>([]), products = ref<Product[]>([])
const query = reactive({ page: 1, size: 10, keyword: '', status: '', warehouseId: undefined as number | undefined })
const dialogVisible = ref(false), detailVisible = ref(false), formRef = ref<FormInstance>(), detail = ref<TransferDetail>()
const form = reactive({ fromWarehouseId: undefined as number | undefined, toWarehouseId: undefined as number | undefined, bizDate: new Date().toISOString().slice(0, 10), remark: '', items: [] as Array<{ productId?: number; qty: number; price: number }> })
const rules: FormRules = { fromWarehouseId: [{ required: true, message: '请选择调出仓', trigger: 'change' }], toWarehouseId: [{ required: true, message: '请选择调入仓', trigger: 'change' }] }
const warehouseMap = computed(() => new Map(warehouses.value.map((item) => [item.id!, item.name])))
const money = (value?: number) => (value ?? 0).toFixed(2)
const warehouseName = (id?: number) => id ? warehouseMap.value.get(id) || '—' : '—'
const statusLabel = (status: string) => statusOptions.find((item) => item.value === status)?.label || status
const statusType = (status: string) => status === 'COMPLETED' ? 'success' : status === 'CANCELLED' ? 'info' : status === 'AUDITED' ? 'primary' : 'warning'
async function load() { loading.value = true; try { const data = await inventoryTransferApi.getTransferList({ ...query, keyword: query.keyword || undefined, status: query.status || undefined }); records.value = data.records; total.value = data.total } finally { loading.value = false } }
function search() { query.page = 1; load() }
function addItem() { form.items.push({ productId: undefined, qty: 1, price: 0 }) }
function openCreate() { Object.assign(form, { fromWarehouseId: undefined, toWarehouseId: undefined, bizDate: new Date().toISOString().slice(0, 10), remark: '', items: [] }); addItem(); dialogVisible.value = true }
async function save() { const valid = await formRef.value?.validate().catch(() => false); if (!valid || form.fromWarehouseId === form.toWarehouseId || !form.items.length || form.items.some((item) => !item.productId)) { if (form.fromWarehouseId === form.toWarehouseId) ElMessage.warning('调出仓和调入仓不能相同'); else if (!form.items.length || form.items.some((item) => !item.productId)) ElMessage.warning('请为每一行选择商品'); return } saving.value = true; try { await inventoryTransferApi.createTransfer({ fromWarehouseId: form.fromWarehouseId!, toWarehouseId: form.toWarehouseId!, bizDate: form.bizDate, remark: form.remark, items: form.items.map((item) => ({ productId: item.productId!, qty: item.qty, price: item.price })) }); ElMessage.success('调拨草稿已保存'); dialogVisible.value = false; await load() } finally { saving.value = false } }
async function showDetail(id: number) { detail.value = await inventoryTransferApi.getTransferDetail(id); detailVisible.value = true }
async function action(row: Transfer, name: string, callback: () => Promise<unknown>) { try { await ElMessageBox.confirm(`${name}后单据状态将更新，确认${name} ${row.docNo}?`, `${name}确认`, { type: 'warning' }); await callback(); ElMessage.success(`${name}成功`); await load() } catch (error) { if (error !== 'cancel') throw error } }
const audit = (row: Transfer) => action(row, '审核', () => inventoryTransferApi.auditTransfer(row.id))
const complete = (row: Transfer) => action(row, '完成', () => inventoryTransferApi.completeTransfer(row.id))
const cancel = (row: Transfer) => action(row, '取消', () => inventoryTransferApi.cancelTransfer(row.id))
onMounted(async () => { const [warehouseResult, productResult] = await Promise.all([warehouseApi.listAll(), productApi.page({ page: 1, size: 500 })]); warehouses.value = warehouseResult; products.value = productResult.records; await load() })
</script>
<style scoped>.toolbar{display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap}.pagination{margin-top:16px;justify-content:flex-end}.add-line{margin-top:8px}.remark{margin-top:16px}.detail-table{margin-top:16px}</style>
