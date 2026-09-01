<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="编码 / 名称 / 简称 / 联系人 / 电话"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="load"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增客户</el-button>
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
        <el-table-column label="业务员" width="100">
          <template #default="{ row }">
            {{ salespersonName(row.salespersonId) }}
          </template>
        </el-table-column>
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑客户' : '新增客户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="客户编码" prop="code">
          <el-input v-model="form.code" maxlength="30" placeholder="留空自动生成" :disabled="!!form.id" />
          <span class="form-tip">留空自动生成,创建后不可修改</span>
        </el-form-item>
        <el-form-item label="客户名称" prop="name">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="简称" prop="shortName">
          <el-input v-model="form.shortName" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" maxlength="50" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" maxlength="200" />
        </el-form-item>
        <el-form-item label="账期(天)" prop="paymentTermDays">
          <el-input-number v-model="form.paymentTermDays" :min="0" :max="365" />
          <span class="form-tip">0 = 现结;应收到期日 = 单据日期 + 账期</span>
        </el-form-item>
        <el-form-item label="信用额度" prop="creditLimit">
          <el-input-number v-model="form.creditLimit" :min="0" :precision="2" :controls="false" />
          <span class="form-tip">0 = 不限</span>
        </el-form-item>
        <el-form-item label="业务员" prop="salespersonId">
          <el-select v-model="form.salespersonId" clearable style="width: 100%">
            <el-option
              v-for="u in users"
              :key="u.id"
              :label="u.realName + ' (' + u.username + ')'"
              :value="u.id"
            />
          </el-select>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { customerApi } from '@/api/customer'
import type { Customer } from '@/api/customer'
import { userApi } from '@/api/user'
import type { UserOption } from '@/api/user'

const loading = ref(false)
const saving = ref(false)
const list = ref<Customer[]>([])
const total = ref(0)
const users = ref<UserOption[]>([])
const query = reactive({ page: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Customer>(defaultForm())

function defaultForm(): Customer {
  return {
    name: '',
    shortName: '',
    contact: '',
    phone: '',
    address: '',
    paymentTermDays: 0,
    creditLimit: 0,
    salespersonId: 0,
  }
}

const rules: FormRules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
}

const userMap = computed(() =>
  new Map(users.value.map((u) => [u.id, u.realName])),
)

function salespersonName(id?: number) {
  if (!id) return '—'
  return userMap.value.get(id) ?? '—'
}

async function load() {
  loading.value = true
  try {
    const data = await customerApi.page({ ...query })
    list.value = data.records
    total.value = data.total
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    users.value = await userApi.listActive()
  } catch {
    // 错误提示由 http 拦截器统一处理
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

function openEdit(row: Customer) {
  Object.assign(form, defaultForm(), row)
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (form.id) {
      await customerApi.update(form.id, form)
    } else {
      await customerApi.create(form)
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

async function handleToggle(row: Customer, active: boolean) {
  try {
    await customerApi.toggleStatus(row.id!, active)
    ElMessage.success(active ? '已启用' : '已停用')
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  }
}

onMounted(() => {
  load()
  loadUsers()
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

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
