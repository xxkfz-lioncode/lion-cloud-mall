# Lion 商城（lion-cloud-mall）

> 一个**用于学习 Spring Cloud 微服务技术栈**的最小可运行商城项目。
> 服务精简（网关 + 用户 + 商品 + 订单），但把微服务的主流知识点串进了一条真实业务线：
> **注册登录 → 浏览商品 → 下单 → 支付 / 取消 / 超时关单**。
>
> 本文以「**下单**」为主线，一次请求走完网关、注册中心、远程调用、分布式锁、分布式事务、
> 定时任务、链路追踪的全部组件。

---

## 一、下单全链路（主线）

```
浏览器  POST /api/order/create     Header: satoken=xxx
   │
   ▼
┌─────────────────────────────────────────────────────────┐
│ Gateway :8080                              ① 统一入口    │
│   · Sa-Token 校验登录态（读 Redis）                       │
│   · 鉴权通过 → 写 X-User-Id 请求头                        │
│   · 路由 /api/order/** → lb://mall-order（Nacos 找实例）   │
└─────────────────────────────────────────────────────────┘
   │
   ▼
┌─────────────────────────────────────────────────────────┐
│ mall-order :8103                           ② 下单主流程   │
│   @GlobalTransactional  → Seata 开启全局事务，生成 XID      │
│                                                          │
│   ③ Feign → mall-user        校验用户是否存在              │
│   ④ Feign → mall-product     查商品、算金额、校验库存与状态 │
│   ⑤ Feign → mall-product     扣库存（Redisson 锁防超卖）   │
│   ⑥ 写 t_order + t_order_item 落库（订单状态=待支付）       │
└─────────────────────────────────────────────────────────┘
   │
   ▼
Seata TC :8091        ⑦ 兜底：任一步失败 → 订单与库存一起回滚
SkyWalking Agent      ⑧ 旁路：全程埋点，链路/拓扑/日志上报 OAP
```

**每一步对应的技术点：**

| 步 | 做什么 | 用到的组件 |
| --- | --- | --- |
| ① | 统一鉴权、路由、负载均衡 | Gateway + Nacos Discovery + LoadBalancer |
| ② | 开启全局事务 | Seata `@GlobalTransactional`（TM） |
| ③ | 跨服务查用户 | OpenFeign + XID / 用户上下文透传 |
| ④ | 查商品算钱 | OpenFeign（商品服务是 RM） |
| ⑤ | 扣库存 | Redisson 分布式锁 + `stock >= ?` 原子 SQL |
| ⑥ | 订单落库 | MyBatis-Plus + MySQL |
| ⑦ | 一致性兜底 | Seata AT（各库 `undo_log`） |
| ⑧ | 可观测 | SkyWalking Agent（无侵入） |

> **为什么必须 Seata**：第 ⑥ 步写 `mall_order` 库、第 ⑤ 步扣 `mall_product` 库，
> 是跨服务跨库写，本地 `@Transactional` 管不到商品服务。没有分布式事务就会出现
> 「订单回滚了、库存却扣了」。

---

## 二、下单之后：订单状态流转

```
下单成功 → 待支付(0)
              ├── 用户支付   → 已支付(1)
              ├── 用户取消   → 已取消(2) + 回滚库存
              └── 30分钟未付 → 已取消(2) + 回滚库存   ← RabbitMQ 延迟消息（XXL-Job 兜底）
```

### 超时关单：延迟消息为主，定时任务兜底

下单成功即发一条 **RabbitMQ 延迟消息**（TTL = 30 分钟，用 TTL + 死信交换机实现，**无需插件**）：

```
下单成功
   └─► order.delay.exchange ─► order.delay.queue   ← 无消费者，消息在这里等 30 分钟
                                     │ TTL 到期，broker 自动投递
                                     ▼
                               order.close.exchange ─► order.close.queue ─► 关单消费者
                                                                             （关单 + 回滚库存）
```

| | XXL-Job 轮询（旧） | RabbitMQ 延迟消息（现在） |
| --- | --- | --- |
| 触发精度 | 分钟级 | 毫秒级 |
| 数据库压力 | 持续轮询扫描 | 零扫描 |

**XXL-Job 保留为兜底**：每小时扫一次，处理 MQ 消息丢失、或服务宕机期间漏掉的订单。

