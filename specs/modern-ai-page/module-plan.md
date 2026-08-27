# 现代模式 AI 页面 Module Plan

## 模块拆分

- AI 展示策略：声明 `Overlay` 与 `Page` 两种展示语义，以及关闭按钮、返回键和默认底部间距规则。
- AI 共享内容：继续由现有 `AIOverlay` 承载对话状态、光效、输入与工具卡片，仅消费展示策略。
- 现代模式集成：由 `ModernHost` 的 `MainSection.LIFI` 分支挂载页面入口，并根据底栏/侧栏布局传入底部间距。
- 回归验证：用纯 Kotlin 单元测试覆盖展示策略，用现有导航测试覆盖 AI 路由。

## 平台映射

- Android AI UI：`app/src/main/java/com/aritxonly/deadliner/ui/agent`
- Android 现代主界面：`app/src/main/java/com/aritxonly/deadliner/ui/main/modern`
- Android 单元测试：`app/src/test/java/com/aritxonly/deadliner/ui/agent` 与 `ui/main/modern`
- iOS / HarmonyOS：本次不改动。

## 文件拆分策略

- 新建小型页面入口/展示策略文件，避免继续扩张 `AIOverlay.kt` 的职责。
- `AIOverlay.kt` 仅增加策略参数及条件渲染，不复制业务逻辑。
- `ModernHost.kt` 仅替换 `LIFI` 空白占位和清理现代模式专属的旧 Overlay 接线。

## 风险点

- 底部导航采用多种自适应布局，需要区分紧凑底栏与宽屏侧栏的输入框间距。
- 页面态必须关闭内部 `BackHandler`，否则会吞掉系统返回事件。
- Overlay 默认参数必须保持原值，避免简洁模式发生视觉回归。
- 核心文件不得因本功能继续显著膨胀；若需要大规模拆分，应另立重构任务。

## 实际落位

- `AIAgentPage.kt`：页面入口、展示策略和导航间距规则。
- `AIOverlay.kt`：消费展示策略，保留唯一的 AI 内容实现。
- `ModernHost.kt`：选择页面入口并提供当前自适应导航的覆盖关系。
- `AIAgentPageTest.kt`：覆盖 Overlay/Page chrome 和输入间距策略。

`AIOverlay.kt` 在本功能前已为 1356 行，本次策略接入后为 1366 行。新增页面职责已放入 61 行的独立文件，未继续把现代导航判断并入该大文件；对既有大文件的进一步拆分不纳入本次低风险集成。
