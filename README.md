# Lion 商城（lion-cloud-mall）

> 一个**用于学习/复习 Spring Cloud 微服务技术栈**的最小可运行商城项目：
> 服务数量精简（网关 + 用户 + 商品 + 订单），覆盖微服务开发主流知识点，
> 可完整走通「注册登录 → 浏览商品 → 加购 → 下单 → 支付 / 取消」的商品流程。

---

## 一、技术栈

| 分类 | 技术 | 说明 |
| --- | --- | --- |
| 基础框架 | Spring Boot 3.2.10（JDK 21） | 后端基础框架 |
| 微服务 | Spring Cloud 2023.0.1 | 微服务标准 |
| 微服务 | Spring Cloud Alibaba 2023.0.1.0 | 阿里微服务套件 |
| 注册中心 | Nacos Discovery | 服务注册与发现 |
| 配置中心 | Nacos Config | 配置集中管理与动态刷新 |
| 服务调用 | OpenFeign + Spring Cloud LoadBalancer | 声明式远程调用 + 负载均衡 |
| 网关 | Spring Cloud Gateway | 统一入口、路由、鉴权、跨域 |
| 认证 | Sa-Token + Redis | 登录鉴权，多服务共享登录态 |
| 缓存 / 锁 | Redis + Redisson | 分布式锁防超卖 |
| 持久层 | MyBatis-Plus + MySQL 8 | ORM 与分页 |
| 接口文档 | SpringDoc（OpenAPI 3 / Swagger） | 在线接口文档 |
| 前端 | Vue3 + Vite + Element Plus + Pinia | 商城页面 |
| 链路追踪 | Apache SkyWalking 9.7.0 + Java Agent | APM：链路追踪、服务拓扑、性能指标 |
| 分布式事务 | Seata 2.0.0（AT 模式） | 下单跨服务一致性：订单回滚时库存一并回滚 |
| 部署 | Docker + Docker Compose | 一键编排启动 |

---

## 二、模块结构

```
lion-cloud-mall
├── mall-common        # 公共模块：统一响应 R、全局异常、BaseEntity、用户上下文
├── mall-api           # 服务间调用：OpenFeign 接口 + DTO + 降级工厂
├── mall-gateway       # 网关服务   :8080
├── mall-user          # 用户服务   :8101   库 mall_user
├── mall-product       # 商品服务   :8102   库 mall_product
├── mall-order         # 订单服务   :8103   库 mall_order
├── mall-frontend      # 前端       :5173(开发) / :80(容器)
├── docker             # MySQL 初始化脚本、Nacos 配置示例
└── docker-compose.yml # 一键编排（MySQL / Redis / Nacos / 各微服务 / 前端）
```

各服务端口与接口文档：

| 服务 | 端口 | Swagger 地址 |
| --- | --- | --- |
| mall-gateway | 8080 | 统一入口 `/api/**` |
| mall-user | 8101 | http://localhost:8101/swagger-ui.html |
| mall-product | 8102 | http://localhost:8102/swagger-ui.html |
| mall-order | 8103 | http://localhost:8103/swagger-ui.html |

---

## 三、核心流程

```
前端(Vue) ──► Gateway(8080, 鉴权 + 路由 + 负载均衡)
                 ├── /api/user/**    ──► mall-user      (注册/登录，Sa-Token 签发 token)
                 ├── /api/product/** ──► mall-product   (商品查询/维护，Redisson 锁扣库存)
                 └── /api/order/**   ──► mall-order     (下单)
                                          ├── Feign ──► mall-user    (校验用户)
                                          └── Feign ──► mall-product (查商品/扣库存/回滚库存)
```

- **登录态共享**：登录 token 存于 Redis，网关与各服务共用同一套 Sa-Token 配置。
- **用户ID 透传**：网关鉴权通过后写入 `X-User-Id` 请求头，下游服务通过 `UserContext` 读取。
- **Feign 调用透传**：`FeignConfig` 中的拦截器会把 `satoken`、`X-User-Id` 带到下游服务。
- **防超卖**：商品服务扣库存 = Redisson 分布式锁 + `update ... where stock >= ?` 原子 SQL。

---

## 四、快速开始

### 方式一：Docker Compose（推荐，含所有中间件）

```bash
# 1. 打包（生成各模块 target/*.jar）
mvn -DskipTests clean package

# 2. 构建镜像并启动
docker compose up -d --build

# 3. 查看日志
docker compose logs -f mall-gateway
```

