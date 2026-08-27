# 现代模式 AI 页面 Tasks

## 阶段 1：规格冻结

- [x] 明确现代页面态与简洁 Overlay 态的差异。
- [x] 确认现代模式已有 AI 路由，简洁模式使用独立 Overlay Host。

## 阶段 2：基础设施

- [x] 增加 AI 展示策略与页面入口。
- [x] 为展示策略增加单元测试。

## 阶段 3：功能实现

- [x] 在共享 AI 内容中按策略控制关闭按钮、返回键与输入框位置。
- [x] 在现代模式 `LIFI` 分支挂载 AI 页面。
- [x] 清理现代模式已不需要的 Overlay 接线，不改简洁模式。

## 阶段 4：验证与回写

- [x] 运行相关单元测试。
- [x] 运行 Android 编译验证。
- [x] 检查核心文件行数并记录结果。
- [x] 回写实现状态和验证结果。

## 验证记录

- `./gradlew :app:testDebugUnitTest --tests 'com.aritxonly.deadliner.ui.agent.AIAgentPageTest' --tests 'com.aritxonly.deadliner.ui.main.modern.*'`：通过。
- `./gradlew :app:compileDebugKotlin`：通过。
- `validate_feature_spec.py specs/modern-ai-page`：通过。
- 仓库未提供 `scripts/check_large_core_files.py` / `core_suffixes.txt`，已用 `wc -l` 记录相关核心文件：`AIOverlay.kt` 1366 行、`AIAgentPage.kt` 61 行、`ModernHost.kt` 881 行。
- 构建仍提示 AGP 8.10.1 对 compileSdk 37 的既有兼容性警告，以及仓库既有弃用警告；无本功能新增编译错误。
