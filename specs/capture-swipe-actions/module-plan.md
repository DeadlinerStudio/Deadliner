# 灵感卡滑动操作模块计划

## 模块拆分
SwipeableItemCard/TaskCardSwipeRules 支持单尾部操作与仅展开主操作；InspirationLightCard 单独持有外观与回调适配；CaptureContent 持有全列表唯一的 activeSwipeKey。

## 平台映射
Android capture/ui 接入既有捕获 ViewModel，跨平台业务不变。

## 文件拆分策略
从 CaptureScreen 提取灵感卡与时间格式化，避免屏幕文件膨胀。

## 风险点
单删除按钮锚点关闭、多选手势禁用、宽屏双列、右滑误触发 AI、主页共享组件回归。