两条链路共用 `OrderService#closeTimeoutOrder`，靠两点保证只关一次：

- **CAS 条件更新**：`UPDATE t_order SET status=2 WHERE id=? AND status=0`，
  只有抢到更新的线程才回滚库存（防并发重复回滚）
- **手动 ack**：消费者处理成功才 `basicAck`，失败重新入队

### 下单短信通知（异步解耦）

下单主流程只往 `order.sms.queue` 丢一条消息就返回，**短信由消费者异步发送** ——
短信服务的耗时和失败都不影响下单响应时间。

消息在**事务提交后**才发送（挂 `afterCommit`），避免「事务回滚了、短信却已发出」。

---

## 三、技术栈

| 分类 | 技术 | 说明 |
| --- | --- | --- |
| 基础 | Spring Boot 3.2.10 / JDK 21 | |
| 微服务 | Spring Cloud 2023.0.1 + Alibaba 2023.0.1.0 | BOM 统一锁版本 |
| 注册/配置中心 | Nacos 2.3.2 | 服务发现 + 配置集中管理 |
| 网关 | Spring Cloud Gateway | 路由、鉴权、跨域 |
| 服务调用 | OpenFeign + LoadBalancer | 声明式调用 + 客户端负载均衡 |
| 认证 | Sa-Token + Redis | 多服务共享登录态 |
| 缓存/锁 | Redis + Redisson | 防超卖 |
| 持久层 | MyBatis-Plus + MySQL 8 | |
| 分布式事务 | Seata 2.0.0（AT） | 跨服务一致性 |
| 消息队列 | RabbitMQ 3.13 | 超时关单延迟消息 + 下单短信通知 |
| 定时任务 | XXL-Job 2.4.1 | 超时关单**兜底补偿**（每小时） |
| 链路追踪 | SkyWalking 9.7.0 + Agent | 拓扑 / 链路 / 日志 |
| 接口文档 | SpringDoc（OpenAPI 3） | |
| 前端 | Vue3 + Vite + Element Plus + Pinia | |
| 部署 | Docker + Docker Compose | 唯一编排入口 |

---

## 四、模块结构

```
lion-cloud-mall
├── mall-common        公共模块：统一响应 R、全局异常、UserContext
├── mall-api           服务间契约：OpenFeign 接口 + DTO + 降级工厂
├── mall-gateway  :8080
├── mall-user     :8101   库 mall_user
├── mall-product  :8102   库 mall_product
├── mall-order    :8103   库 mall_order     ← 下单主流程在此
├── mall-frontend :80(容器) / 5173(开发)
├── docker              MySQL 初始化脚本、Seata / SkyWalking 配置
└── docker-compose.yml  全栈编排（唯一入口）
```

Swagger：http://localhost:8101/swagger-ui.html （8102 / 8103 同理）

---

## 五、组件集成速查

| 组件 | 作用 | 集成位置 | 怎么用 / 怎么验证 |
| --- | --- | --- | --- |
| **Nacos** | 服务注册发现 + 配置中心 | 各服务 `bootstrap.yml` | 控制台 http://localhost:8848/nacos（`nacos`/`nacos`），服务列表应含 4 个微服务 |
| **Gateway** | 统一入口、鉴权、路由 | `mall-gateway/application.yml` 的 `routes` | 所有请求走 `:8080/api/**`，`StripPrefix=1` 去前缀 |
| **Sa-Token** | 登录鉴权 | 网关过滤器 + 各服务 | token 存 Redis；鉴权后写 `X-User-Id` 头，下游用 `UserContext` 读 |
| **OpenFeign** | 服务间调用 | `mall-api` 的 `feign` 包 | `FeignConfig` 拦截器透传 `satoken`、`X-User-Id`、`TX_XID` |
| **Redisson** | 防超卖 | `ProductServiceImpl#deductStock` | 分布式锁 + `update ... where stock >= ?` 原子 SQL 双保险 |
| **Seata** | 分布式事务 | `OrderServiceImpl#create` 加 `@GlobalTransactional` | 控制台 http://localhost:7091（`seata`/`seata`）；下单故意抛异常，验证订单与库存同时回滚 |
| **RabbitMQ** | 超时关单延迟消息 + 短信异步解耦 | `RabbitConfig`、`OrderMqProducer`、`OrderCloseConsumer`、`SmsNotifyConsumer` | 控制台 http://localhost:15672（`guest`/`guest`）；下单后看 `order.delay.queue` 是否有消息 |
| **XXL-Job** | 超时关单**兜底**（每小时） | `OrderTimeoutJob` + `XxlJobConfig` | 控制台 http://localhost:8082/xxl-job-admin（`admin`/`123456`） |
| **SkyWalking** | 链路追踪 | Agent 挂载，**不改业务代码** | UI http://localhost:8081；下单后看 Trace / Topology |

