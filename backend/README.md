# 后端本地启动说明

本文说明如何从项目根目录启动订单服务和 API 网关。订单服务监听 `8081`，网关监听 `8080`，小程序和管理端通过网关访问后端。

## 启动前准备

- 已安装与项目兼容的 JDK 和 Maven。
- MySQL、Redis 已启动并可连接。
- 项目根目录存在本地 `.env`，至少包含数据库、Redis 连接配置。
- `.env` 中的敏感项只用于本地开发；不要提交 `.env` 或将本地配置用于生产。

数据库迁移由订单服务启动时的 Flyway 自动执行。数据库表结构不兼容、迁移校验失败时，订单服务会启动失败；应先检查启动日志和迁移历史，不要直接删除迁移记录或数据库数据。

## 启动订单服务

在一个终端窗口执行：

```bash
cd "/Users/chenhaoran/Desktop/product/haode"
set -a
source .env
set +a
mvn -f backend/order-service/pom.xml \
  -Dmaven.repo.local=.runtime/m2 \
  spring-boot:run
```

订单服务启动后监听 `http://127.0.0.1:8081`。首次启动或存在新迁移时，等待 Flyway 迁移成功及 Spring Boot 显示 `Started OrderServiceApplication`。

## 启动 API 网关

保持订单服务运行，在另一个终端窗口执行：

```bash
cd "/Users/chenhaoran/Desktop/product/haode"
set -a
source .env
set +a
mvn -f backend/gateway/pom.xml \
  -Dmaven.repo.local=.runtime/m2 \
  spring-boot:run
```

网关启动后监听 `http://127.0.0.1:8080`，并将 `/api/**` 中已配置的路由转发到订单服务 `8081`。

## 检查服务

```bash
lsof -nP -iTCP:8081 -sTCP:LISTEN
lsof -nP -iTCP:8080 -sTCP:LISTEN
curl -i http://127.0.0.1:8081/api/products
curl -i http://127.0.0.1:8080/api/products
curl -i http://127.0.0.1:8080/actuator/health
```

`/api/products` 为公开读取接口。用户资料、订单、评价、售后等接口需要有效的用户会话令牌；收到 `401` 不代表服务未启动。

## 常见问题

- **端口已占用**：检查 `8080`、`8081` 的监听进程，先正常停止已运行的对应服务，再启动新实例。
- **网关返回 404**：确认订单服务已启动，并检查网关路由配置是否包含对应 API 路径。
- **订单服务启动失败**：先核对 MySQL/Redis 连通性、`.env` 配置及 Flyway 校验/迁移日志。
- **启动后配置变更未生效**：停止并重新启动对应服务；网关配置变更需重启网关。

开发环境中，微信开发者工具可按需启用“不校验合法域名、web-view、TLS 版本以及 HTTPS 证书”。真机不能访问 `127.0.0.1`，需使用设备可访问的局域网地址或合法 HTTPS 地址。
