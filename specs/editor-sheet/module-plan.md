# 普通添加与编辑 Sheet Module Plan

## 模块拆分
- 入口与路由：DeadlineEditorEntryHost 提供 CompositionLocal 打开完整编辑器；MainActivity 接受兼容外部启动。
- 表单状态与保存：DeadlineEditorState 复用已有 KMP ViewModel、AI、日历及分类逻辑，独立于 Activity；草稿快照独立文件，采用项目现有 Gson 并用 Keep 保留字段；恢复草稿不自动重放保存。
- 展示：DeadlineEditorSheet 管理 Sheet 和表单，DeadlineEditorDialogs 管理附属弹窗；复用 DeadlineEditorComponents。
- 样式：通用表单容器 alpha 只在编辑器子树提供，其他页面默认不受影响。

## 平台映射
仅 Android：app/ui/editor、ui/main、ui/quickadd、capture/ui 和主题宿主；KMP/iOS/HarmonyOS 不变。

## 文件拆分策略
将原 937 行 AddDDLActivity 拆成薄转发壳、状态、快照、Sheet 与弹窗文件；新文件均控制在 1000 有效行以内，不扩充大型宿主职责。

## 风险点
全局主题宿主位于已有 Scaffold popup host 内，获取页面注册的 backdrop；Sheet 仅引用页面采样，表单不录制自身。处理配置恢复、日历权限延续和保存回调生命周期。旧入口采用 NoDisplay Activity 仅转发，新磁贴/快捷方式直接唤起主页面。FragmentActivity 复用原有日期选择偏好与宿主；ComponentActivity-only 的 Capture 使用同一 Compose 滚轮作为可用回退。

## 顶栏与保存区材质调整
- `ui/base/sheet/MiuixSheetContentLayout.kt`：完整保留 MIUIX 0.9.4-rc01（4a6b750b）原生 Sheet 布局/手势闭包与许可证，增加禁用原生 handle/title 预留的开关，并将原拖拽阻尼/settle 路径传给自有顶栏。避免负偏移或依赖布局高度常量。
- `ScaffoldSheetOverlay.kt`：复用同版本 OverlayBottomSheet 的公开 popup adapter，Sheet 顶部保留 8dp 拖动条，Scaffold 紧接其下，不保留原生空标题行。
- `MiuixBottomSheet`：固定 headerContent 槽用于任务/习惯 Tab；禁止 Sheet Action 启用页面柔光玻璃；只有编辑器保存区使用一个共享内容 recorder，且不记录顶/底 chrome。
- `DeadlineBottomActions`：内容模糊之后叠加主题 surface 底色，alpha 跟随已有 navigationTintAlpha，禁用材质时保留实色回退。

- 紧凑拖动条复用原生按压和拖动动画；条本身保持 4dp，只占用 8dp 布局高度（上方 4dp、下方 0dp，拖动条居底），标题行随后开始。

## 快速添加衔接
QuickAddContent/Properties 增加确认回调以复用输入、草案预览与属性操作；QuickAddEditorEntry 负责导航与无副作用草稿映射/恢复。编辑 Sheet 持有 rememberQuickAddState 并通过独立 DeadlineEditorQuickAdd 集成文件管理初始化、识别续接和填表；原编辑 State 删除重复 AI 网络实现，保留保存权限职责。

## Sheet 与识别交互精简
SheetHeight 负责高度语义及安全区预算；ScaffoldSheetOverlay 和本地 MIUIX 布局 fork 仅增加来源边界入场及动画进度，玻璃内层继续使用固定完整形状。SheetExpansionMotion 独立负责边界/文字透明度几何。QuickAddEditorEntry 在 Intent 中携带屏幕来源边界；QuickAddState 持有临时面板坐标与展开状态。EditorQuickAddCard 独立精简 UI，仍调用共享识别 State；EditorQuickAddSync 维护已应用结果与仅识别字段合并，避免覆盖手动表单。
