# Dialog Window Backdrop Module Plan

## 模块拆分
1. ui/theme：窗口 recorder 移至根 Scaffold 外，覆盖页面和 Sheet 浮层。
2. ui/base：RetainedMiuixDialogHost 从 OverlayDialog 改为 WindowDialog，保留 retained 生命周期；WindowGlassBackdropHost 的跨窗口采样约束不变。
3. 主机回归：Sheet 内来源一致、独立 Dialog 窗口、退出释放和主题颜色。

## 平台映射
仅 Android Compose 层；不改 KMP 数据或其他平台。

## 文件拆分策略
现有小文件内调整，不增加大型聚合文件。

## 风险点
OverlayDialog 同窗绘制，不能直接消费整个 Scaffold 的 recorder；改用 WindowDialog 后才允许采样该层。不得在窗口创建后恢复旧 LocalView，否则 Dialog 内容误拿到 Sheet 的 View。
