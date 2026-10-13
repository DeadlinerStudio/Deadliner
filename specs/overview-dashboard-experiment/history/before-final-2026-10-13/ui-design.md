# Android 实现 UI 与跨端交接

2026-10-12。Android 当前工作区；快照 commit `39f0bfe` 之后的功能改动。iOS/鸿蒙尚未实现。
产品 PRD：`ProjectDeadliner/Features/PRD/overview-dashboard/`。

## 页面层级

| 页面 | 自上而下内容 | 交互 |
| --- | --- | --- |
| 总览 | 月份总览标题/年份副标题、Material Symbols 月切换；完整月历热力图；紧凑专注管理/归档双卡；任务统计卡；带任务短柱图/专注浅填充曲线的趋势卡；月度复盘卡 / AI 洞察；既有浮动底栏 | 日期 → 摘要；各入口分别进入独立二级页；返回保留月份和滚动位置 |
| 任务统计 | 今日概览；任务完成时间段统计；任务状态统计与环形图 | 仅三个任务模块，无 Tab、专注内容或所选月附加图 |
| 趋势 | 年度任务贡献；最近 7 天任务/专注；最近 6 个月任务/专注；最近 4 周任务/专注 | 原图表交互、类型摘要、每日逾期系列设置保留；各图使用原有时间范围 |
| 月度复盘 | 月标题/口径说明；AI 洞察或加载/不可用状态；两列月指标；月度活动分布；分享 | 固定最近已结束月份，不随总览月份切换；沿用文字色调、背景图、预览、保存及系统分享 |
| 专注管理 | 今日保存时长/次数；历史总时长/次数及任务/习惯拆分；全部目标和分类时间分配；全部已保存记录（标题、开始时间、时长、习惯日期） | 更正时长、确认删除；保存失败保留弹窗；操作后今日、累计、分布、记录及根页自动刷新 |
| 归档 | 既有任务/习惯归档与底部操作 | 复用恢复、单项/当前类型批量删除确认；不受月份影响 |

专注管理入口在日历下方，与归档并排，仅显示今日保存时长，无说明和曲线；它是紧凑管理入口，根页不再另放「每日专注」大图。趋势卡承担图表预览，所有长期趋势移入其独立目的地。月度复盘为独立卡片，标题在卡内，复用与趋势相同的 DashboardDataCard 紧凑布局；卡片展示真实 AI 分析摘要（完整正文及关键词），点击查看完整复盘及分享。AI 未配置/失败展示不可用和重试，不编造洞察。

卡片沿用 surfaceContainer、项目圆角、titleMedium / headlineMedium / titleLarge 和 bodySmall。双卡可用宽度 <280dp 或字体倍率 >1.8 时堆叠。月历使用 16dp 内边距，日期高度 32dp、行间距 4dp，周一至周日，热力颜色编码任务完成数量，小点编码存在专注。

## 统计与管理规则

- 月份默认本月，禁止未来月份。任务成果/热力图按实际完成日期；月度指标与 AI 保留按截止日期月份的既有规则并显示口径说明。
- 专注管理是跨月份记录管理：今日按本地 startedAt 日期；累计及分布涵盖全部未删除的已保存 Task/Habit 记录。正在计时的未保存时长不纳入。
- 更正时长复用 `FocusTimerViewModel.adjustSessionDurationNow`，有效范围 1 至当前记录时长、墙上时间跨度与 Core 上限三者的最小值；只能调低，不扩大既有业务规则。
- 删除复用 `FocusTimerViewModel.deleteSessionNow` 软删除与同步。习惯打卡的计量规则保持 Core 现有行为，不将删除专注等同于撤销打卡。
- 按 uid 为记录提供稳定列表键；全部已保存记录可达，不截断为最近 5 或 20 条。失败保留输入/确认状态，避免表现成保存成功。
- 根页/分享页复用应用级 `MonthlyInsightRepository`；AI 结果与尝试记录按 yyyy-MM 持久化。成功结果不重复生成；失败只允许显式重试。未结束月份不生成复盘，复盘入口固定最近已结束月份，当前为 2026-09。

## Scaffold 与沉浸合同

以 `ui/settings/SettingsComponents.kt` 的 `CollapsingTopBarScaffold` 为参照：`AdaptiveMaterialScaffold`、零 `contentWindowInsets`、`useCurrentTopBarHeightForContentPadding=true`、同一 MiuixScrollBehavior 嵌套滚动、列表同步 ProgressiveTopBarMaterial。顶栏自身包含状态栏区域，不叠加 Scaffold 默认的状态栏 inset。顶栏收起时内容预留高度随当前实测高度收缩。

