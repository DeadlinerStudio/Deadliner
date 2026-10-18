# 快速添加入口与表单材质统一 Module Plan

## 模块拆分
1. quickadd：外部入口复用 Overlay，桥接页面 modalBackdrop，保留退出生命周期。
2. theme：保留全局色板，提供组件专属颜色策略。
3. base/editor/category：局部表单容器颜色 override，编辑 Sheet 设置默认中性 0.3。

## 平台映射
只涉及 Android UI，iOS/HarmonyOS 不变。

## 文件拆分策略
表单颜色由独立 FormContainerStyle 管理，不扩大 SettingsComponents，全部改动文件低于 1000 有效行。

## 风险点
外部入口在页面采样注册前可能短暂无采样；状态更新后自动取新采样。退出完成再移除宿主，保留展开草稿。

语义色收敛：ComponentContainerColors 生成 TextFieldContainerColors（普通/Sheet 两色），ColorScheme 扩展读取主题提供值；FormContainerStyle 管理共用 Squircle 填充与 Sheet 作用域，MiuixBottomSheet 在内容子组合边界启用。移除编辑页独有的颜色局部 override。
