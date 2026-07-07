# Minecraft Alt Manager

Fabric / Forge 分段构建的 Minecraft 账号管理器。当前仓库已经拆成 Gradle 多模块：

- `core`：加载器无关的账号、登录、存储和 Session 切换核心，按 Java 8 编译。
- `fabric-1.21.11`：Fabric 1.21.11 客户端适配，按 Java 21 编译。

## 当前能力

- Microsoft device code 登录链路：Microsoft OAuth -> Xbox Live -> XSTS -> Minecraft Services。
- Minecraft Services access token 导入登录。
- JSON 账号列表存储，第一阶段为明文 token，适合开发和受控环境。
- 多人游戏界面 `Accounts` 按钮。
- 游戏未连接世界/服务器时，在账号界面切换当前 Session。
- 已进服时不会热切账号，会提示先断开连接。

## 构建

```powershell
.\gradlew :core:test
.\gradlew :fabric-1.21.11:build
```

## Access token 验证

在进游戏前可以先用同一套 core 登录链路验证 token 是否可用：

```powershell
$env:ALT_MANAGER_TOKEN = "your-minecraft-services-access-token"
.\gradlew :core:verifyToken
```

成功时会输出 `LOGIN_OK` 和 profile；过期 token 会输出明确的过期时间。

产物位于：

```text
fabric-1.21.11/build/libs/
```

## Microsoft 登录配置

首次启动 Fabric 版本后会生成：

```text
config/minecraft-alt-manager/config.json
```

填入你自己的 Microsoft Entra / Azure 公开客户端 ID：

```json
{
  "microsoftClientId": "your-public-client-id"
}
```

不要把 `client_secret` 放进客户端 mod。没有配置 `microsoftClientId` 时，access token 导入和账号列表仍可用，Microsoft 登录按钮会提示先配置。

## Fabric 1.21.11 使用

1. 把 `minecraft-alt-manager-fabric-1.21.11-0.1.0-SNAPSHOT.jar` 放进 `mods`。
2. 使用 Fabric Loader `0.18.1+` 启动 Minecraft `1.21.11`。
3. 进入多人游戏界面，点击右上角 `Accounts`。
4. 使用 `Microsoft` 或 `Import Token` 添加账号。
5. 在未连接服务器/世界时点击 `Use` / `Use Selected Account` 切换账号。

## 全版本路线

不会做单 jar 覆盖所有 Fabric/Forge/MC 版本。后续按版本段继续增加模块：

- `fabric-1.20.1`
- `fabric-1.20.6`
- `forge-1.20.x`
- `forge-1.12.2`
- `forge-1.8.9`

每个模块独立声明 Minecraft、Loader、Java 和映射版本，复用 `core`。

## 参考

- Microsoft device code flow: https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code
- Xbox Live / XSTS authentication: https://learn.microsoft.com/en-us/gaming/gdk/docs/services/fundamentals/s2s-auth-calls/service-authentication/live-website-authentication
- Fabric 1.21.11 announcement: https://fabricmc.net/2025/12/05/12111.html
- Fabric Meta: https://meta.fabricmc.net/
