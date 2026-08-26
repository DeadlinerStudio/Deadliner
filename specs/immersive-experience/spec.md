# 沉浸体验

## 背景

现有“手机使用悬浮底栏”只描述了一个 UI 结果，但它将演进为一组协同的现代界面能力：Compact 手机使用柔光玻璃悬浮底栏，顶栏使用由实到虚的渐进模糊，并让顶栏图标按钮拥有独立的圆形柔光玻璃承载层。现有柔光玻璃只有 Modifier 级渲染入口，调用方需要重复解析主题、Tint、Backdrop 与配方，不利于继续扩展到顶栏按钮等组件。

## 目标

- 将设置项改名为“启用沉浸体验”，一个开关统一控制悬浮底栏与渐进模糊顶栏。
- 保留旧 `miuix_floating_tab_bar` 偏好的用户选择，并迁移到新的沉浸体验偏好。
- 在低层 `deadlinerGlassMaterial` 之上提供通用柔光玻璃 Surface 与圆形图标按钮层，统一解析 Backdrop、明暗 Tint 与材质配方。
- 悬浮底栏保持当前 54dp 外部尺寸和现有柔光玻璃视觉配方，轮廓改用连续曲率 Capsule。
- 独立重写阻尼式选择胶囊交互：按压收缩、拖动跟手、速度拉伸、松手吸附到明确 Tab，并避免外部选中态造成动画重播。
- 沉浸体验开启时，所有 MIUIX 界面的完整顶栏槽使用向内容区逐渐消失的模糊，并从屏幕物理顶部完整覆盖状态栏、标题内容和可选顶部 Tab。
- 顶栏主要图标按钮使用圆形柔光玻璃容器；材质背景根据标题栏折叠进度由透明渐显，图标和点击区域始终保持完全可见。

## 非目标

- 不改变 Material3/Vivid 风格以及 Medium/Expanded 宽度的导航结构。
- 不改变悬浮底栏高度、最大宽度、外部水平边距、图标大小、文字排布及现有高级材质精调默认值。
- 不直接复制 NexioSchedule 的 AGPL-3.0 源码；只参考其可观察交互，使用 Deadliner 自己的状态机与现有依赖独立实现。
- 本阶段不为渐进强度、按压比例或拖动弹性新增设置项。
- 不把标题栏之外的普通 IconButton 全局替换为玻璃按钮；圆形材质只在通用 `TopAppBar` 的沉浸上下文中生效。

## 用户场景

1. 既有用户升级后，原先已开启“手机使用悬浮底栏”，新版本的“启用沉浸体验”仍保持开启。
2. 用户在现代设计的 Compact 手机窗口开启沉浸体验，底部看到与现有尺寸、材质一致的悬浮底栏，顶部看到由状态栏侧实模糊向内容区透明过渡的标题栏。
3. 用户按住选中胶囊时胶囊轻微缩小；水平拖动时胶囊连续跟手并随速度轻度拉伸，松手后准确落到最近的可用 Tab，且不会先吸附后再次重播旧位置到新位置的动画。
4. 用户点击非选中 Tab 时，胶囊以弹性动画连续移动到目标；系统或业务代码改变当前 Tab 时也只有一次动画。
5. 用户进入任意 MIUIX 页面时，标题栏渐进模糊覆盖到屏幕最顶端；大标题完全展开时按钮没有玻璃底，开始滚动并折叠后玻璃材质平滑显现。
6. Small/Center/Large 三种标题栏初始都不显示按钮玻璃底；页面向上滚动超过轻微阈值后才渐显，回到顶部时再平滑消失。主列表页使用自身附件栏折叠状态驱动同一动画。

## 验收标准

