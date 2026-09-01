<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="单号/客户"
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
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增收款单</el-button>
      </div>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="docNo" label="单号" width="160" />
        <el-table-column label="客户" min-width="160">
          <template #default="{ row }">{{ customerName(row.customerId) }}</template>
        </el-table-column>
        <el-table-column prop="businessDate" label="业务日期" width="110" align="center" />
        <el-table-column label="收款金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="已核销金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.allocatedAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="剩余金额" width="120" align="right">
          <template #default="{ row }">¥{{ (row.amount - row.allocatedAmount)?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="收款方式" width="100" align="center">
          <template #default="{ row }">{{ getPaymentMethodText(row.paymentMethod) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="handleAudit(row)">审核</el-button>
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

    <!-- 新建收款单 -->
    <el-dialog v-model="dialogVisible" title="新增收款单" width="900px">
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
            <el-form-item label="收款日期" prop="businessDate">
              <el-date-picker v-model="form.businessDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收款金额" prop="amount">
              <el-input-number v-model="form.amount" :min="0.01" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="收款方式" prop="paymentMethod">
          <el-radio-group v-model="form.paymentMethod">
            <el-radio label="CASH">现金</el-radio>
            <el-radio label="BANK">银行转账</el-radio>
            <el-radio label="CHEQUE">支票</el-radio>
            <el-radio label="OTHER">其他</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 应收账款明细 -->
        <el-alert title="待核销应收账款" type="info" :closable="false" class="receivable-alert" />

        <el-table :data="form.allocations" border size="small">
          <el-table-column label="应收单号" width="160">
            <template #default="{ row }">{{ row.receivableDocNo }}</template>
          </el-table-column>
          <el-table-column label="应收金额" width="120" align="right">
            <template #default="{ row }">¥{{ row.receivableAmount?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="已收金额" width="120" align="right">
            <template #default="{ row }">¥{{ row.paidAmount?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="本次核销" width="120">
            <template #default="{ $index }">
              <el-input-number
                v-model="form.allocations[$index].allocatedAmount"
                :min="0"
                :max="form.allocations[$index].remainingAmount"
                :precision="2"
                :controls="false"
                size="small"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="余额" width="100" align="right">
            <template #default="{ row }">¥{{ row.remainingAmount?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="form.allocations.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存草稿</el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog v-model="auditDialogVisible" title="审核收款单" width="500px">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="80px">
        <el-form-item label="操作" prop="action">
          <el-radio-group v-model="auditForm.action">
            <el-radio label="audit">通过</el-radio>
            <el-radio label="reject">驳回</el-radio>
          </el-radio-group>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { financeApi } from '@/api/finance'
import type { Payment, PaymentListResponse } from '@/api/finance'
import { customerApi } from '@/api/customer'
import type { Customer } from '@/api/customer'

const loading = ref(false)
const saving = ref(false)
const auditing = ref(false)
const list = ref<PaymentListResponse[]>([])
const total = ref(0)
const customers = ref<Customer[]>([])
const receivables = ref<any[]>([])

const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  customerId: undefined as number | undefined,
  businessDate: new Date().toISOString().slice(0, 10),
  amount: 0,
  paymentMethod: 'CASH',
  remark: '',
  allocations: [] as any[]
})

const rules: FormRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  businessDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入收款金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择收款方式', trigger: 'change' }]
}

const auditDialogVisible = ref(false)
const auditFormRef = ref<FormInstance>()
const auditForm = reactive({
  action: 'audit' as 'audit' | 'reject',
  remark: ''
})

const auditRules: FormRules = {
  action: [{ required: true, message: '请选择操作', trigger: 'change' }]
}

const customerMap = computed(() => new Map(customers.value.map((c) => [c.id!, c.name])))

function customerName(id?: number) {
  return id ? (customerMap.value.get(id) ?? '—') : '—'
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

function getPaymentMethodText(method?: string) {
  switch (method) {
    case 'CASH': return '现金'
    case 'BANK': return '银行转账'
    case 'CHEQUE': return '支票'
    case 'OTHER': return '其他'
    default: return method || '—'
  }
}

async function load() {
  loading.value = true
  try {
    const data = await financeApi.getPayments({ ...query, status: query.status || undefined })
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

function openCreate() {
  Object.assign(form, {
    customerId: undefined,
    businessDate: new Date().toISOString().slice(0, 10),
    amount: 0,
    paymentMethod: 'CASH',
    remark: '',
    allocations: []
  })
  dialogVisible.value = true
}

async function loadCustomerReceivables(customerId: number) {
  try {
    const data = await financeApi.getReceivablesByCustomer(customerId)
    receivables.value = data
    return data
  } catch {
    return []
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!form.allocations.length || form.allocations.some((i) => !i.allocatedAmount || i.allocatedAmount <= 0)) {
    ElMessage.warning('请添加核销明细')
    return
  }

  saving.value = true
  try {
    await financeApi.createPayment({
      customerId: form.customerId!,
      businessDate: form.businessDate,
      amount: form.amount,
      paymentMethod: form.paymentMethod,
      remark: form.remark,
      allocations: form.allocations.map((i) => ({
        receivableId: i.receivableId,
        allocatedAmount: i.allocatedAmount
      }))
    })
    ElMessage.success('收款单已保存')
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleAudit(row: Payment) {
  auditForm.action = 'audit'
  auditForm.remark = ''
  auditDialogVisible.value = true
}

async function handleAuditSubmit() {
  const valid = await auditFormRef.value?.validate().catch(() => false)
  if (!valid) return

  auditing.value = true
  try {
    await financeApi.auditPayment(currentPayment.value.id!, auditForm)
    ElMessage.success(auditForm.action === 'audit' ? '审核成功' : '驳回成功')
    auditDialogVisible.value = false
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    auditing.value = false
  }
}

function viewDetail(row: Payment) {
  ElMessageBox.alert(
    `收款单号: ${row.docNo}\n` +
    `客户: ${customerName(row.customerId)}\n` +
    `业务日期: ${row.businessDate}\n` +
    `收款金额: ¥${row.amount?.toFixed(2)}\n` +
    `已核销: ¥${row.allocatedAmount?.toFixed(2)}\n` +
    `状态: ${getStatusText(row.status)}`,
    '收款单详情',
    { confirmButtonText: '确定' }
  )
}

const currentPayment = ref<any>(null)

async function loadOptions() {
  try {
    const data = await customerApi.page({ page: 1, size: 200 })
    customers.value = data.records
  } catch {
    // 错误提示由 http 拦截器统一处理
  }
}

// 监听客户选择，加载应收账款
const customerIdWatch = computed({
  get: () => form.customerId,
  set: async (val) => {
    form.customerId = val
    if (val) {
      const data = await loadCustomerReceivables(val)
      form.allocations = data.map(r => ({
        receivableId: r.id,
        receivableDocNo: r.docNo,
        receivableAmount: r.amount,
        paidAmount: r.paidAmount,
        remainingAmount: r.remainingAmount,
        allocatedAmount: 0
      }))
    }
  }
})

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

.receivable-alert {
  margin-bottom: 16px;
}
</style>