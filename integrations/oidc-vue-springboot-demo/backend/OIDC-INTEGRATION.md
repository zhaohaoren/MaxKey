# Spring Boot 对接 MaxKey OIDC

本文以当前 `oidc-vue-springboot-demo/backend` 的实际代码为准，说明配置项、登录流程、MaxKey 管理端配置以及生产部署注意事项。

## 1. 先理解双方角色

- MaxKey 是 OpenID Provider（OP），也就是身份提供方（IdP），负责登录、签发 Token 和发布验签公钥。
- Spring Boot Demo 是 Relying Party（RP），在 MaxKey 管理页面中也称 Service Provider（SP），负责发起登录、接收回调、验证身份并建立本地 Session。
- Vue 只负责展示和调用 Spring Boot，不保存 `client_secret`，也不直接交换 Token。

## 2. 最少需要哪些配置

你的理解“配置 id、secret、redirect、scope”基本正确，但客户端还必须知道连接哪个 OIDC Provider。

对于完全符合 Discovery 规范的 OIDC Provider，通常配置以下五类信息即可：

1. `client-id`：应用在 Provider 中的公开编号。
2. `client-secret`：后端应用凭证，不能暴露给浏览器。
3. `redirect-uri`：登录后回调地址，必须与 Provider 登记值一致。
4. `scope`：至少包含 `openid`，按需加入 `profile`、`email`。
5. `issuer-uri`：Provider 标识；Spring 根据它自动发现其他端点。

标准 Spring Boot 配置通常可以写成：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          maxkey:
            client-id: ${MAXKEY_CLIENT_ID}
            client-secret: ${MAXKEY_CLIENT_SECRET}
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,email
        provider:
          maxkey:
            issuer-uri: https://sso.example.com/sign/maxkey
```

当前 Demo 没有只配 `issuer-uri`，原因是 MaxKey 的 Discovery 地址与 `issuer` 默认推导路径不完全一致，所以在 `SecurityConfiguration` 中手工提供了 Provider 端点。

## 3. 当前 application.yml 配置说明

### 3.1 Client 注册信息

| 配置 | 用途 | 是否属于核心 OIDC 配置 |
| --- | --- | --- |
| `client-id` | 标识 DemoTest 应用，也是 `id_token.aud` 的预期值 | 是 |
| `client-secret` | 后端换 Token 时证明应用身份 | 机密客户端需要 |
| `client-authentication-method` | 指定 secret 放在 POST 表单还是 Basic Header | OAuth2 Client 认证方式 |
| `authorization-grant-type` | 本项目固定为授权码模式 `authorization_code` | 是 |
| `redirect-uri` | MaxKey 登录后的回调地址 | 是 |
| `scope` | 申请 OIDC 与用户资料权限 | 是 |

`openid` 是关键开关。没有它只是 OAuth 2.0 授权，不会获得表示用户身份的 `id_token`。

### 3.2 Provider 端点

| 配置 | 谁调用 | 作用 |
| --- | --- | --- |
| `issuer` | Spring Boot | 校验 `id_token.iss`，确认 Token 的签发者 |
| `authorization-uri` | 浏览器 | 跳转 MaxKey 登录并取得一次性 code |
| `token-uri` | Spring Boot | 使用 code、client id、secret 换取 Token |
| `user-info-uri` | Spring Boot | 使用 access token 获取更多用户 Claims |
| `jwk-set-uri` | Spring Boot | 下载 RSA 公钥并验证 id token 签名 |

这些地址不是每个业务应用都应该手工填写的“业务参数”，而是 OIDC Provider 元数据。标准情况下由 Discovery 自动提供；这里只是为兼容当前 MaxKey 而显式配置。

### 3.3 Demo 自身配置

- `server.port`：Spring Boot 本地端口，当前为 `8088`。
- `server.forward-headers-strategy`：在反向代理后恢复外部 URL，避免生成错误回调地址。
- `demo.frontend-url`：认证成功或注销后返回的 Vue 首页。
- `logging.level`：只控制日志详细程度，不影响协议。

## 4. 完整登录流程

```text
Vue                              Spring Boot                         MaxKey
 |                                   |                                |
 | GET /oauth2/authorization/maxkey  |                                |
 |---------------------------------->|                                |
 |                                   | 保存 state、nonce、原始请求       |
 |       302 authorization-uri       |                                |
 |<----------------------------------|                                |
 |------------------------------------------------------------------->|
 |                                   |                    用户登录/SSO |
 |       redirect-uri?code&state                                      |
 |<-------------------------------------------------------------------|
 |---------------------------------->| 校验 state                      |
 |                                   | POST token-uri(code + secret)  |
 |                                   |------------------------------->|
 |                                   | access_token + id_token         |
 |                                   |<-------------------------------|
 |                                   | GET jwk-set-uri，验证 ID Token   |
 |                                   | GET user-info-uri，补充 Claims   |
 |                                   | 建立服务端 Session               |
 |             302 Vue 首页          |                                |
 |<----------------------------------|                                |
 | GET /api/me（携带 Session Cookie） |                                |
 |---------------------------------->|                                |
 | 当前用户与 Claims                  |                                |
 |<----------------------------------|                                |
