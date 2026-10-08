# Module Plan

## 模块拆分

### KMP
- NotificationMetadata：设置策略、元数据、交互回执模型。
- NotificationMetadataPlanner：时间边界、任务/习惯窗口、动作与下一次刷新时间。
- NotificationOrchestrator/DeadlinerDatabase：可选元数据配置，CRUD 监听统一触发。

### Android
- NotificationPolicyPreferences/NotificationInteractionStore：迁移设置和持久化操作回执。
- AndroidNotificationMetadataScheduler/Receiver：注册账本、唯一 Alarm 身份、投递去重与状态刷新。
- RealtimeNotificationService/RealtimeNotificationRenderer：共享前台生命周期、独立任务/习惯卡片、结果保留和系统表面适配。
- NotificationMetadataActionActivity：可验证动作入口；任务沿用 KMP 状态机（结果态 RESTORE_ACTIVE 撤回并清除回执）、习惯走 Add 命令。
- XiaomiIslandExtras：App 图标、摘要长度、状态色与双按钮。
- NotificationSettings：复用现有 Settings 组件，三开关和二选一。

## 平台映射
KMP 规则和模型供 iOS/HarmonyOS 后续消费；本次 Android opt-in，旧 native gateway 保持兼容。无单文件业务巨型实现。

## 文件拆分策略
规划器、调度、渲染、交互和设置分别维护，各文件低于 1000 行。

## 风险点
新增可选元数据路径保持旧平台宿主兼容；Android 新策略不再读取旧提前时长。OEM 最终展示需要真机确认。

- KMP HabitRealtimeMetadataPlanner / NotificationMetadataPlanner 生成习惯进度、打卡结果与过期边界；Android HabitIslandPresentation 只做周期本地化、名称截断和 x/y 显示。

- IslandStateIcons 管理超级岛状态资源映射；Miuix 状态 SVG 转为独立 Android VectorDrawable，统一 24dp、Medium 字重。
