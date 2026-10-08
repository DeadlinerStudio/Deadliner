# Tasks

## 阶段 1：规格冻结
- [x] 固定时间规则、3 分钟结果保留、习惯忽略本次和默认迁移策略。

## 阶段 2：基础设施
- [x] KMP 元数据模型/规划器/监听入口。

## 阶段 3：功能实现
- [x] Android 设置与原生调度/投递/交互/卡片。

## 阶段 4：验证与回写
- [x] KMP 时间边界与周期/结果测试、Android 投递/去重/按钮测试、构建/规格/文件检查。

## 验证记录

- Android 全量 238 项单测通过（无失败、错误或跳过）。
- KMP macOS Native 定向 8 项规划器测试全部通过。
- Core Release AAR、Android Debug APK 与 Release APK 构建通过；Release 未签名，仅用于混淆验证。
- 检查 Release APK 的实际 DEX：三个持久化元数据类型的字段名称完整保留。
- 本次源文件与构建快照一致，修改的 Kotlin 文件均未超过 1000 行有效代码；APK/AAR ZIP 完整性检查通过。
- 产物位于 `build/notification-policy-v2/`，附 SHA-256 和定向测试记录。
- 未进行真机测试；超级岛实际宽度/颜色、Root 发布和系统后台恢复仍需真机验证。

### 图标调整补充验证

- 展开态业务图标与收起态恢复状态色时钟；App 身份图标资源保持独立。
- 9 项通知集成测试通过，校验展开态/收起态时钟资源与 App 资源分离。
- Debug 构建及 APK 完整性检查通过，安装包位于 `build/island-clock-icon/app-debug.apk`；左右角实际位置仍需真机确认。

### 挖孔旁间距对照验证

- 根据真机截图，问题是挖孔两侧空白；暂未确认 OEM 具体布局根因。
- 大岛改为 Capsulyric 已使用的成对 A/B 图文结构，省略空日期前缀，保留所有信息。
- 9 项通知集成测试与 Debug 构建通过；`build/island-spacing/app-debug.apk` 为真机对照包，尚未确认实际间距改善。

### 结果态撤回与静态原生提示

- [x] KMP 结果卡片撤回元数据、Android 状态机交互与回执清理。
- [x] 原生胶囊固定时间、隐藏 when 与 chronometer；旧任务实时通知入口同步静态化。
- [x] 定向 Core/Android 测试、完整构建与产物验证。

- 补充验证：Android 全量 240 项单测、KMP Native 8 项定向测试通过，Core Release AAR 和 Android Debug 构建通过。
- 测试覆盖完成/放弃撤回后恢复临近/逾期、窗口外隐藏、重复点击拒绝、结果回执清理，以及静态通知不含时间计数器/有效进度条。
- 构建快照与本次修改源文件一致，APK/AAR 完整性检查通过，产物位于 `build/island-undo-static/`。
- 本次静态 native 胶囊与撤回按钮仍需真机确认；大岛间距已获用户确认正常。

## 2026-10-08 习惯结果与图标修订
- [x] 图标与文字稳定性、KMP 习惯结果状态、Android 展示、回归测试与 APK。
- Google 官方 Material Symbols Rounded alarm 24px 转为框架 VectorDrawable；状态色由通知元数据控制。
- TextInfo 上游模型确认 narrowFont 是窄字体开关，未提供固定字重字段。任务、习惯和专注收起态统一设 false，完成/放弃保持相同组件；字重最终效果待真机复测。
- KMP 添加习惯窗口、x/y 和 HABIT_CHECKED_IN，当前提醒打卡后保留三分钟；达标关闭、未达标恢复剩余窗口。记录与删除标记进入版本，避免打卡后/清空后旧按钮失效校验遗漏。
- 11 项 Core macOS 通知规划单测、254 项 Android 单测通过，无失败/错误/跳过；Core Release AAR 和 Android Debug APK 构建通过；源快照一致性与 APK/AAR ZIP、SHA-256 验证通过。
- 已覆盖周期窗口进度、跨午夜与临近结束打卡、删除恢复、持久化清空后旧按钮拒绝、名称 Unicode 截断、已打卡绿色结果与 x/y；产物 build/habit-island-polish/。
- 本轮无连接设备，未执行小米真机视觉和操作验证，也未复跑 API 36 仪器测试。

## Miuix 状态图标
- [x] 固定版本来源核对、矢量转换与预览、状态映射和展开/收起同步、定向回归及 Debug APK。
- 核对项目 miuix-icons 0.9.4-rc01 缓存及上游固定提交 4a6b750b 的图标源码；八种 Medium 图标原始 PathNode 和 Y 变换完整转为框架矢量，来源/Apache 2.0 许可证随 APK 分发。
- AppKit 预览核对各状态轮廓、方向和配色；任务/习惯 phase 和专注状态统一映射展开、收起与小岛，保留独立 APP 身份图标。
- 25 项通知定向单测、Debug 构建、源快照与 ZIP 完整性/SHA-256 验证通过。产物 build/miuix-island-state-icons/，含图标预览。设备未连接，实际超级岛视觉待真机确认。
