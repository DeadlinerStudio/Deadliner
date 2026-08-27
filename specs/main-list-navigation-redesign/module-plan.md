# 主清单导航重构 Module Plan

## 模块拆分

- `list chrome policy`：纯函数决定 MIUIX 标题折叠资格、沉浸搜索入口可见性、搜索激活时底栏选择语义，避免条件散落在 Host。
- `list top app bar`：专用 MIUIX TopAppBar 负责大标题、左中右固定行、头像/设置动作和 `ImmersiveTextTabRow`；对外只接收页面类型、选择回调和 `ScrollBehavior`。
- `list search entry`：MIUIX TopAppBar `bottomContent` 中的可折叠 SearchBar 入口，负责上报共享边界并触发展开，不持有搜索结果状态。
- `list chrome scroll coordinator`：在 SearchBar 槽与 MIUIX ScrollBehavior 之间分配 nested scroll；上滑先搜索后标题，下滑先标题后搜索，并负责半程手势的稳定归位。
- `search transition host`：共享转场作用域与展开搜索页，管理搜索胶囊容器变换、查询输入、Scope、返回关闭和结果内容显隐。
- `search transition host` 的 SearchBar 共享边界使用 `scaleToBounds`：完整搜索布局保持稳定测量，只缩放至动画边界，禁止用固定动画约束逐帧重测复杂 SearchBar。
- `chromatic glass tint`：柔光玻璃材质的显式 tint 策略；中性表面沿用现有合成，普通 Chromatic 保留当前策略，有色 FAB 使用独立 `SeedChromatic` profile，以原 seed RGB 和透明覆盖同时保留颜色与折射。
- `top-tab material state`：文字 Tab 通过统一 material progress 在半透明 container 与柔光玻璃之间过渡；沉浸关闭时锁定 container 降级态。
- `search transition presentation`：搜索入口自定义低对比提示文字，共享边界使用顶对齐 FillWidth 缩放，避免对完整搜索页做纵向非等比拉伸。
- `immersive navigation action`：通用沉浸 Dock 支持“从 items 拆出一个位置，但用独立 action model 呈现”，本轮以彩色新增按钮替换搜索目的地。
- `host integration`：`ModernHost` 组装现有 ViewModel、ActivityResult launcher、列表状态和各模块；不承载 MIUIX 组件细节。
- `theme bridge`：在不改变 `LocalAppDesignSystem` 的前提下，为 Vivid 提供 MIUIX ColorScheme 上下文，使专用 MIUIX 主清单标题栏可稳定渲染。
- `MIUIX preset scheme`：保存亮暗原始 seed，并调用 Material Kolor 2021 TonalSpot 生成完整 Material role；最终只替换 primary 和白色 onPrimary，container 及其他字段逐项保持生成值。
- `home FAB color policy`：纯函数把 Preset、用户 SeedColor 与 SystemDynamic 映射为统一 container，并固定白色 content，供常规 FAB 与沉浸独立动作共同消费。
- `appearance preference`：持久化默认开启的撞色主页按钮，仅在 Modern + Preset 的设计与色彩页暴露，同时保持跨模式选择。
- `overlay color policy`：基础 Dialog 与 Sheet 共同解析当前设计系统的 `surfaceContainer`，隔离 MIUIX 纯黑 window background 与浮层容器语义。
- `regular navigation icon scale`：普通导航项目拥有独立视觉 scale；灵感资源和 AI 放大不传入沉浸 Capsule。
- `immersive icon assets`：沉浸 Home 独立承载 MIUIX `Home.Light` 路径和 1.0 visual scale；Capture 仅通过同色描边提高视觉重量，不与普通导航资源耦合。
- `unified soft-glass shadow`：`SoftGlassShadow` 只暴露一套中等扩散 token，Icon Button、顶部文字 Tab、底栏与 FAB 不再选择不同 style。
- `search page chrome`：通用 `MainSearchBar` 负责 expanded 后的焦点/IME 生命周期，并为搜索页暴露可选归档动作；Host 只负责启动现有归档 Activity。

## 平台映射

