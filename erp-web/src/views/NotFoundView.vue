<template>
  <div class="not-found">
    <el-result icon="warning" title="页面不存在" sub-title="请检查访问地址或返回首页">
      <template #extra><el-button type="primary" @click="router.push('/dashboard')">返回首页</el-button></template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.not-found { padding: 80px 24px; }
</style>
