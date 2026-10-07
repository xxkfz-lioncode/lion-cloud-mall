# mall-demo × Kubernetes 练习手册

用 `mall-demo` 模块（零外部依赖、秒级启动）作为练手对象，覆盖了 K8s 最常用的核心对象与日常操作。

## 目录结构

```
mall-demo/k8s/
├── 00-namespace.yaml   # 命名空间：资源隔离
├── 10-configmap.yaml   # ConfigMap：配置与镜像解耦
├── 20-deployment.yaml  # Deployment：副本管理 / 滚动更新 / 探针 / 优雅停机
└── 21-service.yaml     # Service：稳定入口 + 负载均衡
```

## 一、准备镜像

需要先打出 jar 包并构建镜像（本地构建即可，不用推远程仓库）：

```powershell
# 1. 打包（父 pom 通过 -am 一并参与构建）
mvn -pl mall-demo -am clean package -DskipTests

# 2. 构建镜像
cd e:\xxkfz\lion-cloud-mall
docker build -t lion/mall-demo:1.0.0 ./mall-demo

# 3. 确认镜像存在
docker images lion/mall-demo
```

> Docker Desktop 的 K8s 与本机 Docker 共用同一个守护进程，
> 所以本地 `docker build` 的镜像 K8s 直接可见 —— 这就是清单里 `imagePullPolicy: IfNotPresent` 的原因。

## 二、部署

```powershell
cd e:\xxkfz\lion-cloud-mall\mall-demo
kubectl apply -f k8s/                                   # 一次性部署全部
kubectl get all -n mall-demo -o wide                    # 查看结果
kubectl rollout status deployment/mall-demo -n mall-demo
```

## 三、访问服务

**优先用 port-forward**（Docker Desktop on Windows 下 NodePort 有时不通，这是已知限制）：

```powershell
kubectl port-forward -n mall-demo svc/mall-demo 8100:8100
```

保持该窗口不要关，然后另开一个终端测试：

```powershell
curl http://localhost:8100/demo/hello
curl http://localhost:8100/demo/info
curl http://localhost:8100/actuator/health/readiness
```

NodePort 方式（若宿主机可达）：

```powershell
curl http://localhost:30081/demo/hello
```

---

## 四、练习清单

### 练习 1：观察 Deployment 的副本管理

```powershell
kubectl get pods -n mall-demo -o wide
kubectl get rs -n mall-demo                     # Deployment 实际管理的是 ReplicaSet
kubectl describe deploy mall-demo -n mall-demo  # 看 Events：创建 ReplicaSet / Pod 的全过程
```

关注：`-o wide` 里每个 Pod 的 **IP 都不一样**，但 Service 只有一个。

### 练习 2：验证 Service 负载均衡（重点）

> ⚠️ **不要用 port-forward 来验证负载均衡！**
> `kubectl port-forward svc/mall-demo ...` 只会在**建立隧道的那一刻**从后端挑一个 Pod，
> 之后整条隧道都固定连它，永远是同一个 Pod —— 这是 port-forward 的设计使然，不是配置错误。
> 它工作在 TCP 层，不理解 HTTP，无法做到"每次请求重新选后端"。

正确做法：**从集群内部的 Pod 发起请求**（容器镜像自带 curl）：

```powershell
kubectl exec -n mall-demo pod/mall-demo-77c8bc7c7d-k7849 -- bash -c `
  'for i in 1 2 3 4 5 6; do curl -s http://mall-demo:8100/demo/hello; echo; done'
```

预期现象：
- `hostname` 字段在 3 个 Pod 之间**变化** → Service 确实把请求分发到了多个副本
- `requestCountInThisPod` 各自独立累加 → 证明请求落到了不同副本

注意两点：
1. **不是严格轮询**。默认 kube-proxy 走 iptables 模式，是**随机**挑后端，
   所以连续几次命中同一个 Pod 很正常，多打几次统计才趋于均匀。
2. `mall-demo` 是 Service 短名，由 CoreDNS 解析（同 namespace 内可用）；
   跨 namespace 要写全名 `mall-demo.mall-demo.svc.cluster.local:8100`。

### 练习 3：扩缩容

```powershell
kubectl scale deployment mall-demo -n mall-demo --replicas=5
kubectl get pods -n mall-demo -w          # -w 实时观察 Pod 逐个创建
kubectl scale deployment mall-demo -n mall-demo --replicas=2
```

关键点：缩容时的**销毁顺序不确定**，K8s 不保证先删哪个 Pod —— 这正是需要优雅停机的原因。

### 练习 4：自愈能力（Pod 被删会自动重建）

```powershell
kubectl get pods -n mall-demo
kubectl delete pod <某个Pod名> -n mall-demo
kubectl get pods -n mall-demo -w          # 立刻会看到 Terminating + 新建 Pod
```

Deployment → ReplicaSet → Pod 三级结构保证了"数量恒定"。

### 练习 5：滚动更新（零停机发布）

```powershell
# 方式一：改配置触发更新（Deployment 的 template 变了就会滚动）
kubectl set env deployment/mall-demo -n mall-demo DEMO_MESSAGE="V2 版本上线"

