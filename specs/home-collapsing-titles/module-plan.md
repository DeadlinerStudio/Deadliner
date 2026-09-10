# 主页统一折叠标题 Module Plan

## 模块拆分

1. `ui/base/TopAppBar.kt` 的既有 `miuixScrollBehavior` 参数作为统一顶栏能力。
2. `ui/main/modern/ModernHost.kt` 为概览和收集页持有滚动行为并连接内容。
3. `OverviewActivity.kt` 与 `capture/ui/CaptureScreen.kt` 为独立页面复用同样行为。

## 平台映射

- Android：上述 Compose 文件。
- iOS/HarmonyOS：本次不涉及；两端不使用这些 Android Activity/Compose 容器。

## 文件拆分策略

- 不新增页面组件；只在现有顶栏的公开参数中传递滚动行为，避免复制顶栏实现。
- 不向现有大型内容文件添加业务逻辑。

## 风险点

- 概览分页和收集多选必须保留各自当前状态；多选仅使用小标题操作栏。
