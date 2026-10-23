# 红日大家纺商城

当前仓库采用单仓库结构，第一阶段只拆分真正需要独立部署的边界：

- `backend/gateway`：统一入口、跨域与路由，不承载订单业务。
- `backend/order-service`：订单生命周期、支付单幂等与支付回调处理。
- `miniapp`：原生微信小程序，只通过网关访问后端。
- `ops`：MySQL 初始化脚本与 Redis 本地/云端基础配置。
- `deploy`：Nginx、systemd 等 Linux 部署产物。

## 环境变量

复制 `.env.example` 为本地环境文件，或在启动进程前导入同名环境变量。真实 `.env` 不提交到仓库。

生产域名是 `hongridajiafang.shop`，公网 IP 是 `211.101.233.24`。小程序只连 `https://hongridajiafang.shop`，不要写裸 IP。本地开发不依赖生产域名，统一使用 `http://127.0.0.1:8080`。域名证书、微信 AppID/Secret、数据库密码、Redis 密码和支付证书只通过环境变量注入。

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


## 云端部署
  本次需要的完整部署指令已保存为部署脚本 (.runtime/redeploy-20261023.sh)，包含迁
  移兼容检查、备份、替换 JAR、按顺序启动、健康检查和失败自动恢复旧 JAR。以后在这
  台服务器上按下面顺序执行即可；以 root 执行。

  # 1. 进入项目，拉取更新（你本次已经执行过）
  cd /root/hongridajiafang
  git pull --ff-only

  # 2. 编译、运行测试、打包两个后端服务
  # -o 使用本机缓存；如果更新引入了未缓存依赖，去掉 -o
  mvn -o -f backend/pom.xml \
    -Dmaven.repo.local=/root/.m2/repository \
    clean verify

  # 3. 执行部署：先备份，再替换和重启，失败时恢复旧 JAR
  bash .runtime/redeploy-20261023.sh

  查看脚本中的全部底层指令：

  cat /root/hongridajiafang/.runtime/redeploy-20261023.sh

  部署后检查和排查日志的指令：

  # 服务运行状态和开机启动状态
  systemctl status haode-order-service haode-gateway --no-pager
  systemctl is-enabled haode-order-service haode-gateway

  # 本机健康检查
  curl --noproxy '*' -fsS http://127.0.0.1:8081/actuator/health
  curl --noproxy '*' -fsS http://127.0.0.1:8080/actuator/health

  # 公网 HTTPS 接口检查
  curl --noproxy '*' -i https://hongridajiafang.shop/api/products

  # 最近的服务日志
  journalctl -u haode-order-service -n 100 --no-pager
  journalctl -u haode-gateway -n 100 --no-pager

  # 持续跟踪两个服务日志，Ctrl+C 退出
  journalctl -u haode-order-service -u haode-gateway -f

  # 本次构建和部署记录
  cat .runtime/build-20261023.log
  cat .runtime/deploy-20261023.log

  # 最近一次部署备份位置
  cat .runtime/last-backend-backup-path

  现有 /etc/haode.env 由 systemd 自动加载，部署时不需要手动 source。本次已验证服
  务和接口可用；真实微信支付状态同步仍需用实际订单验证。