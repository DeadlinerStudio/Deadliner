# 总览看板 TRD · Android 参考 v1.0

2026-10-13。基于 Android 当前工作区与共享 DeadlinerCore 的实际实现；功能改动尚未形成最终发布提交。保护快照为39f0bfe，不能用该快照代替当前实现。本文供iOS/鸿蒙复用产品规则和业务边界；不代表两端已开发或设备验收完成。

## 1. 依赖和职责

Android Compose/MIUIX版本以工程锁定依赖为准，当前MIUIX为0.9.4-rc01。UI文件在 app/src/main/java/com/aritxonly/deadliner/ui/overview/，宿主为OverviewActivity与ui/main/modern/ModernHost。共享Core源码为 ProjectDeadliner/DeadlinerCore/shared/src/commonMain/kotlin/com/deadliner/。

Android在preBuild经syncDeadlinerCoreKmp运行Core/runscript/buildAndDeploy.sh的Android路径，将AAR部署到.deadliner-core/kmp/deadliner-core.aar。iOS/鸿蒙通过各端现有桥接消费最新Core投影，不复制Android日期聚合到平台UI。

## 2. 路由与状态

| 目的地 | 路由域/入口 | 数据上下文 |
| --- | --- | --- |
| 根看板 | 主宿主总览或OverviewDashboardScreen | rememberOverviewMonth默认设备本月，rememberSaveable保存月键 |
| 任务统计 | OverviewActivity.domainIntent(...,"tasks",month) | 当前全局统计；month不作为详情筛选 |
| 全部趋势 | "trends" | 当前OverviewUiState与FocusOverview，图表自身范围 |
| 月度复盘 | "review" | 强制latestCompletedOverviewMonth(LocalDate.now())，忽略历史月参数 |
| 专注管理 | "focus" | 全部已保存且未删除Task/Habit FocusSession |
| 归档 | ArchiveActivity.newIntent | 当前任务/习惯归档存量 |

旧OverviewActivity统计Intent回退到tasks；月份解析失败回退本月，未来月份夹到本月。日期点击使用弹窗摘要。选中月份和列表滚动由Compose状态管理，页面仍在导航栈时返回恢复；不承诺跨杀进程持久化所有滚动状态。

## 3. 共享投影合同

Core OverviewViewModel.loadNow()提供OverviewUiState：summary/history/completionBuckets/daily/monthly/weekly/contribution/lastMonth。loadMonthNow(monthStart)提供OverviewMonthUiState：monthStart、endDateExclusive、asOfDate、timeZoneId、completedCount、daily、review、archivedTaskCount、archivedHabitCount、statusDistribution。monthStart必须为1日，未来月不可查询。

statusDistribution类型OverviewHistory，字段completed/pending/overdue/abandoned。Core过滤已删除与未来完成时间记录；completed由所选月daily完成数求和。其余仅选dueDate在月内的记录：先排除已完成和放弃，再按dueAt<now分类逾期，dueAt>=now分类待办。放弃包括ABANDONED/ABANDONED_ARCHIVED，完成包括COMPLETED/ARCHIVED。时间戳按现有Core兼容解析，无法确定月份的时间戳不凭空分配。

OverviewPeriodComparison对月daily日期映射求同期间总和：本月截至asOfDate日；历史月截至该月末；上月上限min(截止日,上月长度)。percentage=(current-previous)*100.0/previous；previous为0则null并用项数变化。此辅助函数只计算比较值，不重新定义Core任务状态。数值与饼图的completed一致。

月报review由既有OverviewLastMonth构建：指标按dueDate所属月份，活动按completedDate。保留totalTasks/completedTasks/completionRate/overdueTasks/mostActivePeriod/averageCompletionMinutes及前月比较字段。既有overdueTasks计算为该到期月未完成的任务数量，含放弃；不要将其替换成statusDistribution.overdue。最活跃时段与平均完成耗时只使用该到期月已完成记录的可解析时间；各端保持同一投影。

专注月图用database.focusStatistics.snapshot(FocusStatisticsQuery(FocusStatisticsDateRange(start,endExclusive)))，归档计数为当前非删除ARCHIVED或ABANDONED_ARCHIVED任务及ARCHIVED习惯。主看板趋势使用loadNow的最近7天daily与FocusOverview.daily末7天，不受月选择影响。

## 4. 加载、失效与错误

OverviewDashboardState.rememberMonthlyProjection以Lifecycle.STARTED订阅database.observeOverviewChanges原始失效，不依赖另一个领域的去重结果。查询放在Dispatchers.IO，返回DashboardLoad(data,loading,failed)。每30秒检测本地日期/ZoneId变化并重新订阅，适配午夜和时区变更；后台停止订阅，回前台重新加载。

不同模块独立显示加载/失败/重试，单个专注失败不屏蔽任务数据。取消异常继续抛出，其余查询失败转换状态；无数据用真实零值、空图与空记录提示。iOS/鸿蒙采用各端等价生命周期和数据库失效机制，不照搬30秒轮询为必须API。

## 5. 按月 AI 本地归档

MonthlyInsightState仅以snapshot.lastMonthKey和显式retry为生成effect键。应用单例MonthlyInsightRepository负责以下顺序：

