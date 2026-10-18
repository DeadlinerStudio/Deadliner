# 全局 MIUIX 点击反馈

## 背景
普通 clickable 被内层 MaterialTheme 覆盖为 ripple，Material 控件内部还显式创建 ripple；少量业务代码显式调用 ripple。

## 目标
全局列表点击、选择与长按使用官方 MiuixIndication 半透明按压层；Material 内部 ripple 关闭，按钮与玻璃操作控件接入官方 SinkFeedback + 按形状裁切的半透明层，已有选中底色的导航项仅 Sink。移除玻璃自研按压缩放/光学增强，保留切换与拖拽动效。disabled、滚动取消、长按和点击回调保持正确。

## 非目标
不改变控件布局/颜色/点击范围，不修改 MIUIX 或 Material 上游源码，不影响系统窗口与桌面小组件的宿主效果。

## 用户场景
设置行和任务卡按下后整块半透明变色，没有扩散水波或缩放；按钮与玻璃按钮 Sink 下沉并出现柔和按压层，选中导航项只下沉。

## 验收标准
主题位于 MaterialTheme 内层提供 MiuixIndication 和 LocalRippleConfiguration=null；业务无 ripple 调用。Material 控件有按压适配、玻璃自研按压逻辑移除且不重复缩放。主机测试验证全局主题、按压/取消/禁用与回调，Kotlin 编译通过。

## 风险与约束
MIUIX 0.9.4-rc01，核对缓存版本的 Pressable.kt/PressFeedback.kt；禁止设备操作。仅 Android UI，保留多种主题与可访问性语义。

## 第二轮反馈调优
改造前完整源码快照与恢复 Material ripple 的逆向补丁保存在 .local-history/press-feedback-20261018。全局列表使用 MIUIX 半透明静态按压层（无扩散水波）；按钮/玻璃按钮使用官方 SinkFeedback + 按形状裁切的半透明层。已有选中底色的玻璃导航项只使用 Sink，保留拖拽/切换动效，移除自研按压缩放及按压光学变化。按压层遵循深浅色 onSurface。保持点击、长按、禁用与滚动取消行为。
