# Miuix Progressive Blur Migration Module Plan

## 模块拆分

- `contract`：复用 Miuix `ProgressiveBlur.Top` 与 `progressiveTextureBlur` 的公开契约；不新增应用级 API。
- `domain`：复用 `AdvancedMaterialSpec.enabled` 作为“高级材质已开启”的唯一开关。
- `presentation`：`ProgressiveTopBarMaterial` 选择 Miuix backdrop 渐进模糊或既有透明度渐变；`AdaptiveMaterialScaffold` 与导航宿主维持现有调用关系。
- `infra`：将两个 Android Miuix artifact 升级到 `0.9.4-rc01`，并同步 AGP 9.1.1、Gradle 9.3.1、Kotlin 2.4.10 与 minSdk 33。

## 平台映射

- iOS：不在本次 Android Gradle 模块范围内。
- HarmonyOS：不在本次 Android Gradle 模块范围内。
- Android：API 33+ 可由 Miuix RuntimeShader 路径执行渐进模糊；高级材质关闭时使用现有透明度渐变。

## 文件拆分策略

- 单个核心文件尽量不超过 1000 行有效代码
- 若必须逃逸，先申请开发者批准

## 风险点

- 候选版依赖解析失败或 API 签名变化会直接阻断编译。
- RuntimeShader 不可用时 Miuix 会跳过 shader 效果，因此透明度层必须继续提供可读性。
