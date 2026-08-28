# Miuix Progressive Blur Migration

## 背景

- 创建日期：2026-08-28
- 功能标识：`miuix-progressive-blur`
- 需求来源：将 Android 端现有手写的顶栏渐进模糊迁移到 Miuix `0.9.4-rc01` 的公开渐进模糊 API。

## 目标

- 将 `miuix-ui-android` 与 `miuix-blur-android` 升级到 `0.9.4-rc01`。
- 将构建工具链升级到兼容该候选库和 compileSdk 37 的 AGP 9.1.1 / Gradle 9.3.1 / Kotlin 2.4.10，并将 minSdk 提升到 33。
- 以 `Modifier.progressiveTextureBlur` 取代顶栏中固定 `BlurEffect` 与自定义 AGSL 淡出 shader 的组合。
- 高级材质开启且存在 backdrop 时，使用 Miuix 的 `ProgressiveBlur.Top` 渐进模糊；关闭高级材质时，保留现有透明度渐变回退。
- 保持顶栏尺寸、状态栏占位、内容层级和现有 tint 视觉策略不变。

## 非目标

- 不重做普通（非渐进）毛玻璃、弹窗模糊、海报模糊或折射效果。
- 不改变高级材质的偏好、调参数据模型或各页面的导航结构。
- 不将候选版本称为稳定版 `0.9.4`。

## 用户场景

1. 用户开启沉浸体验及高级材质时，顶部栏从顶部的模糊背景平滑过渡到底部的清晰背景。
2. 用户关闭高级材质时，顶部栏仍采用原有的透明度渐变，不依赖 backdrop 或 RuntimeShader。

## 验收标准

- `app/build.gradle.kts` 中两个 Miuix Android artifact 均为 `0.9.4-rc01`。
- `ProgressiveTopBarMaterial` 不再包含自定义 `BlurEffect`、`runtimeShaderEffect` 或 AGSL 渐进模糊 shader。
- 高级材质开启路径调用 Miuix `progressiveTextureBlur`，使用 `ProgressiveBlur.Top` 及原有淡出起点。
- 高级材质关闭路径保留既有透明度渐变。
- Android debug 编译与相关单元测试通过。
- Android debug/release APK 构建通过。

## 风险与约束

- 渐进模糊依赖 RuntimeShader/API 33；支持性由 Miuix 内部处理，透明度覆盖层始终保证可读性。
- `miuix-blur-android` 的 AAR 声明 minSdk 33；候选 Miuix 还解析到要求 AGP 9.1 的 Compose 依赖，因此工具链与 minSdk 同步升级。
- `v0.9.4-rc01` 是候选发布，API 已依据固定提交 `4a6b750b578880146876e4ab77097d9b01702413` 核对。
- Miuix 渐进模糊含清晰端合成，GPU 开销高于均匀模糊；仅用于窄顶栏。
