# 总览看板 Module Plan · v1.0

## 模块拆分

| 模块 | 职责 | Android / Core 实现 |
| --- | --- | --- |
| 共享投影 | 月活动、完成数、互斥状态、既有月报 | Core OverviewViewModel / OverviewMonthUiState.statusDistribution |
| 看板展示 | 月份、月历、双卡、任务状态、趋势预览、完整洞察 | OverviewDashboardScreen / Cards / MonthCalendar / TaskResultCard / TaskStatusChart / MiniCharts / ReviewSection |
| 状态加载 | 生命周期/失效订阅、日/时区变化、错误重试、同期计算 | OverviewDashboardState / OverviewPeriodComparison |
| 独立分析 | 任务、趋势、复盘独立页面与原功能 | OverviewDomainScreen / OverviewStatsScreen / TrendAnalysisScreen / DashboardScreen |
| AI 本地归档 | 最近结束月份门禁、按月保存、并发合并、显式重试 | MonthlyInsightRepository / Preferences / State |
| 管理 | 全量专注记录、校正/软删除；既有归档管理 | FocusManagementState / Screen；Core FocusTimerViewModel；ArchiveActivity |
| 主宿主/顶栏 | 主导航、月份状态、覆盖层避让、subtitle/E2E | ModernHost / OverviewActivity / TopAppBar / AdaptiveMaterialScaffold |

## 平台映射

iOS 对接 Core/Application/UseCases、Data/Repositories、Features/Overview；鸿蒙对接 model、common/repository、pages/routes/overview、pages/components。这是建议的职责落点，最终文件名按各端现有工程确定。复用共享业务规则；AI 存储/导航/分享/安全区由平台适配，禁止在 UI 内重复聚合月任务口径。

## 文件拆分策略

各页面、日历、卡片、图表、AI存储、管理状态独立文件。新增/本轮修改的 Kotlin 核心文件控制在1000有效行内。不将独立页重新并回统计 Tab 或单文件巨型看板。

## 风险点

月历/任务状态与月报指标有不同日期口径；AI 持久化不代表指标冻结；专注更正只能调低。主宿主与独立 Activity 复用各自标准 Scaffold，避免重复安全区和 backdrop 周期。旧统计 Intent 兼容任务统计，删除操作保留确认。Android 当前为实验实现，iOS/鸿蒙待开发。

## 交接材料

spec.md 是最终 PRD；android-reference-trd.md 是实际实现与跨端技术合同；ui-design.md 是最终视觉参考；prototype.html 与 prototype.fragment.html 是交互示例及可编辑源；tasks.md 保留验证记录。README.md 为入口，history/ 为历史。
