# Billsight · 多源账单管理与个人财务分析系统（后端）

Java 21 + Spring Boot 4 多模块后端。面向个人账单场景，提供 CSV / Excel 动态表头流式导入、支付详情页 OCR 识别、三级智能分类（规则 → 朴素贝叶斯 → 大模型兜底）、人工纠错闭环反哺、原始数据双表溯源。

> 智能应用开发比赛 · 赛题 B · 队伍：黎德锦 · 报名序号：2

---

## 模块架构

```
finance-bill-backend        聚合 POM，统一构建入口
├── finance-domain          领域层：POJO、MyBatis Mapper 接口与 XML、枚举（无业务逻辑，被全层依赖）
├── finance-common          公共层：账单解析器（CSV / Excel）、文件存储抽象、当前用户上下文
├── finance-service         业务层：导入、匹配、规则、贝叶斯、分类、大模型、OCR、交易
└── finance-web             接入层：Spring Boot 启动模块、Controller、线程池与 OCR 配置
```

依赖方向：`web → service → common → domain`。三个库模块均已在 POM 中跳过 `repackage`，仅 `finance-web` 产出可执行 Jar。

## 技术栈

| 分类 | 选型 |
| --- | --- |
| 运行时 | JDK 21 |
| 框架 | Spring Boot 4、Spring Web MVC |
| 持久化 | MyBatis + MySQL 8.0（HikariCP 连接池） |
| 文件解析 | OpenCSV（流式逐行）、Apache POI（流式读取） |
| OCR | 阿里云 OCR SDK（支付详情页截图识别） |
| 大模型 | OpenAI 兼容协议（默认对接 DeepSeek） |
| 并发 | 自定义 ThreadPoolExecutor + CompletableFuture |
| 构建 | Maven 3.9+（多模块） |
| 其他 | Lombok、JAXB |

## 环境要求

- **JDK 21**（`java -version` 需为 21）
- **Maven 3.9+**
- **MySQL 8.0+**

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < sql/setup.sql
```

脚本会创建 `match` 库、6 张表（`txn`、`raw_record`、`sys_rule`、`user_rule`、`bayes_category_stat`、`bayes_word_freq`），并写入系统内置规则与贝叶斯初始词频作为种子数据。

> 注意：脚本包含 `DROP TABLE IF EXISTS`，用于重建表结构，请勿在已有生产数据上直接执行。

### 2. 配置私有参数

数据库口令与第三方密钥不进入版本库，需本地生成：

```bash
# Windows
copy src\main\resources\application-example.yml finance-web\src\main\resources\application-local.yml

# macOS / Linux
cp finance-web/src/main/resources/application-example.yml finance-web/src/main/resources/application-local.yml
```

打开 `finance-web/src/main/resources/application-local.yml`，填写：

| 配置项 | 必填 | 说明 |
| --- | --- | --- |
| `spring.datasource.password` | 是 | MySQL 密码 |
| `llm.api-key` | 否 | 大模型兜底分类密钥，留空则降级为规则 + 贝叶斯 |
| `aliyun.ocr.access-key-id` | 否 | 阿里云 OCR，留空则 OCR 接口返回未启用 |
| `aliyun.ocr.access-key-secret` | 否 | 同上 |

所有配置项同时支持环境变量注入（`DB_PASSWORD`、`LLM_API_KEY`、`ALIYUN_OCR_ACCESS_KEY_ID` 等），优先级高于配置文件，适合部署环境使用。

### 3. 构建

```bash
mvn -f finance-bill-backend/pom.xml clean package -DskipTests
```

产物：`finance-web/target/finance-web-0.0.1-SNAPSHOT.jar`（约 61 MB，含全部依赖）

### 4. 启动

```bash
java -jar finance-web/target/finance-web-0.0.1-SNAPSHOT.jar
```

启动成功后监听 `http://localhost:8080`。验证：

```bash
curl http://localhost:8080/api/files/upload
```

