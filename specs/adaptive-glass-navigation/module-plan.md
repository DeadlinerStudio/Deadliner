# 自适应柔光玻璃导航 Module Plan

## 模块拆分

1. ui/navigation：侧栏材质、展开动画、选择手势；悬浮 dock 接受 Horizontal / Stacked。
2. ui/base：按窗口宽度选择 host，统一渐进顶栏和 backdrop，删除旧导航布局。
3. model / localutils / ui/settings：删除沉浸偏好和设置项。
4. ui/theme / ui/material/glass：统一渐进材质与模态材质，保持高级材质降级。

## 平台映射

Android：app/src/main/java/com/aritxonly/deadliner 对应模块。
iOS / HarmonyOS：本轮没有变更，各端继续使用各自现有导航体系。

## 文件拆分策略

新侧栏与 host 独立文件；AdaptiveNavigationSuiteScaffold 删除旧实现。
既有 ModernHost 与 GlobalUtils 仅局部接线和删减，不新增业务职责。

## 风险点

侧栏采样只捕获内容；浮动计时器与 FAB 保持 sibling chrome；导航使用窗口宽度，避免受横屏高度分类影响。
