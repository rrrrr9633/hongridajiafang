# 红日大家纺商城

当前仓库采用单仓库结构，第一阶段只拆分真正需要独立部署的边界：

- `backend/gateway`：统一入口、跨域与路由，不承载订单业务。
- `backend/order-service`：订单生命周期、支付单幂等与支付回调处理。
- `miniapp`：原生微信小程序，只通过网关访问后端。
- `ops`：MySQL 初始化脚本与 Redis 本地/云端基础配置。
- `deploy`：Nginx、systemd 等 Linux 部署产物。

## 环境变量

复制 `.env.example` 为本地环境文件，或在启动进程前导入同名环境变量。真实 `.env` 不提交到仓库。

生产域名由部署环境单独配置，禁止把中文域名直接写进程序。`小漾sama.top` 的标准 Punycode 是 `xn--sama-px9gg69g.top`；本地开发环境不依赖生产域名，统一使用 `http://127.0.0.1:8080`。域名证书、微信 AppID/Secret、数据库密码、Redis 密码和支付证书只通过环境变量注入。

## 订单与支付状态

```text
订单：CREATED -> PENDING_PAYMENT -> PAID -> PROCESSING -> COMPLETED
                         |                 |
                         v                 v
                      CANCELLED         REFUNDING -> REFUNDED

支付单：INITIATED -> PAYING -> SUCCESS
                         |          |
                         v          v
                      CLOSED      REFUNDING -> REFUNDED
```

状态迁移必须由服务端校验；微信支付回调以 `transaction_id` 幂等处理，Redis 只保存短期幂等键、支付锁和会话数据，MySQL 是订单与支付事实的最终来源。

## 本地启动

1. 导入本地环境变量。
2. 启动 MySQL 与 Redis。
3. 在 `backend` 执行 `mvn spring-boot:run` 分别启动服务。
4. 用微信开发者工具打开 `miniapp`，把开发环境请求地址指向本地网关。

启动前请先补充微信小程序 AppID、支付商户配置与实际数据库凭据；仓库仅提供可运行的结构和安全配置边界，不放置任何真实密钥。


网关

cd /Users/chenhaoran/Desktop/product/haode
set -a && source .env && set +a
mvn -f backend/gateway/pom.xml -Dmaven.repo.local=.runtime/m2 spring-boot:run

订单

cd /Users/chenhaoran/Desktop/product/haode
set -a && source .env && set +a
mvn -f backend/order-service/pom.xml -Dmaven.repo.local=.runtime/m2 spring-boot:run