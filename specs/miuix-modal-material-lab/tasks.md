# MIUIX 弹层材质实验室 Tasks

## 阶段 1：规格冻结

- [x] 明确 Dialog/Sheet 独立调节与生产包装组件复用范围。
- [x] 明确实验初始值、范围、回退策略和非目标。
- [x] 明确不改变 Backdrop 录制层级。

## 阶段 2：基础设施

- [x] 扩展高级材质细调模型与规范化规则。
- [x] 扩展 SharedPreferences 和高级材质预设 JSON 字段。
- [x] 为柔光玻璃材质增加绝对半径与不透明度覆盖能力。

## 阶段 3：功能实现

- [x] 让统一 MIUIX Dialog/Sheet 包装组件消费弹层专属参数。
- [x] 新增实验室四个拖动条、当前值、恢复默认动作。
- [x] 新增真实 Dialog 与 Bottom Sheet 预览入口。

## 阶段 4：验证与回写

- [x] 补充模型规范化、持久化和配方解析测试。
- [x] 执行规格校验、大文件扫描、差异检查和相关单元测试。
- [x] 执行 Debug 与 Release/R8 编译并回写真实验证结果。
- [ ] 在在线真机上确认透明度、模糊、深浅色与弹层可读性；无设备时标记待验证。

## 阶段 5：参数固化与沉浸 Dialog 优化

- [x] 固化 Dialog 65%/80dp、Sheet 70%/80dp，并迁移旧默认而保留自定义值。
- [x] 将沉浸式 Dialog 主体改为标准高级材质链路，只保留弱边缘折射高光。
- [x] 移除 Dialog 边缘镜片的色散与折射后二次模糊，保留无着色器时的高级材质回退。
- [x] 补充默认值迁移、规范化和弱边缘配方测试。
- [x] 执行全量 Debug 单元测试与 Release/R8 构建。
- [x] 修复大半径模糊降采样后折射 Shader 仍使用全分辨率坐标导致的透明边界采样。
- [x] 增加 Shader 几何坐标随 `downscaleFactor` 同步缩放的回归测试。
- [x] 根据真机反馈移除 Dialog 自定义折射 Shader，回退为纯高级材质加 0.5dp 弱渐变高光描边。
- [x] 更新配方测试，确保 Dialog 折射关闭且高光描边参数稳定。

### 2026-09-03 验证记录

- Miuix 依赖确认仍为 `0.9.4-rc01`；本地源码确认 `textureBlur` 的半径单位为 dp、范围由 `BlurDefaults.MaxBlurRadius` 钳制。
- Dialog 与 Sheet 生产包装组件现分别读取专属背景不透明度和绝对模糊半径；沉浸柔光玻璃与非沉浸纹理模糊分支共用相同解析结果。
- 实验室页面提供四个全局滑块、恢复实验初始值以及真实 Dialog/Sheet 预览；预览弹层内部也可继续拖动并即时观察自身材质变化，松手后持久化。
- 新增 3 项测试并扩展 2 项既有测试；全量 181 项 Debug 单元测试零失败，`assembleDebug`、Release Kotlin、R8、`lintVitalRelease`、规格校验、XML 校验与 `git diff --check` 通过。
- 技能建议的 `scripts/check_large_core_files.py`/`core_suffixes.txt` 组合在当前工作区不可用，已用 Kotlin 文件行数扫描替代；新增实验室文件 311 行、新增模型文件 40 行，均低于 1000 行。
- ADB 当前没有在线设备，运行时着色器、深浅色和实际弹层可读性的真机确认待用户安装后完成。
- Release 已使用正式证书签名为 `Deadliner-v5.0.0-miuix-modal-material-lab-signed.apk`；APK Signature Scheme v3、单签名者、16 KiB ZIP 对齐均校验通过，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，文件 SHA-256 为 `a5278337108a9541f1a7a563ca6b20fe96ea932af05ff0bce24c23f6abc41986`。
- 已通过飞书机器人发送给 Aritx Zhou，消息 ID：`om_x100b66a69d302ca8c37ffe20db383fb`。