前端仓库：[billsight-frontend](https://github.com/xlucky-hash/billsight-frontend)。前端将 `VITE_USE_MOCK` 置为 `false` 并配置 `/api` 代理后即可与本服务联调。

## 目录结构

```
finance-bill-backend/        聚合 POM
finance-domain/
├── Enum/                    业务枚举（MatchType 等）
├── mapper/                  MyBatis Mapper 接口（Txn / RawRecord / Match / Bayes*）
├── pojo/                    实体与 DTO（Txn、RawRecord、Result 等）
└── resources/com/ldj/mapper/  Mapper XML
finance-common/
└── common/utlis/            账单解析（BillParser + CSV/Excel 实现）、文件存储、用户上下文
finance-service/
└── service/impl/            BillImport / Match / Classification / Bayes / Llm / Ocr / Txn
finance-web/
├── config/                  线程池、OCR 客户端配置
├── controller/              FileController、TransactionsController
└── resources/               application.yml 与本地私有配置
sql/setup.sql                数据库初始化脚本（结构 + 种子数据）
```

## 核心设计要点

**1. 动态表头流式导入**
不同银行导出的 CSV / Excel 列名与顺序不一致，导入时先读取表头做列名映射（别名表匹配），再逐行流式解析，全程不将整表载入内存，避免大文件 OOM。解析失败的行写入 `raw_record` 并标记状态，不阻断整批导入。

**2. 三级分类与降级链路**

| 层级 | 策略 | 命中即返回 |
| --- | --- | --- |
| 一级 | 规则匹配：用户规则优先于系统规则，按权重与优先级排序 | 是 |
| 二级 | 朴素贝叶斯：基于 `bayes_word_freq` 词频计算后验概率 | 置信度达阈值即返回 |
| 三级 | 大模型兜底：3 秒超时，超时或失败则回落到默认分类 | 是 |

任一层不可用时自动降级，保证导入流程不中断。

**3. 人工纠错闭环反哺**
用户在 `PATCH /api/transactions/{id}/category` 修改分类后，系统同步更新用户规则命中计数与贝叶斯词频统计，下一次同类账单可被规则或模型直接命中，形成「纠错 → 学习 → 更准」的闭环。

**4. 双表溯源**
`txn` 存储结构化交易，`raw_record` 永久保留原始行文本或 OCR 结构化结果，通过 `batch_id` 关联。分类争议时可回溯原始凭证，也便于离线重跑解析逻辑。

**5. 并发导入**
批量文件的解析与 OCR 识别通过线程池 + `CompletableFuture` 并发执行，按批次汇总结果，单文件失败不影响其他文件。

**6. 敏感字段处理**
商户名称等敏感字段以 `counterparty_enc` 加密列存储；金额保持明文以支持聚合统计。文件落盘时对上传路径做规范化校验，防止路径穿越。

## 已实现接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/files/upload` | 上传 CSV / Excel，触发解析与分类导入 |
| PATCH | `/api/transactions/{id}/category` | 修改账单分类并触发规则/词频反哺 |

其余接口（登录、账户、分类字典、报表、用户规则、导入批次查询）已在服务层与 Mapper 层具备实现，控制器层尚未全部暴露；前端当前通过 Mock 层对接，接口契约见提交材料中的《接口设计》文档。

## 已知限制

1. **控制器覆盖不全**：仅 2 个接口对外暴露，其余服务能力待补充 Controller。
2. **无自动化测试**：`src/test` 下仅有应用启动测试，核心分类逻辑缺少单元测试覆盖。
3. **大模型超时策略固定**：3 秒硬超时，未按模型响应特征做动态调整。
4. **无分布式锁**：并发导入同一批次时依赖数据库唯一约束兜底，缺少分布式场景下的互斥控制。
5. **OCR 依赖外部服务**：未配置密钥时该链路整体不可用，需保证降级路径正确。
6. **缺少数据库迁移工具**：表结构通过 `sql/setup.sql` 手工执行，未接入 Flyway / Liquibase。

## 相关文档

需求分析、原型设计、数据库设计、接口设计、初步验证五份技术文档随比赛材料提交。其中「初步验证」记录了本仓库的构建结果（五模块 `BUILD SUCCESS`）、启动耗时与上述限制项。
