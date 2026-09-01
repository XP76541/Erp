# 小型贸易公司 ERP

Vue 3 + Spring Boot 的进销存系统,服务小型贸易公司。一期范围:基础数据 + 简化采购 + 库存 + 销售(订单/出库/应收/收款)+ 基础报表 + 权限。

## 文档

| 文档 | 内容 |
|---|---|
| [docs/requirements.md](docs/requirements.md) | 功能列表、角色权限矩阵、业务流程图、业务规则、验收标准 |
| [docs/database-design.md](docs/database-design.md) | ER 图、表结构、索引、并发与事务要点 |

## 目录结构

```
ERP/
├── docs/                  # 需求与设计文档
├── erp-server/            # 后端 Spring Boot 3(JDK 17)
│   └── src/main/
│       ├── java/com/erp/
│       │   ├── common/        # Result / 分页 / 异常
│       │   ├── config/        # MyBatis-Plus / 拦截器注册
│       │   └── module/
│       │       ├── system/        # 认证、用户角色、单据取号、操作日志
│       │       └── masterdata/    # 商品、客户、供应商、仓库
│       └── resources/
│           ├── application.yml
│           └── db/migration/      # Flyway 建表脚本(V1__init.sql,26 张表)
└── erp-web/               # 前端 Vite + Vue3 + TS + Element Plus + Pinia
    └── src/
        ├── api/               # axios 封装与接口定义
        ├── layout/            # 主框架(侧边菜单)
        ├── router/            # 路由与登录守卫
        ├── stores/            # Pinia
        └── views/             # 页面,按业务域分目录
```

## 技术栈

- 后端:JDK 17 / Spring Boot 3.3 / MyBatis-Plus 3.5 / Flyway / SQL Server 2016+(Express 可用)
- 前端:Vite 5 / Vue 3.5 / TypeScript / Element Plus / Pinia / Vue Router / Axios

## 环境要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | 本机已装 JDK 17.0.12 ✓ |
| Maven | 3.8+ | 本机 3.9.16(D:\apache-maven-3.9.16,注意配置 PATH) |
| SQL Server | 2012+ | 本机 2012 SP4 已验证可跑通(生产建议 2022) |
| Node.js | 18+ | 本机 v24 ✓ |

## 快速启动

1. **创建数据库**(表结构由 Flyway 启动时自动执行,无需手工建表):

   ```sql
   CREATE DATABASE erp COLLATE Chinese_PRC_CI_AS;
   ```

2. **启动后端**(数据库账号密码不同时用环境变量覆盖):

   ```bash
   cd erp-server
   mvn spring-boot:run        # 或 IDEA 中直接运行 ErpApplication
   # DB_USER=sa DB_PASSWORD=你的密码 mvn spring-boot:run
   ```

   首次启动自动初始化:默认账号 **admin / admin123**、三个仓库(正品仓/次品仓/样品仓)、四个角色。

3. **启动前端**:

   ```bash
   cd erp-web
   npm install
   npm run dev                # http://localhost:5173,/api 已代理到 8080
   ```

4. 浏览器打开 http://localhost:5173,用 admin / admin123 登录。

## 已实现

- 统一响应 `Result`、全局异常处理、登录拦截(Bearer Token)
- 登录 / 退出 / 当前用户;路由守卫
- 单据取号服务 `DocSequenceService`(行锁防并发重号,单据号 `SO20260831-0001` 格式)
- **基础数据模块(F101,全部完成)** —— 后续模块照此分层模式开发:
  - 商品档案:分页搜索、编码自动生成 `SKU%06d`、修改、停用启用
  - 商品分类:两级树形(上级必须是根分类)、同级重名拦截、有启用子分类不许停用
  - 仓库档案:类型限正品仓/次品仓/样品仓、编码唯一且不可改
  - 客户档案:编码自动生成 `CUS%06d`、编码+名称双唯一、账期/信用额度、业务员(数据权限锚点)
  - 供应商档案:编码自动生成 `SUP%06d`、编码+名称双唯一、账期/结算方式/开户行账号

## 开发约定(重要)

1. 新模块按 `module/<域>/{entity,mapper,service,controller}` 分层,Controller 只做参数转换,业务在 Service。
2. 库存数量只能由**已审核单据**改变,禁止直接改 `inventory`;扣库存用原子 UPDATE(见 database-design.md §6)。
3. 已审核单据不可修改,错误走红字/退货流程。
4. 所有单据带 `biz_date`(业务日期)与审计字段;状态机:DRAFT → AUDITED → VOID。
5. 前端页面放 `views/<业务域>/`,接口定义放 `api/`,错误提示统一交给 http 拦截器。

## 下一步(按优先级)

1. **采购入库单 + 应付 + 付款核销**(第一张业务单据,打通"审核写库存/台账/应付"事务模式)
2. 销售订单 + 出库(含部分发货、限价校验、信用额度预警)
3. 收款核销 + 应收账龄
4. 报表(销售日报、进销存汇总)
