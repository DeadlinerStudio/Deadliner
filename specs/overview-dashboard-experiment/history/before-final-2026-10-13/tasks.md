# 月度总览看板 Tasks

## 阶段 1：规格冻结
- [x] 用户确认主次看板、月度日历和领域拆分。
- [x] 明确旧功能映射、归档当前存量、将来习惯可扩展。

## 阶段 2：基础设施
- [x] KMP 月度任务投影及合同测试。
- [x] Android 分领域状态与月份路由。

## 阶段 3：功能实现
- [x] 月历热力图和主次卡片。
- [x] 任务分析去除专注混排，保留月报与导出。
- [x] 独立专注统计，现有归档接线。

## 阶段 4：验证与回写
- [x] 一次实现批次后编译和相关单元测试。
- [x] 文件规模/规格/差异静态校验。
- [x] 原型同步到实际界面与交互，保存至产品 PRD。
- [ ] 设备视觉验收：用户明确不需要模拟器，本轮不运行。

## 验证记录
Android assembleDebug 成功；Core OverviewViewModelTest 3 项通过。修改的 Kotlin 文件均不超过 1000 有效行。未运行设备/模拟器及视觉验收；原型与交接文档保存到产品 PRD。

## 界面修订
- [x] 修正双卡宽度阈值与顶栏月状态。
- [x] Material Symbols 与月度图表。
- [x] 任务统计页面背景修复及构建。
- [x] 更新 PRD 原型。

## 图标崩溃修复
- [x] 将总览月份左箭头加入 Material Symbols 生成清单并重新生成注册表。
- [x] 增加全部字体 Drawable 与 Compose 注册表的一致性回归测试。
- [x] MaterialSymbolRegistryTest 通过（覆盖 133 个字体图标），assembleDebug 成功；未运行模拟器。

## 信息架构再拆分
- [x] 根看板及任务/趋势/复盘独立路由。
- [x] 专注管理记录更正/删除与响应式统计。
- [x] 二级页 Scaffold/inset/E2E 对齐。
- [x] 构建、针对性验证、PRD/原型回写（不运行模拟器）。

## 最终验证（信息架构修订）
Android assembleDebug 成功；FocusManagementStateTest 3 项与 MaterialSymbolRegistryTest 1 项通过（涵盖 133 个字体图标）；Core FocusTimerViewModelTest 2 项、FocusTimerIntegrationTest 9 项通过，包括只能调低与删除移出累计。资源 XML、规格和文件规模检查通过。浏览器检查独立任务/趋势入口、专注记录更正后的统计联动；原型数字/文案为示例。不运行模拟器，实机 E2E 与顶部折叠视觉尚待复核。

## 月度复盘卡视觉对齐
月度复盘不再采用外置标题加自定义卡片，复用趋势卡的 DashboardDataCard：16dp 内边距、8dp 内容间距、titleMedium 卡片标题、titleLarge「AI 月度洞察」、bodySmall 月份及同色 Material Symbols 箭头；摘要保留四行截断，加载/失败/重试和导航行为保留。

本次视觉对齐：assembleDebug 成功（55 秒），规格及原型语法检查通过；未运行模拟器，未进行实机视觉验收。

## 紧凑看板与持久化复盘
- [x] 月切换去玻璃、默认本月和紧凑日历。
- [x] 任务同期间比较及任务/专注微型图。
- [x] 复盘固定已结束月份、按月缓存及单次分析。
- [x] 业务边界测试、构建及 PRD/原型同步。

本次验证：OverviewPeriodComparisonTest 4 项、MonthlyInsightRepositoryTest 4 项全部通过；最终 assembleDebug 成功。覆盖同期间截止日、短月份、零基数、跨年、已结束月门禁、按月保存、失败显式重试及并发/离页不中断。资源 XML、文件规模和 diff whitespace 检查通过。原型浏览器检查紧凑日历、微型图表及 10 月总览进入固定 9 月复盘；PRD 与原型同步。不运行模拟器，Android 实机视觉未验收。

## 去冗余与状态饼图
- [x] 删除三张卡描述和专注图，任务卡更名/状态饼图。
- [x] TopAppBar 原生 subtitle。
- [x] Core 状态边界验证、Android 构建及 PRD/原型同步。

本轮验证：Core OverviewViewModelTest 共 4 项通过（含新月状态测试）；Android assembleDebug 成功。原型检查无描述双卡、四色任务饼图及任务入口；JS 语法、XML、文件规模与 diff whitespace 检查通过。未运行模拟器，顶部折叠的实机外观尚未验收。

## 完整洞察展示
- [x] 删除重复标题与四行截断，同步原型/PRD 并构建验证。

完整洞察修订：assembleDebug 成功（47秒）；原型 JS 语法和 diff whitespace 检查通过。不运行模拟器，本次纯展示调整不新增测试。
