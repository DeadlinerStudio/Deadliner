# Android 云与同步 Provider 设置

## 背景

Android 设置首页当前将唯一的同步入口称为“WebDAV”。Deadliner Core 已同时提供 WebDAV 和 BYOS（Bring Your Own Server）同步能力，Android 尚未暴露 BYOS 的服务地址、Access Token 和 provider 选择。

## 目标

- 将设置首页的“WebDAV”小节改为“云与同步”。
- 提供独立的 BYOS 与 WebDAV 配置入口，并在入口页显示当前选中的同步方式。
- 保存当前 provider、BYOS 服务地址、Access Token、稳定设备 ID 与本地 scope ID；密钥不进入日志或 Core 持久化层。
- 将手动刷新和 WorkManager 周期同步路由至当前 provider；WebDAV 保留已有的进程内自动同步，BYOS 使用同一周期调度。

## 非目标

- 不修改 Core 的 BYOS 协议、服务端 API、冲突算法或 WebDAV 格式。
- 不实现 BYOS 账号注册、Token 签发、实时推送或跨账号切换 UI。
- 不迁移既有 WebDAV 凭据或改变其已保存配置。

## 用户场景

用户打开“云与同步”，选择“BYOS”，填写自己的 DeadlinerServer 地址和 Access Token 后启用同步；随后周期任务和手动同步均使用 BYOS。用户切换至“WebDAV”后，可继续使用原有账号密码配置和自动同步。

## 验收标准

- 首页显示“云与同步”，进入后可进入“BYOS”和“WebDAV”两个配置页。
- 仅被启用的 provider 成为当前同步方式；BYOS 与 WebDAV 配置均可分别保留。
- BYOS 配置完整时，`KmpV6Runtime.syncOnce()` 创建 Core 的 `DeadlinerServerSyncEngine` 并返回统一结果；WebDAV 保持原有行为。
- 周期 Worker 按当前 provider 判断配置完整性，配置缺失时不发起网络同步。
- Android release 编译通过，且新增 token 不会出现在 AppLog 消息中。

## 风险与约束

- 现有工作区有未提交的 UI 改动；本功能只提交明确列出的设置、同步与资源文件。
- BYOS Access Token 仅存在 Android 宿主偏好设置中，传给 Core 时只用于单次 transport 生命周期，不能写入 `scopeId` 或日志。
- `SettingsActivity.kt` 已较大；新增 UI 置于独立 settings 文件，路由只作最小接线。

## 背景

- 创建日期：2026-08-30
- 功能标识：`android-cloud-sync-providers`
- 需求来源：

## 目标

- 

## 非目标

- 

## 用户场景

1. 

## 验收标准

- 

## 风险与约束

- 
