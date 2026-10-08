# 专注计时展示模块

## 模块拆分
- FocusTimerEntryHost/FocusTimerSetupSheet：入口 Sheet、草稿和启动后导航，业务动作调用共享 ViewModel。
- FocusTimerScreen/FocusTimerDial：独立运行展示、计时视觉和会话控制。
- FocusTimerControls：共享圆形图标 FAB 和紧凑选项，复用 SoftGlassFloatingActionButton；选择控件采用 Foundation selectableGroup/RadioButton 语义。
- FocusGlobalBar/FocusTimerDockLayout：主页图标操作、进度线、Spotify 几何常量。
- 现有 MainActivity/DeadlineDetailActivity/TaskItem：接入入口与当前计时，不新增业务引擎。

## 平台映射
Android 文件在 app/src/main/java/com/aritxonly/deadliner/ui/focus；KMP shared focus 模型及服务继续作为权威，iOS/HarmonyOS 无本轮改动。

## 文件拆分策略
单个新增文件目标小于 300 行；运行页面去除设置组件，设置独立文件；只在现有宿主添加薄接入。

## 风险点
根 Overlay Sheet 生命周期与启动失败重试；已有计时和设置任务不一致时应打开当前计时；导航宽度必须计入 dock 内侧 8dp padding 和 442dp 宽度上限。
