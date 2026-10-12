# 小组件强制深浅色模式 Module Plan

## 模块拆分

- preferences：枚举、默认值、模式解析和实例存储清理。
- theme：根据选项选择深浅色，再生成对应应用配色与小组件调色板。
- configuration：新增单选项，预览和配色样本读取小组件模式与调色板。
- ui/collection：两个 Glance 布局和服务列表传入实例选项。

## 平台映射

仅 Android app/widgets/glance 与中英文资源，iOS、HarmonyOS 不在本次范围内。

## 文件拆分策略

复用已有配置和渲染模块，各 Kotlin 文件维持 1000 行有效代码以内。

## 风险点

默认值兼容旧配置；强制深色下仍需保留所选色系；预览主题与编辑表单主题分离，仅给预览内容显式配色，不新增主题或 Scaffold。

## 后台刷新修复模块

- 新增 TaskWidgetSystemThemeObserver，以应用级 ComponentCallbacks 监听深浅色变化，过滤相同模式和仅方向变化。
- TaskWidgetRefreshCoordinator.start 注册进程级观察器；去抖请求保留待处理的强制刷新标志。
- 删除两类小组件 Receiver 中无法由 Manifest 接收的配置变化声明，保留开机和包更新处理。
