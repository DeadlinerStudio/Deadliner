# 高级材质管线与柔光调校 Module Plan

## 模块拆分
1. model/localutils：参数默认值、级别节点及版本迁移，管线 bool 复用现有 Appearance 状态并使用正式偏好键。
2. ui/material/glass：调整自研高光输出，不修改 vendored MIUIX 源；明暗共用可测试的计算路径。
3. ui/settings：Dropdown、MIUIX 原生关键点滑块、真实管线预览。

## 平台映射
Android model/localutils/ui，iOS 和 HarmonyOS 无改动。

## 文件拆分策略
不扩张共用 SettingsComponents；新高光计算独立文件，现有设置及迁移文件均低于 1000 有效行。

## 风险点
偏好读写强制关闭逻辑须一并移除，滑块关键点必须真实吸附。MIUIX 固定预设不应假装响应自研参数。

## 容器辨识度调优补充
TopAppBar 将公共 softGlassShadow 移至按压 clip 外；SoftGlassShadow 根据当前管线选择自研浅色/官方旧强度，深色继续共用保守值。GlassEdgeHighlight 集中处理填色后的细边宽度与明暗渐变，避免各页面散落光学常量。Shader 折射带、色散及色板均不变。

## 顶栏外层阴影宿主
TopBarGlassShadowHost 位于原生 MIUIX 顶栏 clip 外、渐进背景之上。softGlassShadow 在同 View 的宿主中仅登记，统一复用 drawSoftGlassShadow 绘制，避免双份阴影；其他场景保持本地绘制。登记布局边界在 composition 内订阅，坐标/alpha 更新与 DisposableEffect 注销同步。无交互的影子节点不改变槽位测量与点击语义。

## 自定义顶栏覆盖
ProgressiveTopBarMaterial 的 Content 子组合负责统一承载阴影；TopBarGlassShadowHost 遇到同 View 的现有宿主时直接复用。无需修改清单/Lifi 的布局、按钮尺寸、材质配方和背景采样范围。

## 亮边与阴影再平衡
仅调整 SoftGlassShadow 的自研浅色 token，以及 GlassEdgeHighlight 的浅色绘制分支。统一覆盖顶栏/底栏及通用玻璃容器，保留公共宿主避免重新裁切。Native Graphics 将新旧阴影在同背景、同几何下做像素对比；深色和官方分支做稳定性回归。

回归测量注意：低密度阴影在 8-bit 截图远端存在抖动量化，清单下边界用相邻像素平均值检查；Lifi 的 10dp 底部留白已大于新阴影的可见范围，改测按钮轮廓外的阴影及外层宿主登记，不要求在自然衰减后仍有可见阴影。

## 预设默认值升级
model 更新参数及归一化回退，冻结 V5/V4/V3 历史快照；localutils 升级版本 6、按来源版本读取快照，避免历史默认值被新构造参数污染。SoftGlassSurface 中的 SoftGlassIconButton 分离外层阴影与内层材质。将用户 JSON 作为测试夹具验证全部默认字段；迁移/自定义/再次写入读取及上下栏同几何阴影像素对齐回归。

## 局部轮廓定义
新增 GlassBoundaryDefinition 处理统一的双层内边缘和轻压白层，复用真实 shape，缓存 outline；边缘在主体之后绘制，不修改布局或背景采样。GlassMaterialModifier 合并 optical/fallback 边缘调用并保持官方路径提前返回。明暗边缘及高光关闭/宽度为零要保留原有语义。

绘制回归采用与真实玻璃组件一致的独立外层阴影及内层 shape 裁切；低 alpha 阴影在不同屏幕偏移会发生 8-bit 抖动量化，用区域平均密度（误差小于 0.00005）和单像素最大 2 级误差共同检查，避免将相位差误判为阴影增减。

反馈修正：glassBoundary 仅绘制原白色边缘，删除灰色渐变及加宽描边；glassBodyDefinition 保留。

## 阴影视觉补偿
SoftGlassShadow 增加内部 profile，默认 Standard 保持原输出；TopBarControl 仅修改自研浅色 ambient。导航按钮显式指定，SoftGlassIconButton 仅在渐进顶栏上下文启用，独立 FAB/底部按钮保持 Standard。profile 经外层宿主登记并转交绘制，不能在转交时丢失。

最新反馈修正：删除 glassBodyDefinition 及其调用；glassBoundary 仅保留白色亮边。softGlassTintColor 将浅色调参值量化到 8-bit 灰度，默认用户预设对应 249，保留存储参数及自定义调参能力。
