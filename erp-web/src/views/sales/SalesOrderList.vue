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
          <el-option label="作废" value="VOID" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增销售订单</el-button>
      </div>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="docNo" label="单号" width="160" />
        <el-table-column label="客户" min-width="160">
          <template #default="{ row }">{{ customerName(row.customerId) }}</template>
        </el-table-column>
        <el-table-column label="销售人员" width="110">
          <template #default="{ row }">{{ salespersonName(row.salespersonId) }}</template>
        </el-table-column>
        <el-table-column prop="bizDate" label="业务日期" width="110" align="center" />
        <el-table-column label="订单状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发货状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">
              {{ getShipStatusText(row.shipStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="110" align="right">
          <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="handleAudit(row)">审核</el-button>
            <el-button v-if="row.status === 'AUDITED' && row.shipStatus === 'UN_SHIPPED'" link type="success" @click="createOutbound(row)">出库</el-button>
            <el-button link type="info" @click="viewDetail(row)">详情</el-button>
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

    <!-- 新建销售订单 -->
    <el-dialog v-model="dialogVisible" title="新增销售订单" width="820px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="客户" prop="customerId">
              <el-select v-model="form.customerId" filterable style="width: 100%">
                <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务日期" prop="bizDate">
              <el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售人员" prop="salespersonId">
              <el-select v-model="form.salespersonId" style="width: 100%">
                <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id!" />
              </el-select>
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
          <el-table-column label="售价" width="120">
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

    <!-- 审核对话框 -->
    <el-dialog v-model="auditDialogVisible" title="审核销售订单" width="500px">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="80px">
        <el-form-item label="操作" prop="action">
          <el-radio-group v-model="auditForm.action">
            <el-radio label="audit">通过</el-radio>
            <el-radio label="reject">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="auditForm.action === 'audit'" label="低价确认">
          <el-checkbox v-model="auditForm.forceConfirm">确认低于最低限价仍审核</el-checkbox>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="auditForm.remark" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditing" @click="handleAuditSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 出库对话框 -->
    <el-dialog v-model="outboundDialogVisible" title="创建销售出库单" width="820px">
      <el-form ref="outboundFormRef" :model="outboundForm" :rules="outboundRules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="出库仓库" prop="warehouseId">
              <el-select v-model="outboundForm.warehouseId" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务日期" prop="bizDate">
              <el-date-picker v-model="outboundForm.bizDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 发货明细 -->
        <el-alert title="发货明细" type="info" :closable="false" class="outbound-alert" />
        <el-table :data="outboundForm.items" border size="small">
          <el-table-column label="商品" width="200">
            <template #default="{ row }">
              {{ productName(row.productId) }}
            </template>
          </el-table-column>
          <el-table-column label="订单数量" width="100" align="right">
            <template #default="{ row }">{{ row.orderedQty?.toFixed(4) }}</template>
          </el-table-column>
          <el-table-column label="已发货" width="100" align="right">
            <template #default="{ row }">{{ row.shippedQty?.toFixed(4) }}</template>
          </el-table-column>
          <el-table-column label="本次发货" width="120">
            <template #default="{ $index }">
              <el-input-number
                v-model="outboundForm.items[$index].qty"
                :min="0.0001"
                :max="outboundForm.items[$index].remainingQty"
                :precision="4"
                :controls="false"
                size="small"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="outboundForm.items.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button plain size="small" :icon="Plus" class="add-line" @click="addOutboundItem">添加发货行</el-button>

        <el-form-item label="备注" prop="remark" class="remark">
          <el-input v-model="outboundForm.remark" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outboundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingOutbound" @click="handleCreateOutbound">创建出库单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { salesApi } from '@/api/sales'
import type { SalesOrder, SalesOrderListResponse, SalesOutboundCreateRequest } from '@/api/sales'
import { customerApi } from '@/api/customer'
import type { Customer } from '@/api/customer'
import { warehouseApi } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { productApi } from '@/api/product'
import type { Product } from '@/api/product'
import { userApi } from '@/api/user'
import type { User } from '@/api/user'

const loading = ref(false)
const saving = ref(false)
const auditing = ref(false)
const creatingOutbound = ref(false)
const list = ref<SalesOrderListResponse[]>([])
const total = ref(0)
const customers = ref<Customer[]>([])
const warehouses = ref<Warehouse[]>([])
const products = ref<Product[]>([])
const users = ref<User[]>([])
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive(defaultForm())

function defaultForm() {
  return {
    customerId: undefined as number | undefined,
    bizDate: new Date().toISOString().slice(0, 10),
    salespersonId: undefined as number | undefined,
    remark: '',
    items: [] as { productId?: number; qty: number; price: number }[],
  }
}

const rules: FormRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  bizDate: [{ required: true, message: '请选择业务日期', trigger: 'change' }],
  salespersonId: [{ required: true, message: '请选择销售人员', trigger: 'change' }],
}

const auditDialogVisible = ref(false)
const auditFormRef = ref<FormInstance>()
const auditForm = reactive({
  action: 'audit' as 'audit' | 'reject',
  remark: '',
  forceConfirm: false,
})

const auditRules: FormRules = {
  action: [{ required: true, message: '请选择操作', trigger: 'change' }],
}

const currentOrder = ref<SalesOrder | null>(null)
const outboundDialogVisible = ref(false)
const outboundFormRef = ref<FormInstance>()
const outboundForm = reactive({
  orderId: 0,
  warehouseId: undefined as number | undefined,
  bizDate: new Date().toISOString().slice(0, 10),
  remark: '',
  items: [] as { orderItemId: number; productId: number; qty: number; remainingQty: number }[],
})

const outboundRules: FormRules = {
  warehouseId: [{ required: true, message: '请选择出库仓库', trigger: 'change' }],
}

const customerMap = computed(() => new Map(customers.value.map((c) => [c.id!, c.name])))
const productMap = computed(() => new Map(products.value.map((p) => [p.id!, p.name])))

function customerName(id?: number) {
  return id ? (customerMap.value.get(id) ?? '—') : '—'
}

function productName(id?: number) {
  return id ? (productMap.value.get(id) ?? '—') : '—'
}

function salespersonName(id?: number) {
  const user = users.value.find(u => u.id === id)
  return user?.realName || '—'
}

function getStatusType(status: string) {
  switch (status) {
    case 'DRAFT': return 'warning'
    case 'AUDITED': return 'success'
    case 'VOID': return 'info'
    default: return ''
  }
}

function getStatusText(status: string) {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'AUDITED': return '已审核'
    case 'VOID': return '作废'
    default: return status
  }
}

function getShipStatusText(status: string) {
  switch (status) {
    case 'UN_SHIPPED': return '未发货'
    case 'PART_SHIPPED': return '部分发货'
    case 'SHIPPED': return '已发货'
    default: return status
  }
}

async function load() {
  loading.value = true
  try {
    const data = await salesApi.getOrders({ ...query, status: query.status || undefined })
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
    await salesApi.createOrder({
      customerId: form.customerId!,
      bizDate: form.bizDate,
      salespersonId: form.salespersonId!,
      remark: form.remark,
      items: form.items.map((i) => ({ productId: i.productId!, qty: i.qty, price: i.price })),
    })
    ElMessage.success('销售订单已保存')
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleAudit(row: SalesOrder) {
  currentOrder.value = row
  auditForm.action = 'audit'
  auditForm.remark = ''
  auditForm.forceConfirm = false
  auditDialogVisible.value = true
}

async function handleAuditSubmit() {
  const valid = await auditFormRef.value?.validate().catch(() => false)
  if (!valid) return

  auditing.value = true
  try {
    await salesApi.auditOrder(currentOrder.value.id!, auditForm)
    ElMessage.success(auditForm.action === 'audit' ? '审核成功' : '驳回成功')
    auditDialogVisible.value = false
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    auditing.value = false
  }
}

async function createOutbound(row: SalesOrder) {
  currentOrder.value = row
  // 加载仓库列表
  if (!warehouses.value.length) {
    const data = await warehouseApi.listAll()
    warehouses.value = data
  }

  // 加载未发货明细
  const detail = await salesApi.getOrderDetail(row.id!)
  outboundForm.orderId = row.id!
  outboundForm.warehouseId = undefined
  outboundForm.bizDate = new Date().toISOString().slice(0, 10)
  outboundForm.remark = ''
  outboundForm.items = detail.items.map((item, index) => ({
    orderItemId: item.id!,
    productId: item.productId,
    qty: 0,
    remainingQty: (item.qty - (item.shippedQty || 0))
  }))

  outboundDialogVisible.value = true
}

function addOutboundItem() {
  outboundForm.items.push({
    orderItemId: 0,
    productId: undefined,
    qty: 1,
    remainingQty: 0
  })
}

async function handleCreateOutbound() {
  const valid = await outboundFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!outboundForm.items.length || outboundForm.items.some((i) => !i.productId)) {
    ElMessage.warning('请为每一行选择商品')
    return
  }

  creatingOutbound.value = true
  try {
    const result = await salesApi.createOutboundFromOrder(currentOrder.value.id!, {
      orderId: currentOrder.value.id!,
      warehouseId: outboundForm.warehouseId!,
      bizDate: outboundForm.bizDate,
      remark: outboundForm.remark,
      items: outboundForm.items.map((i) => ({
        orderItemId: i.orderItemId,
        qty: i.qty
      }))
    })
    ElMessage.success('出库单创建成功: ' + result.outboundDocNo)
    outboundDialogVisible.value = false
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    creatingOutbound.value = false
  }
}

function viewDetail(row: SalesOrder) {
  ElMessageBox.alert(`订单号: ${row.docNo}\n客户: ${customerName(row.customerId)}\n业务日期: ${row.bizDate}\n状态: ${getStatusText(row.status)}`, '订单详情', {
    confirmButtonText: '确定'
  })
}

async function loadOptions() {
  try {
    const [c, w, p, u] = await Promise.all([
      customerApi.page({ page: 1, size: 200 }),
      warehouseApi.listAll(),
      productApi.page({ page: 1, size: 500 }),
      userApi.page({ page: 1, size: 200 })
    ])
    customers.value = c.records
    warehouses.value = w
    products.value = p.records
    users.value = u.records.filter(user => user.isActive === 1)
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

.outbound-alert {
  margin-bottom: 12px;
}
</style>