# 主清单导航重构

## 背景

Android 的 Modern Host 同时承载现代与鲜明两种设计，但主清单仍使用一套自定义标题附件栏：任务/习惯切换位于标题下方，标题折叠只通过附件栏显隐模拟；沉浸搜索则仍是底栏独立目标，主页新增 FAB 与搜索动作分散在两个位置。这与 HarmonyOS、iOS 已形成的“左侧筛选 / 中部任务习惯切换 / 右侧头像”和“任务页搜索入口随内容滚走”的信息结构不一致。

本轮以 MIUIX `TopAppBar`、`MiuixScrollBehavior` 与 `SearchBar` 为基础重构 Android 主清单 Chrome，并复用现有 `ImmersiveTextTabRow` 和柔光玻璃材质。重构前完整工作区已提交为 `907c18e`。

## 目标

- Modern Host 的现代与鲜明设计都使用专用 MIUIX 主清单标题栏。
- 标题栏固定区域采用三段布局：左侧筛选按钮、中间任务/习惯 Tab、右侧头像或设置按钮。
- 任务页标题栏随列表滚动在大标题与紧凑标题之间折叠；习惯页始终保持展开且不消费折叠滚动。
- 中间 Tab 复用 `ImmersiveTextTabRow`，宽度占可用宽度 50%，最大 240dp。
- 现代沉浸 Compact 模式在标题栏底部提供驻留的 MIUIX SearchBar 入口，并以共享边界动画展开到现有搜索结果页；上滑时必须先完整收起搜索框，再开始折叠大标题。
- 现代沉浸 Compact 底栏右侧独立搜索按钮改为彩色 `+` 动作，在任何主导航页面直接打开 `AddDDLActivity`。
- 移除现代沉浸 Compact 主页原有悬浮 FAB，避免与新的底栏新增动作重复。
- MIUIX 预设颜色以亮色 `#3382FF`、暗色 `#277AF7` 为固定主色，其他 Material 强调色由 Material 色板算法派生。
- 主页新增 FAB 在非系统动态取色时直接使用原始 seed，系统动态取色时使用动态色板主色；加号前景始终为白色。现代预设色默认改用琥珀色 `#FFFF6D6D`，并允许用户关闭撞色后回到预设 primary。

## 非目标

- 本轮不实现筛选按钮的业务行为；按钮只保留语义、按压反馈与稳定布局。
- 不改变 Overview、Capture、AI 页本身的顶栏、内容和主 Capsule 导航顺序。
- 不改变非沉浸 Compact、Medium/Expanded、Rail 及 Simplified Host 的搜索导航结构。
- 不重写搜索结果、筛选 Scope、任务/习惯数据加载或 `AddDDLActivity`。
- 不把所有 Vivid 页面全局迁移到 MIUIX 组件；只为 MIUIX 组件提供主题上下文，并在主清单标题栏显式消费它。

## 用户场景

1. 用户进入现代或鲜明风格的清单页，顶部看到左侧筛选、中间任务/习惯、右侧头像或设置三段布局。
2. 用户在任务页向上滚动，MIUIX 大标题自然折叠；向下回到顶部后自然展开。
3. 用户切换到习惯，标题栏立即恢复展开，继续滚动习惯内容也不会折叠标题。
4. 用户在现代沉浸 Compact 的任务页看到标题栏底部搜索入口；向上滚动时搜索入口先压缩离场，继续滚动才折叠大标题，反向滚动时按相反顺序恢复。
5. 用户点击搜索入口，搜索胶囊从原位置连续扩展到现有搜索页；返回后恢复任务列表和原查询初始状态。
6. 用户在现代沉浸 Compact 的任意主导航页面点击底栏右侧彩色加号，直接进入新增页；新增完成返回后任务与习惯数据刷新。

## 验收标准

