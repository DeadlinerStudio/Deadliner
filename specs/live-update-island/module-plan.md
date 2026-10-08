# Live Update 与小米超级岛适配 Module Plan

## 模块拆分
- LiveUpdateNotificationAdapter：Android 16 提升请求、状态文本、立即展示；保留通知原布局。
- XiaomiIslandExtras：Xiaomi Focus V3 的 param_v2/param_island JSON 与 Icon Bundle 编码。
- IslandWhitelistBypass：默认关闭的持久化模式、授权、通知合并、100ms 阻断窗口、恢复与失败暂缓重试。
- IslandFirewallWindow / IslandPrivilegedServices / IIslandFirewall：Shizuku daemon 与 libsu RootService 中调用 OEM_DENY_3 的 UID 规则；特权进程 5 秒自动恢复；不关闭全局 firewall chain。
- IslandNotificationSettings：复用通知页设置组件，增加原生权限入口、Shizuku/Root 互斥开关、授权检查、手动恢复。
- Existing services：截止提醒和专注计时器传入当前标题与时钟；延迟重发时重新读取有效任务/计时状态。

## 平台映射
仅 Android notification、ui/settings 与 AIDL 层；KMP 调度、iOS 和 HarmonyOS 不新增业务。

## 文件拆分策略
协议、特权 IPC、窗口恢复、设置 UI 分开维护，核心文件均低于 1000 行。

## 风险点
协议为 OEM 私有接口。按 Capsulyric 使用的 HyperNotification 1.4 源码核对字段，独立实现而非复制 GPL 源文件。未引入 Focus API 的 kotlinx-serialization 1.10.0 依赖，避免影响当前 KMP 定制运行库。Shizuku API 13.1.5、libsu service 6.0.0 负责特权进程；没有给应用进程添加隐藏 API 豁免。

## 参考
- Capsulyric: https://github.com/FrancoGiudans/Capsulyric/tree/dd020bcc39d129695d2064fda405870d96c6e58f
- Android Live Updates: https://developer.android.com/develop/ui/views/notifications/live-update
- HyperNotification wire schema: https://github.com/xzakota/HyperNotification
- Shizuku API: https://github.com/RikkaApps/Shizuku-API
- libsu RootService: https://github.com/topjohnwu/libsu

## 临近任务模板与动作
- TaskIslandPresentation：本地时区截止标签与稳定发布签名。
- XiaomiIslandExtras：任务专属模板 17、浅蓝高亮与双文字按钮；复用既有图片 Bundle。
- TaskNotificationActions / NotificationTaskActionActivity：可验证、幂等的动作入口与计时冲突处理。
- UpcomingLiveUpdateService：传入任务模板、普通通知动作、静态岛内容去重。
