# 月度总览 Module Plan

## 模块拆分

| 模块 | 职责 | Android 落点 |
| --- | --- | --- |
| 月投影 | 所选月任务、活动、专注数据 | OverviewDashboardState / 共享 Core OverviewMonthUiState |
| 根看板 | 月历 → 紧凑专注/归档 → 任务 → 趋势 → AI 复盘 | OverviewDashboardScreen / Cards / MonthCalendar |
| 独立分析 | tasks、trends、review 独立路由、同一 Scaffold/inset 合同 | OverviewDomainScreen；旧三个 Tab 的内容组件分别复用 |
| AI 洞察 | 所选月缓存、生成、加载/失败、摘要重试 | MonthlyInsightState / OverviewReviewSection / DashboardScreen |
| 专注管理 | 全量记录、今日/累计/分布、更正与删除错误状态 | FocusManagementState / Screen；复用 Core 用例及同步规则 |
| 归档 | 当前 Task/Habit 归档恢复/删除 | 既有 ArchiveActivity / ArchiveScreen |
| 主宿主 | 共享月份、底栏/全局专注避让、旧路由兼容 | ModernHost / MorePanelCard / OverviewActivity |

## 平台映射

- Android UI 位于 `app/src/main/java/com/aritxonly/deadliner/ui/overview/`，平台入口在 OverviewActivity；共享业务规则在 ProjectDeadliner/DeadlinerCore，不在本轮更改。
- iOS 参考 `Core/Application/UseCases`、`Data/Repositories` 和 `Features/Overview`，各页面和管理独立。
- 鸿蒙参考 `model`、`common/repository`、`pages/routes/overview`、`pages/components`，复用月份/管理合同；不复制 Android 专属玻璃代码。
- PRD 保存当前交互原型与 android-ui-reference.md，原型记录与 AI 使用示例数据。

## 文件拆分策略

根页、日历、卡片、月状态、领域宿主、专注状态、专注管理、AI 状态和摘要分别成文件。复用已有任务/趋势/月报组件，不重新聚合进巨型文件。新增及本轮更改的核心 Kotlin 文件控制在 1000 有效行以内。

## 风险点

不增加底栏槽位，不改变数据协议与业务用例。月历按完成日期、复盘按截止日期月份，显示口径差异。二级页对齐设置页 Scaffold 的零 inset 和当前顶栏高度；导航栏安全距离位于列表内部，背景参与完整 backdrop 捕获。旧统计 Intent 兼容进入 tasks。所有业务删除由确认弹窗与 Core 软删除实现，原型只修改内存示例。

## 本批次模块
OverviewPeriodComparison：日期对齐与同期间任务数量差；OverviewMiniCharts：紧凑微型柱/线图；MonthlyInsightRepository/Persistence：按月缓存、并发合并、失败重试防抖与旧缓存迁移。月份选择和已结束月复盘分别维护。各文件仍控制在 1000 有效行内。

月状态投影：Core OverviewMonthUiState.statusDistribution + OverviewViewModel；Android OverviewTaskStatusChart 仅绘制共享计数。TopAppBar 补齐原生 subtitle 转发。

OverviewReviewSection：完整正文及关键词展示；DashboardDataCard 空 value 不占位。
