# MIUIX Single Design System and Monet Background Strategy Module Plan

## 模块拆分

- `appearance-contract`：`BackgroundColorStrategy` 与 `AppearancePreferences`；负责旧设计模式偏好的读取迁移和新的单一外观事实源。
- `appearance-infra`：`GlobalUtils` 的持久化规范化、Flow 同步，以及 Material Monet / MIUIX 背景策略输入。
- `theme-and-chrome`：根 `DeadlinerTheme`、颜色 token/MIUIX scheme 工厂、基础组件与自适应导航；只使用 MIUIX 运行时分支。
- `appearance-presentation`：设置与首次引导，去除鲜明模式选择并暴露背景颜色策略。
- `verification-delivery`：迁移单测、静态残留检查、Release 签名和飞书交付。

## 平台映射

- iOS：不改动；本次策略仅为 Android Compose 运行时迁移。
- HarmonyOS：不改动；现有语义背景与 Android 保持色彩意图一致即可。
- Android：`model/` 维护策略契约，`localutils/GlobalUtils.kt` 维护偏好迁移，`ui/theme/` 维护调色与主题，`ui/base/` 和 `ui/settings/` 完成组件/设置收敛。

## 文件拆分策略

- 单个核心文件尽量不超过 1000 行有效代码
- 若必须逃逸，先申请开发者批准
- 不向已接近上限的 `GlobalUtils.kt`、`DesignColorSettings.kt` 堆入新的 UI 分支；背景策略解析保持在模型/主题层，设置页面只调用明确的策略 API。

## 风险点

- 应用仍有 Material3 类型依赖；迁移优先消除应用层“根据设计系统二选一”的组件分支，保留必要兼容类型直到所有页面可安全替换。
- 工作区已有未提交的分类、小组件与材质改动；任何重叠 patch 都必须先保留现有逻辑。
- 旧 SharedPreferences 同时存在多个主题键，必须只读兼容并在下一次写入时规范化。
