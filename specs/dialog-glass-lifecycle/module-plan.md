# Deadliner Dialog 柔光玻璃与退场 Module Plan

## 模块拆分
- AdvancedMaterialDialogBlurHost：共享宿主状态和根生命周期。
- RetainedMiuixDialogHost：请求注册、退出保留、上下文恢复及输入拦截。
- MiuixModalSurface：统一玻璃策略与 MiuixDialog 公共入口。
- WindowGlassBackdropHost：复用窗口采样，嵌套时不重复录制。

## 平台映射
仅 Android ui/base，DeadlinerTheme 已统一提供 AdvancedMaterialDialogBlurHost，不逐页改业务调用。

## 文件拆分策略
生命周期单独文件，玻璃策略保持现有文件，所有核心文件少于 1000 有效行。

## 风险点
根录制的反馈循环、外部条件销毁、退出期间重复点击、局部主题/材质上下文、窗口配置变化。

颜色修复由 RetainedMiuixDialogHost 在弹层子组合恢复调用方 locals 后统一完成；同时桥接 Material3/MIUIX 内容色与 Material3 默认 TextStyle，按钮自身仍覆盖局部内容色。
