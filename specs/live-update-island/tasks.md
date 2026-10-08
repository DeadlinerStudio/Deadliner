# Live Update 与小米超级岛适配 Tasks

## 阶段 1：规格冻结
- [x] 核对参考项目版本、原生实现和 Xiaomi V3 协议。
- [x] 明确现有布局和未提交工作保留范围。

## 阶段 2：基础设施
- [x] 实现独立原生适配与超级岛协议编码。
- [x] 实现 Shizuku/Root AIDL 与短时联网阻断、特权 watchdog 和恢复记录。

## 阶段 3：功能实现
- [x] 接入截止提醒和专注计时通知，重发前读取当前有效状态。
- [x] 通知设置新增互斥方式选择、授权、原生权限入口与手动恢复。
- [x] 持续卡片结束后保留原有普通通知路径。

## 阶段 4：验证与回写
- [x] 一批实现后统一编译和通知适配测试。
- [x] 规格与文件大小校验；记录真机限制。

## 验证记录
- git diff --check 通过；validate_feature_spec.py 通过。
- 技能提及的 scripts/check_large_core_files.py 和 core_suffixes.txt 不在此仓库，以等价行数扫描检查本轮核心文件，均低于 1000 行。
- 首次测试启动触发 DeadlinerApp AndroidKeyStore 初始化，测试改用 android.app.Application，避免加载无关启动业务。
- 本地 AAR 与并行工作区新增 cancelAllNotifications 调用不匹配，最终验证使用项目既有 syncDeadlinerCoreKmp 流程更新 Core。
- 真机待验收：Android 16 系统提升、HyperOS 3 超级岛显示、Shizuku/Root 权限和推送恢复。自动测试不宣称真机绕过已经成功。
- Core 增量构建输出的内部 classes.jar 不完整，最终以 --rerun-tasks、kotlin.incremental=false、compiler.execution.strategy=in-process 非增量重建；校验内部 JAR 有效且 widget/notification 类存在后部署。
- 最终 :app:testDebugUnitTest 通过，通知适配与特权窗口恢复 8 项测试全部通过；主代码、资源和 AIDL 编译通过。未运行真机验证。

## 临近任务模板优化
- [x] 核对用户模板、14:42 Root 成功日志、现有 KMP 任务动作入口。
- [x] 摘要态、展开态与按钮实现。
- [x] 时间标签、JSON/图片、动作防重放/状态变更及静态发布去重回归；生成 APK。

### 临近任务模板验证
- 独立源码快照全部 221 项 Android 单测通过，Debug APK 构建通过，APK CRC 和变更源码一致性通过。产物 build/island-task-template/app-debug.apk，包含此前日志 ANR 修复。
- 新增 9 项回归：本地时区/午夜/月年边界标签、双按钮 Intent URI/图片键及无进度 JSON、KMP 完成/放弃与闹钟撤销、防伪造/过期/重复操作、专注冲突结果、静态内容去重、重复服务启动不覆盖完整通知，以及直接发布状态。
- Root/Shizuku 认证失败或冷却期回退不会记为静态岛发布成功，保持后续重试。
- 按钮使用 actionIntentType=1 的显式 Activity，避免广播/服务动作在不同 Android 版本受到后台启动限制。入口为透明 Activity，校验 capability 与任务版本；普通通知也有相同两项操作。
- 真机模板外观、文字截断及 OEM 按钮点击尚未验证；用户此前已验证 Root 可以展开超级岛，不能据此认定新模板已在设备验收。