- 中文标题为“启用沉浸体验”；中英文辅助文案都说明它包含悬浮底栏与渐进模糊顶栏。
- `AppearancePreferences` 使用 `useImmersiveExperience`；读取时优先新键，无新键时读取旧 `miuix_floating_tab_bar`，写入只以新语义键为准。
- 悬浮底栏仅在 MIUIX + Compact navigation breakpoint 生效；渐进模糊标题栏在沉浸体验开启后的所有 MIUIX 页面生效，不受导航宽度断点限制。
- `SoftGlassSurface` 不写死尺寸与 Padding，可传 Shape、Recipe、Tint 和交互参数；底栏与顶栏按钮都通过该通用层渲染。
- 外层底栏高 54dp、最大宽 380dp、图标 28dp、底部间距 4dp，柔光玻璃 recipe 与当前精调参数映射保持不变。
- 外层与选择胶囊使用 MIUIX `SmoothCapsuleShape` 连续曲率轮廓。
- 拖动目标按胶囊最终连续位置四舍五入，并解析到最近 enabled Tab；不得使用容易落入相邻项的独立 release pointer 区间推断。
- 拖动期间不触发外部选择回调；松手只提交一次，外部选中态回流不得再次从旧位置播放动画。
- 渐进模糊由 Scaffold 的完整顶栏槽统一托管，只遮罩背景材质层；`TopAppBar` 仅在没有 Scaffold 宿主时提供兼容回退，二者不得重复绘制。
- 顶栏占用高度优先采用完整顶栏槽的实际测量值，因此 `Column` 内的顶部 Tab、外部 Padding 与其他附加内容会自动计入；同时不得低于“状态栏高度 + 56dp 标准内容区”。
- 渐进材质覆盖高度为“顶栏占用高度除以 0.75，再减去 6dp”，以保留自然消散尾部并补偿模糊的边界外延；该尾部只溢出绘制，不能增加 Scaffold 传给内容的顶部 Padding。
- 渐进背景必须从窗口 y=0 开始覆盖状态栏区域，顶部不得残留透明或纯色空隙。
- 标题、图标和点击区域保持完全不透明；高级材质不可用时以渐变 Tint 降级。
- 顶栏玻璃按钮为独立圆形容器，复用柔光玻璃配方与当前 Backdrop；材质 alpha 与前景 content 分层绘制。
- Large TopAppBar 的按钮材质优先使用 Material3/MIUIX `collapsedFraction`；Small/Center 使用 Scaffold 观测到的垂直滚动距离。三种模式初始值都必须为 0，超过轻微阈值后平滑升至 1。
- 主列表页显式用附件栏折叠状态覆盖按钮材质进度，未滚动时为 0，折叠后动画到 1。
- 动画由通用 `TopAppBar` 统一负责；页面只提供目标进度，不能因离散折叠状态切换而瞬间跳变。
- 顶栏按钮保留独立 Backdrop 采样与完整柔光玻璃效果；顶栏渐进背景使用同一个已注册的 Backdrop，但只执行一次固定小半径高斯模糊和一次 RuntimeShader 渐隐，不调用高级材质、柔光玻璃折射或可调材质参数。
- 渐进顶栏的模糊输出与 Surface tint 必须在同一条效果链中由顶部完整强度平滑降至底部 0 alpha；不得在模糊层外再叠加一层高级材质或不透明背景。
- 在真实渐进 Backdrop 之上额外绘制 Surface 透明度渐变遮罩：顶部 alpha 为 0.94，连续衰减，并在总覆盖高度 68% 位置到达 0；该遮罩只覆盖背景材质，不能改变标题和按钮透明度。无 Backdrop 降级分支遵循同一透明度曲线。
- 沉浸顶栏的返回/关闭按钮不显示 Ripple；按下时整个圆形柔光玻璃承载区同步轻微缩小并出现全区域高亮，松开或取消后弹性恢复。其他沉浸顶栏圆形图标按钮复用同一反馈。
- Compact 沉浸导航把搜索从主 Capsule Tab 列表中拆出，右侧使用与主底栏同高的独立圆形柔光玻璃按钮；主底栏居左、搜索按钮居右且底边对齐，点击搜索按钮继续进入现有搜索状态。
- 主 Capsule 内按“清单、概览、灵感、Lifi”排序；Lifi 位于灵感右侧，显示现有 Lifi 图标和文字但保持 disabled，不改变页面路由。
- 主容器和选中胶囊使用 Kyant Shapes 的 continuous `Capsule` 轮廓；保持 Deadliner 的 54dp 外部高度、柔光玻璃配方和阴影，同时对齐参考组件的 7dp 水平/3dp 垂直内边距、选中胶囊横向溢出、阻尼拖动、边缘位移和速度形变反馈。
- Android 13/API 33 及以上使用 Backdrop + RuntimeShader；运行时 Shader 不可用或没有 Backdrop 时降级为同方向的半透明 Surface 渐变，标题、按钮和点击区域始终位于材质层之上。
- 偏好迁移、拖动目标解析与运动参数映射有 JVM 回归测试；Android Kotlin 编译、规格校验、格式检查和大文件扫描通过。
- 主 Capsule 的堆叠 Tab 图标为 24dp、文字为 10sp/11sp 行高；独立搜索按钮保持更强的 28dp 图标识别度。
- 浅色模式的底栏主 Capsule 与搜索圆钮使用更高的环境阴影强度和 14dp 阴影高度，阴影向四周扩散；深色模式保持既有克制参数。
- 搜索激活时主 Capsule 不显示选择胶囊，主栏所有项目均使用未选中内容状态；关闭搜索或选择主项目后恢复对应选择胶囊。
- 沉浸 Compact 主页 FAB 使用 58dp 彩色柔光玻璃材质，复用页面 Backdrop，水平居中放置在底栏上方；其他窗口断点和设计模式继续使用原 FAB。
- Detail 页在沉浸模式下将原有添加计划 Extended FAB 替换为 58dp 圆形柔光玻璃 FAB，并保持原点击与无障碍语义；非沉浸模式继续使用 Extended FAB。
- 预设颜色下主页沉浸 FAB 使用 `#F08A7E`，Detail 页沉浸 FAB 始终使用当前主题 `primary/onPrimary`，不得继承主页珊瑚色语义。
- 58dp 柔光 FAB 内的加号统一使用 30dp，主页与 Detail 保持一致。
- 沉浸柔光 FAB 最终尺寸收敛为 58dp，内部加号继续保持 30dp；主页与 Detail 使用同一尺寸 Token。
- 沉浸底栏的启用项不再通过前景色或透明度区分选中态：浅色统一黑色、深色统一白色，仅由选择胶囊和字重表达当前位置。
- 通用导航契约允许提供沉浸专用图标：清单使用 `ic_home_imm`、概览使用 `ic_overview_imm`、搜索使用 `ic_search_imm`；没有 `_imm` 资源的灵感与 AI 分别回退到无 fill 的 `ic_capture` 和 `ic_lifi`。非沉浸清单使用 `ic_home_fill/ic_home`，灵感始终使用无 fill 的 `ic_capture`。
- 原 Lifi 占位页在底栏显示标题“AI”，路由 key 与空白占位行为保持不变。
- 渐进顶栏 Surface 遮罩强化为顶部 0.94，并在覆盖高度 68% 处归零；自动计算的材质覆盖高度在既有尾部公式结果上缩短 6dp，以补偿模糊向边界外的视觉延伸，Scaffold 内容 Padding 不变。
- 所有柔光玻璃悬浮容器使用零偏移、无单一光源方向的轻量双层 DropShadow：外层只提供四向弥散，近缘层以低 alpha 保住连续 Capsule/圆形轮廓；不得出现重黑边或明显下坠阴影。阴影实现不得创建会把越界柔影裁到布局边界的中间 GraphicsLayer，顶栏按钮的阴影 alpha 必须跟随材质显现进度。
- 主 Tab 选中胶囊按下时，选择图标从 1.0 缩小至 0.90，文字尺寸保持稳定；不再执行选中内容放大反馈。
- Lifi 是可点击的正式导航目标，位于灵感右侧；当前路由到无标题、无内容、无 FAB 的空白占位页面，为后续功能重构保留状态入口。
- MIUIX 沉浸模式下，公共顶部 `TabRow` 使用专用的纯文字柔光玻璃组件：外层连续曲率胶囊高 42dp，内层为连续曲率选择胶囊，不显示图标、分割线或 Ripple。
- 顶部玻璃 Tab 复用页面 Scaffold 已注册的 Backdrop 与柔光玻璃配方；Overview、Add 以及其他公共 `TabRow` 调用方自动获得一致外观，非沉浸 MIUIX 与 Material3 保持原实现。

