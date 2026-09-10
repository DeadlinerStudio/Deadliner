# MIUIX Single Design System and Monet Background Strategy

## 背景

- 创建日期：2026-09-03
- 功能标识：`miuix-material-background`
- 需求来源：Android Deadliner 取消“现代（MIUIX）/鲜明（Material）”两个组件分支；Material 的动态配色能力保留为 MIUIX 的一项背景颜色生成策略。

## 目标

- 应用运行时只装配一套 MIUIX 组件树；不再根据外观偏好切换 Material3/MIUIX 组件。
- 移除“鲜明”设计模式及其持久化写入，并把存量用户迁移到 MIUIX。
- 将原 Material/Monet（material-kolor）色彩生成明确为 `MaterialMonet` 背景颜色策略，输出仍由 MIUIX 色板工厂消费。
- 保留现有 MIUIX/HyperOS 预设背景策略，以及种子色、系统动态色和调色板样式的兼容读取。
- 最终产出经历史发布证书签名的 APK，并投递到已验证的本人飞书私聊。

## 非目标

- 不删除 Compose Material3 依赖中仍被非替代性 Android API、图标或兼容层使用的类型；本次删除的是应用层的双设计系统组件策略。
- 不在本次重写全部页面的布局与信息架构，也不改动共享 KMP、iOS 或 HarmonyOS 工程。
- 不迁移已有用户的种子颜色、动态调色板样式或高级材质参数。

## 用户场景

1. 老用户此前选择“鲜明”模式，升级后直接进入 MIUIX 界面，原来的 Material 动态色偏好变成 Material Monet 背景策略，不出现额外的选择中断。
2. 用户在外观设置中选择“Material Monet”时，MIUIX 按系统/种子色和既有动态调色板样式生成背景与表面色；选择“MIUIX/HyperOS”时，使用现有预设/设备背景色板。

## 验收标准

- `AppearanceDesignMode.Vivid`、`AppThemeStyle`、`AppDesignSystem.MATERIAL3` 和可选的 Material 顶栏开关不再参与运行时组件选择。
- 根 `DeadlinerTheme` 始终提供一套 MIUIX 主题和其 Scaffold 主机；基础按钮、勾选、输入、滑块、对话框、脚手架、导航和顶栏不再保留 Material/MIUIX 二选一分支。
- 设置与首次引导不再显示“现代/鲜明”模式；背景颜色策略显示为 MIUIX/HyperOS 与 Material Monet。
- 存量 `appearance_design_mode`、`theme_style`、`miuix_mode`、`vivid_hide_divider` 能被读取但不再写入；旧的 Material3/鲜明值自动规范为 MIUIX + Material Monet。
- 单测、Debug 单测与 Release 构建、签名和 APK 校验通过；签名 APK 发送至经身份核验的飞书本人私聊。

## 风险与约束

- 当前工作区已有其他功能的未提交改动；不得用重置、清理或全工作区基线提交覆盖它们。本分支承担隔离作用，验证需将既有改动一并纳入。
- 这是破坏性外观迁移：旧偏好必须一方向前兼容，不能保留可回写的双分支开关。
- MIUIX 版本固定为 `0.9.4-rc01`；使用的组件和配色 token 必须以该版本为准。
- 签名口令只能通过受控管道传递给签名工具，绝不输出到终端、日志、规格或飞书消息正文。