启动后访问：

- 前端：http://localhost
- Nacos 控制台：http://localhost:8848/nacos（账号/密码：`nacos` / `nacos`）

### 方式二：本地开发（中间件用 Docker，服务本地启动）

```bash
# 1. 只启动中间件（专用基础设施编排文件，详见下一节）
docker compose -f docker-compose-infra.yml up -d

# 2. 依次启动服务（也可在 IDEA 中直接运行启动类）
mvn -pl mall-gateway -am spring-boot:run
mvn -pl mall-user    -am spring-boot:run
mvn -pl mall-product -am spring-boot:run
mvn -pl mall-order   -am spring-boot:run

# 3. 启动前端
cd mall-frontend
npm install
npm run dev
```

> 默认连接信息：MySQL `root/root:3307`（宿主机端口，本机 3306 已被占用），
> Redis `lion123:6379`，Nacos `nacos/nacos:8848`。
> 若本机环境不同，可通过环境变量覆盖：`MYSQL_HOST`、`MYSQL_PWD`、`REDIS_HOST`、`REDIS_PWD`、`NACOS_ADDR`。

### 基础设施编排（Windows 本地开发推荐）

根目录的 `docker-compose-infra.yml` 只包含 MySQL / Redis / Nacos，不含业务服务，
适合本地用 IDEA 跑服务、中间件放容器的开发方式。

| 组件 | 地址 | 账号 | 说明 |
| --- | --- | --- | --- |
| MySQL | `127.0.0.1:3307` | `root` / `root` | 首次启动自动执行 `docker/mysql/init/01-schema.sql`（容器内端口仍为 3306） |
| Redis | `127.0.0.1:6379` | 密码 `lion123` | Sa-Token token 存储 + Redisson 锁 |
| Nacos | `127.0.0.1:8848` | `nacos` / `nacos` | 控制台 http://localhost:8848/nacos |
| SkyWalking UI | http://localhost:8081 | 无需登录 | APM 控制台：拓扑图 / 链路 / 性能指标 |
| SkyWalking OAP | gRPC `11800` / HTTP `12800` | - | Agent 上报端口，一般不直接访问 |

常用命令（PowerShell 或终端，切到项目根目录）：

```powershell
# 后台启动（首次会拉取镜像，Nacos 完全就绪约需 1~2 分钟）
docker compose -f docker-compose-infra.yml up -d

# 查看容器状态（healthy 表示可用）
docker compose -f docker-compose-infra.yml ps

# 实时看某个组件日志
docker compose -f docker-compose-infra.yml logs -f nacos

# 停止 / 启动（保留数据）
docker compose -f docker-compose-infra.yml stop
docker compose -f docker-compose-infra.yml start

# 删除容器（保留数据卷）
docker compose -f docker-compose-infra.yml down

# 彻底重置（删除数据卷，下次启动会重新初始化数据库）
docker compose -f docker-compose-infra.yml down -v
```

Windows 注意事项：

1. 安装 Docker Desktop 并启用 **WSL2 后端**，内存建议分配 ≥ 4G（Nacos 较吃内存）。
2. 端口被占用会启动失败，可先检查：`netstat -ano | findstr 3307`，或修改 compose 中的宿主机端口（如 `"3308:3306"`）。
   MySQL 默认映射 `3307:3306`，若你改了端口，记得同步设置环境变量 `MYSQL_PORT`。
3. 若 Docker Desktop 弹出文件共享确认，允许项目所在盘符（脚本挂载依赖此权限）。
4. 想改 Redis 密码：同步修改 compose 的 `--requirepass` 与服务配置中的 `${REDIS_PWD}`。
5. Nacos 的 9848/9849 端口必须映射出来 —— Java 服务（Nacos Client 2.x）通过 gRPC 与服务端通信。

#### Nacos 数据存储方式

`docker-compose-infra.yml` 中的 Nacos 采用**外置 MySQL 持久化**（`SPRING_DATASOURCE_PLATFORM: mysql`），
配置与服务注册信息都落在 `nacos_config` 库中，方便直接查表观察。

相关文件与变量：

| 项 | 值 | 说明 |
| --- | --- | --- |
| 建库建表脚本 | `docker/mysql/init/02-nacos-schema.sql` | MySQL 首次启动自动执行（按文件名顺序，在 01 之后） |
| 数据库 | `nacos_config` | 由脚本自动创建 |
| `MYSQL_SERVICE_HOST` | `mysql` | 容器网络内的服务名 |
| `MYSQL_SERVICE_PORT` | `3306` | **容器内端口**，不是宿主机的 3307 |
| `SPRING_DATASOURCE_PLATFORM` | `mysql` | 指定使用 MySQL 而非内置 Derby |

