# Deadliner v6 KMP Migration Module Plan

## 模块拆分

- `contract`：共享 UI state、effect、用户意图、错误与平台 port；不得依赖 AndroidX、Compose、SwiftUI 或 ArkUI。
- `domain`：任务/习惯/捕获/设置规则、时间计算、筛选、统计、迁移映射与 Lifi 模型。
- `presentation`：`commonMain/viewmodel` 中的平台无关 ViewModel/Store，使用 `StateFlow`/`SharedFlow` 暴露状态与一次性 effect。
- `lifi`：LLM gateway、prompt、解析、memory、skill、tool、orchestrator 与 utility service；平台只注入配置/凭据和系统工具端口。
- `infra`：SQLDelight repository、同步协调器、数据迁移器以及 Android/iOS/HarmonyOS 的 driver/permission/notification adapter。
- `distribution`：`DeadlinerCore/runscript/buildAndDeploy.sh` 构建并原子部署 Android AAR、Apple XCFramework、HarmonyOS SO。

## 平台映射

- iOS：共享状态/用例来自 `DeadlinerCore`; SwiftUI Observable adapter 留在 `Deadliner/Features`，平台端口实现在 `Data` 或 `Core/Application/Ports` 对应适配层。
- HarmonyOS：共享 C API/桥接消费 `DeadlinerCore`; 页面和状态观察留在 `entry/src/main/ets/pages`，系统能力适配在 `common/repository`/`common/data`。
- Android：AAR 位于 `.deadliner-core/kmp/deadliner-core.aar`；Compose/Activity 留在 `app/ui`，AndroidX lifecycle adapter 留在 `app/platform/kmp`，Room-to-SQLDelight 数据迁移位于 `app/data/migration`。

## 文件拆分策略

- 单个核心文件尽量不超过 1000 行有效代码
- 若必须逃逸，先申请开发者批准
- 每个 feature 独立定义 `*UiState`、`*Effect` 和 ViewModel；通用时间/映射逻辑放在 domain helper，避免 ViewModel 膨胀。
- Android 适配器不得复制筛选、排序、统计、提示词或状态转换；只做生命周期、类型和平台回调转换。
- 优先消费 Core 已有接口；现有公开类型/构造器保持源码与二进制语义，不用 Android 需求反向污染其他端。
- Lifi 保持 `llm / prompt / pipeline / orchestrator / memory / skill / tool / utility` 现有拆分。

## 迁移切片

1. 构建接入：Android 直接消费本地/发布版 KMP AAR，统一 Core source 配置。
2. 无数据依赖状态：Intro wizard 与共享错误/effect 契约。
3. 数据功能：Capture、Task list/detail/editor/archive/search、Habit list、Overview、Settings。
4. Lifi：用共享 AI utility/orchestrator 替代 Android `AIUtils`，平台工具通过 ports 注入。
5. 数据切换：SQLite v5 数据一次性迁入 SQLDelight；迁移失败阻止进入 v6，不回退旧数据层。页面完成切换后删除重复 repository/ViewModel/sync。

## 风险点

- 当前 Android 发布同步脚本仍默认指向旧的 `/Users/aritxonly/Codes/Agent/deadliner_core`，而本机 Core 在 `/Users/aritxonly/Codes/ProjectDeadliner/DeadlinerCore`。
- Android 目前声明 UniFFI/JNA bindings 与 JNI libs，但 Core 新 runscript 产物是 KMP AAR；两条接入链路需要分阶段收敛。
- Core `main` 有在途 repository mutation notifier/sync coordinator 变更，迁移 ViewModel 时必须使用它的最终通知语义。
- Room 的 `Long` ID 与共享层 `String uid`、Java time 与 `kotlinx.datetime` 的映射是数据迁移高风险点。
- Lifi 凭据不能进入共享二进制或日志；共享配置只接收运行期注入值。
