# MIUIX Floating Tab Bar

## 背景

- 创建日期：2026-08-16
- 功能标识：`miuix-floating-tab-bar`
- 需求来源：为 Android 的“现代”设计风格制作一版胶囊状悬浮底部 Tab，先通过 Compose Preview 冻结视觉方案。

## 目标

- 提供只在 `AppDesignSystem.MIUIX` 下渲染的可复用 Compose 组件。
- 外层悬浮容器固定高 56dp、左右各留出 8dp，内部间距为 2dp，图标为 28dp；选中胶囊额外保留 2dp 横向、1dp 纵向留白。
- 图标与文字使用 MIUIX 黑、白、灰中性色；选中态使用半透明灰色胶囊。
- 同一组件支持图标与文字横向排列，以及文字位于图标下方的纵向排列。
- 支持 Deadliner 现有高级材质模糊参数、柔光阴影和克制的整圈渐变边缘发光。
- 在底栏覆盖区域提供从屏幕底部背景色到 Tab 顶边 0 alpha 的渐进遮罩。
- 提供可交互的浅色与深色 Preview，方便后续视觉评审。
- 提供默认关闭的外观设置开关，并把纵向悬浮版接入 Compact 手机宽度断点。

## 非目标

- 不默认替换 `AdaptiveNavigationSuiteScaffold` 中正在使用的底部导航，仅在开关和 Compact 条件满足时启用。
- 本阶段不修改 Material 3 /“生动”设计风格。
- 本阶段不调整平板与桌面宽布局使用的 Navigation Rail。

## 用户场景

1. 开发者在 Android Studio Preview 中查看浅色和深色背景上的悬浮底栏，并点击不同 Tab 检查选中动画。
2. 用户在“现代”设计设置中打开悬浮底栏后，Compact 手机窗口使用纵向悬浮版。
3. 用户关闭开关、切换到 Material 3，或把窗口扩展到 Medium 及以上宽度时，继续使用原有导航。

## 验收标准

- MIUIX 下渲染 56dp 高的胶囊容器，内部间距为 2dp，并在组件左右各留出 8dp。
- 选中胶囊在 Tab 切换时平滑移动，选中图标和文字同步切换为高对比中性色。
- `Horizontal` 与 `Stacked` 两种布局共享材质、描边、状态和交互语义，并能在 Preview 中直接对比。
- 设置开关持久化，默认关闭且仅在“现代”设置区显示。
- 实际接入同时满足 MIUIX、开关开启、宽度小于 600dp、`ShortNavigationBarCompact` 四项条件。
- 实际接入使用 `Stacked` 布局；Medium、Expanded 与 Rail 不受影响。
- 高级材质启用且提供 `LayerBackdrop` 时使用纹理模糊；未提供时仍有可读的半透明降级背景。
- 外层容器绘制一圈低亮度渐变发光描边；选中胶囊保持无描边，通过材质与灰度区分层级。
- 底栏覆盖区复用 AI 页面同类 `edgeFade` alpha 遮罩：屏幕最底部保持 MIUIX `surface` 背景色，到 Tab 顶边恰好过渡为透明。
- Material 3 下不渲染该组件。
- 新增 Kotlin 文件通过 Android 编译，规格校验和大文件扫描通过。

## 风险与约束

- MIUIX blur 依赖 backdrop 捕获；Preview 或调用方缺少 backdrop 时无法展示真实背景折射，因此必须有非模糊降级层。
- 56dp 高度下，纵向版使用 28dp 图标和 11sp / 12sp 行高的 label，并让图标与文字直接相邻；两种布局均对文字做单行截断。
- Tab 保留 `selectable` 交互与无障碍语义，但禁用 indication，不显示点击 ripple。
- 从选中胶囊按下时胶囊缩放至约 94%；横向拖动超过系统触摸阈值后由单一 `Animatable` 跟手移动。
- 拖动松手时按手指所在 Tab 区域确定目标，先无动画吸附再更新选中状态，避免重播旧 Tab 到新 Tab 的滑动动画或误落到相邻 Tab。
- Compact 接入在系统导航栏上方保留 4dp 底部间距。
- 渐进遮罩高度由系统导航栏高度、4dp 底部间距和 56dp Tab 高度共同计算，不覆盖 Tab 顶边以上的内容。
- 组件 API 保持展示层纯粹，不直接依赖 Navigation Controller；宿主负责把现有导航状态映射为 Tab item。
- 开发前安全点提交：`d26667d`。

## 实现与验证记录

- 已实现横向与纵向两种独立布局，以及浅色/深色交互 Preview；本阶段接入 Compact 手机主界面导航。
- `:app:compileDebugKotlin` 于 2026-08-16 通过；仅保留仓库既有的 compile SDK、Manifest 与 `GlobalUtils.kt` 警告。
- 规格校验通过。
- Kotlin 1000 行扫描发现 7 个既有大文件（其中 3 个为生成/缓存绑定）；本功能未修改这些文件，新增文件分别为 299 行与 208 行。
- 快照提交后出现的 `res/drawable/ic_back.xml` 并发修改被完整保留，未纳入本功能。
