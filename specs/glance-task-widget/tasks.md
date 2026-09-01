# Glance Task Widgets Tasks

## 已完成基础阶段

- [x] 建立 2×2 Glance provider、KMP 数据源、主题解析和刷新协调器
- [x] 清理旧 RemoteViews 小组件，仅保留 Glance 技术栈
- [x] 增加标准 / 4×6 / 4×7 的 2×2 顶部 padding 预设
- [x] 通过既有 JVM 测试、debug 构建、规格校验和大文件扫描

## 阶段 6：跨端规格冻结（2026-09-02）

- [x] 完整阅读 HarmonyOS `TaskWidgetCardSmall.ets` 与其 `WidgetUpdater` / model
- [x] 完整阅读 iOS `SmallListWidgetView.swift` 与其 snapshot reader / helper
- [x] 完整阅读 HarmonyOS `TaskWidgetCardCapsule.ets` 与其 model
- [x] 冻结 ACTIVE 过滤、无截止计数、截止排序、三态、时间文案和摘要优先级
- [x] 冻结 MIUIX 语义色、表面层级、圆角与位图体积约束
- [ ] 创建大型重构前全工作区 Git 回退点

## 阶段 7：共享数据与主题重构

- [ ] 扩展 snapshot/item/status/visual-state model
- [ ] 重构 repository：包含无截止任务、按截止时间排序、三态与摘要计算
- [ ] 扩展中英文 strings 与 repository JVM 测试
- [ ] 重构 MIUIX 语义 palette 与状态行 colors
- [ ] 扩展低分辨率背景/任务行 bitmap renderer

## 阶段 8：完整 2×2 迁移

- [ ] 拆分共享顶栏、任务行和空态组件
- [ ] 实现状态驱动背景、徽标和普通/临近/逾期任务行
- [ ] 保留图标、三条任务及 2×2 padding preset 行为
- [ ] 补充 palette / renderer 测试

## 阶段 9：2×1 胶囊迁移

- [ ] 实现 `CapsuleTaskGlanceWidget` 与 receiver
- [ ] 新增 2×1 provider metadata 和中英文描述
- [ ] 实现摘要、鼓励语、大数字、完成图形与状态装饰点
- [ ] 将刷新协调器扩展为同时更新 2×2 / 2×1

## 阶段 10：统一验证与回写

- [ ] 执行 `git diff --check` 与旧栈残留扫描
- [ ] 执行 Glance 相关 JVM 测试和完整 `testDebugUnitTest`
- [ ] 执行 `assembleDebug`
- [ ] 校验 manifest 恰好包含两个 Glance provider
- [ ] 执行 `.kt` 1000 行阈值扫描
- [ ] 执行规格校验并回写最终实现、测试和视觉验证状态

## 历史验证记录

- 2026-09-02：`./gradlew :app:testDebugUnitTest :app:assembleDebug` 通过。
- 2026-09-02：padding preset 测试覆盖 `0/26/18dp` 映射与未知值回退。
- 2026-09-02：旧 RemoteViews 类名、设置键与专属资源引用扫描无残留。
- 既有 `.kt` 扫描报告 7 个超限文件：3 个生成的 UniFFI binding，以及 `AIOverlay.kt`、`SettingsComponents.kt`、`AddDDLActivity.kt`、`GlobalUtils.kt`；此前变更没有新增超限文件。
