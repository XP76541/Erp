<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="单号/备注"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
          @clear="load"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增付款</el-button>
      </div>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="docNo" label="单号" width="150" />
        <el-table-column label="供应商" min-width="140">
          <template #default="{ row }">{{ supplierName(row.supplierId) }}</template>
        </el-table-column>
        <el-table-column prop="bizDate" label="业务日期" width="110" align="center" />
        <el-table-column prop="amount" label="金额" width="100" align="right">
          <template #default="{ row }">{{ row.amount.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="method" label="付款方式" width="90" />
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
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
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

    <!-- 新建付款单 -->
    <el-dialog v-model="dialogVisible" title="新增付款单" width="900px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" filterable @change="onSupplierChange" style="width: 100%">
                <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="业务日期" prop="bizDate">
              <el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="金额" prop="amount">
              <el-input-number v-model="form.amount" :min="0.01" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="付款方式" prop="method">
              <el-select v-model="form.method" style="width: 100%">
                <el-option label="转账" value="转账" />
                <el-option label="现金" value="现金" />
                <el-option label="支票" value="支票" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 应付核销 -->
        <div class="allocation-title">应付核销</div>
        <el-table :data="form.allocations" border size="small">
          <el-table-column label="应付单" min-width="160">
            <template #default="{ $index }">
              <el-select v-model="form.allocations[$index].payableId" filterable placeholder="选择应付单" size="small" @change="onPayableChange($index)">
                <el-option v-for="p in payables" :key="p.id" :label="p.docNo + ' (未核 ' + (p.amount - p.paidAmount).toFixed(2) + ')'" :value="p.id!" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="应付总额" width="100" align="right">
            <template #default="{ $index }">{{ payableTotal(form.allocations[$index].payableId).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="未核金额" width="100" align="right">
            <template #default="{ $index }">{{ outstanding(form.allocations[$index].payableId).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="核销金额" width="120">
            <template #default="{ $index }">
              <el-input-number v-model="form.allocations[$index].amount" :min="0.01" :precision="2" :controls="false" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="form.allocations.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="allocation-summary">
          <span>核销总额: {{ totalAllocation().toFixed(2) }}</span>
          <span :class="{ 'match': totalAllocation() === form.amount, 'mismatch': totalAllocation() !== form.amount }">
            {{ totalAllocation() === form.amount ? '✓ 匹配' : '不匹配(需=' + form.amount + ')' }}
          </span>
        </div>
        <el-button plain size="small" :icon="Plus" class="add-line" @click="addAllocation">添加核销行</el-button>

        <el-form-item label="银行账号" prop="bankAccount" style="margin-top: 16px">
          <el-input v-model="form.bankAccount" maxlength="50" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存草稿</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="付款单详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="单号">{{ detailDoc.docNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ supplierName(detailDoc.supplierId) }}</el-descriptions-item>
        <el-descriptions-item label="业务日期">{{ detailDoc.bizDate }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detailDoc.amount.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ detailDoc.method }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailDoc.status === 'AUDITED' ? 'success' : 'warning'">
            {{ detailDoc.status === 'AUDITED' ? '已审核' : '草稿' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 16px">
        <div class="allocation-title">核销明细</div>
        <el-table :data="detailDoc.allocations" border size="small">
          <el-table-column prop="payableDocNo" label="应付单" width="150" />
          <el-table-column label="核销金额" width="120" align="right">
            <template #default="{ row }">{{ row.amount.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="应付单未核余额(审核前)" width="140" align="right">
            <template #default="{ row }">{{ row.outstandingAmount?.toFixed(2) || '—' }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { paymentApi, type Payment, type PaymentAllocation, type PaymentDetail } from '@/api/payment'
import { payableApi, type Payable } from '@/api/payable'
import { supplierApi } from '@/api/supplier'
import type { Supplier } from '@/api/supplier'

const loading = ref(false)
const saving = ref(false)
const list = ref<Payment[]>([])
const total = ref(0)
const suppliers = ref<Supplier[]>([])
const payables = ref<Payable[]>([])
const query = reactive({ page: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive(defaultForm())

function defaultForm() {
  return {
    supplierId: undefined as number | undefined,
    bizDate: new Date().toISOString().slice(0, 10),
    amount: 0,
    method: '转账',
    bankAccount: '',
    remark: '',
    allocations: [] as { payableId: number | undefined; amount: number }[],
  }
}

const rules: FormRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  bizDate: [{ required: true, message: '请选择业务日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  method: [{ required: true, message: '请选择付款方式', trigger: 'change' }],
}

const supplierMap = computed(() => new Map(suppliers.value.map((s) => [s.id!, s.name])))
const payableMap = computed(() => new Map(payables.value.map((p) => [p.id, p])))

function supplierName(id?: number) {
  return id ? (supplierMap.value.get(id) ?? '—') : '—'
}

function payableTotal(payableId?: number) {
  if (!payableId) return 0
  const p = payableMap.value.get(payableId)
  return p ? p.amount : 0
}

function outstanding(payableId?: number) {
  if (!payableId) return 0
  const p = payableMap.value.get(payableId)
  return p ? p.amount - p.paidAmount : 0
}

function totalAllocation() {
  return form.allocations.reduce((sum, a) => sum + (a.amount || 0), 0)
}

async function load() {
  loading.value = true
  try {
    const data = await paymentApi.page({ ...query })
    list.value = data.records
    total.value = data.total
  } catch {
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  load()
}

async function onSupplierChange() {
  if (!form.supplierId) {
    payables.value = []
    return
  }
  const data = await payableApi.page({ page: 1, size: 200, supplierId: form.supplierId, status: 'UNSETTLED' })
  payables.value = data.records.filter((p) => p.paidAmount < p.amount)
}

function addAllocation() {
  form.allocations.push({ payableId: undefined, amount: 0 })
}

function onPayableChange(idx: number) {
  const row = form.allocations[idx]
  const p = payableMap.value.get(row.payableId!)
  if (p) {
    row.amount = p.amount - p.paidAmount
  }
}

function openCreate() {
  Object.assign(form, defaultForm())
  addAllocation()
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!form.allocations.length || form.allocations.some((a) => !a.payableId)) {
    ElMessage.warning('请为每一行选择应付单')
    return
  }
  const totalAlloc = totalAllocation()
  if (totalAlloc !== form.amount) {
    ElMessage.warning(`核销总额(${totalAlloc})与付款金额(${form.amount})不一致`)
    return
  }

  saving.value = true
  try {
    await paymentApi.create({
      supplierId: form.supplierId!,
      bizDate: form.bizDate!,
      amount: form.amount,
      method: form.method,
      bankAccount: form.bankAccount,
      remark: form.remark,
      allocations: form.allocations.map((a) => ({ payableId: a.payableId!, amount: a.amount })),
    })
    ElMessage.success('草稿已保存,可在列表中审核')
    dialogVisible.value = false
    await load()
  } catch {
  } finally {
    saving.value = false
  }
}

async function handleAudit(row: Payment) {
  try {
    await ElMessageBox.confirm(`审核后应付核销将立即生效且单据不可修改,确认审核 ${row.docNo}?`, '审核确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await paymentApi.audit(row.id!)
    ElMessage.success('审核成功')
    await load()
  } catch {
  }
}

const detailVisible = ref(false)
const detailDoc = ref<PaymentDetail>({ doc: {} as Payment, allocations: [] })

async function handleDetail(row: Payment) {
  const data = await paymentApi.detail(row.id!)
  detailDoc.value = data
  detailVisible.value = true
}

onMounted(() => {
  load()
  supplierApi.page({ page: 1, size: 200 }).then((d) => (suppliers.value = d.records))
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

.allocation-title {
  font-weight: 600;
  margin: 12px 0 8px;
  font-size: 14px;
}

.add-line {
  margin-top: 8px;
}

.allocation-summary {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 13px;
}

.match {
  color: #67c23a;
}

.mismatch {
  color: #f56c6c;
}
</style>