二级页背景和 backdrop 使用同一个实体主题 surface。导航栏 inset 只加入可滚动内容的 bottom padding，不在页面外裁去导航栏区域。`OverviewActivity` 创建、恢复、配置/多窗口变更调用 `enableEdgeToEdgeForAllDevices`，由封装关闭导航栏对比度遮罩。归档继续复用既有 Activity/Scaffold。

## 模块迁移与代码

- `OverviewDashboardScreen.kt`：根布局、月状态、四个独立统计/管理路由和归档入口。
- `OverviewReviewSection.kt` / `MonthlyInsightState.kt`：月度 AI 摘要、缓存、失败重试。
- `OverviewDomainScreen.kt`：tasks / trends / review / focus 独立二级页，无统计 Tab。
- `FocusManagementState.kt` / `FocusManagementScreen.kt`：全量记录、统计展示与既有更正/删除用例。
- `OverviewStatsScreen.kt` / `TrendAnalysisScreen.kt` / `DashboardScreen.kt`：原三个 Tab 的组件分别保留至独立页；月底报告仍只导出任务指标。
- `OverviewActivity.kt`：域和月份参数、兼容旧统计 Intent（默认任务统计）、E2E 生命周期。
- `OverviewDashboardState.kt`：STARTED 生命周期数据库原始失效订阅及本地午夜/时区刷新。

上述文件均在 Android 的 `app/src/main/java/com/aritxonly/deadliner/` 下，UI 文件在 `ui/overview/`。月度投影仍用 KMP `OverviewMonthUiState` 和 `FocusStatisticsQuery`，本次不更改数据库、同步或计时规则。

## 跨端原型

`prototype.html` 为独立交互版本，`prototype.fragment.html` 为可编辑源。支持月份/日期、独立任务/趋势/复盘导航、专注更正/删除及统计联动、统计设置、分享选项示意、归档操作。
所有数字、AI 文案、图表与记录均为示例；修改/删除仅作用于内存。原型复现内容层级和交互，字体、玻璃、图表和原生弹窗以 Android 实现为准；不作为设备截图或像素级验收。iOS/鸿蒙共享入口与数据合同，采用各端既有主题和 Scaffold/inset 规则。

## 验证

Android 构建、投影单测、图标注册回归与 Core 专注更正/删除测试结果写于 tasks.md。原型完成 JavaScript 语法与浏览器导航/更正交互检查。遵照用户要求不运行模拟器；实机顶部折叠和导航栏外观需在用户设备复核。

## 月度复盘卡视觉对齐
月度复盘不再采用外置标题加自定义卡片，复用趋势卡的 DashboardDataCard：16dp 内边距、8dp 内容间距、titleMedium 卡片标题、移除重复「AI 月度洞察」文字、bodySmall 月份及同色 Material Symbols 箭头；正文及关键词完整展示，加载/失败/重试和导航行为保留。

## 紧凑看板修订
月份切换使用无玻璃的 Material Symbols 图标按钮，保留 48dp 点击区域并增加 4dp 间隔。任务统计卡复用紧凑卡字号（titleLarge），右侧 88dp 本月状态饼图；数字下方显示较上月同期百分比，增长绿色、下降红色、持平中性色。上月基数为零时显示新增项数。比较本月 1–今日与上月 1–同日，较短月份截到月末；历史月份以所选月末为截止日。
专注入口移除预览图，趋势卡任务短柱图和专注细曲线各占一半、各 48dp，无坐标轴；完整趋势在二级页。复盘 AI 按月固化，指标与活动图沿用数据库投影；原型数据为示例。

## 最新视觉去冗余
专注管理/归档无灰色描述。任务统计移除月份口径和截止日，只保留数字、同期变化、88dp 状态饼图及两列状态计数图例：待办橙、完成绿、逾期红、放弃灰。共享 Core 月投影提供互斥统计，已完成采用完成日期，其余状态按截止日期归入月份；历史月份状态以当前记录为准，不是历史时点快照。月份说明直接传给 MIUIX TopAppBar 原生 subtitle，展开与收起均由其布局，不再固定于顶栏外。静态原型示意展开状态，原生折叠位置以 Android 为准。

## 完整洞察展示
总览月度复盘卡只保留「月度复盘」标题与月份，完整展示 AI 正文及关键词，不限制行数，不用省略号；卡片高度随正文变化。进入详情和分享、按月缓存及失败重试规则保留。
