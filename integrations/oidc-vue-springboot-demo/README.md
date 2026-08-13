# MaxKey OIDC Vue + Spring Boot Demo

这个示例用于验证 MaxKey 的 OpenID Connect 登录流程：Vue 发起登录，Spring Boot 完成授权码回调、`id_token` 校验、UserInfo 获取和服务端 Session 保存。

后端 OIDC 的配置项、完整时序、代码职责、MaxKey 页面填写方法和常见错误，请参阅：

- [`backend/OIDC-INTEGRATION.md`](backend/OIDC-INTEGRATION.md)

## 1. 在 MaxKey 创建应用

在统一服务的管理端新增应用：

- 协议：`OpenID Connect v1.0`
- 回调地址：`http://localhost:5173/login/oauth2/code/maxkey`
- 授权类型：`authorization_code`
- Scope：`openid`、`profile`、`email`
- 许可确认：`auto`
- PKCE：`no`
- Subject：`username`
- Issuer：`http://localhost:9527/sign/maxkey`
- Signature：`RS256`，点击生成密钥
- UserInfo Response：`NORMAL`
- 登录地址：`http://localhost:5173/oauth2/authorization/maxkey`
- 扩展信息中的“引导”：`SP`
- 应用状态：启用

保存后记录 `client_id` 和 `client_secret`，并给测试用户授权访问该应用。

当前 MaxKey 统一服务默认地址是 `http://localhost:9527/sign`。如果你的端口、上下文路径或域名不同，需要同步修改 `backend/src/main/resources/application.yml` 和 MaxKey 应用配置。

## 2. 启动后端

PowerShell：

```powershell
$env:MAXKEY_CLIENT_ID = "这里填写 MaxKey 的 client_id"
$env:MAXKEY_CLIENT_SECRET = "这里填写 MaxKey 的 client_secret"
mvn spring-boot:run
```

命令需要在 `integrations/oidc-vue-springboot-demo/backend` 目录执行。

也可以直接修改 `application.yml` 中的默认值，但不建议把真实密钥提交到 Git。

## 3. 启动前端

新开 PowerShell：

```powershell
npm install
npm run dev
```

命令需要在 `integrations/oidc-vue-springboot-demo/frontend` 目录执行，然后访问：

```text
http://localhost:5173
```

## 4. 预期流程

1. Vue 请求 Spring Boot 的 `/api/me`，未登录时显示未登录。
2. 点击“使用 MaxKey 登录”。
3. 浏览器跳转 MaxKey 登录页。
4. MaxKey 回调 Spring Boot `/login/oauth2/code/maxkey`。
5. Spring Boot 使用授权码换取 Token，并校验 RS256 签名的 `id_token`。
6. Spring Boot 建立 Session，跳回 Vue。
7. Vue 再次请求 `/api/me`，页面显示 OIDC Claims。

## 5. 重要说明

- `client_secret` 只放在 Spring Boot 环境变量中，不放在 Vue 代码中。
- 这个 demo 使用授权码模式，适合有后端的 Vue 系统。
- 当前示例将 PKCE 设为 `no`，用于先验证基础 OIDC 流程；正式项目可在 MaxKey 应用中启用 PKCE，并在客户端同步配置。
- MaxKey 的 OIDC Discovery 地址是 `http://localhost:9527/sign/authz/oauth/v20/.well-known/openid-configuration`，但其 `issuer` 为 `http://localhost:9527/sign/maxkey`，因此后端采用手动端点配置。
