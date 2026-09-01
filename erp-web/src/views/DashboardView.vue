<template>
  <el-card shadow="never">
    <h3>欢迎使用贸易 ERP</h3>
    <p class="desc">当前为项目骨架,已完成:登录认证、商品档案 CRUD 参考实现。</p>

    <el-divider />

    <h4>一期开发计划(对应 docs/requirements.md)</h4>
    <el-table :data="plan" border stripe size="small">
      <el-table-column prop="module" label="模块" width="180" />
      <el-table-column prop="scope" label="内容" />
      <el-table-column prop="status" label="状态" width="120" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
const plan = [
  { module: '基础数据', scope: '商品/客户/供应商/仓库档案', status: '商品已完成' },
  { module: '采购(简化)', scope: '采购入库单、付款核销', status: '待开发' },
  { module: '库存', scope: '即时库存、出入库流水、移动加权成本', status: '待开发' },
  { module: '销售', scope: '销售订单、销售出库、应收、收款核销', status: '待开发' },
  { module: '报表', scope: '销售日报/月报、应收账龄、进销存汇总', status: '待开发' },
  { module: '系统管理', scope: '用户/角色/权限、操作日志、单据编号', status: '编号与登录已完成' },
]
</script>

<style scoped>
.desc {
  color: #909399;
  margin-top: 8px;
}

h4 {
  margin-bottom: 12px;
}
</style>
