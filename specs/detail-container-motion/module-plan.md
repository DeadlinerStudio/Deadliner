# 详情共享容器动画 Module Plan

## 模块拆分
- TaskDetailAnchor：卡片绘制层与窗口几何捕获、点击发起。
- TaskDetailSnapshotStore：限额内存传递快照，不通过 Intent 传图片。
- TaskDetailMotionSpec：几何、透明度、时长/缓动、无效几何校验。
- TaskDetailMotion：裁切容器、来源画面和详情内容合成、输入拦截。
- DeadlineDetailActivity：快照消费、关闭完成、任务删除与配置变化处理。

## 平台映射
仅 Android app/src/main/java/com/aritxonly/deadliner/ui/detail 及详情 Activity；其他平台不变。

## 文件拆分策略
锚点、纯动画模型、渲染与缓存分文件，避免单文件堆叠。

## 风险点
源卡片部分离屏、双击、异步快照失败、旋转时失效的坐标、删除后列表重排，以及动画被快速返回中断。

## 分层修订
DDLItemCardSimplified 向 TaskDetailAnchor 提供原始背景、进度颜色与比例，独立记录前景 GraphicsLayer。交接缓存传递单张前景图及轻量绘制元数据，不增加多张位图。MotionSpec 拥有纵向/横向展开、文字/进度淡化、背景交叉渐变、详情上浮各阶段时序。

## 空间连续性修订
Motion 使用独立的容器几何与内容浮入 Animatable；两者中断时保留当前状态。MotionSpec 提供统一的内容位置/等比尺寸映射，渲染层将详情内容固定到当前容器左上角。有效来源的返回末尾保持容器不透明，由最终几何对齐交接，避免底层卡片与移动前景双重曝光。