```

### state 与 nonce 的作用

- `state` 把回调与最初的登录请求关联起来，并防止登录 CSRF。
- `nonce` 被写入认证请求和 `id_token`，防止旧 ID Token 被重放。
- 这两个值由 Spring Security 自动生成和验证，业务代码不需要自行实现。

这也是 MaxKey 门户中的 DemoTest 必须配置为 `引导=SP` 的原因：先进入 `/oauth2/authorization/maxkey`，Spring 才能创建 `state/nonce`。如果 MaxKey 直接把 code 推到回调地址，Spring 没有原始请求可匹配，就会进入 `/login?error`。

## 5. 代码分别负责什么

### SecurityConfiguration

`SecurityConfiguration.java` 有两部分职责：

1. 构造 `ClientRegistration`：把 Client 信息和 Provider 端点交给 Spring Security。
2. 构造 `SecurityFilterChain`：启用 `oauth2Login`、保护 `/api/me`、配置成功跳转和本地注销。

Spring Security 自动提供两个重要地址：

- `/oauth2/authorization/maxkey`：登录发起入口。
- `/login/oauth2/code/maxkey`：授权码回调处理入口。

这两个地址都不需要项目自己编写 Controller。

### MaxKeyTokenResponseClient

标准 Token 响应字段是：

```json
{
  "access_token": "...",
  "token_type": "Bearer",
  "expires_in": 300,
  "id_token": "..."
}
```

当前 MaxKey 还可能返回 Java Bean 风格字段：

```json
{
  "value": "...",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "additionalInformation": {
    "id_token": "..."
  }
}
```

`MaxKeyTokenResponseClient` 是兼容层，负责：

- 提交 `grant_type/code/redirect_uri/client_id/client_secret`；
- 同时识别标准字段与 MaxKey camelCase 字段；
- 展开 `additionalInformation`，让 Spring 能取得 `id_token`；
- 将结果重新组装为 Spring Security 标准的 `OAuth2AccessTokenResponse`。

如果未来 MaxKey Token 端点完全返回标准 OAuth2 JSON，这个自定义类就可以删除，改回 Spring Security 默认 Token Client。

### SessionController

`/api/me` 从 Spring Security Session 中读取已经验证的 `OidcUser`，返回 `sub` 和 Claims 给 Vue。它不负责验证 Token，验证工作在进入 Controller 前已经完成。

## 6. MaxKey 管理端如何填写

编辑 DemoTest：

### 基本信息

- 协议：`OpenID Connect v1.0`
- 登录地址：`http://localhost:5173/oauth2/authorization/maxkey`
- 状态：启用

### OIDC/OAuth 配置

