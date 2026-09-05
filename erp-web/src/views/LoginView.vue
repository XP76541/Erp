<template>
  <div class="login-page">
    <div class="login-shell">
      <section class="brand-panel">
        <div class="brand-lockup"><span class="brand-mark">贸</span><span>贸易 ERP</span></div>
        <div class="brand-copy">
          <p class="kicker">INVENTORY · SALES · FINANCE</p>
          <h1>把日常业务，<br />管得清楚一些。</h1>
          <p>从采购、库存到销售和收付款，<br />在一个工作台里掌握经营进度。</p>
        </div>
        <span class="brand-footer">内部业务管理系统</span>
      </section>
      <section class="login-panel">
        <div class="login-heading">
          <p class="eyebrow">欢迎回来</p>
          <h2>登录工作台</h2>
          <p>请输入账号和密码继续</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent>
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" size="large" autocomplete="username" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              autocomplete="current-password"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>
        <p class="tip">请使用已分配的企业账号登录</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    await router.push('/')
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 100%;
  place-items: center;
  padding: 32px 20px;
  background: #eaf0f1;
}

.login-shell {
  display: grid;
  width: min(900px, 100%);
  min-height: 500px;
  grid-template-columns: 1fr 1fr;
  overflow: hidden;
  border: 1px solid rgba(34, 68, 80, 0.08);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 14px 40px rgba(36, 70, 82, 0.12);
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 44px 46px;
  color: #eaf4f5;
  background: #214b5b;
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.brand-mark {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 6px;
  color: #214b5b;
  background: #c9e1e4;
  font-weight: 700;
}

.brand-copy { margin: auto 0; }
.kicker { margin-bottom: 18px; color: #a9c9ce; font-size: 11px; letter-spacing: 1.7px; }
.brand-copy h1 { margin: 0; color: #fff; font-size: 31px; font-weight: 600; line-height: 1.45; letter-spacing: 1px; }
.brand-copy p:last-child { margin-top: 19px; color: #c0d6d9; font-size: 14px; line-height: 1.9; }
.brand-footer { color: #91b5bb; font-size: 12px; }

.login-panel { display: flex; flex-direction: column; justify-content: center; padding: 58px 62px; }
.login-heading { margin-bottom: 30px; }.eyebrow { margin-bottom: 9px; color: var(--erp-brand); font-size: 13px; font-weight: 600; }.login-heading h2 { margin: 0; color: #263640; font-size: 25px; font-weight: 600; }.login-heading p:last-child { margin-top: 9px; color: #8a969d; font-size: 13px; }
.login-panel :deep(.el-form-item) { margin-bottom: 20px; }.login-panel :deep(.el-input__wrapper) { min-height: 46px; padding: 1px 14px; border-radius: 6px; }.login-btn { width: 100%; height: 46px; margin-top: 4px; border-radius: 6px; font-size: 15px; }
.tip { margin-top: 25px; color: #a0abb0; font-size: 12px; text-align: center; }

@media (max-width: 680px) {
  .login-page { padding: 18px; }
  .login-shell { display: block; min-height: auto; }
  .brand-panel { min-height: 180px; padding: 25px 28px; }
  .brand-copy { margin: 26px 0 0; }.brand-copy h1 { font-size: 23px; }.brand-copy p:last-child, .brand-footer { display: none; }
  .login-panel { padding: 36px 28px 42px; }
}
</style>
