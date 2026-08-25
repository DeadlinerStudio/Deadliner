# 沉浸体验 Module Plan

## 模块拆分

- `glass primitive`：保留 `deadlinerGlassMaterial` 作为光学渲染器；新增无尺寸约束的 `SoftGlassSurface` 和可复用圆形 `SoftGlassIconButton`，负责主题与 Backdrop 解析。
- `navigation motion`：独立封装 Tab 连续位置、按压进度、拖动速度、目标索引解析和外部选中态同步规则。
- `floating navigation presentation`：`MiuixFloatingTabBar` 只负责布局、指针事件、选择胶囊变换与 Tab 内容。
- `immersive top bar`：由主题提供全局沉浸开关，通用 `TopAppBar` 自己绘制渐进模糊背景；按钮材质进度由折叠状态或页面级显式进度提供。
- `appearance contract`：将旧悬浮底栏布尔值升级为沉浸体验布尔值，提供 SharedPreferences 向后迁移。

## 平台映射

- Android 数据/契约：`model/AppearancePreferences.kt`、`localutils/GlobalUtils.kt`。
- Android 通用材质：`ui/material/glass/SoftGlassSurface.kt`、`ui/material/glass/ProgressiveTopBarMaterial.kt`；前者拆分材质背景与前景，后者承载全局开关和折叠进度上下文。
- Android 底栏：`ui/navigation/FloatingTabMotion.kt`、`ui/navigation/MiuixFloatingTabBar.kt`。
- Android 全局接入：`ui/theme/Theme.kt`、`ui/base/TopAppBar.kt`、`ui/base/AdaptiveMaterialScaffold.kt`；主界面只负责提供附件栏折叠进度，不再局部包裹渐进材质。
- Android 设置：`ui/settings/DesignColorSettings.kt` 与中英文字符串资源。
- iOS/HarmonyOS：本阶段不改代码；通用分层保持“材质 primitive / 导航 motion / feature host”边界，便于未来映射到 SwiftUI Material 与 ArkUI effect。

## 文件拆分策略

- 不向已超过或接近阈值的通用 UI 文件加入运动算法或玻璃渲染细节。
- `SoftGlassSurface.kt` 只负责高级封装，不迁移底层 Shader，以避免 `GlassMaterialModifier.kt` 继续增长。
- `FloatingTabMotion.kt` 提供纯函数和 Compose motion state；可测试的索引解析不得留在 PointerInput lambda 内。
- `ProgressiveTopBarMaterial.kt` 同时定义渐进背景容器与沉浸顶栏 CompositionLocal，避免将遮罩代码塞进 Scaffold。
- `MiuixFloatingTabBar.kt` 若重构后仍明显增长，则将 Tab item presentation 再拆为独立文件。

## 风险点

- MIUIX `SmoothCapsuleShape` 的 Path 与 Shader 圆角半径必须保持视觉兼容；Shader 仍按半高解析几何，轮廓由连续 Shape 裁切。
- 使用 VelocityTracker 时需同时处理 RTL 方向和取消手势，取消后必须回弹到业务选中项。
- 顶栏背景的 alpha mask 不能包住 content，否则标题和按钮会随模糊一起淡出。
- `TopAppBar` 与 Scaffold 不得同时绘制高级材质；沉浸模式下 Scaffold 关闭旧的整块顶栏材质容器。
- MIUIX 与 Material3 的滚动状态类型不同，但都暴露 `collapsedFraction`；映射函数需保持平台无关并可单元测试。
- 顶栏 action 可能包含文字按钮；本阶段只把明确的图标按钮接入 `SoftGlassIconButton`，不强行把文字按钮压入圆形。
- 参考仓库许可证为 AGPL-3.0，Deadliner 为 MIT；实现只复刻交互目标和数学行为，不移植源文件或其辅助类。
