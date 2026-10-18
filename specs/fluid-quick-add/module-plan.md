# 流体快速添加 Module Plan

## 模块拆分
- 状态模块：QuickAddState 管理输入、解析、AI、属性和保存。
- 呈现模块：QuickAddOverlay 管理锚点、弹性展开、键盘和草案；QuickAddProperties 管理日历/时间/分类。
- 数据模块：QuickAddPersistence 复用 KMP editor，增加起止时间、分类和日历关联保存参数。
- 接入模块：首页 ModernHost / SimplifiedHost 加号回调和锚点；QuickAddSheet 作为外部入口兼容容器，QuickAddEditorEntry 负责向完整编辑器携带全部属性。
- 布局模块：AdaptiveNavigationSuiteScaffold 的可选 overlayContent 槽位在 backdrop 录制区域外呈现浮层；简洁首页只录制列表内容。
- 共用展示：EditorDateTimeFormat 对齐草案与完整编辑器的本地化日期格式。

## 平台映射
Android ui/quickadd、ui/main/modern、ui/main/simplified、ui/navigation、ui/base、ui/editor；共享 domain/quickadd 只消费现有协议。iOS Features/QuickAdd、HarmonyOS pages/components 可映射同等状态，本次不修改。

## 文件拆分策略
新文件保持小于 400 行；ModernHost 已接近 1000 行，新增逻辑移至 quickadd 模块，仅添加必要接入。

## 风险点
共享提案不包含开始时间和分类，使用 Android 本地属性状态；已有导航 backdrop 必须共享，禁止递归采样浮层。

## 实现结果
输入框使用固定 28dp 光学几何的 quickAdd recipe，并以尺寸/位移动画呈现；内部属性按钮和外部入口内的提交按钮使用普通绘制。QuickAddPlacement 统一处理窗口坐标、整排控件避让与键盘独立按钮的位置，增加对应回归测试。导航 action 支持字体图标覆盖和禁用状态，保留原位置提交。

所有本轮触及核心文件均通过 1000 行限制检查。已有 AIOverlay / SettingsComponents 超限，本轮没有修改它们。

## 本轮模块补充
- calendar/CalendarImportDialogs：从 AddDDLActivity 抽出既有导入与账号过滤 Dialog，普通添加与快速添加复用。
- ui/quickadd/QuickAddCalendarImport：快速添加权限、加载、过滤与 Dialog 状态；QuickAddProperties 只负责按钮与起止时间卡片。
- Core domain/quickadd：修正未识别输入的 AI 标记并增加解析回归测试；不改变 DTO/API。

横行反馈：QuickAddProperties 负责紧凑卡片与横向滚动；QuickAddState 负责默认时间和任务编辑资格；Core QuickAddParser 排除没有文字的标题，不改变接口。

形状调整仅由 QuickAddProperties 实现圆形动作按钮与时间胶囊；解析、保存、日历 Dialog 和默认时间状态不变。

QuickAddRoundAction 仅绘制圆形图标，不绘制可见标签。

QuickAddProperties 使用 fillMaxWidth Row 与 weight 时间胶囊，移除滚动和开始时间按钮；QuickAddOverlay 隐藏开始时间展示。状态与保存逻辑不变。

动态属性模块：QuickAddProperties 渲染 FlowRow 胶囊与常驻图标；QuickAddAttributeDialogs 编辑备注/重复；QuickAddState 提供删除、显式属性与独立 AI 重新识别操作；QuickAddLocalRecognition 为明确开始/范围语句复用 Core 单时间解析，不改变 Core DTO。

材质调整收敛到 DeadlinerGlassRecipes.quickAdd 和 QuickAddContainer 的 canonical 阴影；配方测试锁定模糊及自定义参数继承。

ModernHost/SimplifiedHost 原加号改为 close 回调；QuickAddOverlay 的 IME 独立按钮也只关闭；QuickAddState 分开 canSubmit/canRecognize/canClose，发送与 AI 独立。

QuickAddToggleButton 复用 canonical 玻璃按钮并统一旋转；导航 detached action 增加中性 tint 与旋转参数。QuickAddPlacement 固定 action 坐标，面板独立避让 IME；QuickAddProperties 负责紧凑发送与 AI 文本按钮。

QuickAddToggleButton 默认尺寸使用依赖 MIUIX FloatingActionButtonDefaults.MinWidth，与上游保持一致，简洁页显式传入 56dp。

QuickAddMorphHost 用 SubcomposeLayout 将玻璃表面与内容分开测量，实际形变共用一个 backdrop；QuickAddMorphGeometry 独立测试源点/目标。QuickAddContent 为常规入口和形变入口共用正文。QuickAddAIStatus 映射错误与结果消息，不记录输入/Key。

QuickAddAnchor 用布局尺寸构造窗口锚点；QuickAddMorphGeometry 提供交接曲线，MorphHost 同步模糊与可见内容。GlassMaterialInteraction 的中性色过渡在 canonical tint composite 内完成，所有 fallback 同步插值。

## 对称边距与平板入口
QuickAddMorphGeometry负责窄/宽屏终态水平边界；QuickAddMorphHost消费边界。TabletNavigationScaffold单独摆放FAB，复用ModernHost现有QuickAddToggleButton和Overlay，保持采样隔离。

手动草稿：QuickAddState 统一计算有效草稿/可提交性；QuickAddProperties 无条件显示手工时间，Overlay 与编辑页使用同一个有效草稿。DateTimePickers 两种后端共用初始值规则并保留时间约束。
