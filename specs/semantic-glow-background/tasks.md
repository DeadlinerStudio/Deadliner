# Android 浮光语义背景 Tasks

## 阶段 1：规格冻结

- [x] 读取白皮书与 HarmonyOS 现有实现
- [x] 完成 `spec.md`
- [x] 完成 `module-plan.md`
- [x] 完成需求、三通路边界与现有 Android 配色体系自检

## 阶段 2：基础设施

- [x] 建立语义 contract、Deadline resolver 与页面聚合 resolver
- [x] 建立复用进度条颜色方案的 Compose palette
- [x] 为纯领域规则补齐单元测试

## 阶段 3：功能实现

- [x] 实现顶部渐变、双光晕、底部淡出和滚动收缩
- [x] 接入现代首页任务/习惯当前筛选快照
- [x] 让任务卡状态判定复用 24 小时语义 resolver
- [x] 复核紧凑/宽屏、浅色/深色及多进度条配色方案
- [x] 大型实现批次完成后再统一编译

## 阶段 4：验证与回写

- [x] 执行规格校验
- [x] 执行大文件扫描（技能包约定脚本在本仓库/技能目录均不存在，已按同一有效行规则人工扫描；最大为 `ModernHost.kt` 919 行）
- [x] 执行相关单测与完整 Android 单元测试
- [x] 编译 debug/release，生成并校验签名 APK
- [x] 回写最终实现状态与验证结果
- [x] 通过飞书发送签名 APK

## 阶段 5：视觉反馈修订

- [x] 对照 iOS `SemanticTopAtmosphereOverlay` 与 Android 柔光玻璃阈值冻结修订规格
- [x] 将主渐变与双光晕降为低透明度，并改为 iOS 式左右错位构图
- [x] 将浮光改为固定 `224dp` 高度
- [x] 使用柔光玻璃的同一 `4dp–28dp` 阈值和曲线驱动浮光透明度
- [x] 更新几何/透明度测试并完成 Android 构建验证

## 阶段 6：其他主界面与总开关

- [x] 对照 HarmonyOS 根页面模块色和“顶部浮光”设置冻结规格
- [x] 为概览、灵感、Lifi 接入对应的浅色/深色模块语义色
- [x] 将“顶部浮光”加入 Android 外观偏好与设置页，默认开启并即时生效
- [x] 验证关闭时不创建浮光绘制层，且清单进度条配色映射不受影响
- [x] 补齐模块色/开关相关测试并完成完整 Release 构建
- [x] 生成同证书签名 APK 并通过飞书交付

## 阶段 7：页面滚动与可见性修正

- [x] 定位灵感滚动不联动、概览被背景遮挡及 Lifi 错误显示的根因
- [x] 灵感普通列表与宽屏瀑布流复用柔光玻璃滚动进度驱动浮光渐隐
- [x] 概览嵌入主页时移除遮挡浮光的不透明页面底色，并保留独立 Activity 原样
- [x] 从 Lifi 装配路径移除顶部浮光及无用 AI 模块色
- [x] 更新回归测试并完成完整 Release 构建
- [x] 使用原证书签名并通过飞书交付修订 APK

## 验证记录

- `validate_feature_spec.py specs/semantic-glow-background`：通过。
- `./gradlew testDebugUnitTest assembleRelease`：通过，`BUILD SUCCESSFUL`。
- `git diff --check`：通过。
- 签名校验：APK Signature Scheme v3；证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，与历史发布证书一致。
- 飞书交付：机器人消息 `om_x100b66502e3cecb0dd8e99eada76f8d`，发送到已验证本人账号 `Aritx Zhou`。
- 视觉修订验证：`./gradlew testDebugUnitTest assembleRelease` 通过，修订包签名证书保持不变。
- 视觉修订交付：`Deadliner-v5.0.0-semantic-glow-v2-signed.apk`，SHA-256 为 `4955429cf042601118df23ae14b6a589d8cbe67b237a230b60902d4c5e38bd8e`；飞书消息 `om_x100b665166c2a8a4c32c33504d6a4d8`。
- ADB 设备检查：当前无已连接设备；因此本轮无真机截图证据，交付以领域测试、完整单测、Debug/Release 编译与签名校验为准。
- 其他主界面与总开关验证：HarmonyOS 模块色逐值单测通过；`AppearancePreferences` 默认/关闭状态单测通过；`./gradlew testDebugUnitTest assembleRelease` 通过（`BUILD SUCCESSFUL`）。
- 本轮 ADB 检查受沙箱限制无法启动本机 ADB server，未新增真机截图；不影响单测、Lint Vital、R8 与 Release 打包结果。
- 用户明确授权后完成签名：`Deadliner-v5.0.0-semantic-glow-v3-signed.apk` 使用 APK Signature Scheme v3；证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，APK SHA-256 为 `bc303530838d8dc1f395e90985dc3871d9068e092ca39f24c3ac38fe47e35ad3`。
- 其他主界面与总开关交付：飞书机器人消息 `om_x100b66518c7f68a8c234e87475ee0c1`，发送到已验证本人账号 `Aritx Zhou`。
- 页面滚动与可见性修正：`./gradlew testDebugUnitTest assembleRelease` 通过（`BUILD SUCCESSFUL`）；规格校验与 `git diff --check` 通过，`ModernHost.kt` 仍低于 1000 行限制。
- 修订签名包：`Deadliner-v5.0.0-semantic-glow-v4-signed.apk` 使用 APK Signature Scheme v3；证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，APK SHA-256 为 `fa5102a1268c74eee12a1e5c85dc8d23e5e9e7bed3f96292147e5bf866b17cbb`。
- 页面修订交付：飞书机器人消息 `om_x100b66521b7bb8acb2ee9cf482ae66b`，发送到已验证本人账号 `Aritx Zhou`。
