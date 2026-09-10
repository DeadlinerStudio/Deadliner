# Widget Scroll, Layout, and Refresh Module Plan

## 模块拆分

- `contract`：`TaskWidgetConfiguration` 增加 `isScrollable`，预设和默认值定义实例行为。
- `domain`：`TaskWidgetLayoutMetrics` 仅保留固定间距与静态三项上限，不再推算宿主可用高度。
- `presentation`：配置页提供滚动开关；`CompactTaskGlanceWidget` 在静态 Column 与 Glance LazyColumn 间切换。
- `infra`：`TaskWidgetConfigurationStore` 负责字段迁移；配置 Activity 与刷新协调器负责宿主恢复后的刷新时序。
- `render-state`：新增进程内 `StateFlow` 渲染状态，刷新协调器发布最新快照和单调递增修订号，Glance composition 使用 `collectAsState` 观察。

## 平台映射

- iOS：本轮不改；WidgetKit 小组件不支持用户直接滚动列表。
- HarmonyOS：本轮不改；现有 2×2 继续保持三项展示。
- Android：`widgets/glance/{configuration,preferences,theme,ui}`，滚动能力作为 Android Glance 增强项。

## 文件拆分策略

- 单个核心文件尽量不超过 1000 行有效代码
- 若必须逃逸，先申请开发者批准
- 滚动与静态列表拆为独立 composable，避免继续放大 `CompactTaskGlanceWidget.kt`。
- 刷新时序留在 `TaskWidgetRefreshCoordinator`，配置 Activity 只表达保存与生命周期事件。

## 风险点

- LazyColumn 与外层整卡 clickable 的手势竞争。
- 4×6 宿主上报高度与视觉网格不完全相同，需给底部保留显式安全余量。
- `LocalSize` 是宿主 options 推导值，不能用它和多个取整步骤精确拼满 RemoteViews；列表应依赖 Column weight 的真实剩余空间。
- 新配置字段必须对旧实例提供 `false` 默认值。
