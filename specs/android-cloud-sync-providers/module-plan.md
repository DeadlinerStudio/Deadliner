# Android 云与同步 Provider 设置 Module Plan

## 模块拆分

- `sync-provider-preferences`：`CloudSyncPreferences` 保存 provider、BYOS endpoint/token、设备 ID 与非敏感 scope ID；`GlobalUtils` 仅提供既有 SharedPreferences 容器。
- `sync-provider-runtime`：`KmpV6Runtime` 将当前 provider 映射到 WebDAV facade 或 Core BYOS engine；`SyncWorker` 只负责当前配置的调度调用。
- `sync-provider-presentation`：`CloudSyncSettingsScreen` 提供入口选择，`ByosSettingsScreen` 与既有 `WebSettingsScreen` 分别编辑各自配置。

## 平台映射

- Android：`app/src/main/java/com/aritxonly/deadliner/{localutils,platform/kmp,sync,ui/settings}`。
- Core：复用 `DeadlinerServerSyncEngine`、`KtorDeadlinerServerTransport` 与 WebDAV facade，不新增 Core 代码。
- iOS/HarmonyOS：本批不改 UI 或配置；Android host 仅使用已有的 provider-neutral Core API。

## 文件拆分策略

- 新的云同步入口与 BYOS 表单各自独立文件，避免继续扩张既有 `WebSettings.kt`。
- `SettingsActivity.kt` 仅增加 route/import/composable 接线。
- 不在 `SyncWorker` 重复构造 transport，统一调用 `KmpV6Runtime`。

## 风险点

- provider 切换必须先关闭 WebDAV 的进程内自动同步，避免两种协议同时修改本地数据。
- BYOS scope ID 不能含 Access Token；它在端点或 token 变化时重置。
- 手动与周期同步的失败信息必须避免包含 Token。

## 模块拆分

- `contract`：
- `domain`：
- `presentation`：
- `infra`：

## 平台映射

- iOS：
- HarmonyOS：
- Android：

## 文件拆分策略

- 单个核心文件尽量不超过 1000 行有效代码
- 若必须逃逸，先申请开发者批准

## 风险点

- 
