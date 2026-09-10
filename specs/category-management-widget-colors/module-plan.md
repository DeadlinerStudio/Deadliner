# Android 分类管理与小组件分类色 Module Plan

## 模块拆分

1. 分类契约与颜色适配：扩展 Android 分类展示注册表，公开受支持的图标/颜色选项，并提供 Compose/ARGB 两种颜色解析。
2. 分类数据编排：Android 分类管理控制器负责加载、创建、更新、软删除和 KMP mutation 参数构造。
3. 分类管理 UI：管理页与编辑页复用设置页同款 Activity 根宿主和 `NavHost`；每个页面的 `CollapsingTopBarScaffold` 仅在内容 Box 录制 `LayerBackdrop`，顶栏删除动作与顶底栏作为兄弟节点消费。
4. 分类删除 UI：列表行和编辑页复用单一删除确认 Composable；确认事件回到各自 Activity，由控制器执行软删除、刷新列表和小组件。
5. 现有入口集成：分类选择/筛选底部页提供“管理分类”动作，主页面和任务编辑页面在返回前台时重载分类；Sheet 自身持有退出阶段展示状态。
6. 小组件数据链路：数据源将任务的有效分类颜色带入快照，渲染层以分类色覆盖竖线的默认语义色。

## 平台映射

- Android KMP 访问：`app/src/main/java/com/aritxonly/deadliner/platform/kmp/`
- Android 分类 UI：`app/src/main/java/com/aritxonly/deadliner/ui/category/`
- Android 页面注册：`app/src/main/AndroidManifest.xml`
- Android Glance：`app/src/main/java/com/aritxonly/deadliner/widgets/glance/{data,model,ui}/`
- KMP 领域与仓储：`DeadlinerCore/shared/src/commonMain/kotlin/com/deadliner/{domain/category,db/repository}/`（本轮只复用，不修改）

## 文件拆分策略

- `CategoryManagementActivity.kt` 负责 Activity 生命周期、设置页同款根材质宿主、`NavHost` 路由和保存/删除编排，但不承担 Backdrop 录制，避免把消费者纳入录制树。
- `AdaptiveMaterialScaffold.kt` 在内容容器录制 Backdrop，并把顶栏、底栏、浮动动作和弹窗保持为录制层外的消费者。
- 不再单独维护 `CategoryEditorActivity.kt`；编辑页作为管理 Activity 导航栈中的二级页面，避免重复页面根和材质宿主。
- `CategoryManagementScreen.kt` 只负责分类列表。
- `CategoryEditorScreen.kt` 负责编辑表单和选择控件；若接近 500 行则继续拆分选择网格。
- `CategoryEditorScreen.kt` 为短表单补足一个顶栏高度的尾部滚动余量，保证内容能进入顶栏 Backdrop 采样区，而不改变安全的内容层录制边界。
- `CategoryManagementController.kt` 保持为可单测的非 Compose 逻辑。
- `CategoryComponents.kt` 统一负责分类 Sheet 的动画生命周期、语义色和分类标签基础样式，并公开只供任务卡片使用、受备注行高约束的紧凑规格，不把退出延时或重复标签实现散落到调用页。
- 小组件沿现有 data/model/ui 分层扩展，不把分类数据库查询放进 Glance Composable。
- 所有核心文件保持低于 1000 行有效代码。

## 风险点

- 首页与任务编辑页当前对分类多为一次性 `remember` 快照，需要用生命周期刷新信号避免返回后数据陈旧。
- 预设分类允许编辑但不能丢失 `isPreset` 等元数据，必须调用 KMP update 规则而非重建对象。
- 分类删除使用 KMP `CategoryRepository.softDelete`；任务和习惯保留原 UID 引用，由现有 resolver 按“未分类”处理。
- 分类 UID 或已删除分类可能悬空，小组件数据源必须只认当前有效分类。
- MIUIX 与 Material 两套外观共用页面时需继续从现有 `DeadlinerTheme` 和应用页面壳取颜色、圆角与间距。
- `layerBackdrop` 不会录制父级 Surface；透明页面容器必须在采样回调内先绘制当前设计系统的 Surface，再绘制页面内容。
- 页面切换动画只能包裹 `NavHost` 内容；页面级模糊宿主与不透明 Surface 必须像设置页一样位于动画之外，保持 Backdrop 录制与消费坐标稳定。
- `layerBackdrop` 的祖先子树不得包含引用同一 Backdrop 的模糊消费者；否则 Android HWUI 会把 GraphicsLayer 重新挂回自身，形成 RenderNode 无限递归。
- 柔光描边只证明材质进度已触发，不代表采样区域存在可辨识内容；短页面需确保真实内容能滚动到顶栏下方，避免只模糊均匀 Surface。
