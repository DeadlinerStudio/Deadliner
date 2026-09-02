# Glance Task Widgets Module Plan

## 模块拆分

- `model`：`TaskWidgetSnapshot`、`TaskWidgetItem`、`TaskWidgetStatus`、`TaskWidgetVisualState`，定义两种尺寸共享的不可变展示契约。
- `data`：`TaskWidgetDataSource` 与 `TaskWidgetRepository`，负责 KMP 映射、ACTIVE/删除过滤、截止时间解析、排序、三态判断、相对时间和胶囊摘要。
- `theme`：`TaskWidgetPaletteFactory`、`TaskWidgetThemeResolver`、`TaskWidgetColors`，把 Android 外观首选项映射到 MIUIX 语义表面/文字/主色/错误色和 Deadliner 临近、完成色。
- `rendering`：`TaskWidgetBackgroundRenderer`，生成 2×2 背景、2×1 背景和三种任务行的小尺寸位图。
- `presentation-2x2`：`CompactTaskGlanceWidget` 与共享任务行/顶栏/空态组件。
- `presentation-2x1`：`CapsuleTaskGlanceWidget`，只消费共享快照与 palette，不重复数据计算。
- `receivers`：两个 `GlanceAppWidgetReceiver` 分别注册 2×2 与 2×1 metadata。
- `preferences`：`TaskWidgetPaddingPreset` / `TaskWidgetPreferences`，保留既有存储键，但其数值表示 2×2 卡片顶部外边距，仅供 2×2 使用。
- `infra`：`TaskWidgetRefreshCoordinator`，统一订阅任务与外观变化并更新两个 widget class。

## 平台映射

- HarmonyOS 2×2：`entry/src/main/ets/widget/pages/TaskWidgetCardSmall.ets` → Android `CompactTaskGlanceWidget` + shared components。
- iOS 2×2：`DeadlinerWidget/Views/SmallListWidgetView.swift` → Android 三态 palette、状态背景、任务行分层与计数规则。
- HarmonyOS 2×1：`entry/src/main/ets/widget/pages/TaskWidgetCardCapsule.ets` → Android `CapsuleTaskGlanceWidget`。
- HarmonyOS 数据：`utils/WidgetUpdater.ets`；iOS 数据：`Providers/KMPWidgetSnapshotReader.swift` 与 `DeadlinerWidgetHelpers.swift` → Android `TaskWidgetRepository`。
- Android 落位：`app/src/main/java/com/aritxonly/deadliner/widgets/glance/{model,data,theme,ui,preferences}`，metadata 位于 `app/src/main/res/xml`。

## 文件拆分策略

- `CompactTaskGlanceWidget.kt` 只保留 2×2 provider/root 布局，任务行、顶栏和空态移入 `TaskWidgetComponents.kt`。
- `CapsuleTaskGlanceWidget.kt` 独立维护 2×1 几何，不把尺寸分支塞入 2×2 文件。
- receiver 各自独立，避免一个文件承担 provider、广播和 UI 三种职责。
- palette 只返回颜色与状态行颜色组；Canvas/Bitmap 绘制留在 renderer。
- KMP `Task_` 不泄漏到 model/UI；UI 不解析时间、不读取数据库。
- 单个新增或重构文件目标低于 350 行，硬阈值 1000 行。

## MIUIX 视觉映射

- 背景/卡片表面：对应 `background` / `surfaceContainer` 角色。
- 主要文字：对应实际容器的 `onBackground` / `onSurfaceContainer`；次要时间对应 `onSurfaceVariantSummary`。
- 普通状态：`primary`；逾期状态：`error`；临近橙和完成绿使用 Deadliner 自有语义色。
- 任务行以表面层级、高光与轻描边建立 MIUIX 深度，不引入 Material 组件或虚构 Miuix `warning/success` token。
- Glance 不支持 Miuix squircle shader；圆角采用 24dp 外卡片、8–9dp 任务行和 16–20dp 胶囊的低阶兼容表达。

## 风险点

- 多个位图可能触发 RemoteViews/Binder 体积问题：2×2 背景控制在约 240×240，任务行约 180×40，并按状态复用；2×1 背景单独使用宽幅低分辨率图。
- 无截止时间任务此前被 Android 排除，改为计数并置后会改变用户可见总数；这是与 HarmonyOS/iOS 数据语义对齐的预期变化。
- 相对时间从向上取整/七天后日期切换为参考实现的向下取整分钟/小时/天语义，需更新测试。
- 2×1 不消费 2×2 卡片顶部外边距预设，防止 18/26dp 整体偏移破坏单行胶囊。
- 编译通过不能证明 launcher 实际缩放与位图层次；若本轮无法运行设备/模拟器，必须明确记录视觉运行验证待办。
