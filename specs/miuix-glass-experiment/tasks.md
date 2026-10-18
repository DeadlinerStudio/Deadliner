# MIUIX 官方玻璃实验 Tasks

## 阶段 1：规格冻结
- [x] 定义实验范围、默认关闭、回退和设备限制。
- [x] 核对 PR 源码依赖闭包和可替代范围。

## 阶段 2：基础设施
- [x] 接入固定官方材质实现，记录来源与许可证。

## 阶段 3：功能实现
- [x] 偏好保存、设置开关、实时预览、统一后端分流。

## 阶段 4：验证与回写
- [x] 统一编译、主机测试、规格与文件规模校验，记录视觉验收限制。

## 实现与验证结果
- 开关入口：设置 → 外观 → 高级材质 → MIUIX 官方玻璃（实验性）；默认关闭，依赖高级材质总开关。
- 切换即时生效、保存偏好、预览同步；官方模式隐藏旧光学调参，关闭后恢复全部原始参数。
- 所有 deadlinerGlassMaterial 表面路由至官方 glass 核心；Sheet、对话框、快速添加、Lifi 输入采用官方 PopupViewGlass（60dp）；导航采用 PuredThinGlass（20dp）；按钮使用官方折射着色；普通及渐进顶栏使用 ActionBarMask。渐进顶栏的页面淡出遮罩仍由应用维护。
- 保留应用布局、阴影、按压、页面转场、背景捕获和业务逻辑；这次实验不是整套官方组件替换。
- 使用 third_party/miuix-glass 中的官方材质依赖闭包，保留原始包名、版权和 Apache-2.0。源码固定至 ec4c96963abf206ab2f2b0253a5efca71ba28043。未修改任何 shader、光学常量或 token 值。
- 原 GlassStyles.kt 1736 行，机械拆分为 facade 和四个 token holder；35 个 token 定义逐个与上游核对一致，其余文件 SHA-256 验证通过。所有新增/变更 Kotlin 文件均不超过 1000 有效行。
- `:app:compileDebugKotlin :app:testDebugUnitTest -x :app:syncDeadlinerCoreKmp --offline` 通过。96 套件 / 351 测试，零失败。
- 新增 Robolectric 测试：默认关闭、启用后重新加载仍有效、材质规格正确接收选择、关闭后原偏好完整恢复。
- `git diff --check`、规格校验通过。官方许可证及出处随 APK 的 assets/licenses 打包。
- 未进行物理设备操作或 connected 测试；未进行模拟器视觉验收，尚不能确认大面板实际效果和性能。