## 风险与约束

- 当前 `AdaptiveNavigationSuiteScaffold.kt`、`ModernHost.kt` 等文件职责较多；新材质与运动状态必须落入独立文件，不继续堆积核心文件。
- PointerInput、外部 selectedKey 和 Animatable 必须由单一运动状态协调，避免拖动和业务状态并发抢写位置。
- Backdrop 只能注册在实际内容之上一次；渐进顶栏、顶栏按钮与底栏共享同一个 `LayerBackdrop`，各自绘制效果但不能重复捕获。
- 连续 Capsule 使用独立的 Apache-2.0 `io.github.kyant0:shapes` 依赖；不从 NexioSchedule 复制 AGPL 文件。柔光玻璃、导航契约与动画状态机仍由 Deadliner 自己实现。
- Lifi 占位必须进入 `MainSection` 的穷尽状态分支，不能只在导航点击回调中临时吞掉事件；这样后续接入页面时无需再次改造导航契约。
- 顶部玻璃 Tab 只能采样父 Scaffold 提供的 Backdrop，不能自行注册捕获层；没有 Backdrop 或高级材质关闭时必须稳定降级为不透明柔光 Surface。
- 重构前完整工作区已提交为 `c2f3c23`，用于安全回退。
- 全局标题栏改造前，上一轮沉浸体验实现已提交为 `387e2b5`。
- 统一滚动显现语义前，全局渐进标题栏实现已提交为 `e021405`。
- 本轮沉浸导航重构前，渐进顶栏与按钮材质实现已提交为 `e45f52d`。

