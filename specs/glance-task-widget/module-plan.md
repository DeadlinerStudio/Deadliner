# Glance 2x2 Task Widget Module Plan

## 模块拆分

- `model`：`TaskWidgetSnapshot` 与 `TaskWidgetItem`，仅承载渲染所需的不可变数据。
- `data`：`TaskWidgetDataSource`、KMP 适配器与 `TaskWidgetRepository`，负责过滤、时间解析、排序、紧急判断和相对时间。
- `theme`：`TaskWidgetThemeResolver` 与背景位图生成器，读取 Android 外观首选项并输出日/夜颜色。
- `presentation`：`CompactTaskGlanceWidget`、Glance composables 和 receiver，只消费快照与主题。
- `infra`：`TaskWidgetRefreshCoordinator`，统一处理应用前台、外观设置和系统配置触发的刷新。
- `preferences`：`TaskWidgetPaddingPreset` 与 `TaskWidgetPreferences`，定义标准/4×6/4×7 的持久化值、额外顶部 padding 和对应最大展示条数。

## 平台映射

- iOS：本轮不改；视觉语义可映射到 WidgetKit 的 timeline entry + SwiftUI view。
- HarmonyOS：参考 `entry/src/main/ets/widget/pages/TaskWidgetCardSmall.ets` 的背景、顶栏、任务卡与空状态层级。
- Android：新增 `app/src/main/java/com/aritxonly/deadliner/widgets/glance/{model,data,theme,ui}`；provider metadata 位于 `app/src/main/res/xml`。
- Android 设置页：`ui/settings/WidgetSettings.kt` 只保留 Glance 卡片边距配置与既有控制中心入口；旧 RemoteViews 开关和 `GlobalUtils` 属性一并删除。

## 文件拆分策略

- 每个文件只承担单一职责，预计均低于 300 行，不在 `MainActivity` 或旧 widget 文件中堆叠新实现。
- 共享快照不暴露 KMP `Task` 类型给 UI，降低 KMP AAR API 变化对 Glance 层的影响。
- 背景生成与颜色推导独立于 Glance composition，便于 JVM 测试并避免大体积 `RemoteViews`。
- 单个核心文件尽量不超过 1000 行有效代码；若必须逃逸，先申请开发者批准。
- 旧 RemoteViews Kotlin、provider XML、布局、配置页和仅被其使用的资源按引用关系删除；`MainActivity` 只保留 Glance 主动刷新。

## 风险点

- Glance 1.1.1 的布局与背景能力有限，不能直接复刻 ArkUI 的 `Rect.linearGradient` 与 `Circle`，需用轻量 Bitmap 生成器等价表达。
- `KmpV6Runtime` 为 Android composition root；receiver 启动时依赖 `DeadlinerApp.onCreate()` 已完成初始化。
- SharedPreferences 外观变更没有跨进程可靠内存通知，注册应用内监听并保留 30 分钟周期刷新作为兜底。
- launcher 对 2×2 的实际 dp 尺寸不同，布局固定为紧凑信息层级并允许整体拉伸，不依赖精确像素。

## 最终实现映射

- `model`、`data`、`theme`、`presentation`、`infra` 均按计划拆分，没有把 KMP `Task` 泄漏到 Glance UI。
- `TaskWidgetRefreshCoordinator` 除外观首选项监听外，还订阅 KMP `TaskListUiState.tasks`；旧组件的 Activity 生命周期刷新逻辑保持不变。
- Glance 1.1.1 不提供线性布局权重，本 2×2 目标布局使用受控宽度保证标题省略和到期文本同时可见。

## 本轮扩展计划

- 新增独立 preferences 文件，避免把小组件布局设置继续堆入 `GlobalUtils`。
- composition 读取 preset，并在现有 10dp 内容内边距之外追加 `0/26/18dp` 顶部 padding。
- 非标准预设渲染前两条任务，标准预设维持最多三条。
- manifest 最终只保留 `CompactTaskGlanceWidgetReceiver`。