### 2026-09-03 参数固化与 Dialog 边缘材质验证

- 最终默认值固化为 Dialog 65%/80dp、Sheet 70%/80dp；偏好默认版本升级到 4，只迁移版本 3 的旧默认值，用户已调整的数值保持不变。
- 沉浸式 Dialog 改用 Miuix `textureBlurEffect` 作为高级材质主体，继续使用主题 `surfaceContainer`；边缘配方为默认 9dp 宽、约 1.8dp 位移、0.12 深度、无色散、无折射后二次模糊，浅色高光 alpha 约 0.171。
- 全量 183 项 Debug 单元测试零失败；Release Kotlin、R8、`lintVitalRelease`、规格校验与 `git diff --check` 通过。ADB 查询无在线设备，真机视觉确认待安装后完成。
- 正式签名 APK：`Deadliner-v5.0.0-dialog-advanced-edge-signed.apk`；APK Signature Scheme v3、单签名者、16 KiB ZIP 对齐均通过，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，文件 SHA-256 为 `e8f73a0f80a424f845cfbce7b077c4a998b731ec611f82ad52f38af1e60c11be`。
- 已通过飞书机器人发送给 Aritx Zhou，消息 ID：`om_x100b66a705ac2ca0c11ba8b7b1d7bab`。

### 2026-09-03 Dialog 降采样坐标修复验证

- 用户截图确认 Dialog 左下角出现不规则透明/重复采样带，而同页 Sheet 正常；与 Miuix `textureBlurEffect` 大半径降采样后，自定义折射 Shader 仍使用全分辨率坐标的实现差异吻合。
- 已将内容原点、内容尺寸、圆角、折射带宽、位移与色散距离统一除以实际 `downscaleFactor`；新增 8 倍降采样坐标回归测试。
- 全量 184 项 Debug 单元测试零失败；Release Kotlin、R8、`lintVitalRelease`、规格校验与 `git diff --check` 通过。无在线设备，修复后的真机视觉结果待用户安装确认。
- 正式签名 APK：`Deadliner-v5.0.0-dialog-downsample-fix-signed.apk`；v3、单签名者、16 KiB ZIP 对齐均通过，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，文件 SHA-256 为 `a54b28d4d5361f96bc25c0f8524e7d2ebe7c13ec6a5e46a17849d5bfbf2bd111`。
- 已通过飞书机器人发送给 Aritx Zhou，消息 ID：`om_x100b66a7cd43f8a8c3892ecec425e7a`。

### 2026-09-03 Dialog 纯高级材质回退

- 真机继续观察到由边缘向内扩散的光，说明即使修正降采样坐标，自定义折射仍不适合与 Dialog 的大半径高级材质串接；上一节的折射方案由本方案取代。
- Dialog 已完全移除自定义折射、色散和二次模糊 Shader，改用 Miuix 标准高级材质，并仅叠加 0.5dp、不重新采样内容的弱渐变高光描边；Sheet 保持现有柔光玻璃实现。
- 配方测试明确断言 Dialog 的 `refraction.enabled` 为 `false`，并校验描边宽度与深浅色高光强度。
- 全量 183 项 Debug 单元测试零失败；Release Kotlin、R8、`lintVitalRelease`、规格校验与 `git diff --check` 通过。无在线设备，最终真机视觉结果待用户安装确认。
- 正式签名 APK：`Deadliner-v5.0.0-dialog-advanced-highlight-signed.apk`；v3、单签名者、16 KiB ZIP 对齐均通过，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，文件 SHA-256 为 `82ea9f5d74c00676b8111128f4175a888009c89395323ce120f999451d6846d1`。
- 已通过飞书机器人发送给 Aritx Zhou，消息 ID：`om_x100b66a7f24924a0c2ee66eb7180fa6`。
