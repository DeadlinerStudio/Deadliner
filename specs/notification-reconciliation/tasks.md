# 通知状态协调 Tasks

## 阶段 1：规格冻结
- [x] 审计 Core 与 Android 链路，明确范围与兼容策略。

## 阶段 2：基础设施
- [x] 统一入口与事务监听、差异配置注册。

## 阶段 3：功能实现
- [x] Android 调度身份、时间与 Receiver 有效性。
- [x] 多任务实时活动、立即更新/撤销、设置与启动恢复。

## 阶段 4：验证与回写
- [x] 一次完整实现后统一运行 Core 与 Android 测试/编译。
- [x] 规格校验、文件行数检查、记录设备验证限制。

## 验证记录
- 规格校验通过；仓库没有技能文档引用的 check_large_core_files.py / core_suffixes.txt，改用逐文件有效行扫描，本次调度相关核心文件均低于 1000 行。
- Core 通知测试与实际数据库集成测试：直接执行 native 测试二进制，6 项通过。Gradle 定向验证同样通过（包含显式取消后重建断言）。
- Core 全量 macosArm64Test：68 项运行后在 legacyV3HabitSnapshotMigratesBeforeMerge 发生 Kotlin/Native GC native SIGSEGV，未通过；不把此结果作为全量通过。
- Android 首次构建受到工作区同时新增的灵动岛依赖/AIDL 配置影响，依赖就绪后 Debug APK 成功生成，199 项单测中 191 项通过、8 个同时新增的灵动岛用例因 Keystore 初始化失败；测试初始化已调整；最终独立源码快照构建与全部 199 项 Android 单测通过。
- 未做真机后台杀进程、重启、权限撤销和多任务通知栏验证；iOS/HarmonyOS 宿主尚未切换到新监听入口。
- 额外修复：取消备份选择不会清空现有调度；显式清空通知同步清空差异账本。

- 并发工作区构建出现 Kotlin 缓存冲突及 AAR 内 classes.jar 损坏；以当前源码独立构建 Core（非增量、in-process），校验 AAR 与内部 JAR CRC 后在独立 Android 源码快照中验证。通知源文件逐一比较与工作区相同。已将验证产物保存至 `build/notification-verification/`，并原子更新本地 `.deadliner-core/kmp/deadliner-core.aar`。

## 追加阶段：设备展示诊断
- [x] Application 前置日志初始化、生产通知链路埋点、Core 调度事件。
- [x] 独立立即/闹钟测试、停止和 TXT 导出入口。
- [x] 系统晋升与超级岛状态观察及回归测试、编译。

### 设备展示诊断验证
- Core Android Release AAR 独立源码构建通过，AAR 和内部 classes.jar CRC 校验通过。
- Android Debug APK 构建通过，203 项单测全部通过（含闹钟替换/取消、旧投递拒绝、权限拒绝日志与 Native/KMP TXT 合并）。
- 通知相关 Android 源码与验证快照逐一比较一致；APK 和 AAR 保存到 `build/notification-diagnostics/`。
- 真机超级岛展示、Live Update 系统晋升和后台限制尚未验证；通过新设置入口复现并导出 TXT 后定位设备结果。

## 日志复现修复
- [x] 定位真实任务 URI 闹钟与接收器不匹配，补充 manifest 及窗口内立即启动。
- [x] 对齐 Capsulyric 标准模式派发顺序及位图图片协议，补充 Root 后端执行日志。
- [x] 验证真实 PendingIntent 解析、真实任务启动与发布、既有通知回归和 APK 构建。

### 日志复现验证结果
- 独立 Android 快照 Debug APK 构建和 208 项单测通过。新增真实 Core PendingIntent 的 manifest 解析、无 URI 旧调度、错误 host 拒绝、真实任务窗口内直接服务启动及内容发布、OFF 唯一派发和模式互斥测试。
- 当前工作区本地 AAR 被其他构建替换为 Java 22 产物，首次 JDK 21 测试出现 UnsupportedClassVersionError；最终使用此前 CRC 验证的 Java 21 诊断 Core AAR 完成独立测试，未覆盖工作区的新 AAR。
- 源码快照通知文件与工作区一致，APK CRC 通过，产物在 `build/notification-root-fix/`。
- Root/HyperOS 4 真机最终展示仍待用户验证；自动测试不能证明系统白名单绕过成功。

## 系统模块直接发布
- 新增 SYSTEM_HOOK 设置与互斥选择，直接派发且无防火墙待恢复记录；模块模式不请求原生 promotion。
- 独立快照 Android Debug 构建和 209 项单测全部通过，产物 `build/system-hook-mode/app-debug.apk`。真机白名单钩子生效与超级岛展示待验证。

## 启动 ANR 日志修复
- [x] 设备日志确认启动 ANR 栈位于 AppLog.pruneLocked 的 Gson 序列化，通知批量取消导致满容量重复清理。
- [x] Native/KMP 共用后台写入队列；历史清理至 75% 容量，流式保留；TXT/JSONL 稳定导出快照。
- [x] 反馈 ZIP 导出改为 IO 调度。
- [x] 大日志启动与导出回归、完整 Android 单测、APK 构建和 CRC 校验。

### 启动 ANR 验证结果
- 新 Core Android Release AAR 编译通过，AAR 与内部 JAR CRC 通过。宿主通过 Android 专属 configureFileWriter 回调共用后台队列，公共 KMP 日志接口保持原样。
- Android 独立快照全部 212 项单测通过，含 3 项新的日志 ANR 回归；Debug APK 构建及 CRC 通过。修复源文件与测试快照一致。
- 新测试先阻塞后台 writer，再验证初始化与 100 条 Native + 100 条 KMP 日志可返回，导出完整；另验证 10 MB 历史清理、500 条连续记录、过期清理与导出快照稳定性。
- 首次新增测试因单例日志状态在测试间复用造成计数污染；测试隔离状态后全量通过。
- 产物 build/log-anr-fix/app-debug.apk 与 deadliner-core.aar；未替换可能正在并发构建的工作区 AAR。默认构建的 syncDeadlinerCoreKmp 会根据修改后的 Core 源码重新生成。
- 真机启动恢复、超级岛和临近任务 LiveUpdates 展示仍待设备验证；当前日志只证明启动 ANR 位于日志清理链路。
