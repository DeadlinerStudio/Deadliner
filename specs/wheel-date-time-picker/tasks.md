# 滚轮日期与时间选择器 Tasks

## 阶段 1：规格冻结

- [x] 核对 NumberPicker 固定版本签名。
- [x] 确定默认行为、日期边界、模块边界。

## 阶段 2：基础设施

- [x] 抽离 Material 实现与日期校验规则。
- [x] 持久化设置、Activity 对应请求 host。

## 阶段 3：功能实现

- [x] 日期与时间滚轮、过渡、确认与取消。
- [x] 起止时间、提醒、补签代理接线。
- [x] 中英设置文案。

## 阶段 4：验证与回写

- [x] 完整实现后编译、规则测试。
- [x] 模拟器验证设置与两种交互。
- [x] diff、规格和大文件校验，回写结论。

## 验证记录

- 固定版本 NumberPicker 签名通过官方本地 sources.jar 核对，未升级依赖。
- :app:assembleDebug 与 picker / AppearancePreferences 定向单元测试通过。
- 平板模拟器验证：设置默认关闭；开启后日期滚轮可滚动，下一步进入时间滚轮，最终确认回填；任一步取消不修改字段。
- 设置持久化 XML 状态与应用重启后的 Material 日历、24 小时时钟回退已核对。
- 日期、时间滚轮截图分别在 /tmp/deadliner-picker-shots/date-wheel.png 与 time-wheel.png。
- Dialog 直接复用 MiuixDialog 高级材质实现；已有页面捕获器通过主题的共享 modalBackdrop 注册承载，无重复 backdrop。
- diff 与规格校验通过；本次新增文件均低于 1000 行。大文件工具提示的 AIOverlay、SettingsComponents 与生成绑定为既有文件，本次未增加其代码。

## 过渡与循环进位修订

- [x] 固定版本 NumberPicker 本地适配初始化行高度，首次绘制即有五行数字。
- [x] 连续日期/时间索引支持日→月、月→年、分→时、时→日期进位及反向借位，业务上下界保留。
- [x] 日期→时间 / 更改日期支持淡入淡出、横向位移，弹窗保持固定高度。
- [x] assembleDebug 和 14 项 picker 规则/控制器测试通过。
- [x] 平板模拟器验证未滑动的数字、日滚轮 10.30 → 11.1 月份联动，以及进入时间页保留 2026.11.1。截图：/tmp/deadliner-picker-shots/date-carry-initial.png、date-carry-november.png。
- [x] 时间滚轮模拟器验证 01:58 → 02:00，小时同步进位；截图 /tmp/deadliner-picker-shots/time-carry.png。

## 滚动即时联动与切换流畅度

固定手势起点，在数字跨越中线时立即同步选择，父级回传不重启动画；拖动与惯性阶段均实时进位。日期/时间切换改为固定玻璃表面高度、单份滚轮依次淡出淡入，避免尺寸动画与双树绘制。

## 即时联动验证结果

- [x] assembleDebug 和 16 项 picker 规则/控制器测试通过；diff、规格校验通过。
- [x] 模拟器长按拖动 8 秒，第 6 秒截图时 input 手势进程仍运行；日数字进入 01，月已同步为 11，抬起后继续到 11.2。/tmp/deadliner-picker-shots/live-during.png 与 live-after.png。
- [x] 日期与时间页保持相同弹窗尺寸；时间页正确保留 2026.11.2，内容、确认操作正常。/tmp/deadliner-picker-shots/live-transition-final.png。
- [x] 拖动和惯性使用独立位移来源，阶段交接不发布瞬时归零；数字标签按条目缓存。
- [x] 阶段淡出保留少量内容透明度并去掉额外空帧等待，减少切换空白感。

未进行真机帧率测量；上述性能修订针对双份内容、逐帧改变玻璃尺寸和数字重复格式化的明确开销。

### 最终构建状态

实时联动与位移交接版本已完整构建并通过 16 项测试；模拟器已验证。随后减少切换空白帧的两行 UI 动画调整触发最终全量构建时，工作区并发新增了 QuickAdd / 通知图标实现，编译失败于 com.deadliner.domain.quickadd、QuickAddProposal 和 ic_island_miuix_* 引用。日志 /tmp/deadliner-picker-live-build.log 无 ui/picker 编译错误；未覆盖或回退其他模块改动，最终全量构建尚未通过。

## 同屏选择、字重与直接发布验证

- [x] 数字使用 Normal 字重；拖动和惯性动画直接同步发布数字，去掉 snapshotFlow。
- [x] 正常手机上下布局三行数字，宽屏并排五行；小屏/矮屏/大字体回退分步。
- [x] :app:assembleDebug 和 19 项 picker 单元测试通过，包含同步发布和屏幕/缩放布局规则。此次正常同步 Core 后，全量构建通过，解除上述历史构建阻塞。
- [x] 平板与普通手机模拟器同屏布局截图已检查：/tmp/deadliner-picker-shots/combined-tablet.png、combined-phone.png。

- 用户要求停止进一步验证，改为自行编译 Release 实机测试；大字体界面复核和新的月底长拖场景未完成，不作为已验证结果。

## 手机布局回退为两页

- [x] 按用户反馈删除上下两组的合并布局，手机恢复日期→时间两页及现有过渡，大屏仍并排同屏。
- [x] 更新已有布局测试预期与规格。遵循用户自行编译 Release 实测的安排，本轮未运行构建或模拟器验证。
