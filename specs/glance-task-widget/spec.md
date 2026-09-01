# Glance 2x2 Task Widget

## 背景

- 创建日期：2026-08-16
- 功能标识：`glance-task-widget`
- Android 现有四个小组件基于 `AppWidgetProvider + RemoteViews + XML layout`，与已落地的 2×2 Glance 任务卡重复维护。
- 新小组件参考 HarmonyOS `TaskWidgetCardSmall.ets`：渐变氛围背景、品牌顶栏、待办总数、最多三条紧凑任务卡，以及“全部搞定”空状态。

## 目标

- 新增一个独立的 2×2 任务小组件，UI 完全由 Jetpack Glance 的 Compose 风格 API 声明。
- 小组件能够直接读取现有 KMP `DeadlinerDatabase.tasks` 数据，并把平台任务映射为稳定的展示快照。
- 小组件跟随 Android 的“现代 / 鲜明”设计模式、明暗模式、现代中性色板，以及鲜明模式的动态色或自定义种子色。
- 应用前台任务变化、外观设置变化、系统配置变化和平台周期更新时能够刷新小组件。
- 为不同桌面网格提供可选的顶部边距预设：标准、4×6、4×7。
- 删除非 Glance 小组件，只保留 2×2 Glance 任务卡的 provider、设置与刷新链路。

## 非目标

- 本轮不加入独立小组件配置 Activity、自定义背景图、任意数值边距输入，以及在小组件内直接完成或删除任务。
- 不修改 HarmonyOS 附件文件；它只作为视觉与信息层级参考。
- 不把标准 Compose UI 组件混入 Glance；Glance 最终仍受 `RemoteViews` 能力约束。

## 用户场景

1. 用户从桌面小组件选择器添加“Deadliner 任务卡片（2×2）”，立即看到最多三个按优先级与剩余时间排序的活跃任务。
2. 用户在应用中切换“现代 / 鲜明”、明暗模式、现代品牌色板或鲜明种子色后，桌面卡片刷新为对应颜色。
3. 用户新增、编辑或完成任务后，返回/离开主界面或等待平台周期更新，小组件从 KMP 数据库重新生成展示快照。
4. 用户点击卡片任意位置，进入 Deadliner；没有活跃任务时显示本地化的完成状态。
5. 用户在“小组件”设置中选择标准、4×6 或 4×7 后，已添加的 2×2 卡片立即刷新；4×6 仅增加 26dp 顶部 padding，4×7 仅增加 18dp 顶部 padding。

## 验收标准

- 应用只注册一个目标尺寸为 2×2 的 Glance 小组件 provider；旧 `HabitMiniWidget`、`HabitMediumWidget`、`MultiDeadlineWidget`、`LargeDeadlineWidget` 及其配置 Activity、metadata/layout 专属资源被移除。
- 小组件由 `GlanceAppWidget` / `GlanceAppWidgetReceiver` 实现；除平台要求的 provider metadata XML 外，不新增 UI layout XML。
- 默认画面包含圆角渐变/氛围圆背景、18dp 品牌图标、`Deadliner` 标题、总数徽标、最多三条 30dp 左右的任务卡。
- 每条任务卡包含状态色指示条、单行省略标题、相对到期时间；逾期或 24 小时内到期的任务使用紧急色。
- 空列表显示庆祝图形和“全部搞定”本地化文本。
- “现代”模式使用当前现代品牌中性色板和克制的品牌色混合；“鲜明”模式使用动态/种子强调色与更明显的渐变，两种模式都支持夜间颜色。
- 数据源直接读取 `KmpV6Runtime.database.tasks.list()`，只展示未删除、`ACTIVE` 且存在 `dueAt` 的任务。
- 数据映射、排序、紧急判断与相对时间格式有 JVM 单元测试覆盖；完整 Android debug 构建通过。
- 标准预设不增加外层 padding；4×6 把参考实现的 12dp 顶部 + 14dp 底部合并为 26dp 顶部；4×7 把 8dp 顶部 + 10dp 底部合并为 18dp 顶部，两个预设的额外底部 padding 都为 0dp。
- 边距设置写入持久化首选项，并由刷新协调器监听后主动刷新所有 Glance 实例。

## 风险与约束

- Jetpack Glance 不是标准 Compose UI，不能直接复用 Material/Miuix Composable，也没有通用 Canvas；渐变与氛围圆通过运行时生成的小型位图背景表达。
- 采用当前稳定版 `androidx.glance:glance-appwidget:1.1.1`，避免为预览 API 引入 RC/Alpha 依赖。
- Widget 进程内存状态不可靠；每次 composition 都从持久化 KMP 数据和 SharedPreferences 重新构建快照。
- 平台后台周期刷新下限为 30 分钟；前台数据与外观变更采用主动 `updateAll`，其余由系统广播和周期刷新兜底。
- 核心文件不得超过 1000 行有效代码；本功能按 `model / data / theme / ui / refresh` 拆分。
- 增加顶部 padding 后可用高度减少；非标准预设最多显示两条任务，避免固定高度内容溢出，总任务数徽标仍展示完整计数。
- 删除 provider 会让升级后已有旧小组件从桌面失效；这是本轮明确接受的 breaking change，实施前需保留 Git 回退点。