- 生效宿主为 `ModernHost`，因此覆盖 `AppearanceDesignMode.Modern` 与 `AppearanceDesignMode.Vivid`；`SimplifiedHost` 不变。
- 默认清单标题栏直接使用 `top.yukonga.miuix.kmp.basic.TopAppBar`，不得通过 `forceMaterial3` 回退。
- 标题栏左侧使用 `iconResource(R.drawable.ic_filter)`；点击为无操作但必须具备可点击和无障碍语义。
- 标题栏右侧优先显示圆形头像；没有头像时显示设置图标；点击继续打开 `SettingsActivity`。
- 顶部固定行的中间 Tab 水平居中，宽度为父级可用宽度的 50%，最大 240dp，包含“任务 / 习惯”两个项目并复用 `ImmersiveTextTabRow`。
- MIUIX 大标题使用当前应用标题；折叠后固定行与三个操作区仍保持稳定，不因左右内容宽度不同而偏离屏幕中心。
- 任务页且非选择、非搜索状态时，`MiuixScrollBehavior.canScroll` 为 true，并由任务列表的 nested scroll 驱动。
- 习惯页、选择状态或搜索状态时 `canScroll` 为 false；切换到习惯后 TopAppBar height/content offset 重置为完全展开。
- 现有选择模式标题栏及完成、归档、删除动作保持不变，不强行套入新的三段默认标题栏。
- 宽屏双栏仍显示任务与习惯内容；标题栏中间 Tab 可隐藏或禁用切换，但三段标题栏整体仍使用 MIUIX 组件且不破坏当前双栏选择隔离。
- 搜索入口仅在“现代设计 + 启用沉浸体验 + Compact 悬浮导航 + 任务页 + 非选择状态”显示；鲜明模式、非沉浸和习惯页不新增入口。
- 搜索入口必须位于 MIUIX `TopAppBar.bottomContent`，不得占用任务 `LazyColumn` 的首项；展开态驻留在标题栏底部，折叠态不保留可点击或占位高度。
- 上滑滚动由两阶段协调器处理：搜索槽先消费至完全折叠，再把剩余位移交给 MIUIX `ScrollBehavior`；下滑时标题先展开，标题完全展开后才恢复搜索槽。
- 下拉展开位移必须在 `PullToRefreshBox` 之前由主清单 Chrome 消费：大标题和搜索槽都完全展开后，剩余下拉才允许触发刷新；不得因 Refresh 容器抢占 pre-scroll 导致顶栏无法恢复。
- 搜索入口和展开搜索页都使用官方 MIUIX `SearchBar`/`InputField`；展开页继续复用 `MainSearchResultsContent`、搜索 Scope 和 Capture inspirations。
- 标题栏内搜索入口不得增加上下 padding，水平 padding 固定为 8dp；搜索槽高度跟随 MIUIX InputField 的 45dp 最小高度，不保留额外空白。
- 搜索进入/退出使用共享边界或等价的容器变换：胶囊位置、宽度与圆角连续过渡，结果内容淡入；不得只做整页突然替换。
- 完整 Material3 SearchBar 与 45dp 搜索入口之间的共享边界不得使用 `RemeasureToBounds`：系统手势返回时，SearchBar 的内部最小尺寸约束会在反向逐帧压缩中产生负子尺寸。必须使用稳定测量后的 `scaleToBounds` 或等价的不重测方案，并保证系统返回不崩溃。
- 展开后的搜索页面复用重构前底部 Search Tab 使用的 `MainSearchBar` 页面结构，不维护第二套全屏 MIUIX SearchBar 布局。
- 搜索激活时主清单标题栏隐藏，底部主 Capsule 不显示搜索选中框；关闭后回到列表选中态。
- 沉浸 Compact 导航仍从通用导航项目中拆出 `search`，但右侧呈现彩色 `+` 动作而非搜索目的地；主 Capsule 项目和其他页面导航不变。
- 彩色 `+` 复用 `LocalHomeFabColors` 与柔光玻璃按钮，外部尺寸与独立底栏圆钮一致，图标语义为新增任务。
- 彩色柔光玻璃不得通过接近不透明的实色表面获得颜色；有色 tint 必须走独立的色彩保真合成策略，以较低覆盖 alpha 保留背景折射，并补偿饱和度与明度，避免主色在明暗主题下灰暗。
- 新增动作在 LIST、OVERVIEW、CAPTURE、AI 任意主界面均可用；传给 `AddDDLActivity` 的当前类型跟随最后选择的任务/习惯类型。
- 现代沉浸 Compact 不再绘制原主页居中 FAB；非沉浸与非 Compact 的主页 FAB 保持当前行为。
- Vivid 分支获得 MIUIX Theme 上下文，但 `LocalAppDesignSystem` 继续为 Material3；除专用主清单标题栏外，现有按设计系统分流的组件不得改变实现。
- 新增纯状态函数覆盖标题栏是否可折叠、搜索入口可见性、沉浸右侧动作选择，并有 JVM 测试。
- 所有常规 Material3/Vivid 导航图标使用固定 24dp 测量框，SVG intrinsic size 不得改变底栏或侧栏高度；Home、Capture、AI、Search 的资源内部图形边界需归一到相近视觉尺寸。
- 清单与概览图标必须使用同一视觉尺寸基准：沉浸布局不得再为清单额外放大；Home Vector 内部边界负责与 Overview 对齐。AI/Lifi Vector 需要放大内部图形，Capture 维持当前视觉尺寸。
- 沉浸主胶囊的拖动位移不得创建包住 DropShadow 的离屏 GraphicsLayer；柔光阴影应越过组件布局边界自然绘制，近缘层 alpha 低于外层的 25%，避免形成深色贴边轮廓。
- TopAppBar 圆形柔光玻璃按钮使用小半径专用阴影 token，降低被 MIUIX/Material 标题栏测量边界裁切的可见程度；底栏、FAB 和大 Capsule 的柔影参数不受影响。
- 渐进顶栏材质在既有自动高度尾部裁切基础上额外减去 24dp；该值必须以独立 `Dp` token 暴露在渐进材质模块，便于后续视觉微调。
- Detail 页无论是否启用沉浸体验，都使用同一 Material3 `ExtendedFloatingActionButton`，不得再分叉到柔光玻璃 FAB。
- MIUIX 预设色亮色 `primary` 必须精确为 `#3382FF`，暗色 `primary` 必须精确为 `#277AF7`，`onPrimary` 固定为不透明白色；除这两个 role 外，container、fixed、inverse、secondary、tertiary、中性色与 error 等全部 Material role 必须原样使用 Material Kolor 2021 TonalSpot 根据对应 seed 生成的结果。
- Detail 顶部进度卡片和 Detail `ExtendedFloatingActionButton` 必须使用 `primaryContainer/onPrimaryContainer`，不得改用被锁定的 `primary/onPrimary`；预设亮暗色板中 `primaryContainer` 都必须与 `primary` 保持可观测的 role 差异。
- 预设色的 `onPrimary` 始终使用不透明白色，供使用 primary 表面的控件与内容消费；`onPrimaryContainer` 仍由 Material Kolor 生成，不为 Checkbox/Switch 额外改写 container role，且不再依赖已删除的独立 Toggle 颜色 helper。
- `LocalHomeFabColors` 必须区分颜色来源：`Preset` 在“撞色主页按钮”开启时使用 `#FFFF6D6D`，关闭时使用当前明暗模式对应的 MIUIX 原始 seed；`SeedColor` 使用用户保存的原始 seed，`SystemDynamic` 使用当前动态 Material 色板的 `primary`；无效或缺失 seed 安全回退到当前色板主色。
- “撞色主页按钮”默认开启并持久化，只在“现代模式 + 预设颜色”时显示；切换到其他设计或颜色来源时不得丢失用户选择。设计与色彩页中“启用沉浸体验”必须位于其所属现代模式 section 的第一项。
- Modern Host 的常规主页 Material3 FAB 与沉浸 Compact 底栏独立 `+` 必须消费同一 `LocalHomeFabColors`，不得分别读取 `MaterialTheme.primary` 或保留旧 `#FF6D6D` 特判；两者的 plus 图标前景始终为 `Color.White`。
- 浅色沉浸 `+` 的柔光玻璃使用独立 seed 保真 tint profile：基础色 RGB 不对 seed 做提亮或额外增艳，提高基础色覆盖但保持 alpha 小于 1，且继续保留 backdrop 模糊、折射与高光；不得用 Screen 白色提亮把 seed 洗淡。中性导航 Capsule 与普通 Chromatic 玻璃不受影响。
- 亮暗模式 `SeedChromatic` 的基础 seed 层在默认底栏 recipe 下有效 alpha 都应约为 0.75，允许牺牲部分透明度以避免背景洗色；仍必须小于 1 并保留 backdrop 折射、模糊与边缘高光。
- MIUIX Dialog 与应用内 Bottom Sheet 的默认容器色统一解析为当前设计系统的 `surfaceContainer`；不得因为 MIUIX 深色窗口背景为纯黑而继承 `background/surface`，也不得逐个设置项修补颜色。
- 非沉浸导航的灵感页必须使用 `ic_draw`/`ic_draw_fill`，沉浸 Capsule 继续使用 `ic_capture`；AI/Lifi 仅在非沉浸导航中适度放大，沉浸导航的图标尺寸保持不变。
- 沉浸主页图标使用 MIUIX Icons 官方 `Home.Light` 的 24dp 路径，静态 visual scale 固定为 1.0；不得继续使用旧 Material Home 路径或额外缩小。
- 沉浸灵感 `ic_capture` 的两段轮廓在原填充外增加约 0.5dp 的同色圆角描边，以提高与 MIUIX Home.Light 并列时的视觉重量；非沉浸 `ic_draw_*` 不受影响。
- 柔光玻璃阴影不再区分 Tab/FAB 的标准 style 与顶栏按钮的紧凑 style；所有柔光玻璃容器使用同一中等强度 token。其扩散距离必须介于旧 2–2.5dp 按钮与 12–14dp Tab 之间，同时降低近缘重量，避免 Tab 贴边发黑并让按钮拥有可见的四向弥散。
- 从沉浸标题栏搜索入口进入搜索页后，搜索输入框必须自动获得焦点并主动显示 IME；返回关闭搜索后按现有逻辑清理焦点。
- 搜索页右上角提供归档按钮，点击打开现有 `ArchiveActivity`；查询非空时清空按钮与归档按钮并排存在，不得互相替代。
- 沉浸 Compact 底栏独立新增动作使用专用 `ic_plus_imm`；不影响常规 Material3 FAB、Detail FAB 或其他 `ic_add` 入口。
- 顶栏柔光玻璃的滚动显现需比现有 12–48dp 更早启动并更早完成；Large TopAppBar 的折叠映射也需同步收窄。按钮与顶部文字 Tab 必须消费同一 material progress，不得各自产生不同节奏。
- MIUIX 顶部文字 Tab 在沉浸模式中从半透明 `surfaceContainer` 平滑叠加到柔光玻璃；未滚动或材质 progress 为 0 时 container 仍可辨识。沉浸模式未开启时，MIUIX 顶部 Tab 始终显示半透明 container，不启用折射材质。
- 沉浸搜索入口提示文字缩小到 14sp 并使用低对比度前景；共享边界转场保留连续容器动画，但不得把完整展开搜索页纵向拉伸到 45dp 入口高度；稳定布局应以顶部对齐的 FillWidth 缩放过渡，且动画时长低于原 420ms。
- Kotlin 编译、完整 Debug JVM 测试、规格校验、`git diff --check` 与修改文件 1000 行门禁通过。

