# 沉浸体验

## 背景

现有“手机使用悬浮底栏”只描述了一个 UI 结果，但它将演进为一组协同的现代界面能力：Compact 手机使用柔光玻璃悬浮底栏，顶栏使用由实到虚的渐进模糊，并让顶栏图标按钮拥有独立的圆形柔光玻璃承载层。现有柔光玻璃只有 Modifier 级渲染入口，调用方需要重复解析主题、Tint、Backdrop 与配方，不利于继续扩展到顶栏按钮等组件。

## 目标

- 将设置项改名为“启用沉浸体验”，一个开关统一控制悬浮底栏与渐进模糊顶栏。
- 保留旧 `miuix_floating_tab_bar` 偏好的用户选择，并迁移到新的沉浸体验偏好。
- 在低层 `deadlinerGlassMaterial` 之上提供通用柔光玻璃 Surface 与圆形图标按钮层，统一解析 Backdrop、明暗 Tint 与材质配方。
- 悬浮底栏保持当前 54dp 外部尺寸和现有柔光玻璃视觉配方，轮廓改用连续曲率 Capsule。
- 独立重写阻尼式选择胶囊交互：按压收缩、拖动跟手、速度拉伸、松手吸附到明确 Tab，并避免外部选中态造成动画重播。
- 沉浸体验开启时，所有 MIUIX 界面的通用标题栏背景使用向内容区逐渐消失的模糊，并从屏幕物理顶部完整覆盖状态栏区域。
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
6. 已经是 Small/Center 形态的标题栏视为折叠态；主列表页则使用自身附件栏折叠状态驱动按钮材质，初始展开状态不显示玻璃底。

## 验收标准

- 中文标题为“启用沉浸体验”；中英文辅助文案都说明它包含悬浮底栏与渐进模糊顶栏。
- `AppearancePreferences` 使用 `useImmersiveExperience`；读取时优先新键，无新键时读取旧 `miuix_floating_tab_bar`，写入只以新语义键为准。
- 悬浮底栏仅在 MIUIX + Compact navigation breakpoint 生效；渐进模糊标题栏在沉浸体验开启后的所有 MIUIX 页面生效，不受导航宽度断点限制。
- `SoftGlassSurface` 不写死尺寸与 Padding，可传 Shape、Recipe、Tint 和交互参数；底栏与顶栏按钮都通过该通用层渲染。
- 外层底栏高 54dp、最大宽 380dp、图标 28dp、底部间距 4dp，柔光玻璃 recipe 与当前精调参数映射保持不变。
- 外层与选择胶囊使用 MIUIX `SmoothCapsuleShape` 连续曲率轮廓。
- 拖动目标按胶囊最终连续位置四舍五入，并解析到最近 enabled Tab；不得使用容易落入相邻项的独立 release pointer 区间推断。
- 拖动期间不触发外部选择回调；松手只提交一次，外部选中态回流不得再次从旧位置播放动画。
- 渐进模糊由通用 `TopAppBar` 自己托管，只遮罩背景材质层；不得再由主界面 Scaffold 局部包裹，避免坐标偏移和重复材质。
- 渐进背景必须从窗口 y=0 开始覆盖状态栏区域，顶部不得残留透明或纯色空隙；底部继续以 28dp 渐变到透明。
- 标题、图标和点击区域保持完全不透明；高级材质不可用时以渐变 Tint 降级。
- 顶栏玻璃按钮为独立圆形容器，复用柔光玻璃配方与当前 Backdrop；材质 alpha 与前景 content 分层绘制。
- Large TopAppBar 的按钮材质使用 Material3/MIUIX `collapsedFraction`：展开端保持 0，滚动超过轻微阈值后平滑升至 1；Small/Center 默认视为折叠态。
- 主列表页显式用附件栏折叠状态覆盖按钮材质进度，未滚动时为 0，折叠后动画到 1。
- 偏好迁移、拖动目标解析与运动参数映射有 JVM 回归测试；Android Kotlin 编译、规格校验、格式检查和大文件扫描通过。

## 风险与约束

- 当前 `AdaptiveNavigationSuiteScaffold.kt`、`ModernHost.kt` 等文件职责较多；新材质与运动状态必须落入独立文件，不继续堆积核心文件。
- PointerInput、外部 selectedKey 和 Animatable 必须由单一运动状态协调，避免拖动和业务状态并发抢写位置。
- Backdrop 只能注册在实际内容之上一次；渐进顶栏与底栏共享同一个 `LayerBackdrop`，不能重复捕获。
- 连续胶囊复用项目已依赖的 MIUIX Shapes，不引入 NexioSchedule 使用的额外 Capsule/Backdrop 实现。
- 重构前完整工作区已提交为 `c2f3c23`，用于安全回退。
- 全局标题栏改造前，上一轮沉浸体验实现已提交为 `387e2b5`。

## 实现与验证记录

- 2026-08-25：重构前将既有高级材质、悬浮底栏与同工作区改动完整提交为 `c2f3c23`。
- 新增 `SoftGlassSurface` 与 `SoftGlassIconButton`；尺寸和 Padding 完全由调用方控制，默认配方会实时读取当前高级材质精调值。悬浮底栏和沉浸顶栏图标按钮都已迁移到该入口。
- 外层与选择胶囊改用 MIUIX Shapes 的 `SmoothCapsuleShape`；未引入参考项目的 Capsule/Backdrop 实现，也未复制 AGPL-3.0 代码。
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
