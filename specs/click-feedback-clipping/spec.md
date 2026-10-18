# 点击反馈同形状裁切审计

## 背景
MiuixIndication 默认绘制矩形按压层；background(shape) 仅限制底色，Card/Surface 的内部 clip 也无法约束更外侧的 clickable indication。

## 目标
审计应用内 clickable/combinedClickable/selectable、Material 按压适配及其父容器裁切。可见圆角/圆形容器的反馈必须使用同一形状，且 clip 位于 indication 前；保留布局、点击范围、语义、长按、禁用及 Sink 行为。

## 非目标
不为普通矩形列表行随意添加圆角；不更改 Glance 桌面宿主和系统控件效果，不操作设备。

## 用户场景
Lifi 轮播、记忆与偏好、卡片、快捷操作和各页面的圆角选项按压时不会露出矩形色块。

## 验收标准
完成应用范围入口清单与分类；修复缺失 clip、外层 indication 和默认形状不匹配。实际 Lifi 控件及共享反馈的主机绘制回归检查角点/中心颜色，交互回归与 Kotlin 编译通过。

## 风险与约束
先裁切再画反馈；共享父卡片内部行沿用父级外轮廓。阴影/外边距不能被错误裁切，不改变导航拖拽反馈或空白遮罩。保留现有 ripple 回退存档。

## 习惯编辑补充要求
Material 连体 Chip/ToggleButton 保持原有 connectedButtonShapes 的单边圆角与选中/按下变形；这些控件局部恢复 Material 内部反馈，移除外层 Sink/胶囊裁切，作为全局反馈策略的明确例外。其他静态 Chip 使用组件 Defaults.shape。
