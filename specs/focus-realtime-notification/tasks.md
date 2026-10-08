# Tasks

## 阶段 1：规格冻结
- [x] 录音模板、原生标准按钮、KMP 会话版本与计时锚点。

## 阶段 2：基础设施
- [x] 动作入口与协议渲染。

## 阶段 3：功能实现
- [x] 专注服务生命周期与状态差异发布。

## 阶段 4：验证与回写
- [x] 模板/按钮/状态流测试、Debug 构建、文件规模与产物验证。

## 验证记录（2026-10-07）

- Core Release AAR、Android Debug APK 和仪器测试包构建通过；248 项安卓单测通过，无失败/错误/跳过。
- 8 项新增专注通知测试覆盖录音协议、时间锚点、暂停冻结、三个动作与 URI、伪造/旧版本/跨会话拒绝、普通 tick 不重新发布、共享结束/完成与倒计时到时。
- Android API 36 只读 Pixel 9 模拟器：1 项仪器测试通过，实际发布 eligible=true / promoted=true / canPost=true。真实 PendingIntent 暂停、继续、结束均成功，结束保存一条记录并移除前台通知。
- 模拟器原数据存在 V5 迁移错误，清理只读副本的应用测试数据后验证通过；没有修改真机和原始 AVD。
- 录音 timerInfo、highlightInfo、actions 与 sameWidthDigitInfo 已核对 HyperNotification 原始模型；暂停用固定 title，避免暂停计时字段歧义。
- 标准图标使用框架 vector，避免 SystemUI 无法加载应用自定义字体 Drawable。
- 构建快照与修改源文件一致，相关文件有效代码均少于 1000 行，APK/AAR 完整性与 SHA-256 已保存。
- 产物：`build/focus-realtime-notification/`。超级岛录音样式及 Root 真机发布尚未进行设备验证。

## 真机反馈修正验证

- 只读提取设备 `5c7da359` 的 `/product/app/MIUISystemUIPlugin/MIUISystemUIPlugin.apk` 到临时目录；未安装/修改真机。
- `IslandSameWidthDigitViewHolder` 明确在 digit 非空时隐藏 HyperChronometer 并显示静态 title；只有 digit 空且 timerInfo 存在时启动计时分支。运行态现仅发送 timerInfo，暂停态仅发送 digit。
- `ModuleButtonViewHolder` 读取 actionBgColor / actionBgColorDark，构造系统圆形背景；现提供显式颜色。图标在 88px 画布内缩至 44px 并居中，缓存键包含内容尺寸。
- 20 项定向单测通过，覆盖计时互斥、暂停固定数字、按钮背景/深色图标和缓存内边距隔离；Debug 构建、源快照一致性及 ZIP 完整性通过。
- 修正包 `build/focus-island-polish/app-debug.apk`；新样式与大岛计时须真机复测。本轮未复跑前轮 API 36 仪器测试，原生行为没有变更。

## 录音布局修正

- 真机截图证实上一轮通用按钮背景配置未适用于录音图标分支。进一步核对 ModuleDecoLandButtonViewHolder：只有 setActionTextData 读取背景颜色，type=0 的 setActionData 通过 actionIcon 名称查系统 MAP；AnimateImageView 在资源 ID 非零时使用系统 drawable。
- 系统横向/纵向录音按钮均支持 action_pause_record、action_restart_record、action_record_end；使用预设而不是自定义透明图标，保留 actionIntentType=1 和原会话校验 URI。
- 按用户要求超级岛仅保留暂停/继续与结束两按钮，移除辅助任务名，白色计时配运行蓝色/暂停灰色状态。原生通知动作保持现有行为。
- [x] 20 项定向单测通过（无失败/错误/跳过），覆盖两按钮系统预设、暂停切换、计时锚点、动作校验和元数据回归；Debug APK、源快照一致性与 ZIP 完整性验证通过。产物为 build/focus-recording-layout/app-debug.apk；系统实际视觉待用户真机复测。
