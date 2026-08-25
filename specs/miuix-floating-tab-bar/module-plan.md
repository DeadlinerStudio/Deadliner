# MIUIX Floating Tab Bar Module Plan

## 模块拆分

- `contract`：`MiuixFloatingTabItem` 定义稳定的 key、标签及选中/未选中图标。
- `presentation`：`MiuixFloatingTabBar` 负责布局、选中胶囊动画、交互语义和 MIUIX 中性色。
- `shadow`：外层容器在 presentation 内使用不参与测量的统一 ambient/spot 黑色阴影配方，不侵入玻璃材质 recipe 或布局 contract。
- `material`：共享 `ui/material/glass` 模块适配 `AdvancedMaterialSpec`、`LayerBackdrop`、API 33+ 单链模糊透镜与无 backdrop 降级背景；导航组件只选择 recipe。
- `preview`：独立 Preview 文件构造示例导航项、背景内容和浅深色主题，不进入生产调用链。
- `preference`：`AppearancePreferences` 与 `GlobalUtils` 负责开关默认值、持久化与响应式更新。
- `adaptive integration`：`AdaptiveNavigationSuiteScaffold` 负责 Compact 宽度判定、Backdrop 宿主和旧导航回退；`ModernHost` 仅传入设置值。

## 平台映射

- iOS：本阶段不修改；视觉评审通过后再映射 SwiftUI 底部 Tab 容器。
- HarmonyOS：本阶段不修改；视觉评审通过后再映射 ArkUI/MIUIX 风格组件。
- Android：落在 `app/src/main/java/com/aritxonly/deadliner/ui/navigation/`，保持为展示层组件。
- Android 接入：设置入口落在 `ui/settings/DesignColorSettings.kt`；自适应选择落在 `ui/base/AdaptiveNavigationSuiteScaffold.kt`。

## 文件拆分策略

- 单个核心文件尽量不超过 1000 行有效代码
- 若必须逃逸，先申请开发者批准
- 生产组件与 Preview 分文件，避免预览示例、背景搭建和正式 API 混杂。
- 不改动已有体量较大的 `AdaptiveNavigationSuiteScaffold.kt`；实际接入另开一阶段。
- 接入阶段只在现有 scaffold 中增加独立 Compact 浮层分支，不重写 Legacy/Advanced/Wide 分支。

## 风险点

- `textureBlur` 只有在内容已注册到同一个 `LayerBackdrop` 时才能正确采样。
- 等分 Tab 宽度会使长本地化标签受限，需使用省略号并由调用方控制合理项数。
- MIUIX 与 Material 3 同时提供主题 token；组件必须显式从 MIUIX scheme 取容器色，同时保留 Compose 文字排版能力。
- Compact 浮层不参与 Scaffold 的 bottomBar 测量，内容和 FAB/snackbar 必须预留安全抬升距离。
