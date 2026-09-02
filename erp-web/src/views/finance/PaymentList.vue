<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.supplierId" clearable filterable placeholder="供应商" style="width: 180px">
          <el-option v-for="supplier in suppliers" :key="supplier.id" :label="supplier.name" :value="supplier.id!" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px">
          <el-option label="草稿" value="DRAFT" /><el-option label="已审核" value="AUDITED" /><el-option label="作废" value="VOID" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增付款单</el-button>
      </div>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="docNo" label="单号" width="170" />
        <el-table-column prop="supplierName" label="供应商" min-width="150" />
        <el-table-column prop="bizDate" label="业务日期" width="115" />
        <el-table-column label="付款金额" width="120" align="right"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="核销金额" width="120" align="right"><template #default="{ row }">¥{{ money(row.allocatedAmount) }}</template></el-table-column>
        <el-table-column prop="method" label="付款方式" width="110" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 'AUDITED' ? 'success' : 'warning'">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="100"><template #default="{ row }"><el-button v-if="row.status === 'DRAFT'" link type="primary" @click="audit(row)">审核</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" class="pagination" @change="load" />
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增供应商付款单" width="850px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12"><el-col :span="8"><el-form-item label="供应商" prop="supplierId"><el-select v-model="form.supplierId" filterable style="width:100%" @change="loadPayables"><el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id!" /></el-select></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="付款日期" prop="bizDate"><el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="付款金额" prop="amount"><el-input-number v-model="form.amount" :min="0.01" :precision="2" :controls="false" style="width:100%" /></el-form-item></el-col></el-row>
        <el-form-item label="付款方式"><el-select v-model="form.method"><el-option label="转账" value="转账" /><el-option label="现金" value="现金" /><el-option label="支票" value="支票" /></el-select></el-form-item>
        <el-table :data="form.allocations" border size="small"><el-table-column prop="docNo" label="应付单号" /><el-table-column label="应付余额" width="130" align="right"><template #default="{ row }">¥{{ money(row.remainingAmount) }}</template></el-table-column><el-table-column label="本次核销" width="160"><template #default="{ row }"><el-input-number v-model="row.amount" :min="0" :max="row.remainingAmount" :precision="2" :controls="false" size="small" /></template></el-table-column></el-table>
        <el-form-item label="备注" class="remark"><el-input v-model="form.remark" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { financeApi, type Payment, type PaymentCreateRequest } from '@/api/finance'
import { payableApi, type PayableListResponse } from '@/api/payable'
import { supplierApi, type Supplier } from '@/api/supplier'

const loading = ref(false); const saving = ref(false); const list = ref<Payment[]>([]); const suppliers = ref<Supplier[]>([]); const total = ref(0); const dialogVisible = ref(false); const formRef = ref<FormInstance>()
const query = reactive({ supplierId: undefined as number | undefined, status: '', page: 1, size: 10 })
const form = reactive({ supplierId: undefined as number | undefined, bizDate: new Date().toISOString().slice(0, 10), amount: 0, method: '转账', bankAccount: '', remark: '', allocations: [] as Array<PayableListResponse & { amount: number }> })
const rules: FormRules = { supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }], bizDate: [{ required: true, message: '请选择付款日期', trigger: 'change' }], amount: [{ required: true, message: '请输入付款金额', trigger: 'blur' }] }
const money = (v: number) => (v ?? 0).toFixed(2); const statusText = (s: string) => s === 'DRAFT' ? '草稿' : s === 'AUDITED' ? '已审核' : '作废'
async function load() { loading.value = true; try { const data = await financeApi.getPayments(query); list.value = data.records; total.value = data.total } finally { loading.value = false } }
function search() { query.page = 1; load() }
function openCreate() { Object.assign(form, { supplierId: undefined, bizDate: new Date().toISOString().slice(0, 10), amount: 0, method: '转账', bankAccount: '', remark: '', allocations: [] }); dialogVisible.value = true }
async function loadPayables() { form.allocations = form.supplierId ? (await payableApi.list({ supplierId: form.supplierId, status: 'UNSETTLED', size: 500 })).records.map(p => ({ ...p, amount: 0 })) : [] }
async function save() { if (!await formRef.value?.validate().catch(() => false)) return; const allocations = form.allocations.filter(a => a.amount > 0).map(a => ({ payableId: a.id, amount: a.amount })); if (!allocations.length) return ElMessage.warning('请填写核销明细'); if (allocations.reduce((s, a) => s + a.amount, 0) !== form.amount) return ElMessage.warning('核销总额必须等于付款金额'); saving.value = true; try { const data: PaymentCreateRequest = { supplierId: form.supplierId!, bizDate: form.bizDate, amount: form.amount, method: form.method, bankAccount: form.bankAccount, remark: form.remark, allocations }; await financeApi.createPayment(data); ElMessage.success('付款草稿已保存'); dialogVisible.value = false; await load() } finally { saving.value = false } }
async function audit(row: Payment) { await ElMessageBox.confirm(`确认审核付款单 ${row.docNo}?`, '审核确认', { type: 'warning' }); await financeApi.auditPayment(row.id, {}); ElMessage.success('审核成功'); await load() }
onMounted(async () => { suppliers.value = (await supplierApi.page({ page: 1, size: 500 })).records; await load() })
</script>
<style scoped>.toolbar { display:flex; gap:8px; margin-bottom:16px; flex-wrap:wrap; }.pagination { margin-top:16px; justify-content:flex-end; }.remark { margin-top:16px; }</style>
