# Minecraft Alt Manager

一个按 Minecraft / Loader 版本分包发布的账号管理器。核心登录、账号存储和 Session 切换逻辑在 `core` 中复用；每个可安装 mod jar 都由独立版本模块构建。

English documentation is available below.

## 当前发布状态

`v0.1.1` 已验证构建的可安装 Fabric 版本：

| Loader | Minecraft | Java | 构建模块 | 发布资产 |
| --- | --- | --- | --- | --- |
| Fabric | 1.20.1 | 17+ | `fabric-1.20.1` | `minecraft-alt-manager-fabric-1.20.1-0.1.1.jar` |
| Fabric | 1.20.6 | 21+ | `fabric-1.20.6` | `minecraft-alt-manager-fabric-1.20.6-0.1.1.jar` |
| Fabric | 1.21.11 | 21+ | `fabric-1.21.11` | `minecraft-alt-manager-fabric-1.21.11-0.1.1.jar` |

Forge 模块仍在适配中，当前 Release 不包含 Forge jar。项目路线是“多版本多 jar”，不是一个 jar 覆盖所有 Minecraft / Fabric / Forge 版本。

## 功能

- 多人游戏界面右上角添加 `Accounts` 入口。
- Microsoft device code 登录：Microsoft OAuth -> Xbox Live -> XSTS -> Minecraft Services。
- Minecraft Services access token 导入登录。
- 账号列表、删除、刷新 Microsoft token、切换当前 Session。
- 中英双语 UI：`zh_cn` / `en_us`。
- 未连接世界/服务器时可切换账号；已进入世界或服务器时要求先断开连接。
- 默认账号存储为明文 JSON，适合开发和受控环境；请不要把真实 token 文件提交或分享。

## 构建

```powershell
.\gradlew clean build
```

单独构建某个版本：

```powershell
.\gradlew :fabric-1.20.1:build
.\gradlew :fabric-1.20.6:build
.\gradlew :fabric-1.21.11:build
```

构建产物位于各模块的 `build/libs/` 目录。

## 安装

1. 从 Release 下载与你的 Minecraft 版本对应的 jar。
2. 将 jar 放入该实例的 `mods` 目录。
3. 使用对应 Minecraft 版本和 Fabric Loader 启动游戏。
4. 打开多人游戏界面，点击右上角 `Accounts`。

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

## Access Token 验证

在进游戏前可以先用 core 登录链路验证 token：

```powershell
$env:ALT_MANAGER_TOKEN = "your-minecraft-services-access-token"
.\gradlew :core:verifyToken
```

成功时会输出 `LOGIN_OK` 和 profile；过期 token 会输出明确的过期时间。

## English

Minecraft Alt Manager is released as separate mod jars per Minecraft / loader version. Shared authentication, account storage, and Session switching logic lives in `core`; installable jars are built from version-specific loader modules.

## Release Status

Installable Fabric builds verified for `v0.1.1`:

| Loader | Minecraft | Java | Module | Release Asset |
| --- | --- | --- | --- | --- |
| Fabric | 1.20.1 | 17+ | `fabric-1.20.1` | `minecraft-alt-manager-fabric-1.20.1-0.1.1.jar` |
| Fabric | 1.20.6 | 21+ | `fabric-1.20.6` | `minecraft-alt-manager-fabric-1.20.6-0.1.1.jar` |
| Fabric | 1.21.11 | 21+ | `fabric-1.21.11` | `minecraft-alt-manager-fabric-1.21.11-0.1.1.jar` |

Forge modules are still being ported and are not included in the current Release. This project uses separate jars for separate version ranges; it does not promise one universal jar for every Minecraft / Fabric / Forge version.

## Features

- Adds an `Accounts` button to the Multiplayer screen.
- Microsoft device code login: Microsoft OAuth -> Xbox Live -> XSTS -> Minecraft Services.
- Minecraft Services access token import.
- Account list, delete, Microsoft token refresh, and current Session switching.
- Bilingual UI resources: `zh_cn` and `en_us`.
- Account switching is allowed before joining a world/server; disconnect first when already in-game.
- Plaintext JSON account storage is the current default. Do not commit or share real token files.

## Build

```powershell
.\gradlew clean build
```

Build one target:

```powershell
.\gradlew :fabric-1.20.1:build
.\gradlew :fabric-1.20.6:build
.\gradlew :fabric-1.21.11:build
```

Artifacts are written to each module's `build/libs/` directory.

## Install

1. Download the jar that matches your Minecraft version from Releases.
2. Put the jar into the instance `mods` directory.
3. Start the game with the matching Minecraft version and Fabric Loader.
4. Open Multiplayer and click `Accounts` in the top-right corner.

## References

- Microsoft device code flow: https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code
- Xbox Live / XSTS authentication: https://learn.microsoft.com/en-us/gaming/gdk/docs/services/fundamentals/s2s-auth-calls/service-authentication/live-website-authentication
- Fabric Meta: https://meta.fabricmc.net/