MySQL 8 使用 `caching_sha2_password`，连接参数里已带 `allowPublicKeyRetrieval=true`，
否则 Nacos 会报 `Public Key Retrieval is not allowed`。

验证是否切换成功：

```powershell
docker exec -it mall-mysql mysql -uroot -proot -e "use nacos_config; show tables;"
# 应看到 config_info / his_config_info / tenant_info / users 等表
```

想改回**内置 Derby**（不依赖 MySQL）：删除 nacos 服务中的 `SPRING_DATASOURCE_PLATFORM`
与全部 `MYSQL_SERVICE_*` 环境变量，并把挂载改回 `- nacos-data:/home/nacos/data`，然后
`docker compose -f docker-compose-infra.yml up -d --force-recreate nacos`。

中间件起来后，用 IDEA 依次启动 `MallGatewayApplication` → `MallUserApplication` →
`MallProductApplication` → `MallOrderApplication`，前端执行 `npm run dev` 即可。

### 初始化数据

`docker/mysql/init/01-schema.sql` 会在 MySQL 首次启动时自动执行（建库、建表、灌入 8 条商品数据）。
用户密码为 `MD5(密码 + 用户名)` 加盐存储，请通过前端「注册」页面创建账号后登录。

---

## 五、Nacos 配置中心

各服务通过 `bootstrap.yml` 从 Nacos 拉取配置，示例配置在 `docker/nacos-config/`：

| dataId | group | 说明 |
| --- | --- | --- |
| `mall-common.yaml` | DEFAULT_GROUP | Redis、Sa-Token 等共享配置 |
| `mall-gateway.yaml` | DEFAULT_GROUP | 网关专属配置 |

导入方式：Nacos 控制台 → 配置管理 → 配置列表 → 新建配置（Data ID / Group 与上表一致，格式 YAML）。
**不导入也能正常启动**，各服务本地 `application.yml` 已包含完整默认配置；导入后可体验配置热更新。

---

## 六、主要接口

| 方法 | 路径 | 说明 | 需登录 |
| --- | --- | --- | --- |
| POST | `/api/user/register` | 注册 | 否 |
| POST | `/api/user/login` | 登录，返回 `token` | 否 |
| GET | `/api/user/info` | 当前用户信息 | 是 |
| GET | `/api/product/page` | 商品分页（pageNo/pageSize/keyword） | 否 |
| GET | `/api/product/{id}` | 商品详情 | 否 |
| POST | `/api/product` | 发布商品 | 是 |
| PUT | `/api/product` | 修改商品 | 是 |
| DELETE | `/api/product/{id}` | 删除商品 | 是 |
| POST | `/api/order/create` | 下单 | 是 |
| GET | `/api/order/page` | 我的订单 | 是 |
| GET | `/api/order/{id}` | 订单详情 | 是 |
| POST | `/api/order/{id}/pay` | 支付（模拟） | 是 |
| POST | `/api/order/{id}/cancel` | 取消订单（回滚库存） | 是 |

前端请求头携带 token：`satoken: <登录后返回的 token>`。

---

## 七、可扩展的学习点

- `mall-order` 中开启 `spring.cloud.openfeign.circuitbreaker.enabled=true` 并引入
  `spring-cloud-starter-circuitbreaker-resilience4j`，即可体验 `fallbackFactory` 降级。
- 接入 Sentinel 做限流熔断、Seata 解决分布式事务（下单跨服务的库存一致性）。
- 商品列表加 Redis 缓存、订单超时自动取消（RabbitMQ 延迟消息 / Redisson 延迟队列）。

---

## 八、SkyWalking 链路追踪（可选）

SkyWalking 是 APM（应用性能监控）系统，提供**分布式链路追踪 + 服务拓扑 + 性能指标**。
它通过 Java Agent 的字节码增强实现——**不用改任何业务代码**，加个启动参数就能用。

> **版本选择**：固定 9.7.0，存储使用内置 H2，不需要额外的 Elasticsearch / BanyanDB。
> SkyWalking 从 10.2.0 起默认存储改成了 BanyanDB，若升级镜像版本需同步调整 `SW_STORAGE`。

