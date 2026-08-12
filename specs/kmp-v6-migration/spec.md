# Deadliner v6 KMP Migration

## 背景

- 创建日期：2026-08-12
- 功能标识：`kmp-v6-migration`
- 需求来源：将 Deadliner Android v5 的业务上层抽象迁移到 `~/Codes/ProjectDeadliner/DeadlinerCore`，作为 v6 的跨平台共享基线。
- Android v5 已在 `feat/ui_refresh` 的提交 `9a4b5d6` 完整冻结；v6 从该提交切出 `refactor/kmp`。
- `DeadlinerCore` 已提供 SQLDelight 仓储、任务/习惯/捕获/搜索/概览/设置 ViewModel，以及 Lifi 的模型、管线、工具和编排能力；Android 尚未统一消费这些共享实现。

## 目标

- `DeadlinerCore/commonMain` 成为任务、习惯、捕获、归档、搜索、概览、设置、引导等上层状态与业务规则的唯一实现来源。
- Lifi 的配置契约、提示词、解析、编排、工具调用、记忆和 AI utility 全部由 KMP 共享层提供。
- Android 通过 KMP Android AAR 接入共享层；平台代码仅负责 UI、生命周期适配和 Android 专属端口。
- 迁移按垂直切片进行，每一阶段都能构建和回退，最终删除 Android 中重复的业务实现。
- 保持 v5 用户数据可迁移、核心交互语义不回退。

## 非目标

- 本阶段不要求 Compose/Activity 等 Android UI 迁入 KMP。
- 不在迁移中重做 v5 视觉设计或引入新的产品能力。
- 不擅自覆盖、回滚或拆除 `DeadlinerCore/main` 当前未提交的 repository-driven-sync 工作。
- 不要求 AndroidX `ViewModel` 类型本身进入 `commonMain`；允许平台侧保留无业务逻辑的生命周期宿主/适配器。

## 用户场景

1. 用户升级到 v6 后，现有任务、习惯、捕获、归档和设置可继续使用，数据不丢失。
2. 用户在 Android、iOS、HarmonyOS 上执行相同业务操作时，状态转换、筛选、统计与错误语义一致。
3. 用户使用 Lifi 创建/整理任务、生成概览或执行工具调用时，核心行为由同一套 KMP 管线提供。
4. 开发者修改共享 ViewModel 或 Lifi 规则后，可通过 `DeadlinerCore/runscript` 产出 Android AAR 并在 Android v6 验证。

## 验收标准

- `DeadlinerCore/commonMain` 包含并公开 Android 实际使用的所有非 UI ViewModel/Presenter/状态机；Android 不再保留这些能力的第二份业务实现。
- Android 本地 `MainViewModel`、`HabitViewModel`、`CaptureViewModel`、`IntroWizardViewModel` 和 `AIUtils` 的业务职责均已迁入或改由共享实现代理。
- Lifi 的网络无关业务逻辑、提示词、JSON/结果解析、agent orchestration、memory、skill/tool pipeline 位于共享层；Android 只提供凭据持久化、系统权限、Intent/通知等平台适配。
- 任务、习惯、捕获、归档、搜索、概览、设置和引导关键状态流都有共享层单元测试。
- Android 使用 `DeadlinerCore` 生成的 AAR，且本地开发模式能通过明确的 Gradle 属性/脚本同步，不依赖硬编码的旧仓库路径。
- v5 基线提交 `9a4b5d6` 保持可检出；KMP 改动仅发生在 `refactor/kmp` 及 Core 的明确迁移提交中。
- `DeadlinerCore` runscript 构建验证和 Android 构建/测试通过；规格与模块计划已按最终实现回写。

## 风险与约束

- 这是 breaking change；每个跨仓库大批次前必须有可回退提交。
- Android 当前 Room 模型与 KMP SQLDelight/领域模型并存，需要显式数据迁移和时间/ID/枚举映射，禁止静默丢字段。
- KMP AAR 与现有 UniFFI/JNA 发布物并存，切换期间必须避免同名 API、重复 native 库和双写数据库。
- `DeadlinerCore` 工作区在本任务开始前已有未提交的 repository-driven-sync 改动；所有 Core 修改必须增量兼容并单独核对 diff。
- 不得为了 Android 迁移修改 iOS/HarmonyOS 已消费的既有 Core API 语义；新增公开 API 必须是明确的跨端上层抽象，而不是 Android 适配或测试便利接口。
- 单个核心代码文件尽量不超过 1000 行有效代码；超过前需取得开发者批准。
- 编译放在完成一个实质迁移批次后统一执行，避免小改动反复构建。
