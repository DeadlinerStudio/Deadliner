# MIUIX 官方玻璃实验 Module Plan

## 模块拆分
- 固定官方材质源码：隔离 PR 实现及必须的依赖闭包。
- 应用适配：通过统一材质入口选择后端，保持布局/状态/交互。
- 偏好与设置：新增默认关闭实验开关并支持预览。

## 平台映射
仅 Android app 与本地第三方源码；iOS/HarmonyOS 无变动。

## 文件拆分策略
官方源码保留模块边界，应用桥接独立文件，新增文件不超过 1000 有效行。

## 风险点
PR 与当前 MIUIX API 的差异；实验参数和旧调参的边界；大面板的模糊、边缘和降采样效果。

## 最终模块
- third_party/miuix-glass：官方材质依赖闭包及源码哈希、许可证，作为 AGP 9 main.kotlin 源目录。
- ui/material/glass/OfficialMiuixGlassMaterial.kt：应用角色、原有形状与官方预设之间的桥接。
- AppearancePreferences/GlobalUtils/AdvancedMaterialSpec：选择状态持久化与主题传播。
- AdvancedMaterialSettings/AdvancedMaterialPreview：开关、预览和旧调参面板控制。
- localutils/OfficialGlassExperimentPreferencesTest：端到端偏好回退测试。