### 8.1 启动服务端

已写在 `docker-compose-infra.yml` 中，和其它中间件一起起来即可：

```powershell
docker compose -f docker-compose-infra.yml up -d
```

| 组件 | 地址 | 说明 |
| --- | --- | --- |
| SkyWalking UI | http://localhost:8081 | 拓扑图 / 链路 / 指标 |
| OAP gRPC | `127.0.0.1:11800` | Agent 上报数据用 |
| OAP HTTP | `127.0.0.1:12800` | UI 的查询接口 |

### 8.2 下载 Java Agent

```powershell
powershell -ExecutionPolicy Bypass -File .\docker\skywalking\download-agent.ps1
```

会解压到 `docker/skywalking/agent/`（该目录已在 `.gitignore` 中忽略，不用提交）。

### 8.3 本地 IDEA 启动时挂载

各服务 `Run/Debug Configuration` → `VM options`，填入（`mall-user` 换成对应服务名）：

```bash
-javaagent:"E:/xxkfz/lion-cloud-mall/docker/skywalking/agent/skywalking-agent.jar"
-Dskywalking.agent.service_name=mall-user
-Dskywalking.collector.backend_service=127.0.0.1:11800
```

| 服务 | service_name |
| --- | --- |
| 网关 | `mall-gateway` |
| 用户 | `mall-user` |
| 商品 | `mall-product` |
| 订单 | `mall-order` |

### 8.4 容器化部署时挂载

全栈编排（根目录 `docker-compose.yml`）已默认把 `./docker/skywalking/agent` 挂进
4 个业务服务，同样只需先执行 8.2 的下载脚本：

```powershell
mvn -DskipTests clean package
docker compose up -d --build
```

各服务的 Dockerfile 里做了保护：**只有检测到 agent 文件存在才会加 `-javaagent`**，
没下载也不会导致启动失败。

### 8.5 验证效果

服务正常启动后，走一遍业务（浏览商品 → 加购 → 下单），然后打开
http://localhost:8081，左侧菜单应能看到：

- **Service**：4 个微服务，各自的 QPS、响应时间、成功率
- **Topology**：自动生成的调用关系图（`gateway → order → user/product`）
- **Trace**：每次请求的完整调用链，能精确到每一跳的耗时

典型的下单链路：

```
POST /api/order/create
├── mall-gateway
├── mall-order
│   ├── Feign → mall-user
│   └── Feign → mall-product
└── MySQL UPDATE t_product
```

> 提醒：OAP 默认堆内存 1G，建议 Docker Desktop 内存分配 ≥ 6G
> （MySQL + Redis + Nacos 已经占了一部分）。

### 8.6 日志采集（Log 页签）

Java Agent 只上报**链路 + 指标**，业务日志必须由应用主动上报，否则 UI 的「Log」页签
永远是空的。本项目已配好这条链路：

| 位置 | 内容 |
| --- | --- |
| 各服务 `pom.xml` | 引入 `apm-toolkit-logback-1.x`（版本与 Agent 一致，由父 pom 统一管理） |
| 各服务 `logback-spring.xml` | 挂 `GRPCLogClientAppender`，把 `com.lion.mall` 的日志推送到 OAP |
| 日志格式中的 `%tid` | 输出 traceId，日志与链路可互相跳转 |

几点说明：

1. 只有 `com.lion.mall` 包下的业务日志会上报，框架日志（`org.springframework.*` 等）
   只进控制台，避免刷爆 OAP（9.7 默认 H2 存储，容量有限）。
2. 日志上报复用 Agent 的上报通道，所以容器里必须通过
   `SW_AGENT_COLLECTOR_BACKEND_SERVICES=skywalking-oap:11800` 指定地址，
   已在 `docker-compose.yml` 的 4 个服务里配好。
3. `%tid` 依赖 Agent：不挂 agent 裸跑时该位置为空占位符，属正常现象，不影响启动。
4. 查看方式：UI 左侧 **Log** → 服务选 `mall-order` → 选时间范围 → 可再填 `traceId`
   或关键字。下单一单后应能看到「下单成功」「扣减库存成功」等记录。

---

## 九、Seata 分布式事务（AT 模式）

下单要写 `mall_order`（订单库）并远程扣 `mall_product`（商品库）库存，
这是典型的**跨服务、跨库写**，本地 `@Transactional` 管不到商品服务，需要分布式事务。

### 9.1 角色与版本

