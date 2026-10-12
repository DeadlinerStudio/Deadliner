# Module Plan

## 模块拆分
### KMP
复用 FocusTimerSnapshot、FocusTimerResult.activeTimer.updatedAt 和 FocusTimerCommand，沿用 FocusTimerViewModel.executeNow。DeadlinerDatabase 新增只读 focusTimerNotificationInfo，从持久化会话生成通知快照，支持同步回调校验；不修改计时算法或数据库结构。
### Android
- FocusNotificationActions：能力和会话版本校验、操作映射、透明 Activity。
- FocusRealtimeNotificationRenderer：原生标准通知、计时锚点、操作集合和签名。
- FocusIslandTemplate：独立录音协议和资源；录音系统预设按钮仅显示暂停/继续与结束，辅助区域仅显示状态。
- FocusTimerForegroundService：生命周期、状态读取、发布差异及日志。

## 平台映射
Android 适配，两种表面共享 KMP snapshot；不切换 iOS/HarmonyOS。

## 文件拆分策略
不扩大 UI 文件；协议、渲染、动作、服务分开，单文件低于 1000 有效行。

## 风险点
OEM 私有协议、Root 延迟授权与重复按钮。回调重新校验会话/版本；原生采用允许 promotion 的标准布局。计时按钮只通过共享引擎执行。

- FocusResultNotificationPlanner 生成短暂专注结果；Core 数据库/通知编排器把它接入现有元数据边界和提交监听。Android 复用通用实时通知生命周期、结果图标与校验操作入口。
