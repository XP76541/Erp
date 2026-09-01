<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="编码 / 名称 / 联系人 / 电话"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="load"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增供应商</el-button>
      </div>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="code" label="编码" width="130" />
        <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column label="账期" width="90" align="center">
          <template #default="{ row }">
            {{ row.paymentTermDays > 0 ? row.paymentTermDays + ' 天' : '现结' }}
          </template>
        </el-table-column>
        <el-table-column label="结算方式" width="100" align="center" prop="settleType" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isActive === 1"
              @change="(value: boolean | string | number) => handleToggle(row, !!value)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑供应商' : '新增供应商'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="供应商编码" prop="code">
          <el-input v-model="form.code" maxlength="30" placeholder="留空自动生成" :disabled="!!form.id" />
          <span class="form-tip">留空自动生成,创建后不可修改</span>
        </el-form-item>
        <el-form-item label="供应商名称" prop="name">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" maxlength="50" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="账期(天)" prop="paymentTermDays">
          <el-input-number v-model="form.paymentTermDays" :min="0" :max="365" />
          <span class="form-tip">0 = 现结;应付到期日 = 单据日期 + 账期</span>
        </el-form-item>
        <el-form-item label="结算方式" prop="settleType">
          <el-select v-model="form.settleType" filterable allow-create default-first-option style="width: 100%">
            <el-option label="现结" value="现结" />
            <el-option label="月结" value="月结" />
          </el-select>
          <span class="form-tip">可输入自定义结算方式</span>
        </el-form-item>
        <el-form-item label="开户行" prop="bankName">
          <el-input v-model="form.bankName" maxlength="100" />
        </el-form-item>
        <el-form-item label="银行账号" prop="bankAccount">
          <el-input v-model="form.bankAccount" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { supplierApi } from '@/api/supplier'
import type { Supplier } from '@/api/supplier'

const loading = ref(false)
const saving = ref(false)
const list = ref<Supplier[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Supplier>(defaultForm())

function defaultForm(): Supplier {
  return {
    name: '',
    contact: '',
    phone: '',
    paymentTermDays: 0,
    settleType: '现结',
    bankName: '',
    bankAccount: '',
  }
}

const rules: FormRules = {
  name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const data = await supplierApi.page({ ...query })
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
  Object.assign(form, defaultForm(), { id: undefined, code: undefined })
  dialogVisible.value = true
}

function openEdit(row: Supplier) {
  Object.assign(form, defaultForm(), row)
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (form.id) {
      await supplierApi.update(form.id, form)
    } else {
      await supplierApi.create(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleToggle(row: Supplier, active: boolean) {
  try {
    await supplierApi.toggleStatus(row.id!, active)
    ElMessage.success(active ? '已启用' : '已停用')
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  }
}

onMounted(load)
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

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
