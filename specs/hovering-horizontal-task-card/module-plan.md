# 带悬停的横向滑动任务卡片 Module Plan

## 模块拆分

- 滑动规则：方向、阈值、落点和动作决策，保持为无 Compose 依赖的 Kotlin 逻辑，便于单元测试。
- 展示组件：处理拖拽、动画、双侧按钮、点击与长按协调。
- 任务接线：根据任务状态映射完成/恢复、放弃/归档、编辑、删除回调。

## 平台映射

- Android：`app/src/main/java/com/aritxonly/deadliner/ui/main/` 放置滑动规则与 Compose 组件；`ui/main/simplified/TaskItem.kt` 接入业务动作。
- iOS：参考 `Deadliner/Features/Home/Components/DDLItemCard.swift` 的双侧 `swipeActions` 语义，本次不修改。
- HarmonyOS：参考 `entry/src/main/ets/pages/components/TaskItemCard.ets` 的悬停阈值和动画，本次不修改。

## 文件拆分策略

- 从 `Components.kt` 移出 `DDLItemCardSwipeable`，避免核心文件继续越过 1000 行守线。
- 实际落位为 `SwipeableTaskCard.kt` 395 行、`TaskCardSwipeRules.kt` 62 行、`TaskSwipeActionColorFactory.kt` 71 行，`Components.kt` 从 1023 行降至 813 行。
- `TaskCardSwipeRulesTest.kt` 覆盖关闭态、展开态、全滑、展开态反向关闭与缺失尾部动作场景。

## 风险点

- Compose 横向手势需避免误触列表纵向滚动。
- 展开态点击、按钮点击、长按之间不能重复触发。
- 删除为破坏性业务动作，应复用项目已有确认/删除路径，不直接操作仓储。

## 实现结果

- 滑动参数与 HarmonyOS 对齐：最大偏移 `82%`、全滑阈值 `62%`、悬停位 `112dp`、悬停触发 `84dp`、反向关闭 `32dp`。
- 前景卡片使用布局偏移，露出的背景按钮可直接命中；超出悬停位后按钮间距和外侧操作槽增大，按钮本身保持圆形。
- 右滑提供完成/恢复与放弃/归档，左滑提供编辑与删除；删除保留确认弹窗。
- `TaskSwipeActionColorFactory.kt` 根据任务指示标风格产生语义按钮色，并按 WCAG 对比度选择黑/白图标色。
