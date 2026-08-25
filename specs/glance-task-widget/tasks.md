# Glance 2x2 Task Widget Tasks

## 阶段 1：规格冻结

- [x] 完成 `spec.md`
- [x] 完成 `module-plan.md`
- [x] 对照 HarmonyOS 附件与 Android 现有主题/KMP 层完成自检

## 阶段 2：基础设施

- [x] 增加稳定版 Glance AppWidget 依赖
- [x] 建立 widget snapshot、KMP data source 与 repository
- [x] 建立现代/鲜明日夜主题解析与背景生成
- [x] 建立统一刷新协调器

## 阶段 3：功能实现

- [x] 实现 2×2 `GlanceAppWidget` 与 receiver
- [x] 注册第五个 provider 和 provider metadata
- [x] 接入 KMP `TaskListUiState`、应用前台、外观设置和系统配置刷新
- [x] 补充中英文文案与数据映射单元测试
- [x] 完成一批实现后统一编译，不对每个小 patch 单独编译

## 阶段 4：验证与回写

- [x] 执行 repository/theme JVM 单元测试
- [x] 执行 Android debug 构建
- [x] 执行规格校验
- [x] 执行大文件扫描
- [x] 回写最终实现状态与验证结果

## 验证结果

- `./gradlew :app:testDebugUnitTest :app:assembleDebug`：通过，共 21 个 JVM 测试，0 失败；其中本功能新增 7 个测试。
- `validate_feature_spec.py specs/glance-task-widget`：通过。
- `.kt` 1000 行阈值扫描：本功能最大文件为 `CompactTaskGlanceWidget.kt`（221 行），无新增超限文件。
- 全仓扫描仍报告 7 个既有超限文件：3 个生成的 UniFFI binding，以及 `AIOverlay.kt`、`SettingsComponents.kt`、`AddDDLActivity.kt`、`GlobalUtils.kt`；本轮没有扩大这些文件。