# 实时观察：先起新 Pod → 新 Pod Ready → 再删旧 Pod
kubectl rollout status deployment/mall-demo -n mall-demo

# 方式二：重启所有副本
kubectl rollout restart deployment/mall-demo -n mall-demo
```

再次请求 `/demo/hello`，会看到 `message` 已变成新文案。
因为 `maxUnavailable: 0`，整个过程中始终有可用副本，**请求不会中断**。

### 练习 6：版本回滚

```powershell
kubectl rollout history deployment/mall-demo -n mall-demo   # 查看历史版本
kubectl rollout undo deployment/mall-demo -n mall-demo      # 回滚到上一版
kubectl rollout undo deployment/mall-demo -n mall-demo --to-revision=1
```

`revisionHistoryLimit: 10` 决定了最多能回退多少版。

### 练习 7：ConfigMap 动态改配置（理解"环境变量不会热更新"）

```powershell
kubectl edit configmap mall-demo-config -n mall-demo        # 把 DEMO_MESSAGE 改成别的值
curl http://localhost:8100/demo/hello                       # 仍然是旧值！

kubectl rollout restart deployment/mall-demo -n mall-demo   # 重启后才生效
curl http://localhost:8100/demo/hello                       # 此时才是新值
```

> 想做到真正的热更新，需要把 ConfigMap 挂载成**文件**（配合 Spring Boot 的 `@RefreshScope`），
> 挂载方式为 volume 时文件内容会自动同步（但注意 `subPath` 挂载不支持热更新）。

### 练习 8：进入容器内部排查问题

```powershell
kubectl exec -it <Pod名> -n mall-demo -- sh
# 进去后可以查看环境变量、进程等
env | grep -E "POD_NAME|POD_IP|DEMO_MESSAGE"
exit

kubectl logs <Pod名> -n mall-demo -f                        # 实时跟踪单个 Pod 日志
kubectl logs -l app=mall-demo -n mall-demo --tail=20 --prefix  # 多 Pod 日志并排对照
```

`POD_NAME` / `POD_IP` 环境变量来自 Downward API —— 这就是集群给应用的"自我介绍"。

### 练习 9：探针与优雅停机

先发起一个慢请求（会阻塞 8 秒），在它返回前删掉该 Pod：

```powershell
# 终端 A：发起慢请求
curl "http://localhost:8100/demo/slow?ms=8000"

# 终端 B（立刻执行）：删除正在处理请求的那个 Pod
kubectl delete pod <该Pod名> -n mall-demo
```

预期：**请求正常返回**，不会被中断。原因是 readinessProbe 失败后 Service 先把流量摘走，
preStop hook 又等待了 5 秒，最后才发送 SIGTERM。这就是生产环境避免发布抖动的标配。

### 练习 10：查看资源与调度信息

```powershell
kubectl describe pod <Pod名> -n mall-demo | Select-String -Pattern "Node:|IP:|Limits|Requests|Events" -Context 0,3
kubectl top pod -n mall-demo          # 若提示不可用，需先装 metrics-server
```

---

## 五、收尾清理

```powershell
# 只删除本套演示资源，保留命名空间
kubectl delete -f k8s/

# 或者连命名空间一起删掉（最干净）
kubectl delete namespace mall-demo
```

## 常见问题

| 现象 | 原因 / 处理 |
|---|---|
| `ImagePullBackOff` | 镜像没构建或名字不匹配。执行 `docker images lion/mall-demo` 确认，检查 Deployment 里 `image:` 与 `imagePullPolicy` |
| Pod 一直 `Pending` | 一般是资源不足或节点有污点。看 `kubectl describe pod` 的 Events |
| Pod `CrashLoopBackOff` | 先看 `kubectl logs`。本项目零外部依赖，正常不会有此问题 |
| NodePort 打不通 | Docker Desktop 常见限制，改用 `kubectl port-forward` |
| 访问返回 404 | 路径应为 `/demo/hello`，根路径未定义处理器 |
| 探针失败导致重启 | Actuator 端点未开启。确认已引入 `spring-boot-starter-actuator` 且 `management.endpoint.health.probes.enabled: true` |
