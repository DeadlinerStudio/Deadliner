# Advanced Material Continuous Tuning Module Plan

## 模块拆分

- `model`：`AdvancedMaterialTuning` 定义半径范围、默认值、限制规则和分段饱和度插值；`AppearancePreferences` 保存连续半径。
- `persistence`：`GlobalUtils` 负责旧档位迁移、新 Float 键读写，以及档位快捷选择同步。
- `theme`：`DeadlinerTheme` 从连续半径构造全局 `AdvancedMaterialSpec`，饱和度只由纯模型函数计算。
- `settings presentation`：设置页维护拖动中的临时半径、展示数值并在结束时保存。
- `preview`：独立预览组件共享一个 `LayerBackdrop`，绘制顶部模糊和胶囊玻璃两种消费者。
- `tests`：JVM 单元测试覆盖节点、区间中点与范围限制。
- `fine tuning model`：独立 `AdvancedMaterialFineTuning` 保存所有实验参数、范围限制和默认值，不依赖 Compose。
- `fine tuning persistence`：独立 SharedPreferences helper 管理批量键读写，避免继续扩张 `GlobalUtils.kt`。
- `glass recipe resolver`：`DeadlinerGlassRecipes` 从当前精细参数生成不可变 recipe，Preview 与实际导航共用。
- `advanced controls`：新建独立 Compose 文件承载实验 Slider/Switch/恢复默认，不扩张 `SettingsComponents.kt`。
- `preset export`：独立纯 Kotlin JSON encoder 定义 schema，Compose 屏幕只负责调起 `CreateDocument(application/json)` 并写入 URI。
- `theme neutral surfaces`：HyperOS 中性 palette 负责提供精确整窗背景；同一 token 同时喂给内层 Material3 与外层 MIUIX，避免混用 `#F7F7F7`。
- `default migration`：精细参数偏好记录默认配方版本，只迁移仍等于上一版默认值的高光字段，保留用户手动调参。
- `fine tuning access`：独立的进程内解锁状态记录版本号点击次数；关于页只上报点击，高级材质页只订阅是否解锁。
- `HyperOS semantic tokens`：主题 token 工厂为 HyperOS 解析不透明主/次文字色，并保证 `surfaceVariant` 与 `surfaceContainer` 不发生值别名；Material3 ListItem 与 MIUIX 组件共用，品牌 palette 不受影响。

## 平台映射

- Android：修改 `model/`、`localutils/GlobalUtils.kt`、`ui/theme/Theme.kt` 与 `ui/settings/AdvancedMaterialSettings.kt`。
- iOS：本阶段不修改；未来可把同一 28–256 的归一化值映射到 SwiftUI Material 强度。
- HarmonyOS：本阶段不修改；未来可把连续值映射到 ArkUI blur radius，并复用相同插值节点。

## 文件拆分策略

- 新增纯模型文件承载计算，避免把插值逻辑写进 Compose 或 `GlobalUtils.kt`。
- 精细参数面板与 SharedPreferences 映射各自使用独立文件，单文件目标不超过 400 行。
- 不修改已经超过 1000 行的 `SettingsComponents.kt`。
- Preview 与设置页面如果接近 400 行，则拆为 `AdvancedMaterialPreview.kt`，不形成新的大型文件。

## 风险点

- SharedPreferences 迁移必须区分“键不存在”和“值恰好为默认值”。
- 新默认配方迁移需同时覆盖主模糊半径和所有数值精调字段；不能用整对象替换覆盖用户部分手调值。
- 解锁状态仅限进程内，不写入 SharedPreferences；版本项必须保留原有无障碍点击语义。
- Compose Material3 的 `contentColorFor` 是按颜色值而非 token 身份匹配；若两个背景角色同值，会采用顺序更靠前角色的内容色。HyperOS palette 必须保持 variant/container 可区分。
- Slider 临时值与 `appearanceFlow` 持久值需要单向同步，避免松手回弹或外部快捷档位更新后显示旧值。
- Preview 的捕获背景必须在玻璃层之前绘制，玻璃自身不能被注册进同一个 Backdrop。
- 旧档位属性仍被引导页使用，其 setter 必须原子更新 level 与 radius。
