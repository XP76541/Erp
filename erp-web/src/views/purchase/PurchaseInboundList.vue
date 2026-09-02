<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="单号"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
          @clear="load"
        />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px" @change="handleSearch">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已审核" value="AUDITED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增采购入库</el-button>
      </div>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="docNo" label="单号" width="160" />
        <el-table-column label="供应商" min-width="160">
          <template #default="{ row }">{{ supplierName(row.supplierId) }}</template>
        </el-table-column>
        <el-table-column label="入库仓" width="110">
          <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
        </el-table-column>
        <el-table-column prop="bizDate" label="业务日期" width="110" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'AUDITED' ? 'success' : 'warning'" size="small">
              {{ row.status === 'AUDITED' ? '已审核' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditAt" label="审核时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="handleAudit(row)">审核</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @change="load"
      />
    </el-card>

    <!-- 新建采购入库单 -->
    <el-dialog v-model="dialogVisible" title="新增采购入库单" width="820px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" filterable style="width: 100%">
                <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库仓" prop="warehouseId">
              <el-select v-model="form.warehouseId" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务日期" prop="bizDate">
              <el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 明细行 -->
        <el-table :data="form.items" border size="small">
          <el-table-column label="商品" min-width="200">
            <template #default="{ $index }">
              <el-select v-model="form.items[$index].productId" filterable placeholder="选择商品" size="small">
                <el-option v-for="p in products" :key="p.id" :label="p.name + (p.spec ? ' / ' + p.spec : '')" :value="p.id!" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{ $index }">
              <el-input-number v-model="form.items[$index].qty" :min="0.0001" :precision="4" :controls="false" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="进价" width="120">
            <template #default="{ $index }">
              <el-input-number v-model="form.items[$index].price" :min="0" :precision="2" :controls="false" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100" align="right">
            <template #default="{ $index }">
              {{ ((form.items[$index].qty || 0) * (form.items[$index].price || 0)).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="form.items.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button plain size="small" :icon="Plus" class="add-line" @click="addItem">添加一行</el-button>

        <el-form-item label="备注" prop="remark" class="remark">
          <el-input v-model="form.remark" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { purchaseInboundApi } from '@/api/purchase'
import type { PurchaseInboundCreateRequest, PurchaseInboundListResponse } from '@/api/purchase'
import { supplierApi } from '@/api/supplier'
import type { Supplier } from '@/api/supplier'
import { warehouseApi } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { productApi } from '@/api/product'
import type { Product } from '@/api/product'

const loading = ref(false)
const saving = ref(false)
const list = ref<PurchaseInboundListResponse[]>([])
const total = ref(0)
const suppliers = ref<Supplier[]>([])
const warehouses = ref<Warehouse[]>([])
const products = ref<Product[]>([])
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive(defaultForm())

function defaultForm() {
  return {
    supplierId: undefined as number | undefined,
    warehouseId: undefined as number | undefined,
    bizDate: new Date().toISOString().slice(0, 10),
    remark: '',
    items: [] as { productId?: number; qty: number; price: number }[],
  }
}

const rules: FormRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择入库仓', trigger: 'change' }],
}

const supplierMap = computed(() => new Map(suppliers.value.map((s) => [s.id!, s.name])))
const warehouseMap = computed(() => new Map(warehouses.value.map((w) => [w.id!, w.name])))

function supplierName(id?: number) {
  return id ? (supplierMap.value.get(id) ?? '—') : '—'
}

function warehouseName(id?: number) {
  return id ? (warehouseMap.value.get(id) ?? '—') : '—'
}

async function load() {
  loading.value = true
  try {
    const data = await purchaseInboundApi.list({ ...query, status: query.status || undefined })
    list.value = data.records
    total.value = data.total
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  load()
}

function addItem() {
  form.items.push({ productId: undefined, qty: 1, price: 0 })
}

function openCreate() {
  Object.assign(form, defaultForm())
  addItem()
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!form.items.length || form.items.some((i) => !i.productId)) {
    ElMessage.warning('请为每一行选择商品')
    return
  }

  saving.value = true
  try {
    await purchaseInboundApi.create({
      supplierId: form.supplierId!,
      warehouseId: form.warehouseId!,
      bizDate: form.bizDate,
      remark: form.remark,
      items: form.items.map((i) => ({ productId: i.productId!, qty: i.qty, price: i.price })),
    })
    ElMessage.success('草稿已保存,可在列表中审核')
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleAudit(row: PurchaseInbound) {
  try {
    await ElMessageBox.confirm(
      `审核后库存、台账、应付将立即生效且单据不可修改,确认审核 ${row.docNo}?`,
      '审核确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await purchaseInboundApi.audit(row.id!)
    ElMessage.success('审核成功:库存与应付已更新')
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  }
}

async function loadOptions() {
  try {
    const [s, w, p] = await Promise.all([
      supplierApi.page({ page: 1, size: 200 }),
      warehouseApi.listAll(),
      productApi.page({ page: 1, size: 500 }),
    ])
    suppliers.value = s.records
    warehouses.value = w
    products.value = p.records
  } catch {
    // 错误提示由 http 拦截器统一处理
  }
}

onMounted(() => {
  load()
  loadOptions()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.add-line {
  margin-top: 8px;
}

.remark {
  margin-top: 16px;
}
</style>