- Client ID：与后端 `MAXKEY_CLIENT_ID` 相同
- Client Secret：与后端 `MAXKEY_CLIENT_SECRET` 相同
- 授权类型：`authorization_code`
- 回调地址：`http://localhost:5173/login/oauth2/code/maxkey`
- Scope：`openid,profile,email`
- Signature：`RS256`，并生成应用签名密钥
- Subject：`username`
- Issuer：`http://localhost:9527/sign/maxkey`

### 扩展信息

- 引导：`SP`

注意区分：

- 登录地址是“开始登录”的地址：`/oauth2/authorization/maxkey`。
- 回调地址是“完成 MaxKey 登录后返回”的地址：`/login/oauth2/code/maxkey`。

## 7. 为什么回调写 5173，实际却由 8088 处理

开发环境的 `frontend/vite.config.js` 将这些路径代理到 Spring Boot：

- `/oauth2` -> `http://localhost:8088`
- `/login` -> `http://localhost:8088`
- `/api` -> `http://localhost:8088`
- `/logout` -> `http://localhost:8088`

所以浏览器看到 `http://localhost:5173/login/oauth2/code/maxkey`，真正处理回调的是 8088 上的 Spring Security。

生产环境建议由统一域名的反向代理完成同样路由，或者直接把回调配置为公开的后端地址，例如：

```text
https://api.example.com/login/oauth2/code/maxkey
```

## 8. 环境变量与启动

不要把生产 secret 写进配置文件。建议这样启动：

```bash
export MAXKEY_CLIENT_ID='你的-client-id'
export MAXKEY_CLIENT_SECRET='你的-client-secret'
export MAXKEY_REDIRECT_URI='https://app.example.com/login/oauth2/code/maxkey'
export MAXKEY_ISSUER='https://sso.example.com/sign/maxkey'
export MAXKEY_AUTHORIZATION_URI='https://sso.example.com/sign/authz/oauth/v20/authorize'
export MAXKEY_TOKEN_URI='https://sso.example.com/sign/authz/oauth/v20/token'
export MAXKEY_USER_INFO_URI='https://sso.example.com/sign/api/connect/v10/userinfo'
export MAXKEY_JWK_SET_URI='https://sso.example.com/sign/authz/oauth/v20/jwks'
export DEMO_FRONTEND_URL='https://app.example.com'
mvn spring-boot:run
```

## 9. 常见错误

### `/login?error`

常见原因是直接访问了回调地址，或 MaxKey 使用 IDP 引导直接回调，导致 Spring 找不到之前保存的 `state/nonce`。应从 `/oauth2/authorization/maxkey` 开始登录，并将门户“引导”设为 `SP`。

### `invalid_redirect_uri` 或 redirect mismatch

请求里的 `redirect_uri` 与 MaxKey 登记值不完全相同。协议、域名、端口、路径、末尾斜杠都要逐字一致。

### `invalid_id_token` / `Malformed Jwk set`

检查：

- JWKS 地址能否返回 `{"keys":[...]}`；
- Token Header 的 `kid` 能否在 JWKS 中找到；
- MaxKey 应用是否生成并保存了 RS256 签名密钥；
- `issuer` 是否与 `id_token.iss` 完全一致。

### Token 响应解析失败

检查 MaxKey Token JSON。如果返回 `value/tokenType/expiresIn/additionalInformation`，必须保留 `MaxKeyTokenResponseClient` 兼容层。

## 10. 安全注意事项

- `client-secret` 只保存在后端或 Secret Manager 中。
- 生产环境必须使用 HTTPS。
- 不要关闭 `state`、`nonce`、issuer、audience 或签名验证。
- 回调地址应使用精确白名单，不要使用任意通配符。
- 当前 Demo 为方便演示允许 GET 注销；生产项目建议使用带 CSRF 防护的 POST 注销。
- DEBUG 日志只用于排障，避免长期记录 Token 或认证上下文。