| 角色 | 服务 | 说明 |
| --- | --- | --- |
| TC（事务协调者） | `seata-server` 容器 | 维护全局事务与分支事务状态，决定提交/回滚 |
| TM（事务管理器） | `mall-order` | `@GlobalTransactional` 标注处，开启全局事务 |
| RM（资源管理器） | `mall-order`、`mall-product`、`mall-user` | 各自本地分支事务，向 TC 注册分支 |

版本：**客户端与服务端统一 2.0.0**。该版本由 Spring Cloud Alibaba 2023.0.1.0 的 BOM
锁定（`spring-cloud-alibaba-dependencies` 里 `seata.version=2.0.0`），
所以 pom 里引 `io.seata:seata-spring-boot-starter` **不需要写版本**，镜像也用
`seataio/seata-server:2.0.0`，两者必须同版本。

### 9.2 改动清单（已落地）

| 位置 | 改动 |
| --- | --- |
| `docker-compose.yml` / `docker-compose-infra.yml` | 新增 `seata-server` 服务（8091 业务端口 / 7091 控制台），业务服务 `depends_on` 它 |
| `docker/seata/application.yml` | TC 配置：注册中心 Nacos、配置 file、存储 file |
| `docker/mysql/init/01-schema.sql` | 三个业务库各建 `undo_log`（AT 模式回滚日志表） |
| `mall-api` / `mall-user` / `mall-product` / `mall-order` pom | 引入 `seata-spring-boot-starter` |
| 三个服务 `application.yml` | `seata.*` 配置（事务分组 `mall_tx_group`、Nacos 注册中心、AT 代理） |
| `FeignConfig` | 新增 `seataXidInterceptor()`，透传 `TX_XID` 请求头 |
| `OrderServiceImpl#create` | 加 `@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)` |

### 9.3 XID 透传（最关键）

`mall-order` 开启全局事务后，TC 会生成 XID。Feign 调用商品服务时**必须**把 XID 带过去，
否则商品服务不知道自己属于这个全局事务，结果是「订单回滚了、库存却扣了」：

```java
template.header(RootContext.KEY_XID, RootContext.getXID());   // 请求头 TX_XID
```

### 9.4 启动与验证

```powershell
bin\start-all.bat
```

1. Nacos 控制台（http://localhost:8848/nacos）→ 服务列表应能看到 `seata-server`。
2. Seata 控制台：http://localhost:7091（账号 `seata` / `seata`）。
3. 下单一单，日志里应出现：
   - `mall-order`：`Begin new global transaction [...]` / `global transaction ... will be committed`
   - `mall-product`：`branch register success` / `Branch Session rollback`
4. 故意让下单失败（例如在 `OrderServiceImpl#create` 末尾临时 `throw new RuntimeException()`），
   检查 `t_order` 没新增 **且** `t_product.stock` 回滚 → 说明全局事务生效。

### 9.5 常见坑

1. **版本必须对齐**：客户端 2.0.0 ↔ 服务端 2.0.0，不要单独升级其中一个。
2. **`undo_log` 必须每个写库都建**：缺表会报 `Table 'xxx.undo_log' doesn't exist`。
   已初始化过数据卷的库不会重跑建表脚本，需手动执行或 `bin\reset.bat` 重建。
3. **`SEATA_IP` 要选对**：
   - 全容器化（`docker-compose.yml`）：`SEATA_IP: seata-server`，容器内用服务名访问；
   - 服务在 IDEA 里跑（`docker-compose-infra.yml`）：`SEATA_IP: 127.0.0.1`，宿主机直连。
   如果两边混用，把 `SEATA_IP` 改成**宿主机局域网 IP**（如 `192.168.1.100`）即可同时连通。
4. **数据源代理**：本项目单数据源 + HikariCP，Seata 会自动代理；若以后上多数据源，
   需要手动包 `io.seata.rm.datasource.DataSourceProxy`。
5. **隔离级别**：AT 默认全局「读未提交」；`ProductServiceImpl#deductStock` 里的 Redisson 锁
   在分支提交前释放，学习演示无碍，生产严谨场景建议加锁上移或改用 `@GlobalLock`。
6. **TC 存储**：当前 `store.mode=file`，TC 重启会丢事务日志。想持久化改成 `db`：
   建 `seata` 库并按官方 `script/server/db/mysql.sql` 建 `global_table` / `branch_table` /
   `lock_table`，再在 `docker/seata/application.yml` 里配 `seata.store.db` 的连接信息。
