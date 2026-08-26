# MIUIX 预设颜色 Module Plan

## 模块拆分

- `model`：扩展颜色来源枚举，表达预设颜色这一稳定状态。
- `localutils`：读取、迁移、持久化颜色来源，并提供原子化选择接口。
- `ui/theme`：定义预设 ColorScheme、合并中性色令牌、提供主页 FAB 语义色，并在 MIUIX 工厂中隔离 Material 活力辅助色与 MIUIX 控件中性 secondary。
- `ui/settings`：在现代设计的颜色选择器中暴露预设入口。
- `ui/material/glass`：消费主页 FAB 语义色，保留现有玻璃合成与按压行为。
- `ui/base`：确保 MIUIX Checkbox/Switch 从当前主题读取选中前景，不使用独立默认色板。
- `test`：验证固定色值、来源迁移与主题语义。

## 平台映射

- Android：本阶段完整实现，目录为 `app/src/main/java/com/aritxonly/deadliner` 与 `app/src/test/java/com/aritxonly/deadliner`。
- HarmonyOS：仅作为 FAB 色值参考，本阶段不修改 Harmony 工程。
- iOS：当前工作区无对应实现，本阶段不修改。

## 文件拆分策略

- 新建 `MiuixPresetColorSchemes.kt`，避免继续扩大 `DeadlinerColorTokenFactory.kt`。
- 新建 `HomeFabColors.kt`，将业务语义色与通用 Material `primary` 解耦。
- 偏好迁移保持在现有 `GlobalUtils` 主题读写入口内，不新增重复存储层。
- 选择器只负责展示与事件，不在 UI 内生成 ColorScheme。

## 风险点

- Compose 默认参数可能在不同主题作用域求值，需要测试最终 ColorScheme，而不是依赖组件内部实现猜测。
- 旧版本只有 `seed_color` 时应继续推导系统动态/种子色，不能默认迁移到预设颜色。
- FAB 语义色必须仅在 MIUIX + Preset 下提供，避免影响鲜活主题与动态取色。
