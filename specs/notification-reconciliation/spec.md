# 通知状态协调

## 背景
KMP ViewModel 手动调度无法覆盖直接写库、同步和恢复；Android 实时通知只跟踪单任务，取消调度不会取消展示。

## 目标
以 KMP 数据库提交为触发点，统一 triggerNotificationInfoUpdate() 从任务、习惯及打卡记录计算通知期望状态，比较抽象配置后增改取消。实时活动保留跨平台 Gateway 槽位，Android 同时展示多个临近任务并立即响应修改。

## 非目标
不新增 iOS 灵动岛或鸿蒙实况窗渲染，不改变后台保活与 AlarmManager 技术路线。

## 用户场景
修改任务截止时间后，旧闹钟被替换、倒计时立即重算；完成/删除任务时实时活动立即消失；习惯打卡完成后不再收到本周期提醒；同步与恢复也更新调度。

## 验收标准
- 数据库任务、习惯、打卡事务提交后自动协调；旧 ViewModel 调用兼容且不重复注册。
- 相同配置不重复调度，移除实体取消，重启/系统时间变化可强制重建。
- 习惯调度读取持久化记录，通知送达后推进下一个时段。
- Android 多个实时任务互不覆盖，关闭提醒或修改时间离开提前窗口后立即撤销展示。
- Android PendingIntent 不因 UID hash 碰撞合并，带时区时间按实际 instant 计算。

## 风险与约束
两个仓库均有用户在途改动，只修改通知相关文件；新增 API 向后兼容。系统权限及厂商后台限制需设备验证。

## 通知诊断与系统展示测试（追加范围）
通知设置新增立即测试、10 秒闹钟链路测试、停止测试及直接导出标准 TXT。测试使用独立合成倒计时、独立通知 ID，不写入任务/习惯；持续 2 分钟后结束，停止时同时取消延迟闹钟。与生产路径共用 LiveUpdateNotificationAdapter 和超级岛发布逻辑。
Application 最早初始化 Native/KMP 标准日志。日志覆盖 KMP 协调、平台注册/取消、闹钟调度/投递、过滤原因、服务启动、每轮发布、系统 activeNotifications 反馈及超级岛授权/连接/重放/网络恢复。记录 runId/UID、时间、权限、渠道、promotable/promoted 标记与异常类型，避免把 requestPromotion 当成展示成功。
验收：后台冷启动日志可以导出；重复测试替换旧测试、停止后旧闹钟不会重新启动；测试收到系统通知但未晋升时输出可区分的状态；正常调度拒绝路径均给出原因。

## 设备日志复现修复（2026-10-07）
- KMP URI 闹钟必须实际解析到 manifest 接收器；保留无 URI 的旧调度兼容。真实任务已在窗口内时立即请求服务，窗口外继续使用 AlarmManager。
- 依据 Capsulyric dd020bcc 的标准模式：先用普通通知占用前台槽位，唯一完整岛通知在 XMSF chain 9/deny 2 窗口内通过 NotificationManager.notify 发布，100ms 后 default 0 恢复。OFF 或失败时直接降级发布。Root 是同一 IPC 防火墙流程的宿主适配，上游没有独立 libsu Root 模式。
- 岛图片按上游使用 bitmap Icon，smallIslandArea 使用 combinePicInfo；不把重放成功当作岛显示成功。记录特权进程 UID、选中的 backend、chain/rule 和最后异常到标准 TXT。
- 验收：自动测试 Core 实际 PendingIntent 的 manifest 解析；当前窗口任务能请求服务；前台初始通知不携带岛参数，完整通知只在派发回调发布；退出后待执行回调不会复活通知。

- 与上游模式选择一致：Xiaomi 开启 Root/Shizuku 时选择超级岛，不同时请求原生 promoted Live Update；关闭绕过时 Android 16+ 选择原生 Live Update。Root 设备日志中 promoted=true 证明此前同时请求了两个展示机制，系统选择结果尚需修复后真机确认。

## 系统模块白名单解除模式
支持 SYSTEM_HOOK，与 Root/Shizuku/OFF 互斥。LSPosed 模块已放行时直接提交岛通知，不请求原生 promotion、不连接特权进程、不操作防火墙。未安装/未生效时不宣称白名单已解除。

## 启动日志 ANR 修复（2026-10-07）
- Native/KMP 诊断记录由宿主同一后台队列写入；启动与调度调用不等待历史文件扫描、写入或清理。
- 清理采用有界流式保留，并清理至容量上限的 75%，避免满容量后的每条记录反复清理。
- TXT/JSONL 导出等待此前排队记录，并生成稳定快照；保留既有标准格式与 Native/KMP 来源。
- 回归验证：阻塞后台时启动/写日志可返回、超限历史和连续记录只触发一次清理、混合来源导出与快照稳定性。