## 实现与验证记录

- 2026-08-25：重构前将既有高级材质、悬浮底栏与同工作区改动完整提交为 `c2f3c23`。
- 新增 `SoftGlassSurface` 与 `SoftGlassIconButton`；尺寸和 Padding 完全由调用方控制，默认配方会实时读取当前高级材质精调值。悬浮底栏和沉浸顶栏图标按钮都已迁移到该入口。
- 外层与选择胶囊最终改用 Apache-2.0 Kyant Shapes 的 continuous `Capsule`；未复制参考项目的 AGPL-3.0 组件或辅助代码，Backdrop 与材质仍由 Deadliner 自己实现。
- 新的 `FloatingTabMotionState` 使用直接拖动位置与 Animatable 结算位置分层：拖动同步跟手，松手按连续位置四舍五入后寻找最近 enabled Tab，再原子交接给弹性动画。
- 选择回流通过 `FloatingTabSelectionSync` 消费本次拖动提交，业务 `selectedKey` 更新不会再次触发旧位置到新位置的动画；按压比例为 `52/56`，拖动速度最多产生 18% 横向拉伸并连续移动变换锚点。
- Compact MIUIX Scaffold 为内容注册单一 Backdrop；顶栏背景材质单独经过底部 28dp alpha fade，标题与按钮位于未遮罩前景层。高级材质不可用时使用相同方向的渐变 Tint 降级。
- `TopAppBar` 在沉浸上下文中保持透明并停止绘制自己的整块材质；主标题栏的归档、设置、关闭、完成等图标入口通过 `TopBarIconButton` 自动切换为圆形柔光玻璃。
- 全局沉浸状态已提升到 `DeadlinerTheme`；所有 MIUIX 通用标题栏由 `TopAppBar` 自己托管渐进材质，Compact 与宽屏高级材质宿主均提供对应 Backdrop。
- Large 标题栏读取 Material3/MIUIX 的 `collapsedFraction`，在 4% 到 32% 折叠区间内平滑显现按钮材质；Small/Center 默认视为折叠态，主列表由附件栏折叠状态覆盖并以 220ms 动画显现。
- 渐进背景显式关闭 `edgeFade` 默认的顶部淡入，仅保留底部 28dp 淡出，因此状态栏物理顶部不再残留透明空隙。
- 新键为 `miuix_immersive_experience`；首次读取无新键时继承 `miuix_floating_tab_bar`，后续写入新键并移除旧键。设置中英文标题及辅助文案已更新。
- `FloatingTabMotionTest` 4 项、`ImmersiveExperiencePreferenceTest` 3 项、`GlassMaterialRecipeTuningTest` 2 项全部通过；Android Kotlin 编译成功。规格、格式与大文件扫描通过，新增文件均低于 1000 行，既有 `GlobalUtils.kt` 保持重构前 1244 行不增长。
- 全局标题栏阶段新增 `TopBarMaterialProgressTest` 3 项；连同上述沉浸体验回归共 12 项通过，Android Kotlin 编译再次通过，`git diff --check` 无格式错误，变更涉及的既有大文件仍低于 1000 行且 `GlobalUtils.kt` 仍为 1244 行。
- 完整 `testDebugUnitTest` 共执行 53 项，其中 2 个既有跨模块测试因测试运行时字节码版本不兼容抛出 `UnsupportedClassVersionError`（`ToolCallAdapterTest`、`V5DataMigratorTest`）；本功能定向测试无失败。
- Small/Center 不再被视为天然折叠态；`AdaptiveMaterialScaffold` 与 `AdaptiveNavigationSuiteScaffold` 使用不消费事件的 nested-scroll 观察器累计滚动距离，12dp 前保持隐藏，12–48dp 使用 smoothstep 映射目标进度，并在导航页面切换时重置观察状态。
- `TopAppBar` 对所有来源的目标进度统一执行 280ms `FastOutSlowInEasing` 动画；主列表只传递 0/1 折叠目标，不再在页面层重复动画。
- 顶栏背景曾收敛为“半透明渐变遮罩”路径；在验证 NexioSchedule 的轻量实现后，最终改为共享 Backdrop 上的固定 4dp 高斯模糊，再由单个 RuntimeShader 同时渐隐模糊输出与 Surface tint。它仍不调用 `advancedTextureBlur` 或读取高级材质参数。
- 顶栏圆形按钮恢复独立 Backdrop effect 与完整柔光玻璃光学链，渐进背景和按钮材质不再共享同一高级材质渲染层。
- 新增滚动映射 3 项测试，连同进度、导航运动、玻璃参数和偏好迁移共 16 项定向回归通过；Kotlin 编译、规格校验与 `git diff --check` 通过。变更 Kotlin 文件最大 846 行，未超过 1000 行守门线。
- `ProgressiveTopBarMaterial` 最终从 `LocalAdvancedMaterialBackdrop` 读取已注册的共享捕获层：API 33+ 执行固定 4dp `BlurEffect`，随后由单个 RuntimeShader 在 60%/70% 高度分别开始渐隐模糊与 Surface tint，强度固定为 0.20；缺少 Backdrop 或 Shader 时使用四段 Surface 渐变降级。
- 导航和 action 的 `SoftGlassSurface`/`SoftGlassIconButton` 继续独立采样同一个 Backdrop 并保留完整折射链，顶栏背景不读取 `AdvancedMaterialSpec`，因此不会跟随高级材质模糊半径变化或形成双重高级材质。
- 新增 `ProgressiveTopBarMaterialTest` 3 项；连同顶栏进度、滚动观察、底栏运动、玻璃配方和偏好迁移共 19 项定向回归全部通过。Android Kotlin 编译、规格校验、`git diff --check` 与手动大文件扫描通过；变更 Kotlin 文件最大 846 行。
- 真实 Backdrop 分支在 Shader 输出之上使用独立 Surface alpha 遮罩；旧的顶部 0.14 四段曲线在本轮被顶部 0.90、75% 高度归零的连续曲线替代，标题和按钮仍位于两层背景之上。
- `ProgressiveTopBarMaterialTest` 增加遮罩强度与单调衰减约束，沉浸体验定向回归共 20 项通过；Android Kotlin 编译成功。
- 主 Tab 图标/文字收敛为 24dp 与 10sp/11sp，独立搜索图标保持 28dp。底栏与搜索按钮改为共享阴影 Token，浅色环境阴影由 0.24 提升至 0.38、spot 由 0.18 提升至 0.26，并使用 14dp elevation。
- 主栏选择可见性从业务选中索引中分离：搜索激活时选择胶囊以弹簧 alpha 隐藏，所有主 Tab 同时回到未选中内容态，隐藏期间不启动选择胶囊拖动。
- 新增 56dp 彩色 `SoftGlassFloatingActionButton`；沉浸 Compact Scaffold 在 Backdrop 捕获层外将它水平居中放置于底栏上方 20dp，非沉浸 FAB 保持原实现。
- 本阶段 Android Kotlin 编译、8 组定向 JVM 回归、规格校验与 `git diff --check` 通过；本轮修改文件均未越过 1000 行，仓库既有超限文件未增长。
- 选中 Tab 的按压动画只作用于图标，使用纯函数从 1.0 插值到 0.90；文字和项目布局不再随按压缩放。Lifi 去除 disabled 状态，新增 `MainSection.LIFI` 与可测试的 key/section 双向映射，当前页面只渲染空白 Surface 且不显示标题栏或 FAB。
- 本阶段 Android Kotlin 编译与 9 组定向 JVM 回归通过；规格、格式和修改文件体积检查通过。
- 新增 `ImmersiveTextTabRow`：容器高 42dp、最大宽 420dp、内部 padding 3dp，使用 continuous Capsule 和与底栏一致的黑白灰选择填色；文字为 14sp/16sp，选中位置使用弹簧动画，点击无 Ripple。
- 公共 `TabRow` 在 MIUIX + 沉浸体验时自动路由到新组件，Overview、Add 与 Archive 无需修改各自数据契约；其余设计模式仍使用原生 Material3/MIUIX TabRow。Android Kotlin 编译与 10 组定向 JVM 回归通过。
- 渐进顶栏提升为 Scaffold 级测量宿主：实际 topBar slot 高度会自然包含状态栏、标题、顶部 Tab 和外部 Padding，并以 `max(实测高度, 状态栏 + 56dp) / 0.75` 计算材质覆盖；额外 25% 尾部只溢出绘制，不改变正文 Padding。无顶栏内容时不创建材质。
- Backdrop 之上叠加的 Surface 遮罩改为顶部 alpha 0.90，并在覆盖高度 75% 处线性归零；Fallback 使用同一曲线。`TopAppBar` 通过宿主 CompositionLocal 避免重复绘制，同时保留脱离 Scaffold 时的回退能力。
- 柔光玻璃 FAB 统一为 64dp。主页继续消费主页语义色；Detail 页新增独立 `DetailPlanFloatingActionButton`，沉浸模式显式使用 `primary/onPrimary`，非沉浸模式保留 Extended FAB。`AdaptiveMaterialScaffold` 将页面 Backdrop 提供到 FAB slot。
- 本阶段 Java 22 下完整 Debug JVM 测试 84 项通过，Kotlin 编译成功；规格校验、`git diff --check` 与手动文件体积扫描通过。Detail 页面从 985 行拆回 955 行，新增材质与 Detail FAB 文件均远低于 1000 行。
- MIUIX ColorScheme 工厂不再把 Material 的活力 `secondary` 写入 MIUIX 的中性控件角色，改为复用官方明暗 `secondary/secondaryVariant` 及其 disabled 语义；因此 Switch、Checkbox 的未选中轨道恢复中性灰，选中勾与 thumb 仍由 `onPrimary` 保持白色。
- 柔光玻璃阴影抽取为通用双层 Token：外层负责向四周扩散，近缘层负责在白色背景上保住连续 Capsule/圆形轮廓；浅色强于深色，并统一覆盖底栏、独立搜索、顶部文字 Tab、顶栏按钮与 FAB。顶栏按钮的阴影随材质 alpha 同步显隐。
- 主页与 Detail 的 64dp 柔光 FAB 共用 30dp 加号。Java 22 下完整 Debug JVM 测试 86 项通过，Kotlin 编译成功；规格校验、`git diff --check` 与手动文件体积扫描通过，新增阴影文件与测试均远低于 1000 行。
- 沉浸导航项新增独立 `immersiveIcon` 契约，Scaffold 映射后把同一图形用于两种选择状态；清单/概览/搜索分别采用 `_imm` 资源，灵感采用无 fill 的 `ic_capture`，AI 使用 `ic_lifi`。非沉浸清单同步改用 `ic_home_fill/ic_home`，灵感不再使用 draw/fill 图标。
- 底栏与独立搜索按钮的启用态前景统一为浅色黑、深色白，取消未选中透明度差；选择仍通过胶囊与字重表达。Lifi 字符串改为 AI，但路由 key 和空白占位页不变。
- 柔光 FAB 从 64dp 收至 58dp，30dp 加号不变。渐进顶栏遮罩调整为顶部 0.94、68% 归零，材质自动覆盖高度在原计算值上减去 6dp。Java 22 下完整 Debug JVM 测试 87 项通过，Kotlin 编译与 `git diff --check` 通过。
- 柔光玻璃通用阴影从两层 elevation GraphicsLayer 改为两层零偏移 DropShadow：浅色外层为 12dp/0.11、近缘为 3dp/0.05，深色进一步收敛为 10dp/0.09 与 2dp/0.04。新实现不依赖屏幕光源方向，也不会由阴影自身的中间图层裁切越界柔影，所有玻璃容器尺寸保持不变。
- 本阶段 Java 22 下完整 Debug JVM 测试 87 项通过，Kotlin 编译、规格校验、`git diff --check` 与文件体积检查通过。
