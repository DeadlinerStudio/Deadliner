# 通知状态协调 Module Plan

## 模块拆分
- Core NotificationOrchestrator：期望配置、差异协调、统一入口及强制恢复。
- Core DeadlinerDatabase：事务后查询监听，覆盖直接写库、远程合并与备份恢复。
- Core Android NotificationService：闹钟身份、时间与调度生命周期。
- Android Gateway/Receiver/LiveUpdateService：偏好、投递有效性检查、多任务实时渲染。

## 平台映射
Core shared/src/commonMain 提供共用规则；shared/src/androidMain 执行 setAlarm；iOS shared/src/iosMain 与鸿蒙 shared/src/ohosArm64Main 保留已有 Gateway 实现，宿主需显式接入数据库 configureNotificationUpdates；保留 createNotificationOrchestrator 旧工厂，不自动改变 iOS 原生调度所有权。Android app/notification 与 platform/kmp 接入。

## 文件拆分策略
调度与展示保持独立；新增时间规则小文件；本次不新增超过 1000 行核心文件。

## 风险点
进程重启需清理旧平台注册再恢复；监听回调不应令已提交事务报告失败。实时活动启动受 Android 前台服务限制。

## 实现细节
使用 SQLDelight 查询监听在提交后协调，回滚不投递。注册账本为进程内配置 map；启动配置时清理旧平台注册并从持久化聚合恢复，普通变更不 cancelAll。force 仅重应用当前配置，用于设置、时间/时区和精确闹钟权限变化。Android 保留工作区同时新增的灵动岛渲染适配。

## 接入契约
宿主启动时调用 `database.configureNotificationUpdates(gateway)` 一次，取得同一个 orchestrator 供旧 ViewModel 兼容调用；之后任务、习惯和记录写库自动协调。设置、时区或系统恢复调用 `database.triggerNotificationInfoUpdate(force = true)`。显式清空使用 `orchestrator.cancelAllNotifications()`，同步清空平台状态与配置账本。iOS/HarmonyOS 切换前需移除旧宿主并行调度，避免双重注册。

## 诊断模块
- NotificationDiagnostics：标准事件、权限快照和系统发布观察。
- NotificationDiagnosticController / Service / Receiver：独立测试生命周期，复用真实通知适配，立即与闹钟两个入口。
- NotificationDiagnosticSettings：复用现有 SettingsSection / SettingsDetailTextButtonItem，提供 TXT 导出及可读测试状态。
- Native 和 KMP 的业务路径：补充事件，不复制平台调度算法。

## 设备日志修复契约
Android manifest 同时接收无 URI 旧闹钟和 deadliner-notification://当前 applicationId 的新闹钟；gateway 对窗口内配置立即请求 UpcomingLiveUpdateService。普通前台槽位负责 Android 生命周期，完整通知由 IslandWhitelistBypass 回调唯一提交；Root/Shizuku/OFF/失败降级共用这一个提交点。AIDL 增加只读 diagnostics，Shizuku 用户服务版本提升以更新接口。图片缓存仅保存 120/88px Bitmap Icon。
参考源码：本地 Capsulyric dd020bcc39d129695d2064fda405870d96c6e58f 的 SuperIslandNotificationDispatcher、SuperIslandStandardFocusBuilder、SuperIslandIconCache、XmsfNetworkHelper、ShizukuHook 与 README。Root 属于 Deadliner 特权宿主，上游 README 对 Root 用户推荐 HyperCeiler，不存在可直接复制的 libsu Root 路径。

## 启动日志 ANR 补充
- Android AppLog：单线程后台持久化、流式有界清理及导出屏障/缓存快照。
- Core Android PlatformLogSink：提供宿主 fileWriter 接口，保持公共 KMP 日志 API 与标准格式；Android 宿主共用写入队列。
- FeedbackSettings：ZIP 生成与文件复制放入 IO coroutine。
- AppLogTest：阻塞 IO、大历史、混合来源、过期记录和稳定快照回归。
