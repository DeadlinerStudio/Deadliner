# Live Update 与小米超级岛适配

## 背景
Capsulyric 通过 Android 16 promoted ongoing 通知和 Xiaomi Focus V3 实现状态栏展示。Deadliner 当前截止提醒只有普通前台通知，已有 POST_PROMOTED_NOTIFICATIONS 权限。
参考版本：FrancoGiudans/Capsulyric dd020bcc39d129695d2064fda405870d96c6e58f。

## 目标
保留现有通知标题、正文、点击入口和布局，为截止提醒和专注计时通知增加系统提升请求、短状态文本、立即前台展示和超级岛元数据。

## 非目标
不迁移歌词业务、自定义 RemoteViews 或长时间断网模式。独立实现协议，不复制 GPL 项目代码，不新增 Focus API 的序列化依赖。

## 用户场景
任务临近截止时看到状态栏倒计时；专注计时中看到时钟；点击原有通知回到详情或计时器。

## 验收标准
- API 36+ 持续通知请求提升；API 33–35 保留普通通知兼容性。
- Xiaomi/Redmi/Poco API 35+ 附加 miui.focus.param 和 miui.focus.pics，使用 param_v2/param_island。
- 完成/到期后结束对应持续卡片，普通完成/截止提醒不接入系统提升或岛元数据。
- 保留通知布局、通知 ID、频道、更新周期和 PendingIntent。
- 自动测试覆盖版本分支、OEM 分支、终止降级和协议图标引用。

## 风险与约束
系统决定是否提升。HyperOS 3 超级岛受设备能力和白名单限制；原生 Live Update 在小米系统上需要对应版本支持。不绕过用户设置。无 breaking change，不修改 KMP 业务。真机展示需要另外验收。
用户补充授权：在通知设置增加默认关闭的 Shizuku/Root 短时绕过，通过特权进程阻断 XMSF UID 100ms，发布后恢复，特权进程 5 秒 watchdog 与销毁恢复。计时通知更新共用窗口，等待连接时合并重复更新，失败后暂缓重试；恢复记录持久化，应用重启时尝试清理。设置提供授权、关闭和手动恢复。

## 临近任务模板优化（2026-10-07）
用户提供《小米超级岛通知模板库 (20260129)》与 14:42 展示成功日志。
- 临近任务大岛 A 区：浅蓝色（#FF8ABEFF）时钟图标与“临近”；B 区：本地截止 HH:mm，次日“明天HH:mm”，更远日期带日期。
- 展开态采用模板 17：iconTextInfo + picInfo + textButton，任务标题、绝对截止时间、“放弃”和“标记完成”两个按钮，无进度组件。
- 用户操作使用 KMP TaskAction，不直接写数据库；完成/放弃后由现有通知协调撤销。正在专注计时遵循 TIMER_REQUIRES_RESOLUTION 并进入专注页。
- 系统通过显式 Activity Intent URI 执行按钮。入口校验安装级随机 token、task UID/updatedAt 与 ACTIVE 状态，拒绝伪造、过期和重复点击。
- 普通 Android 通知也提供相同按钮。专注与诊断通知保持原模板。
- 岛显示绝对截止时间；周期刷新仅在有效任务、内容、角色、模式或日期标签变化时重新发布，避免静态内容反复认证。
