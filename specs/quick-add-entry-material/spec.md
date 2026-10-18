# 快速添加入口与表单材质统一

## 背景
控制中心走旧兼容 QuickAddSheet，缺少页面局部采样和主页形变布局；Add Sheet 输入及分组容器使用不同表面色。

## 目标
外部快速添加复用主页 Overlay、位置/形变/键盘避让并接入已注册的页面内容采样；不采样其自身。Add Sheet 表单容器共用组件背景策略，透明度 0.65。保持 Material 色板与全局主题映射原样，组件材质在单独的局部策略选择 Material secondaryContainer、MIUIX 原始 secondaryContainer 或与应用 secondary 协调后的颜色，明暗模式一致。

## 非目标
不改任务存储、上游材质算法或其他平台。不使用整个窗口录制作为同窗口快速添加的背景。

## 用户场景
控制中心添加进入主界面后显示与主页相同的快速添加；展开 Add Sheet 后输入、类别和时间卡片使用一致的中性色。

## 验收标准
外部快速添加有共享 LayerBackdrop、可关闭、可展开且传递草稿。应用全局颜色与修改前一致，只有表单类组件背景按策略变化。表单 override 只在编辑 Sheet 作用；主机测试与编译通过。

## 风险与约束
不能让捕获层包含自身，避免 RenderNode 环；保留无采样/关闭高级材质降级。仅 Android，无设备操作。

## 组件颜色策略（用户确认）
“多彩配色”指预设颜色，与任务指示标开关无关。MaterialMonet 使用 Material secondaryContainer 原色；MiuixHyperOs + SystemDynamic/SeedColor 使用官方 MIUIX 默认 secondaryContainer.harmonize(material.secondary)，保留中性饱和度；MiuixHyperOs + Preset 使用官方默认 secondaryContainer。只有 TextField、编辑卡片、分类容器和内嵌识别框消费该策略，Material 全局色板与既有 MIUIX 全局颜色映射保持不变。

## 实施结果与验证
控制中心兼容入口复用 QuickAddOverlay，从 modal host 取已注册页面 LayerBackdrop；不读取整个窗口录制层，不捕获自身。退出等待共享形变实际结束，展开沿用有效草稿。统一表单策略仅修改组件背景；使用官方对应明暗默认值而非全局 MiuixTheme 中已映射的容器色，避免污染应用色板。Kotlin 编译、69 项相关主机测试及静态检查通过；未操作设备，未确认实际 GPU 视觉效果。

## TextField 标准颜色
主题提供不透明的三分支策略原色，标准 TextField（通用 MIUIX 包装、Capture 直接 MIUIX 输入、原生 Material 昵称/订单输入）默认共用策略色。Add Sheet 局部将统一容器色设为 0.65 alpha，覆盖内嵌识别输入和所有表单输入/卡片，不改变全局色板。

## Sheet 输入材质语义化
以 Deadline 名称输入框的 MIUIX Squircle 绘制结果为视觉基准。Factory 生成 textFieldContainer 与 textFieldSheetContainer（0.65 alpha），通过应用扩展属性提供给 Material ColorScheme，不修改其内置色板。共用 MiuixBottomSheet 在弹层内容边界提供 Sheet 语义；所有 Sheet 的输入与 Add Sheet 表单卡片共用颜色及 Squircle 绘制，避免仅同步颜色参数却绘制方式不同。
