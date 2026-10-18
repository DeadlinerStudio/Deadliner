# 全局 MIUIX 点击反馈 Module Plan

## 模块拆分
1. ui/base/feedback：主题级默认 indication/ripple 策略，原生 Material 控件按压适配 modifier。
2. ui/theme：在 MaterialExpressiveTheme 内层应用，覆盖其默认 ripple。
3. 应用交互入口：移除显式 ripple、适配内部生成 ripple 的 Material 控件，替换玻璃自研按压；保留导航切换与拖拽动效。

## 平台映射
Android Compose 层；系统 Dialog 与小组件宿主不变。

## 文件拆分策略
独立反馈文件，批量调用修改仅增加 modifier，保持各核心文件低于1000有效行。

## 风险点
Material 内部 ripple 不读取 LocalIndication；仅禁用会失去反馈。Sink 与按压层共享交互源；宿主提供反馈时子按钮不再叠加 Sink。按压层按按钮形状裁切，深色使用 onSurface 提亮。
