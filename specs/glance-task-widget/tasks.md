# Glance Task Widgets Tasks

## 阶段 1：规格冻结

- [x] 阅读 HarmonyOS 2×2、iOS 2×2、HarmonyOS 2×1 及其数据/helper 实现
- [x] 冻结 ACTIVE 过滤、无截止计数、截止排序、三态、时间文案和摘要优先级
- [x] 冻结 MIUIX 语义色、表面层级、圆角与位图体积约束
- [x] 明确 4×6 / 4×7 增加的是 2×2 卡片顶部外边距，2×1 不消费该设置
- [x] 冻结按实例配置、重新配置入口、配色组合和 margin 安全范围
- [x] 修正规格：4×6 为顶部 18dp，4×7 为顶部 12dp，并允许自定义上下 margin

## 阶段 2：基础设施

- [x] 建立 2×2 Glance provider、KMP 数据源、主题解析和刷新协调器
- [x] 清理旧 RemoteViews 小组件，仅保留 Glance 技术栈
- [x] 增加标准 / 4×6 / 4×7 的 2×2 卡片顶部外边距预设
- [x] 创建大型重构前全工作区 Git 回退点 `627e8a1`
- [x] 扩展 snapshot/item/status/visual-state model
- [x] 重构 repository 与中英文 strings
- [x] 重构 MIUIX 语义 palette、状态行 colors 和低分辨率 bitmap renderer
- [x] 新增按 `appWidgetId` 的配置模型、存储、旧设置迁移和删除清理
- [x] 修复“跟随应用”的颜色来源解析

## 阶段 3：功能实现

### 2×2 任务列表

- [x] 拆分共享顶栏、任务行和空态组件
- [x] 实现状态驱动背景、徽标和普通/临近/逾期任务行
- [x] 保留图标、最多三条任务及 padding preset 行为
- [x] 将 4×6 / 4×7 从内容顶部内边距修正为整张卡片顶部外边距

### 2×1 胶囊

- [x] 实现独立 `CapsuleTaskGlanceWidget` 与 receiver
- [x] 新增 2×1 provider metadata 和中英文描述
- [x] 实现摘要、按状态显示的鼓励语/大数字、完成图形与装饰点
- [x] 将刷新协调器扩展为同时更新 2×2 / 2×1

### 按实例编辑页

- [x] 新增共享配置 Activity，并正确处理 `EXTRA_APPWIDGET_ID` / Activity result
- [x] 为两个 provider 声明 `configure` 与 `reconfigurable|configuration_optional`
- [x] 实现 MIUIX 配置页、尺寸预览、圆角、上下外边距和配色组合
- [x] 将两种 Glance UI 接入按实例圆角、margin 和 palette
- [x] 4×6 / 4×7 更新为 18dp / 12dp，并保留自定义模式

## 阶段 4：验证与回写

- [x] repository 测试覆盖过滤、排序、无截止、三态、时间与摘要优先级
- [x] palette / renderer 测试覆盖语义色、明暗表面和位图尺寸约束
- [x] `git diff --check` 与旧 RemoteViews 残留扫描通过
- [x] `./gradlew :app:testDebugUnitTest :app:assembleDebug` 通过（141 项测试）
- [x] manifest 恰好包含两个 Glance provider
- [x] Glance 核心文件均小于 1000 行
- [ ] 在 MIUI/HyperOS 桌面实机验证 2×2、4×6/4×7 留白和 2×1 缩放视觉
- [x] 执行 app 源集 `.kt` 1000 行阈值扫描；4 个既有超限文件，本功能新增文件均低于 350 行
- [x] 执行规格校验并回写最终结果
- [x] 增加配置存储、范围约束、迁移、配色来源和删除清理测试
- [x] 重新执行完整单测、debug 构建、manifest 与规格校验

## 历史验证记录

- 2026-09-02：卡片顶部外边距 preset 最终覆盖 `0/18/12dp` 映射，并支持按实例自定义上下外边距。
- 2026-09-02：旧 RemoteViews 类名、设置键与专属资源引用扫描无残留。
- app 源集 `.kt` 扫描报告 4 个既有超限文件：`AIOverlay.kt`、`SettingsComponents.kt`、`AddDDLActivity.kt`、`GlobalUtils.kt`；本功能不修改这些文件。