- Android：实现本规格全部模块。
- HarmonyOS：参考 `MainHomePage.ets` 的 FREE 标题模式、任务 Scroller/习惯 dummy Scroller、左侧分类按钮、中部 Segment 与右侧 Avatar；本轮不改 ArkTS。
- iOS：参考 `RichMainTabViews.swift`/`HomeView.swift` 的 leading filter、principal task/habit picker、trailing avatar/settings；本轮不改 SwiftUI。
- 跨端共同语义：任务允许标题折叠且拥有可滚动搜索入口；习惯保持标题展开；筛选居左、页面切换严格居中、账户入口居右。

## 预期文件

- 新增 `ui/main/modern/ModernListChromePolicy.kt`：纯状态规则。
- 新增 `ui/main/modern/ModernListTopBarScrollCoordinator.kt`：两阶段 nested scroll 状态、连接与纯函数规则。
- 新增 `ui/main/modern/components/ModernListTopAppBar.kt`：专用 MIUIX 标题栏。
- 新增 `ui/main/modern/components/ImmersiveListSearch.kt`：搜索入口与共享容器转场。
- 修改 `ui/main/shared/MainDisplay.kt`：移除本轮临时增加的任务列表首部 slot，列表重新只承载数据内容。
- 修改 `ui/main/shared/MainDisplay.kt`：接收可选列表级 NestedScrollConnection，并把它挂在 LazyColumn 与 PullToRefreshBox 之间，保证 Chrome 的下拉优先级。
- 修改 `ui/base/AdaptiveNavigationSuiteScaffold.kt`、`ImmersiveMiuixCompactNavigationScaffold.kt`：传递可选沉浸独立动作。
- 修改 `ui/navigation/MiuixFloatingNavigationDock.kt`：右侧可呈现目的地或独立彩色 action。
- 修改 `ui/material/glass/SoftGlassFloatingActionButton.kt`：允许调用方覆写外部尺寸，以复用底栏圆钮高度。
- 修改 `ui/material/glass/GlassMaterialSpec.kt`、`GlassMaterialModifier.kt`：增加显式色彩 tint 策略及纯函数解析，供彩色 FAB 使用。
- 修改 `ui/base/TopBarMaterialProgress.kt`、`TopBarMaterialScrollObserver.kt`：收窄 Large/Small TopAppBar 的材质显现阈值。
- 修改 `ui/navigation/ImmersiveTextTabRow.kt`、`ui/base/TabRow.kt`：接入顶栏 material progress，增加非沉浸半透明 container 降级。
- 修改 `ui/main/modern/components/ImmersiveListSearch.kt`：拆分入口 hint presentation 并收敛共享容器动画。
- 新增 `res/drawable/ic_plus_imm.xml`：沉浸底栏新增动作专用图标。
- 修改 `ui/material/glass/ProgressiveTopBarMaterial.kt`：以独立 24dp token 收短材质尾部全透明区。
- 修改 `ui/material/glass/SoftGlassShadow.kt`、`SoftGlassSurface.kt` 与顶栏按钮入口：增加仅供小型 TopAppBar 圆钮使用的紧凑阴影风格。
- 修改 `ui/detail/DetailPlanFloatingActionButton.kt`：移除沉浸分支，恢复统一 Material3 FAB。
- 修改 `ui/detail/DetailPlanFloatingActionButton.kt`：显式传入 primaryContainer/onPrimaryContainer，防止组件默认 token 变化后破坏 Detail role 语义。
- 修改 `ui/theme/Theme.kt`：为 Vivid 提供 MIUIX Theme bridge。
- 修改 `ui/theme/MiuixPresetColorSchemes.kt`、`HomeFabColors.kt`、`Theme.kt`：Material 派生 preset role，并把原始 seed 注入主页 FAB 颜色策略。
- 修改 `model/AppearancePreferences.kt`、`localutils/GlobalUtils.kt`、`ui/settings/DesignColorSettings.kt`：增加“撞色主页按钮”偏好、条件展示及 section 排序。
- 新增 `ui/base/OverlayContainerColors.kt` 并修改基础 `AlertDialog` 与 Bottom Sheet 调用点：统一消费 `surfaceContainer`。
- 修改 `ui/base/AdaptiveNavigationSuiteScaffold.kt`、`ui/main/modern/ModernHost.kt`：为非沉浸导航增加独立图标 scale，并拆分灵感的普通/沉浸资源。
- 修改 `res/drawable/ic_home_imm.xml`、`ic_capture.xml` 与 `ui/main/modern/ModernHost.kt`：接入 MIUIX Home.Light 并校准沉浸图标视觉重量。
- 修改 `ui/material/glass/SoftGlassShadow.kt`、`SoftGlassSurface.kt`、`ui/base/TopBarIconButton.kt`、`TopAppBar.kt` 与 `ui/navigation/ImmersiveTextTabRow.kt`：删除 shadow style 分叉并统一 token。
- 修改 `ui/main/simplified/MainSearchBar.kt` 与 `ui/main/modern/ModernHost.kt`：expanded 搜索自动聚焦 IME，并提供归档按钮回调。
- 修改 `ui/base/FloatingActionButton.kt`、`ui/main/modern/ModernHost.kt`：允许常规主页 FAB 显式消费统一 container/content。
- 修改 `ui/main/modern/ModernHost.kt`：删除旧默认头部附件与沉浸主页 FAB，接入新模块。
- 新增 `app/src/test/.../ModernListChromePolicyTest.kt` 及必要的导航 action 回归。

