# Android 浮光语义背景 Module Plan

## 模块拆分

- `contract`：`model/semantic/SemanticContract.kt` 定义任务语义状态、页面 tone、快照与习惯输入。
- `domain`：`model/semantic/DeadlineStatusResolver.kt` 与 `PageToneResolver.kt` 负责单条优先级、24 小时阈值和当前可见集合聚合；保持纯 Kotlin、可单测。
- `presentation`：`ui/semantic/SemanticPalette.kt` 把清单状态/tone 映射到 Android 既有进度条调色结果，并为概览、灵感提供 HarmonyOS 同值的明暗模块色；`ui/semantic/SemanticTopAtmosphere.kt` 只消费颜色和柔光玻璃进度，按固定高度绘制低透明度背景。
- `integration`：`ui/main/modern/ModernSemanticAtmosphere.kt` 在筛选完成后生成快照，按当前主界面选择动态清单 tone 或固定模块色；`ModernHost.kt` 复用 `LocalTopBarScrollProgress` 驱动概览/灵感渐隐，在总开关关闭或进入 Lifi 时不装配氛围层。
- `page-surface`：`OverviewContent` 仅在现代主页嵌入场景允许透明页面背景，保留独立 Activity 的原背景；`CaptureContent` 的普通列表与宽屏瀑布流都注册同一个顶部材质滚动观察器。
- `preferences`：`AppearancePreferences`、`GlobalUtils` 复用现有外观偏好读写与 `appearanceFlow`；`AppearanceSettingsScreen` 增加一个默认开启的全局显示开关。
- `tests`：`app/src/test/.../{model,ui}/semantic/` 覆盖状态优先级、边界时刻、聚合规则、颜色角色和滚动几何纯函数。

## 平台映射

- iOS：仅作为固定语义蓝与三通路分离的产品参照，本轮不修改。
- HarmonyOS：映射 `common/semantic/{SemanticContract,DeadlineStatusResolver,PageToneResolver,SemanticPalette}.ets` 与 `pages/components/semantic/SemanticTopAtmosphere.ets`。
- Android：采用现有 `ui / model` 风格；领域规则放 `model/semantic`，Compose palette/背景放 `ui/semantic`，宿主只负责装配。

## 文件拆分策略

- 单个核心文件尽量不超过 1000 行有效代码
- 若必须逃逸，先申请开发者批准
- 不把 resolver、palette、Canvas 绘制和宿主装配合并为一个大文件。
- `ModernHost.kt` 已接近高复杂度，只添加快照装配与一个背景调用；复杂逻辑全部下沉到新模块。

## 风险点

- Android 进度条色含状态 alpha；浮光 token 必须保留同源 RGB/配色方案，同时使用独立氛围强度，避免重复 alpha 导致几乎不可见。
- HarmonyOS 模块色需要按系统明暗模式成对映射；它们只用于概览与灵感根页面，不能覆盖清单的进度条同源状态色，也不能进入 Lifi。
- 浮光与柔光玻璃必须调用同一个 `resolveTopBarButtonMaterialProgress`，禁止复制一组相似阈值导致两种效果错拍。
- 分类筛选目前在宿主与习惯列表中分别计算；必须复用相同匹配规则和可见分类集合。
- 宽屏同时展示任务与习惯，需要稳定聚合策略：优先表达任务风险；无任务风险时再使用习惯完成状态。
- 现有搜索转场存在用户未提交修改，集成时仅做最小相邻变更并复核 diff。