## 风险与约束

- `ModernHost.kt` 已接近大文件阈值；标题栏、搜索转场和底栏动作必须提取到独立文件，Host 只保留状态装配。
- MIUIX `TopAppBar` 的 `title` 不是可组合槽；中间 Tab 需作为精确居中的覆盖层，不得依赖 navigation/actions 的不对称宽度。
- 折叠 nested scroll 与既有渐进顶栏观察器会同时存在；两者都只能观察/消费各自声明的行为，不能破坏 LazyColumn 滚动和下拉刷新。
- 搜索共享边界必须处于同一个 `SharedTransitionScope`，同时避免搜索框被 LazyColumn 或父级 clip 裁切。
- Vivid 需要 MIUIX Theme 数据但不能改变 Material3 的动态色和控件路由；Theme 包装顺序需保持 Material3 颜色为业务组件的主来源。
- 基线配置文件可能被本地测试任务自动重写；只提交与本功能相关且确认需要的产物，不把生成噪声混入实现提交。

## 实现与验证记录

- 2026-08-26：重构前完整工作区提交为 `907c18e`；生成目录 `.kotlin/` 未纳入提交。
- `ModernListTopAppBar` 直接使用 MIUIX TopAppBar，以独立覆盖层保持任务/习惯 Tab 严格屏幕居中；左右操作宽度不会影响中间位置。Vivid 通过只提供颜色上下文的 MiuixTheme bridge 使用同一组件，`LocalAppDesignSystem` 和其余 Material3 路由不变。
- `MiuixScrollBehavior` 由主 Host 单点持有：正常任务列表允许 nested scroll 折叠，习惯、选择和搜索状态关闭折叠并把 height/content offset 复位到完全展开。
- 初版曾为 `MainDisplay` 增加通用 task header slot，并把沉浸 Compact 搜索入口作为任务 LazyColumn 首项；本轮已移除该临时 slot，列表重新只承载任务/习惯数据。
- 搜索入口与展开 MIUIX SearchBar 置于同一个 `SharedTransitionLayout`，使用 420ms shared-bounds 容器变换和结果淡入；展开后继续复用原 `MainSearchResultsContent`、Scope 与 inspirations，外侧“取消”和系统返回均关闭搜索。
- 沉浸 Dock 新增独立 action model；搜索 item 仍作为分区占位从主 Capsule 移除，但右侧改画与底栏同高、消费 `LocalHomeFabColors` 的柔光玻璃加号。新增动作不改变 section/selectedKey，并在所有主页面启动 `AddDDLActivity`；原沉浸主页 FAB 已移除，非沉浸 FAB 不变。
- OpenJDK 22 下完整 `testDebugUnitTest` 90 项通过，任务同时完成 Kotlin 编译。OpenJDK 21 首次执行时只有两个既有 Java 22 字节码测试因 `UnsupportedClassVersionError` 失败，切换项目兼容运行时后全部通过。
- 规格校验与 `git diff --check` 通过；导航图标归一化后修改 Kotlin 文件最大为 `ModernHost.kt` 860 行，仍低于 1000 行门禁。
- 后续统一了所有导航形态的图标测量与视觉边界：Material3/Vivid 的 `AnimatedNavIcon` 固定为 24dp；异常声明为 `251×276dp` 的 `ic_capture` 恢复为方形 24dp Vector 并保留等比 viewport；Home/Search/Capture/AI 通过 Vector group 归一有效图形占比，沉浸导航在固定布局框内使用独立 visual scale，SVG 不再参与栏高测量。
- 主胶囊拖动从包裹阴影的 `graphicsLayer.translationX` 改为不创建离屏裁切边界的 layout offset；柔影改为负 1dp spread、更大半径和低 alpha 近缘层，浅色近缘 alpha 从 0.05 降至 0.018，深色从 0.04 降至 0.012。
- 后续视觉修订把渐进顶栏的额外全透明尾部留白固化为 24dp token；Detail 页恢复统一 Material3 FAB；彩色柔光玻璃改用色彩保真 tint 管线；主清单搜索入口从 LazyColumn 首项迁至 MIUIX TopAppBar 的 `bottomContent`，滚动由搜索槽与 MIUIX 标题栏组成两阶段折叠序列。
- 两阶段滚动使用 62dp 搜索槽：上滑优先消费搜索槽，剩余位移交给 MIUIX；下滑由 MIUIX 先恢复标题，标题完全展开后再恢复搜索。半程手势以 180ms 曲线归位。共享转场作用域提升到整个导航 Scaffold 之外，使 TopAppBar 源与搜索页目标继续共享同一 key。
- 彩色玻璃显式选择 `GlassTintStyle.Chromatic`：低 alpha 基础层保留折射纹理，Color 层恢复色相/饱和度，暗色环境追加克制的 Screen 提亮；中性 Capsule 继续使用原单层 tint。
- OpenJDK 22 下完整 `testDebugUnitTest` 96 项通过；Kotlin 编译、规格校验、`git diff --check` 与本轮修改核心文件 1000 行门禁通过。
- 交互回归修订后，SearchBar 入口为 45dp 高、仅保留水平 8dp padding；展开页删除了第二套 `ImmersiveExpandedListSearch`，统一复用原 Search Tab 的 `MainSearchBar` 与结果内容。
- 清单沉浸图标与概览统一为 0.98 外层 scale，并在 `ic_home_imm` 内部以 1.15 对齐同图标包的有效边界；Lifi 内部 scale 从 0.8 提升到 0.9，Capture 保持原值。
- TopAppBar 圆形按钮使用独立紧凑阴影：浅色外层半径 2.5dp、深色 2dp，显著小于普通 Capsule 的 14/12dp，避免标题栏边界裁切；其他柔光玻璃阴影不变。
- Chrome NestedScrollConnection 已从 Scaffold 外层移到任务 LazyColumn 直接父链，并在 downward pre-scroll 中先恢复 MIUIX 标题，再恢复搜索槽；两者展开后 PullToRefresh 才接收剩余位移。OpenJDK 22 下完整 Debug JVM 测试更新为 98 项全部通过。
- MIUIX preset 改为亮色 `#3382FF`、暗色 `#277AF7` 原始 seed；Material Kolor 2021 TonalSpot 自动生成完整色板，最终仅锁定指定 primary 与白色 onPrimary，避免 2025 role 映射使亮色 primaryContainer 与锁定后的 primary 重合。设置页 preset 色卡同步展示实际生成的三种强调色。
- 主页 FAB 颜色策略现按来源解析：Preset 使用明暗 seed，SeedColor 使用保存的原始 hex，SystemDynamic 使用 Material 动态 primary，解析失败安全回退；所有来源的 content 固定为白色。常规 Material3 主页 FAB 和沉浸 Compact 柔光玻璃加号均消费同一 CompositionLocal。
- OpenJDK 22 下完整 Debug JVM 测试更新为 99 项全部通过；Kotlin 编译、规格校验、`git diff --check` 与修改文件 1000 行门禁通过。
- 后续严格分离 preset role：preset factory 最终只覆盖 primary/onPrimary，逐字段测试确认其余 46 个 ColorScheme role 与 Material Kolor 2021 原始结果一致；已删除的 `DeadlinerToggleControlColors` 不再出现于实现或单测，白色主色前景由 preset `onPrimary` 契约保证。
- 本轮修正 Detail role 与有色玻璃：Detail Progress Card 显式消费 primaryContainer/onPrimaryContainer，Detail FAB 显式固定同一对 role；柔光 FAB 切换到 `SeedChromatic` profile，浅色时使用 seed 原始 RGB、更高但仍透明的 base/colorize 覆盖且不叠加 Screen lift。
- 针对性主题与 tint 回归、完整 Debug JVM 测试及 Kotlin 编译通过；规格校验、`git diff --check` 通过，当前工作区修改 Kotlin 文件最大为 `ModernHost.kt` 878 行，低于 1000 行门禁。
- 顶栏材质精修后，Small/Center 滚动观察区间从 12–48dp 改为 4–28dp，Large 折叠映射从 4%–32% 改为 1.5%–19.5%；ModernList 按钮与任务/习惯 Tab 共享 220ms material progress。MIUIX 通用顶部 Tab 统一切换到自定义 Capsule，沉浸关闭时保留半透明 surfaceContainer。
- 浅色 SeedChromatic base/colorize/fallback alpha 更新为 0.78/0.82/0.94，结合底栏 recipe 0.90 倍数后基础 seed 有效覆盖约 0.702；沉浸新增动作改用 `ic_plus_imm`。搜索入口 hint 为 14sp/0.56 alpha，共享边界由 420ms FillBounds 改为 300ms TopCenter FillWidth。
- 修复沉浸搜索系统手势返回崩溃：共享 SearchBar 从每帧固定约束重测的 `RemeasureToBounds` 改为 `scaleToBounds(ContentScale.FillBounds)`，避免完整 SearchBar 反向压入 45dp 胶囊时生成负宽高。完整 99 项 Debug JVM 测试与 Kotlin 编译通过。
- 主页 FAB 最终统一为亮暗约 75.15% 的 seed 有效覆盖；现代 preset 默认使用 `#FFFF6D6D`，设计与色彩页新增默认开启、仅 Modern + Preset 可见的“撞色主页按钮”，关闭后回退当前亮暗 MIUIX primary。MIUIX Dialog 与全部现有 Sheet 经统一 resolver 使用 `surfaceContainer`，不再继承纯黑 window background。
- 非沉浸灵感导航恢复 `ic_draw`/`ic_draw_fill`；AI 原始 Vector 恢复完整尺寸，沉浸入口用 0.9 visual scale 抵消，因此只放大普通底栏。完整 104 项 Debug JVM 测试与 Kotlin 编译通过；规格、diff check 与 1000 行门禁通过。
- 沉浸主页图标进一步替换为 MIUIX Icons 官方 Apache-2.0 `Home.Light` 路径并使用 1.0 visual scale；沉浸灵感 `ic_capture` 在原填充外叠加约 0.5dp 同色圆角描边，提升线条重量且不改变 24dp 测量框。Android 资源/Kotlin 编译与完整 104 项 Debug JVM 测试通过。
- 柔光玻璃阴影删除 `Standard/CompactTopBarAction` 分叉，亮色统一为 8dp/0.068 外影、暗色统一为 7dp/0.052 外影，近缘层同步收敛为 3.5/3dp；顶部按钮、文字 Tab、底栏和 FAB 共用相同 token。搜索 expanded 后 Material3/MIUIX 输入框都经 `FocusRequester` 自动聚焦并在下一帧显示 IME，现代搜索页右侧归档按钮打开现有 `ArchiveActivity`，查询非空时仍保留清空键。Kotlin 编译与完整 103 项 Debug JVM 测试通过。
