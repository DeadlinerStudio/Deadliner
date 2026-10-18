# Lifi AI 页面重构 Module Plan

## 模块拆分
- 页面结构与预览控制：`design/lifi-ai-prototype/index.html`。
- 主题、材质与响应式布局：`styles.css`、`conversation.css` 与 `adaptive.css`。
- 模拟会话、审批、结果、历史和记忆：`app.js`。
- 品牌资源：`lifi.svg`，复用现有 Android 矢量路径。

## 平台映射
- Android：新建 `ui/lifi` 独立页面、组件与会话投影；仅复用底层 AI/KMP 和项目公共材质设施。
- iOS：`Features/Main/Components/AIFunction*`，可复用审批摘要与折叠过程结构。
- 鸿蒙：`pages/routes/ai/AIPage`、`pages/common/AIPanelView`，可复用首屏与结果编辑结构。
- iOS/鸿蒙只读参考；Android 按原型实施。

## 文件拆分策略
单个原型文件小于 1000 行；不继续扩充现有 AIOverlay。宽屏复用同一会话 DOM，侧栏展示历史。

## 风险点
跨端历史/记忆能力存在差异；原型不代表 Android 能力已经交付。原型 CSS backdrop-filter 不等同原生光学玻璃实现。页面离开仍使用同一运行状态；审批停留在原会话。

## 原生模块
- ui/lifi/LifiModels.kt：独立消息、历史、审批回执与参数映射。
- ui/lifi/LifiSession.kt：进程内运行、审批等待、归档与数据库持久化。
- ui/lifi/LifiAIPage.kt：页面与导航/宽屏适配，独立 backdrop。
- ui/lifi/LifiComposer.kt、LifiConversation.kt、LifiWelcome.kt、LifiHistorySheet.kt、LifiMemorySheet.kt：全新输入、卡片、历史与记忆管理组件。
- AIUtils/DeadlinerCoreBridge：仅添加可选的执行回执回调与修改参数批准类型，兼容原有调用。
- ModernHost：仅替换现代模式 LIFI 页入口。

原生组件基于项目基础 Miuix/玻璃设施；Miuix 0.9.4-rc01（commit 4a6b750b578880146876e4ab77097d9b01702413），Button 文档与 demo、缓存精确版本源码验证 API。不修改 iOS/鸿蒙。

## Scaffold 与静默操作调整
LifiAIPage 使用 ui/base/Scaffold 无标题操作栏；LifiMemorySheet 通过共享 MiuixBottomSheet 承载 SurfaceContainer 内容卡片并管理真实 userProfile 与记忆。LifiComposer 共享快速添加容器材质与阴影，使用独立 Lifi 内容与单图标操作。QuickAddPlacement 提供共享面板间距，Lifi 按导航真实高度、安全区与抬升定位。Scaffold 内容覆盖状态栏，渐进操作栏叠放在内容上；列表 contentPadding 避让操作栏，输入内图标不叠玻璃，顶栏 Action 按最新要求复用 TopBarIconButton 和滚动渐显材质。LifiSession 不再等待 UI 审批，工具直接批准、返回建议自动执行；记录回执失败及静态创建结果。
