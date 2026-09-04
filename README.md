# 小型贸易公司 ERP

面向小型贸易公司的进销存与财务管理系统，覆盖基础资料、采购、库存、销售、应收应付、收付款及经营报表。项目采用前后端分离架构，后端负责业务规则、事务、权限和数据一致性，前端负责业务操作与展示。

> 当前项目处于持续完善阶段。核心业务链路可以启动并运行，但并发压测、权限矩阵、历史数据升级和部分报表性能仍需要在上线前继续验证。

## 目录

- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [环境要求](#环境要求)
- [项目结构](#项目结构)
- [数据库与配置](#数据库与配置)
- [快速启动](#快速启动)
- [默认账号与角色](#默认账号与角色)
- [主要业务规则](#主要业务规则)
- [接口与前端约定](#接口与前端约定)
- [验证与测试](#验证与测试)
- [生产部署建议](#生产部署建议)
- [已知限制](#已知限制)
- [文档](#文档)

## 功能概览

### 已接入的模块

- **系统管理**：登录、退出、当前用户、用户与角色、操作日志、数据库备份。
- **基础资料**：商品、商品分类、客户、供应商、仓库。
- **采购管理**：采购订单、采购入库、采购退货。
- **销售管理**：销售订单、销售出库。
- **库存管理**：即时库存、出入库流水、库存调拨、库存盘点、库存预警。
- **财务管理**：应收账款、客户收款及多单核销、应付账款、供应商付款核销、催收记录、异常监控、账龄分析。
- **经营报表**：销售日报、库存汇总、财务汇总及 Excel 导出。

### 业务状态

采购入库、销售出库、付款等单据采用草稿/审核状态流转；应收收款核销已统一使用客户收款模型。已审核单据原则上不可直接修改，错误业务应通过退货、红字或冲销流程处理。

部分页面仍属于持续完善项，例如采购订单详情、部分统计接口的 SQL 聚合优化、报表图表增强和更完整的错误/空数据状态。请以实际路由和接口实现为准，不要将“页面存在”视为所有业务规则均已完成。

## 技术栈

| 层次 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.3.4、Spring MVC |
| 持久化 | MyBatis-Plus 3.5.7 |
| 数据库 | Microsoft SQL Server 2012+（已验证 SQL Server 2012 SP4；生产建议使用受支持版本） |
| 数据库迁移 | Flyway 10.10.0，当前迁移版本为 V14 |
| 前端 | Vue 3、Vite 5、TypeScript |
| UI 与状态 | Element Plus、Pinia、Vue Router、Axios |

## 环境要求

| 依赖 | 要求 | 说明 |
|---|---|---|
| JDK | 17 或更高 | 当前开发环境使用 JDK 17.0.12 |
| Maven | 3.8 或更高 | Windows 开发机可使用 `D:\apache-maven-3.9.16\bin\mvn.cmd` |
| SQL Server | 2012 或更高 | 应确认 TCP 1433、数据库账号权限和 SQL Server 登录方式已启用 |
| Node.js | 18 或更高 | 当前开发环境使用 Node.js 24 |
| npm | 随 Node.js | 用于安装前端依赖 |

## 项目结构

```text
ERP/
├── README.md
├── docs/
│   ├── requirements.md       # 需求、角色矩阵、流程和验收标准
│   └── database-design.md    # ER 图、表结构、索引、事务与并发说明
├── erp-server/               # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/erp/
│       │   ├── common/        # Result、分页、业务异常、全局异常处理
│       │   ├── config/        # MyBatis-Plus、Web 和应用配置
│       │   └── module/
│       │       ├── system/    # 认证、用户角色、取号、操作日志、备份
│       │       ├── masterdata/# 商品、客户、供应商、仓库
│       │       ├── purchase/  # 采购订单、入库、退货
│       │       ├── sales/     # 销售订单、出库
│       │       ├── inventory/ # 库存、台账、调拨、盘点、预警
│       │       └── finance/   # 应收应付、收付款、账龄、报表
│       └── resources/
│           ├── application.yml
│           └── db/migration/  # Flyway V1__... 至 V14__...
└── erp-web/                  # Vite + Vue 3 前端
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── api/               # Axios 封装和接口定义
        ├── layout/            # 主框架和菜单
        ├── router/            # 路由和登录/角色守卫
        ├── stores/            # Pinia 状态
        └── views/             # 按业务域划分的页面
```

## 数据库与配置

后端默认使用以下连接配置：

- 地址：`localhost:1433`
- 数据库：`erp`
- context path：`/api`
- HTTP 端口：`8080`
- 前端开发端口：`5173`
- 前端 `/api` 请求代理到 `http://localhost:8080`

先创建空数据库，表结构和基础数据由应用启动时的 Flyway 自动执行：

```sql
CREATE DATABASE erp COLLATE Chinese_PRC_CI_AS;
```

数据库用户名和密码通过环境变量覆盖，环境变量优先于 `application.yml` 中的开发默认值：

```text
DB_USER=erp_app
DB_PASSWORD=请替换为实际密码
```

Windows PowerShell 示例：

```powershell
$env:DB_USER = "erp_app"
$env:DB_PASSWORD = "你的密码"
```

Linux/macOS 示例：

```bash
export DB_USER=erp_app
export DB_PASSWORD='你的密码'
```

> 不要把生产数据库密码提交到 Git。生产环境应使用密钥管理或部署平台的环境变量注入，并为应用账号授予最小必要权限。

## 快速启动

### 1. 启动后端

在项目根目录执行：

```bash
cd erp-server
mvn spring-boot:run
```

Windows 且 Maven 未加入 `PATH` 时：

```powershell
cd erp-server
D:\apache-maven-3.9.16\bin\mvn.cmd spring-boot:run
```

也可以先打包再运行：

```bash
mvn clean package -DskipTests
java -jar target/erp-server-0.1.0-SNAPSHOT.jar
```

后端启动成功时应看到类似日志：

```text
Tomcat started on port 8080 with context path '/api'
Started ErpApplication
```

### 2. 启动前端

另开终端：

```bash
cd erp-web
npm install
npm run dev
```

默认访问地址：

- [http://localhost:5173/](http://localhost:5173/)
- 如果 5173 已被占用，Vite 会自动选择其他端口，请以终端输出的 `Local` 地址为准。

### 3. 登录

浏览器打开前端地址，使用默认管理员账号登录：

```text
用户名：admin
密码：admin123
```

默认账号仅适用于开发/演示环境，首次部署后应立即修改密码或停用该账号。

## 默认账号与角色

系统支持以下主要角色：

| 角色 | 主要权限 |
|---|---|
| ADMIN | 系统管理和全部业务权限 |
| BOSS | 经营查看及授权范围内的管理功能 |
| FINANCE | 应收应付、收付款、账龄、财务报表 |
| SALES | 本人负责客户范围内的销售和相关业务数据 |
| PURCHASE | 采购订单及供应商相关采购业务 |
| WAREHOUSE | 入库、出库、库存、调拨、盘点；不应查看成本和利润 |

角色名称和具体接口权限以服务端授权实现为准。前端菜单隐藏只是用户体验措施，不能替代后端鉴权。

## 主要业务规则

1. **权限优先由后端强制执行**：未登录返回 401，无权访问返回 403；SALES 的客户数据必须限制在其业务员范围内，空范围不能退化为全量数据。
2. **库存只由审核单据改变**：库存变更、库存台账、应付生成和操作日志应在同一事务中完成。
3. **审核并发控制**：审核动作使用数据库条件更新抢占，避免同一单据被重复审核。
4. **库存并发控制**：库存扣减使用数据库原子更新和行锁；多条应收/应付核销按单据 ID 稳定顺序加锁，降低死锁风险。
5. **金额精度**：金额最多保留两位小数，服务端重新计算和校验金额，不能信任前端传入的汇总值。
6. **收款核销**：客户收款可以关联多条同一客户的应收账款；同一应收不能在同一请求中重复核销。收款金额大于核销金额时，差额属于未分配金额，相关业务规则仍应由产品确认后再用于正式财务流程。
7. **幂等请求**：收款支持幂等键和请求指纹。同一幂等键不能复用到内容不同的请求；数据库唯一约束是并发场景的最终仲裁。
8. **日期口径**：报表和账龄查询应明确业务日期、截止日期及开始/结束日期边界；日期范围必须满足开始日期不晚于结束日期。
9. **成本与利润**：销售成本应来自审核时保存的出库成本快照。成本不完整时净利润显示为“不可用”，不得用采购额或其他近似值伪造利润。
10. **WAREHOUSE 脱敏**：WAREHOUSE 角色只查看库存数量和基础信息，服务端 API、前端页面和导出文件均不得泄露成本、库存金额或毛利。

## 接口与前端约定

- 后端所有业务接口均以 `/api` 为统一前缀，例如：`GET /api/customers`。
- 普通 JSON 响应使用统一结构：

  ```json
  {
    "code": 0,
    "message": "ok",
    "data": {}
  }
  ```

- 分页数据使用 `data.total` 和 `data.records`。
- Excel 导出接口返回 Blob，前端不能把 Blob 当作普通 JSON 解包。
- Controller 负责请求接收和响应转换，业务规则放在 Service，数据访问放在 Mapper。
- 新模块按照 `module/<domain>/{entity,mapper,service,controller}` 分层。
- 已审核单据不可直接修改；需要保留可追溯的审计日志。

## 验证与测试

后端测试与打包：

```bash
cd erp-server
mvn test
mvn clean package -DskipTests
```

Windows Maven 未加入 `PATH` 时，将 `mvn` 替换为：

```text
D:\apache-maven-3.9.16\bin\mvn.cmd
```

前端检查与构建：

```bash
cd erp-web
npm run typecheck
npm run build
```

提交前检查差异中的空白错误：

```bash
git diff --check
```

### 基础连通性检查

后端启动后可以先检查未登录响应：

```bash
curl -i http://localhost:8080/api/auth/me
```

预期为 401。登录后再访问客户列表：

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  "http://localhost:8080/api/customers?page=1&size=10"
```

建议至少验证以下链路：登录、客户新增与查询、供应商/商品查询、采购入库审核、库存流水、应收收款核销、报表查询和 Excel 导出。

上线前还必须补充真实 SQL Server 环境下的权限矩阵、收款幂等并发、同一应收并发核销、库存不足回滚、XLSX 内容及 WAREHOUSE 脱敏验证。

## 生产部署建议

1. 使用受支持的 SQL Server 版本，并为应用创建最小权限数据库账号。
2. 通过环境变量或密钥管理系统提供 `DB_USER`、`DB_PASSWORD`，不要使用仓库中的开发默认密码。
3. 生产数据库连接启用 TLS（例如 JDBC `encrypt=true`），并配置可信证书；不要长期依赖 `trustServerCertificate=true`。
4. 关闭 MyBatis SQL 详细日志和过度的 `com.erp` DEBUG 日志，避免敏感数据进入日志。
5. 先备份数据库，再执行新 Flyway 迁移；不要修改或重放已在环境中应用的迁移脚本，也不要执行 `flyway repair` 替代数据修复。
6. 新增唯一索引或约束前，先只读扫描历史重复数据和金额差异，形成可审计的清理方案。
7. 前端构建产物应通过 HTTPS 反向代理提供，反向代理将 `/api` 转发到后端；生产环境禁止开放开发服务器。
8. 关闭或限制数据库备份、用户管理等高风险接口，仅授予管理员使用。
9. 配置日志保留、数据库备份、健康检查和异常告警，并定期演练恢复流程。

## 已知限制

- 后端自动化业务测试目前覆盖不足，尤其是并发、角色矩阵、历史数据升级和 Excel 内容校验。
- 部分统计仍有进一步下推 SQL 聚合和批量加载的空间，大数据量下需要继续做性能验证。
- 收款幂等键发生数据库竞争时，部分场景仍可能先返回“请稍后重试”，调用方应安全重试同一请求。
- 部分前端页面和报表仍需补充空状态、错误重试、图表交互和更完整的 403 展示。
- Vite 生产构建目前存在主 chunk 较大的非阻断警告，需要后续通过拆包优化。
- 当前默认配置为开发配置，不能直接作为生产配置使用。

## 文档

| 文档 | 内容 |
|---|---|
| [docs/requirements.md](docs/requirements.md) | 功能列表、角色权限矩阵、业务流程、业务规则和验收标准 |
| [docs/database-design.md](docs/database-design.md) | ER 图、表结构、索引、事务、并发和数据库设计说明 |

如实现与文档不一致，应优先修正文档或补充已知限制，避免以过时的“下一步”列表误导开发和上线评估。
