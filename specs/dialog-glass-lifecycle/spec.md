# Deadliner Dialog 柔光玻璃与退场

## 背景
统一 MiuixDialog 使用关闭折射的简化材质并依赖页面采样。部分调用方以 if(show) 条件创建弹窗，关闭时组件直接销毁，MIUIX 退出时序中断。

## 目标
统一 Dialog 接入既有柔光玻璃配方和窗口共享采样，宿主保留退出中的弹窗，兼容 show=false 与条件移除。

## 非目标
不改变业务回调时机、不启用官方玻璃实验、不调整其他平台、不操作物理设备。

## 用户场景
设置提示、删除确认、编辑器提示、带输入框弹窗，通过取消、确认、返回或外部状态关闭。

## 验收标准
高级材质启用时使用真实柔光配方和窗口采样；关闭时完整播放 MIUIX 260ms 退场后移除。条件销毁和持续 show=false 两种调用均支持；退出时不可二次操作；禁用高级材质时语义实色回退；内容 CompositionLocal 上下文保持一致。

## 风险与约束
窗口录制不能包含正在采样的弹窗；嵌套宿主复用 root；保留最后内容闭包到退出完成，宿主销毁时释放。使用缓存 0.9.4-rc01 OverlayDialog/DialogContentLayout 源码确认生命周期和 onDismissFinished。

## 实现结果
复用 DeadlinerTheme 已提供的根 AdvancedMaterialDialogBlurHost，增加保留请求队列。MiuixDialog 注册标题、说明、内容与当前 CompositionLocalContext；show=false 或调用方销毁均转为宿主中的隐藏状态，OverlayDialog 播放原生 MIUIX 260ms 退出后由 onDismissFinished 清除请求。退出内容保持绘制但清除操作语义并消费触摸，业务回调仍在点击时执行。

根宿主提供一个 WindowGlassBackdropHost，嵌套宿主复用现有窗口录制。Dialog 渲染在根 Scaffold 弹层分支，位于录制内容之外；仅显示/退出期间启用录制，避免反馈循环。材质使用 DeadlinerGlassRecipes.dialog 的完整柔光配方，保留微调参数、主题颜色及模糊关闭时的不透明回退。原有 BottomSheet 页面采样选择不变。

主机 Compose 测试覆盖条件销毁、持续 show=false、确认回调、禁用材质不录制、启用玻璃退出后释放采样，以及调用方局部上下文保持。手动时钟测试显式发送 Snapshot apply 通知，确保外部状态提交后才检验退场时间。设备 HWUI/RuntimeShader 实际视觉效果未验证，未运行设备截图、安装或 connected 测试。

MIUIX 根弹层会再次跨越 subcomposition 边界，局部上下文测试发现仅在注册 OverlayDialog 外恢复上下文不足。最终在实际 content 插槽内再次恢复调用者的 CompositionLocalContext，并将 LocalDismissState 绑定到当前弹窗。此回归测试曾暴露正文读到根默认值，修复后用于持续检查局部内容不丢失。

最终 Kotlin 编译与 381 个完整主机测试通过（102 个套件、零失败/错误），含 4 个新 Dialog Compose 生命周期/上下文测试。规格与差异/文件规模校验通过，未进行设备操作。

## Dialog 内容颜色
在根弹出层恢复调用方 CompositionLocals 后，显式提供 Dialog 的 Material3 与 MIUIX LocalContentColor；Material3 默认 TextStyle 的颜色保持 Unspecified，使默认文字/图标继承 Dialog 主题颜色，显式文本颜色和按钮的局部内容颜色仍优先。覆盖明暗模式及退出期间的保留内容。