### 关于 XXL-Job

```
控制台   http://localhost:8082/xxl-job-admin      admin / 123456
执行器   mall-order-executor        自动注册，端口 9999（容器内网，不映射宿主机）
任务     orderTimeoutHandler        0 0/1 * * * ?（每分钟）路由策略：分片广播
令牌     default_token              admin 与 order 两边必须一致
```

执行器未上线时调度会报「执行器地址为空」—— 属正常，order 启动后 30 秒内心跳注册即可恢复。

---

## 六、快速开始

### 方式一：Docker Compose（推荐）

```bash
mvn -DskipTests clean package      # 1. 打包（Dockerfile 是 COPY jar，必须先打）
docker compose up -d --build       # 2. 构建镜像并启动全套
docker compose logs -f mall-order  # 3. 看日志
```

- 前端：http://localhost
- Nacos：http://localhost:8848/nacos（`nacos`/`nacos`）

### 方式二：本地开发（中间件容器化，服务 IDEA 里跑）

```bash
docker compose up -d mysql redis nacos seata-server xxl-job-admin skywalking-oap skywalking-ui
```

然后 IDEA 依次启动 `MallGatewayApplication` → `MallUserApplication` →
`MallProductApplication` → `MallOrderApplication`，前端 `npm run dev`。

### 中间件地址

| 组件 | 地址 | 账号 |
| --- | --- | --- |
| MySQL | `127.0.0.1:3307` | `root` / `root`（容器内 3306） |
| Redis | `127.0.0.1:6379` | 密码 `lion123` |
| Nacos | http://localhost:8848 | `nacos` / `nacos` |
| Seata | 业务 `8091` / 控制台 http://localhost:7091 | `seata` / `seata` |
| XXL-Job | http://localhost:8082/xxl-job-admin | `admin` / `123456` |
| RabbitMQ | 控制台 http://localhost:15672（AMQP `5672`） | `guest` / `guest` |
| SkyWalking | http://localhost:8081 | 无需登录 |

### 常用命令

```powershell
docker compose ps                  # 查看状态（healthy 才可用）
docker compose logs -f nacos       # 实时日志
docker compose stop / start        # 启停（保留数据）
docker compose down                # 删容器（保留数据卷）
docker compose down -v             # 彻底重置（会重新初始化数据库）
```

> Windows：`bin/` 下有封装好的脚本（`start-all.bat` / `stop.bat` / `status.bat` / `reset.bat`），可直接替代上面的 compose 命令。
> Docker Desktop 需启用 **WSL2**，内存建议 ≥ 6G（Nacos + OAP 较吃内存）。
> 端口冲突用 `netstat -ano | findstr 3307` 排查，改 compose 里宿主机端口即可。
> Nacos 的 9848/9849 必须映射 —— Java Client 2.x 走 gRPC 通信。

### 初始化数据

`docker/mysql/init/01-schema.sql` 在 MySQL **首次启动**时自动执行（建库建表 + 8 条商品）。
用户请走前端「注册」页面创建账号后登录（密码为 `MD5(密码+用户名)` 加盐存储）。

---

## 七、主要接口

| 方法 | 路径 | 说明 | 需登录 |
| --- | --- | --- | --- |
| POST | `/api/user/register` | 注册 | 否 |
| POST | `/api/user/login` | 登录，返回 token | 否 |
| GET | `/api/user/info` | 当前用户 | 是 |
| GET | `/api/product/page` | 商品分页 | 否 |
| GET | `/api/product/{id}` | 商品详情 | 否 |
| POST/PUT/DELETE | `/api/product` | 发布/修改/删除 | 是 |
| **POST** | **`/api/order/create`** | **下单（主流程）** | 是 |
| GET | `/api/order/page` | 我的订单 | 是 |
| GET | `/api/order/{id}` | 订单详情 | 是 |
| POST | `/api/order/{id}/pay` | 支付（模拟） | 是 |
| POST | `/api/order/{id}/cancel` | 取消（回滚库存） | 是 |

