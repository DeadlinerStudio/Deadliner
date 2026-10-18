# Dialog 窗口级柔光玻璃采样

## 背景
当前屏幕 recorder 位于根 Scaffold 正文内，无法采集 Scaffold 的 Sheet 浮层；OverlayDialog 又与 recorder 同窗口，外移后会形成自采样风险。

## 目标
根 WindowGlassBackdropHost 包住整个 Scaffold（含 Sheet）；Dialog 使用 MIUIX WindowDialog 独立窗口采样下面的 Compose 窗口。保留材质参数、禁用材质回退、主题颜色、条件调用退场动画和确认语义。

## 非目标
不采样其他应用或系统窗口，不申请录屏权限，不改 Sheet 为独立窗口，不把整窗 recorder 用于本窗口内玻璃。

## 验收标准
Sheet 内容能继承根窗口 recorder，Sheet 内打开 Dialog 使用相同 recorder，Dialog 本身位于独立窗口；开关/退出采样消费者释放、深浅色与条件退出行为通过主机测试；Kotlin 编译通过。实际像素效果仅能由后续授权的模拟器/设备视觉检查确认。

## 用户场景
在添加/修改任务 Sheet 内打开确认或选择 Dialog，材质后面看到模糊后的当前 Sheet 内容；关闭 Dialog 后继续编辑 Sheet。

## 风险与约束
根 recorder 仅供独立窗口消费，禁止同窗玻璃采样它，以防 RenderNode 自引用；Dialog 内保留独立窗口 LocalView，业务 locals 在创建窗口之前恢复。仅本地主机检查，禁止操作主设备。
