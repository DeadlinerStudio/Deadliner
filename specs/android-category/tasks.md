# Android 分类功能 Tasks

## 阶段 1：规格冻结

- [x] 读取飞书分类业务原语和 iOS/HarmonyOS 实现。
- [x] 建立 Android 模块边界、兼容策略与验收标准。

## 阶段 2：基础设施

- [x] 建立 category domain/presentation model、registry 与筛选 resolver。
- [x] 为 registry 与 resolver 编写单元测试。

## 阶段 3：功能实现

- [x] 实现分类徽标、任务/习惯表单单选器与自适应主页筛选面板。
- [x] 将 AddDDLActivity 及任务详情编辑接入分类保存；现有 Android 尚无习惯编辑入口。
- [x] 将主页列表投影、左上角按钮与筛选状态接入，并覆盖 Miuix 与简洁首页。

## 阶段 4：验证与回写

- [x] 运行大文件扫描、规格校验、单测与 Kotlin 编译。
- [x] 回写实现事实、验证结果与遗留疑问。
- [ ] 通过 Lark bot 发送最终开发报告。

### 验证记录

- 2026-08-29：`./gradlew testDebugUnitTest` 通过。
- 2026-08-29：`./gradlew :app:compileDebugKotlin` 通过。
- 2026-08-29：`validate_feature_spec.py specs/android-category` 通过。
- 2026-08-29：仓库指引的 `scripts/check_large_core_files.py` 不在当前工作区，已用等价有效行扫描替代；新增分类文件最大 157 行。扫描同时发现已有 `AddDDLActivity.kt`（1062）、`GlobalUtils.kt`、`AIOverlay.kt` 与 `SettingsComponents.kt` 超过 1000 行，本轮未扩大这些既有超限文件的职责边界。
