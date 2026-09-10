# Cross-platform Widget Refresh Coverage Module Plan

## 模块拆分

### 1. KMP contract

- commonMain：`WidgetUpdateReason` 与 `expect fun updateWidget(reason)`。
- commonMain 数据层：组合 repository sync notifier 与 widget notifier，在分类/任务/习惯事务提交后发请求。
- commonMain/平台 bridge：手动刷新入口显式发送 `USER_REFRESH`。

### 2. Android host

- DeadlinerCore androidMain actual：进程内 handler registry，不持有 app Context。
- Android app：`TaskWidgetRefreshCoordinator` 注册 handler、去抖并更新两个 Glance widget。
- `DeadlinerApp`：Application 级 foreground 计数触发；保留 provider `updatePeriodMillis` 兜底。

### 3. iOS host

- DeadlinerCore iosMain actual：发送稳定名称的 `NSNotification`。
- Swift app：统一 WidgetKit refresh coordinator 监听通知并去抖调用 `reloadAllTimelines()`。
- `DeadlinerApp`：scene 进入 `.active` 时刷新；Widget Timeline 继续按 15 分钟请求下一次更新时间。

### 4. HarmonyOS host

- DeadlinerCore ohosArm64 actual：将 reason 交给已注册的 C 回调。
- C ABI/NAPI：注册线程安全 ArkTS callback，并暴露显式手动刷新请求。
- ArkTS：桥接 callback 到现有 `WidgetUpdater`；`EntryAbility.onForeground` 与 Form `updateDuration` 继续作为宿主覆盖。

### 5. macOS compile support

- macosArm64 actual 为无小组件宿主的安全 no-op，仅用于满足 KMP target 完整性。

## 平台映射

| 能力 | KMP | Android | iOS | HarmonyOS |
| --- | --- | --- | --- | --- |
| 数据提交 | repository notifier | actual handler | actual notification | actual C callback |
| 下拉刷新 | `USER_REFRESH` | KMP runtime refresh | iOS KMP bridge | ArkTS NAPI request |
| 前台 | reason contract | Application lifecycle | `scenePhase == .active` | `UIAbility.onForeground` |
| 定时兜底 | reason enum | AppWidget provider | WidgetKit Timeline | Form `updateDuration` |
| 去抖 | 平台职责 | coroutine Job | Swift Task | ArkTS timer/Promise guard |

## 文件拆分策略

- KMP contract 与各平台 actual 分文件，避免把平台桥塞进既有 scheduler。
- Android 复用 `TaskWidgetRefreshCoordinator`，不新增第二套 Glance 更新器。
- iOS 协调器优先独立文件；若 Xcode 工程使用手工 file group，则显式加入 target。
- HarmonyOS 将 ArkTS 协调器放在 `utils`，native ABI 只负责信号传递，不搬运小组件业务数据。
- `DeadlinerPersistenceCApi.kt` 与 `napi_init.cpp` 已较大，只允许增加短小的 ABI glue；复杂状态放入新文件。

## 风险点

- OHOS NAPI 线程与生命周期管理：重复注册、注销与进程退出必须安全。
- iOS Notification name 必须在 Kotlin 与 Swift 保持一致并覆盖 framework 导出行为。
- repository notifier 当前同时负责自动同步，组合 notifier 不能改变原有同步触发语义。
- 批量远端 merge 可能绕过 repository；同步完成路径必须额外请求刷新。
- 定时刷新由系统调度，测试只能验证声明和 Timeline policy，不能保证精确唤醒。