1. 校验yyyy-MM，拒绝当前/未来月（即使显式retry）。
2. 返回该月合法非空缓存；成功缓存不可通过重试覆盖。
3. 在Mutex保护下复用同月未完成Deferred，合并根卡/详情请求。
4. 已尝试且非显式重试时返回不可用；首次/显式重试先落盘尝试记录。
5. 由应用SupervisorJob+IO生成，返回month匹配且summary非空才保存成功结果；页面取消不取消应用生成job。
6. 错误返回不可用，只有显式重试重新请求。

MonthlyInsightPreferences存于SharedPreferences "overview_monthly_reviews"："analysis/yyyy-MM"保存Gson MonthlyAnalysisResult(month,summary,keywords)，"attempted/yyyy-MM"为Boolean；commit在IO执行。旧GlobalUtils.OverviewSettings.monthlyAnalysisJson仅在月份已结束且新缓存缺失时迁移。存储失败由UI转不可用，不能呈现保存成功。

AIUtils.generateMonthlyAnalysis输入monthKey、monthName、metricsSummary、completedTaskNames；生成只在实际请求路径发生。已成功缓存不因数据修改失效，指标仍可更新，必要时产品另立重新生成策略。设备存储被清除后缓存不保留；v1无跨设备AI缓存同步。各端使用自身安全存储/持久化能力实现同合同，避免误把调用次数限制只放在页面remember中。

## 6. 专注管理用例

FocusManagementState先取database.focusSessions.list，过滤isDeleted，按startedAt降序，全部记录可达。今天按设备本地startedAt日期求durationSeconds与次数；累计含全部非删除保存记录。分配图用记录最早日期到max(今天,记录最晚日期)+1日的FocusStatisticsQuery，包含任务/习惯及全部目标/分类。

更正复用KmpV6Runtime.focusTimerViewModel.adjustSessionDurationNow，输入限制1..min(session.durationSeconds, endedAt-startedAt, MAX_ADJUSTED_DURATION_SECONDS)；删除复用deleteSessionNow软删除。失败保留编辑/确认弹窗和输入；成功关闭并通过原始失效刷新所有投影。不把删除专注当成撤销习惯打卡，不拓展计量、同步或时长上调规则。

## 7. UI组件与沉浸合同

DashboardDataCard统一卡片语义和主题：16dp紧凑内边距、8dp内容间隔；空value/supporting不产生占位。OverviewTrendPreviews两列各72dp高，任务短柱和专注浅填充曲线；独立缩放、无轴，语义说明保留。OverviewTaskStatusChart绘制88dp实心饼与两列图例；零值为中性圆。OverviewReviewSection完整bodyMedium正文+FlowRow关键词，无maxLines/ellipsis。其他尺寸见ui-design.md。

TopAppBar把subtitle转发到原生MiuixTopAppBar/SmallTopAppBar，不能另放固定Text在顶栏之外。月份按钮使用普通IconButton，48dp点击区/4dp间隔及symbolPainterResource；字体型Material Symbols drawable必须加入scripts/material_symbols_map.json并用generate_material_symbol_drawables.py生成注册表，禁止直接painterResource加载字体XML。

独立页以SettingsComponents.CollapsingTopBarScaffold为模板：AdaptiveMaterialScaffold、contentWindowInsets=0、useCurrentTopBarHeightForContentPadding=true、同一ScrollBehavior的nestedScroll和syncTopBarMaterialScroll。内容上边距使用当前顶栏实测高度；导航栏inset仅加入列表bottom padding。实体页面surface与backdrop捕获一致，顶/底chrome不进入自身采样层。

OverviewActivity在create/resume/configuration/multiwindow调用enableEdgeToEdgeForAllDevices。主宿主保留现有浮动导航与独立强调动作，内容末端避让LocalNavigationOverlayContentPadding。iOS/鸿蒙对应本端标准Scaffold/标题折叠、安全区与系统栏API，检查相同最终行为，不复制Android API。

## 8. 三端实施顺序与验收

先消费共享月状态、日期范围与失效合同，再实现根看板/四个独立统计管理路由，接入月AI存储和原分享/归档，最后验收标题折叠/系统栏及字体主题。使用spec.md逐条验收，不再恢复三个Tab。历史任务、专注与归档功能均有新入口；习惯总览另立需求。

关键自动验证：同期间截止、短月份/零基数/跨年；月状态互斥、归档/删除、时间边界；AI当前月门禁、重启缓存、失败显式重试、并发合并与调用方取消；专注更正不能上调、删除移出累计。视觉验证覆盖320dp可用宽度、普通手机、字体>1.8、亮/暗、滚动0/中间/收起、手势/按钮导航及全局专注覆盖层。

Android历次验证记录见tasks.md。已通过Core OverviewViewModelTest 4项、专注Core用例测试；Android同期/AI缓存8项、专注投影3项及图标注册测试。当前定版仅改趋势展示，执行assembleDebug、文档/原型静态校验；不新增镜像UI测试。实机标题折叠/沉浸视觉尚待复核，未运行模拟器。原型不是真实AI或设备截图，示例操作只修改内存，固定示例日期不作为应用默认月实现。
