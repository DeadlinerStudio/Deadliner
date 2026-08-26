# 沉浸体验 Module Plan

## 模块拆分

- `glass primitive`：保留 `deadlinerGlassMaterial` 作为光学渲染器；新增无尺寸约束的 `SoftGlassSurface` 和可复用圆形 `SoftGlassIconButton`，负责主题与 Backdrop 解析。
- `soft glass shadow`：通用双层零偏移 DropShadow Token 负责轻量四向弥散和低 alpha 近缘轮廓；FAB、圆形按钮、顶部文字 Tab 与悬浮底栏统一消费，材质渐显按钮可传递 shadow alpha。实现不使用 elevation GraphicsLayer，避免自身边界裁切与屏幕位置相关的光源偏移。
- `navigation motion`：独立封装 Tab 连续位置、按压进度、拖动速度、目标索引解析和外部选中态同步规则。
- `floating navigation presentation`：`MiuixFloatingTabBar` 只负责布局、指针事件、选择胶囊变换与 Tab 内容。
- `floating navigation dock`：独立组件负责将主 Capsule 与右侧搜索玻璃按钮横向编排；Scaffold 只负责从通用导航项中拆出搜索项并映射回路由回调。
- `soft glass interaction`：通用圆形玻璃按钮统一管理无 Ripple 点击、按压进度、全区域高亮和缩放，顶栏 action 与底部搜索按钮直接复用；任意 navigationIcon 宿主使用同一视觉参数观察子点击手势。
- `immersive FAB`：58dp 彩色柔光玻璃 FAB 复用通用图标玻璃按钮的按压状态和 Backdrop 光学链；沉浸 Compact Scaffold 负责主页居中定位，Detail 页在沉浸模式下以 `primary/onPrimary` 接入同一组件。
- `immersive top bar`：由主题提供全局沉浸开关，Scaffold 的完整 topBar slot 实测状态栏、标题内容与可选顶部 Tab 后统一绘制渐进模糊；通用 `TopAppBar` 只保留无宿主回退。Large 使用折叠状态，Small/Center 使用 Scaffold 的滚动距离观察器，页面也可提供显式目标进度。
- `top bar material motion`：纯函数负责将折叠比例或累计滚动距离映射为目标进度，并计算“实测顶栏占用高度 / 0.75 - 6dp”的材质覆盖高度；顶栏背景通过固定半径 Backdrop blur + RuntimeShader alpha/tint 渐隐，并叠加顶部 0.94 到 68% 归零的 Surface 透明度渐变，按钮独立使用完整 Backdrop 柔光玻璃。
- `immersive navigation icon contract`：通用导航项保留 selected/unselected 图标供非沉浸导航使用，并可额外提供单一 immersive 图标；沉浸 Scaffold 映射时只消费该图标，避免 fill 或透明度变化重复表达选中态。
- `appearance contract`：将旧悬浮底栏布尔值升级为沉浸体验布尔值，提供 SharedPreferences 向后迁移。
- `Lifi route placeholder`：`MainSection` 提供稳定的 Lifi 状态；Modern Host 只渲染空内容并隐藏标题栏/FAB，不提前引入 Lifi 业务依赖。
- `immersive text tabs`：独立 `ImmersiveTextTabRow` 管理 42dp 双 Capsule、文字状态、选择位置动画与无 Ripple 点击；公共 `TabRow` 只负责按主题/沉浸状态路由到新旧实现。

## 平台映射

- Android 数据/契约：`model/AppearancePreferences.kt`、`localutils/GlobalUtils.kt`。
- Android 通用材质：`ui/material/glass/SoftGlassSurface.kt`、`ui/material/glass/SoftGlassFloatingActionButton.kt`、`ui/material/glass/ProgressiveTopBarMaterial.kt`；前两者拆分通用玻璃容器、图标按钮与彩色 FAB，后者承载全局开关和折叠进度上下文。
- Android 底栏：`ui/navigation/FloatingTabMotion.kt`、`ui/navigation/MiuixFloatingTabBar.kt`、`ui/navigation/MiuixFloatingNavigationDock.kt`。
- Android 顶部 Tab：`ui/navigation/ImmersiveTextTabRow.kt` 提供沉浸样式，`ui/base/TabRow.kt` 保持跨设计系统入口。
- Android 全局接入：`ui/theme/Theme.kt`、`ui/base/TopAppBar.kt`、`ui/base/TopBarMaterialScrollObserver.kt`、`ui/base/AdaptiveMaterialScaffold.kt`、`ui/base/AdaptiveNavigationSuiteScaffold.kt` 与独立的 `ui/base/ImmersiveMiuixCompactNavigationScaffold.kt`；主界面通过 `ui/main/modern/ModernNavigationRouting.kt` 管理 section/key 映射，只负责提供页面状态和附件栏折叠目标。
- Android 设置：`ui/settings/DesignColorSettings.kt` 与中英文字符串资源。
- iOS/HarmonyOS：本阶段不改代码；通用分层保持“材质 primitive / 导航 motion / feature host”边界，便于未来映射到 SwiftUI Material 与 ArkUI effect。