请求头携带：`satoken: <登录返回的 token>`

---

## 八、验证各组件是否真的生效

| 想验证 | 怎么做 | 预期 |
| --- | --- | --- |
| 服务注册 | Nacos 控制台 → 服务列表 | 4 个微服务 + `seata-server` |
| 登录态共享 | 登录后调 `/api/order/create` | 网关鉴权通过，order 能取到 userId |
| 分布式事务 | 在 `create` 末尾临时 `throw new RuntimeException()` | `t_order` 不新增 **且** `t_product.stock` 回滚 |
| 防超卖 | 并发下单同一商品 | 库存不会为负，多余请求报「库存不足」 |
| 超时关单（MQ） | 下单后看 RabbitMQ 控制台 `order.delay.queue` | 出现 1 条消息，到期自动流转到 `order.close.queue` 被消费，订单关闭 + 库存回滚 |
| 短信通知（MQ） | 下单后看 `mall-order` 日志 | 出现「【短信通知】发送至 ...」 |
| 链路追踪 | 下单后打开 http://localhost:8081 | Trace 显示 `gateway → order → user/product` 每一跳耗时 |
| 业务日志 | SkyWalking UI → Log → 选 `mall-order` | 能看到「下单成功」等日志（含 traceId） |

> SkyWalking Agent 需先下载：`powershell -ExecutionPolicy Bypass -File .\docker\skywalking\download-agent.ps1`
> 容器化已默认挂载；IDEA 裸跑需在 VM options 加 `-javaagent:...\skywalking-agent.jar`。
> Dockerfile 做了保护：没下载 agent 也不会启动失败。

---

## 九、常见坑

**Seata**
1. 客户端与服务端版本必须都是 2.0.0（由 Alibaba BOM 锁定，pom 里不要写版本）
2. **每个写库都要建 `undo_log`**，缺表报 `Table 'xxx.undo_log' doesn't exist`
3. `SEATA_IP` 是 TC 注册到 Nacos 的**对外地址**：全容器化填服务名 `seata-server`；
   服务在宿主机跑则填 `127.0.0.1`。填错症状：日志刷 `can not connect to [172.x.x.x:8091]`
4. `store.mode=file` 时 TC 重启会丢事务日志，想持久化改 `db`

**Nacos / 数据库**
5. 已初始化过的数据卷**不会重跑**建表脚本，改了 SQL 需手动执行或 `docker compose down -v`
6. MySQL 8 用 `caching_sha2_password`，连接串需带 `allowPublicKeyRetrieval=true`
7. Nacos 容器内访问 MySQL 用端口 **3306**（不是宿主机的 3307）

**XXL-Job**
8. admin 的 `accessToken` 与 order 的 `xxl.job.access-token` 必须一致（本项目 `default_token`）
9. 执行器端口 9999 **不要**映射宿主机 —— admin 与 order 同在容器网络，走宿主机反而连不上
10. 重新部署 order 必须 `docker compose up -d --build mall-order`，
    只 `up -d` 会复用旧镜像，代码不生效

**RabbitMQ**
11. 队列的 TTL / 死信配置**只在首次创建时生效** —— 改了 `order.timeout-minutes` 后，
    需在控制台删掉 `order.delay.queue`（或删 RabbitMQ 数据卷）让队列重建
12. 消费者是手动 ack：处理失败会重新入队；订单号这类坏消息直接丢弃，避免无限循环
13. 消息挂在事务 `afterCommit` 之后发送，所以下单事务回滚时不会误发短信

---

## 十、可继续扩展的点

- Sentinel 限流熔断（网关或 Feign 层）
- 商品列表加 Redis 缓存 + 缓存穿透/击穿防护
- 短信通知接入真实服务商（阿里云 / 腾讯云 SDK），替换 `SmsNotifyConsumer` 里的日志模拟
- 订单分库分表、读写分离
