# 沉浸体验 Module Plan

## 模块拆分

- `glass primitive`：保留 `deadlinerGlassMaterial` 作为光学渲染器；新增无尺寸约束的 `SoftGlassSurface` 和可复用圆形 `SoftGlassIconButton`，负责主题与 Backdrop 解析。
- `navigation motion`：独立封装 Tab 连续位置、按压进度、拖动速度、目标索引解析和外部选中态同步规则。
- `floating navigation presentation`：`MiuixFloatingTabBar` 只负责布局、指针事件、选择胶囊变换与 Tab 内容。
- `immersive top bar`：由主题提供全局沉浸开关，通用 `TopAppBar` 自己绘制渐进模糊背景；Large 使用折叠状态，Small/Center 使用 Scaffold 的滚动距离观察器，页面也可提供显式目标进度。
- `top bar material motion`：纯函数负责将折叠比例或累计滚动距离映射为目标进度，公共顶栏统一执行 alpha 动画；顶栏背景通过固定半径 Backdrop blur + RuntimeShader alpha/tint 渐隐，并在其上绘制独立的轻量 Surface 透明度渐变，按钮独立使用完整 Backdrop 柔光玻璃。
- `appearance contract`：将旧悬浮底栏布尔值升级为沉浸体验布尔值，提供 SharedPreferences 向后迁移。

## 平台映射

- Android 数据/契约：`model/AppearancePreferences.kt`、`localutils/GlobalUtils.kt`。
- Android 通用材质：`ui/material/glass/SoftGlassSurface.kt`、`ui/material/glass/ProgressiveTopBarMaterial.kt`；前者拆分材质背景与前景，后者承载全局开关和折叠进度上下文。
- Android 底栏：`ui/navigation/FloatingTabMotion.kt`、`ui/navigation/MiuixFloatingTabBar.kt`。
- Android 全局接入：`ui/theme/Theme.kt`、`ui/base/TopAppBar.kt`、`ui/base/TopBarMaterialScrollObserver.kt`、`ui/base/AdaptiveMaterialScaffold.kt` 与 `ui/base/AdaptiveNavigationSuiteScaffold.kt`；主界面只负责提供附件栏折叠目标，不再局部包裹渐进材质或执行重复动画。
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
- 顶栏渐进 Backdrop 不能包住 content，否则标题和按钮会随背景一起淡出；它只能使用固定轻量 Blur + Shader mask，不得调用高级材质或柔光玻璃折射。按钮仍独立使用完整玻璃效果链。
- 渐进背景与按钮可以读取同一个 `LayerBackdrop`，但 Scaffold 只能注册一次内容捕获；缺少 Backdrop 或 RuntimeShader 时必须稳定降级为普通 Surface 渐变。
- 额外 Surface 遮罩只属于真实 Backdrop 分支；降级分支本身已是 Surface 渐变，不能重复叠加造成过度不透明。
- `TopAppBar` 与 Scaffold 不得同时绘制高级材质；沉浸模式下 Scaffold 关闭旧的整块顶栏材质容器。
- MIUIX 与 Material3 的滚动状态类型不同，但都暴露 `collapsedFraction`；映射函数需保持平台无关并可单元测试。
- 顶栏 action 可能包含文字按钮；本阶段只把明确的图标按钮接入 `SoftGlassIconButton`，不强行把文字按钮压入圆形。
- Small/Center 没有可用的 `collapsedFraction`；滚动观察器只能观察 nested-scroll 消耗量，不得消费事件或改变页面原有滚动物理行为。
- 参考仓库许可证为 AGPL-3.0，Deadliner 为 MIT；实现只复刻交互目标和数学行为，不移植源文件或其辅助类。
