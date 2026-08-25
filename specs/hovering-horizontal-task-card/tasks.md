# 带悬停的横向滑动任务卡片 Tasks

## 阶段 1：规格冻结

- [x] 对照 iOS 与 HarmonyOS 动作语义和阈值。
- [x] 明确 Android 修改边界与验收标准。

## 阶段 2：基础设施

- [x] 提取纯 Kotlin 滑动落点规则。
- [x] 添加规则单元测试。

## 阶段 3：功能实现

- [x] 实现可悬停的双侧 Compose 滑动容器。
- [x] 接入任务状态动作、编辑与删除。
- [x] 实现列表单卡展开协调和选择模式复位。

## 阶段 4：验证与回写

- [x] 执行相关单元测试与统一编译。
- [x] 执行规格校验和核心文件行数检查。
- [x] 回写实际实现与验证结果。

## 验证记录

- `./gradlew testDebugUnitTest --tests com.aritxonly.deadliner.ui.main.TaskCardSwipeRulesTest`：通过。
- `./gradlew assembleDebug`：通过。
- 规格校验：通过。
- Kotlin 核心文件扫描：本次修改文件均未超过 1000 行；仓库仍有 4 个既有手写 Kotlin 文件和 3 个生成绑定文件超限，与本功能无关。
- 构建仅有仓库既有警告：AGP 对 compileSdk 37 的版本提示、旧 API 弃用与既有字符串占位符提示。