## 文件拆分策略

- 不向已超过或接近阈值的通用 UI 文件加入运动算法或玻璃渲染细节。
- `SoftGlassSurface.kt` 只负责高级封装，不迁移底层 Shader，以避免 `GlassMaterialModifier.kt` 继续增长。
- `FloatingTabMotion.kt` 提供纯函数和 Compose motion state；可测试的索引解析不得留在 PointerInput lambda 内。
- `ProgressiveTopBarMaterial.kt` 同时定义无布局增高的测量宿主、渐进背景与沉浸顶栏 CompositionLocal，Scaffold 只调用宿主而不承载材质算法。
- `MiuixFloatingTabBar.kt` 若重构后仍明显增长，则将 Tab item presentation 再拆为独立文件。
- `AdaptiveNavigationSuiteScaffold.kt` 只保留导航模式路由；沉浸 Compact Scaffold、搜索拆分、Dock 布局与项目映射放入独立文件，按压视觉继续归属玻璃组件。

## 风险点

- Kyant continuous `Capsule` 的 Path 与 Shader 圆角半径必须保持视觉兼容；Shader 仍按半高解析几何，轮廓由 Shape 裁切。
- 使用 VelocityTracker 时需同时处理 RTL 方向和取消手势，取消后必须回弹到业务选中项。
- 顶栏渐进 Backdrop 不能包住 content，否则标题和按钮会随背景一起淡出；它只能使用固定轻量 Blur + Shader mask，不得调用高级材质或柔光玻璃折射。按钮仍独立使用完整玻璃效果链。
- 渐进背景与按钮可以读取同一个 `LayerBackdrop`，但 Scaffold 只能注册一次内容捕获；缺少 Backdrop 或 RuntimeShader 时必须稳定降级为普通 Surface 渐变。
- 额外 Surface 遮罩只属于真实 Backdrop 分支；降级分支本身已是 Surface 渐变，不能重复叠加造成过度不透明。
- `TopAppBar` 与 Scaffold 不得同时绘制高级材质；沉浸模式下 Scaffold 关闭旧的整块顶栏材质容器。
- MIUIX 与 Material3 的滚动状态类型不同，但都暴露 `collapsedFraction`；映射函数需保持平台无关并可单元测试。
- 顶栏 action 可能包含文字按钮；本阶段只把明确的图标按钮接入 `SoftGlassIconButton`，不强行把文字按钮压入圆形。
- Small/Center 没有可用的 `collapsedFraction`；滚动观察器只能观察 nested-scroll 消耗量，不得消费事件或改变页面原有滚动物理行为。
- 参考仓库许可证为 AGPL-3.0，Deadliner 为 MIT；实现只复刻交互目标和数学行为，不移植源文件或其辅助类。
- Kyant Shapes `Capsule` 为 Apache-2.0，可作为独立依赖使用；新增依赖需验证与当前 Compose/Kotlin 版本的二进制兼容性。
- “无主栏选择”必须作为显式展示状态传入 TabBar，不能通过伪选中第一个 enabled 项实现；否则搜索激活时仍会残留选择胶囊和选中文字。
- MIUIX Scaffold 不提供与 Material3 一致的 FAB position 透传，沉浸 Compact Scaffold 需在内容覆盖层自行定位居中 FAB，并确保它不进入 Backdrop 捕获层形成循环采样。
- Lifi 占位保持为导航契约的一部分，但当前不依赖 Lifi runtime、ViewModel 或业务模块；空页面不得制造未来需要迁移的临时状态。
- 顶部 Tab 的位置动画与视觉 Token 放入新组件，不能继续扩张 OverviewActivity 或 AddDDLActivity；调用方原有 tabs/index/callback 数据契约保持不变。
- 渐进材质宿主的额外消散尾部必须以 overflow 绘制，布局报告高度只能等于实际 topBar slot 高度，否则内容 Padding 会被重复放大。
- `AdaptiveMaterialScaffold` 必须把已注册的页面 Backdrop 同时提供给沉浸 FAB，避免 Detail 页玻璃 FAB 因离开 content 捕获作用域而退化。
