# Minecraft Alt Manager

一个按 Minecraft / Loader 版本分包发布的账号管理器。核心登录、账号存储和 Session 切换逻辑在 `core` 中复用；每个可安装 mod jar 由对应版本模块独立构建。

English documentation is available below.

## 当前发布状态

`v0.1.3` 已验证构建以下可安装版本：

| Loader | Minecraft | Java | 构建模块 | 发布资产 |
| --- | --- | --- | --- | --- |
| Fabric | 1.20.1 | 17+ | `fabric-1.20.1` | `minecraft-alt-manager-fabric-1.20.1-0.1.3.jar` |
| Fabric | 1.20.6 | 21+ | `fabric-1.20.6` | `minecraft-alt-manager-fabric-1.20.6-0.1.3.jar` |
| Fabric | 1.21.11 | 21+ | `fabric-1.21.11` | `minecraft-alt-manager-fabric-1.21.11-0.1.3.jar` |
| Forge | 1.20.1 | 17+ | `forge-1.20.1` | `minecraft-alt-manager-forge-1.20.1-0.1.3.jar` |
| Forge | 1.12.2 | 8 | `forge-1.12.2` | `minecraft-alt-manager-forge-1.12.2-0.1.3.jar` |
| Forge | 1.8.9 | 8 | `forge-1.8.9` | `minecraft-alt-manager-forge-1.8.9-0.1.3.jar` |

项目采用“多版本多 jar”交付方式，不承诺一个 jar 同时覆盖所有 Minecraft / Fabric / Forge 版本。

## 功能

- 在多人游戏界面添加 `Accounts` 入口，不占用主菜单。
- Microsoft device code 登录：Microsoft OAuth -> Xbox Live -> XSTS -> Minecraft Services。
- Minecraft Services access token 导入登录。
- 账号列表、当前账号标记、删除账号、刷新 Microsoft token、切换当前 Session。
- 中英双语 UI 资源。
- 未进入世界或服务器时可切换账号；已连接时会要求先断开连接。
- 默认账号存储为明文 JSON，适合开发和受控环境。请不要提交或分享真实 token 文件。

## 构建

Fabric 与 core：

```powershell
.\gradlew clean build --no-daemon
```

Forge 独立子构建：

```powershell
cd forge-1.20.1
.\gradlew clean build --no-daemon

cd ..\forge-1.12.2
.\gradlew clean build --no-daemon

cd ..\forge-1.8.9
.\gradlew clean build --no-daemon
```

构建产物位于各模块的 `build/libs/` 目录。Forge 旧版本需要 Java 8；Forge 1.20.1 和 Fabric 1.20+ 需要对应的现代 Java。

## 安装

1. 从 Release 下载与你的 Minecraft 版本和 Loader 对应的 jar。
2. 将 jar 放入该实例的 `mods` 目录。
3. 使用对应 Minecraft、Fabric Loader 或 Forge 版本启动游戏。
4. 打开多人游戏界面，点击 `Accounts`。

## Microsoft 登录配置

首次启动后会生成：

```text
config/minecraft-alt-manager/config.json
```

mod 已内置默认 Microsoft public client id：

```json
{
  "microsoftClientId": "e3c9f9be-7cde-49c9-887a-20cc3f3fa10c"
}
```

如果你想使用自己的 Microsoft Entra / Azure 应用，可以在这里改成你自己的公开客户端 ID。不要把 `client_secret` 放进客户端 mod。

## Access Token 验证

进游戏前可以先用 core 验证 token：

```powershell
$env:ALT_MANAGER_TOKEN = "your-minecraft-services-access-token"
.\gradlew :core:verifyToken
```

成功时会输出 `LOGIN_OK` 和 profile；过期 token 会输出明确的过期时间。

## English

Minecraft Alt Manager is released as separate mod jars per Minecraft / loader version. Shared authentication, account storage, and Session switching logic lives in `core`; installable jars are built by version-specific loader modules.

## Release Status

`v0.1.3` has verified installable builds for:

| Loader | Minecraft | Java | Module | Release Asset |
| --- | --- | --- | --- | --- |
| Fabric | 1.20.1 | 17+ | `fabric-1.20.1` | `minecraft-alt-manager-fabric-1.20.1-0.1.3.jar` |
| Fabric | 1.20.6 | 21+ | `fabric-1.20.6` | `minecraft-alt-manager-fabric-1.20.6-0.1.3.jar` |
| Fabric | 1.21.11 | 21+ | `fabric-1.21.11` | `minecraft-alt-manager-fabric-1.21.11-0.1.3.jar` |
| Forge | 1.20.1 | 17+ | `forge-1.20.1` | `minecraft-alt-manager-forge-1.20.1-0.1.3.jar` |
| Forge | 1.12.2 | 8 | `forge-1.12.2` | `minecraft-alt-manager-forge-1.12.2-0.1.3.jar` |
| Forge | 1.8.9 | 8 | `forge-1.8.9` | `minecraft-alt-manager-forge-1.8.9-0.1.3.jar` |

The project ships multiple jars for different version ranges. It does not promise one universal jar for every Minecraft / Fabric / Forge version.

## Features

- Adds an `Accounts` entry to the Multiplayer screen instead of the title screen.
- Microsoft device code login: Microsoft OAuth -> Xbox Live -> XSTS -> Minecraft Services.
- Minecraft Services access token import.
- Account list, current-account marker, delete, Microsoft token refresh, and current Session switching.
- English and Simplified Chinese UI resources.
- Account switching is allowed before joining a world/server; disconnect first when already connected.
- Plaintext JSON account storage is the current default. Do not commit or share real token files.

## Build

Fabric and core:

```powershell
.\gradlew clean build --no-daemon
```

Forge sub-builds:

```powershell
cd forge-1.20.1
.\gradlew clean build --no-daemon

cd ..\forge-1.12.2
.\gradlew clean build --no-daemon

cd ..\forge-1.8.9
.\gradlew clean build --no-daemon
```

Artifacts are written to each module's `build/libs/` directory. Legacy Forge targets require Java 8; Forge 1.20.1 and Fabric 1.20+ require the matching modern Java runtime.

## Install

1. Download the jar matching your Minecraft version and loader from Releases.
2. Put the jar into the instance `mods` directory.
3. Start the game with the matching Minecraft, Fabric Loader, or Forge version.
4. Open Multiplayer and click `Accounts`.

## Microsoft Client ID

The mod ships with a default Microsoft public client id:

```json
{
  "microsoftClientId": "e3c9f9be-7cde-49c9-887a-20cc3f3fa10c"
}
```

You can still override it in `config/minecraft-alt-manager/config.json` with your own Microsoft Entra / Azure public client id. Do not put a `client_secret` in a client-side mod.

## References

- Microsoft device code flow: https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code
- Xbox Live / XSTS authentication: https://learn.microsoft.com/en-us/gaming/gdk/docs/services/fundamentals/s2s-auth-calls/service-authentication/live-website-authentication
- Fabric Meta: https://meta.fabricmc.net/