## 文件拆分策略

- `ModernHost.kt` 不得超过 1000 行；滚动算法、搜索转场和标题栏均留在独立文件，Host 只负责把共享作用域与页面状态装配起来。
- 新标题栏与搜索转场各自保持单一职责；任何文件预计超过 500 行时继续拆分状态和 presentation。
- `MainDisplay` 只新增通用 slot，不导入 MIUIX、Glass 或 AppearancePreferences。
- Navigation Scaffold 只转发动作模型；颜色、图标与 Activity 启动逻辑由 Host 提供，Dock 只渲染。

## 关键状态流

1. `selectedPage/searchActive/selectionMode` -> `ModernListChromePolicy` -> TopAppBar `canScroll` 与搜索入口可见性。
2. task LazyColumn nested scroll -> 搜索折叠状态 / MIUIX ScrollBehavior -> PullToRefreshBox；上拉搜索后标题，下拉标题后搜索，Chrome 展开完成后刷新才接收剩余位移。
3. TopAppBar bottomContent 搜索入口点击 -> 记录共享容器位置并置 `searchActive=true` -> 展开 SearchBar 与结果内容。
4. 沉浸 Dock `+` -> Host 构造 `AddDDLActivity` Intent -> launcher -> 返回后刷新 task/habit。

## 风险点

- `SharedTransitionLayout` 为 Experimental API，必须集中在搜索组件并以局部 `@OptIn` 隔离。
- 搜索源位于 TopAppBar AnimatedContent、目标位于内容 AnimatedContent；两者必须提升到同一个 SharedTransitionLayout，退出期间同时存活。
- MIUIX `bottomContent` 自身不参与 largeTitle 的折叠高度；必须由外部搜索槽单独改变测量高度，并确保 nested scroll 只消费实际发生的位移。
- 有色 tint 不能依赖自动判断颜色是否“足够彩色”；调用方必须显式选择策略，避免未来中性色主题被错误增艳。
- NestedScrollConnection 若只挂在 Scaffold 外层，会排在 PullToRefreshBox 之后收到事件；必须落到实际 LazyColumn 的直接父链，且宽屏只挂任务 pane，避免同一状态被两个列表同时驱动。
- MIUIX TopAppBar 状态在切换任务/习惯时可能保留负 offset；需显式重置 `heightOffset` 与 `contentOffset`。
- 右侧 action 不再是导航 item，不能错误更新 `selectedKey`、清空当前 section 或打开 SearchTab。
- 为 Vivid 安装 MiuixTheme 时必须使用同一 token 生成的 scheme，避免出现灰度或深浅模式不一致。
