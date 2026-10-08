# 专注计时展示与设置分层

## 背景
计时设置和运行状态混在 Activity，主页计时条的按钮及几何关系没有对齐 Spotify 参考。

## 目标
- 参考 MyHyperModifier SpotifyPlaybackCapsule 与 SpotifyFloatingNavigation：50dp 胶囊高度、与可见导航 dock 相同水平边界、6dp 相邻间距。
- 主页暂停/继续和结束使用无容器 IconButton，颜色和进度动画表示计时状态。
- 任务右滑与详情底栏打开 Miuix 设置 Sheet；开始成功才打开独立运行 Activity。
- Activity 以大号秒表/倒计时和进度环为主，支持暂停、继续、结束、完成任务、放弃。

## 非目标
不变更 KMP 计时引擎、数据库、通知 Intent 协议；不修改 iOS/HarmonyOS。

## 用户场景
任务右滑专注 → Sheet 选择模式/时长 → 开始 → 运行 Activity → 返回主页 → 计时胶囊快捷操作/返回 Activity。
已有计时再次点任务专注时，直接打开当前计时，避免创建第二轮。

## 验收标准
- 主页操作没有带容器的文本 Button，保留 48dp 触控目标及本地化描述。
- 浮动导航路径计时胶囊与 dock 的可见整体边界一致，默认 50dp 高度、6dp 间距；大字体允许按内容增高。
- 计时条共享 page backdrop，不记录自身；暂停停止时间推进动画。
- Sheet 取消不启动计时；失败保留草稿和反馈；开始成功只打开一次 Activity。
- 运行 Activity 的大号计时和剩余/已投入时间遵循 KMP snapshot，完成后显示完成状态或关闭。
- 字重体现信息层次：计时数字与选中选项使用 SemiBold，任务标题、状态、主要操作标签和统计数值使用 Medium，说明与次要标签使用 Normal。Sheet、运行页与主页计时条保持一致。
- 编译通过，入口及尺寸的关键规则测试通过；无设备时明确标记视觉未验证。

## 风险与约束
保持原有 Activity Intent 协议，不引入持久化 breaking change。Spotify 是 Android View 注入实现，复用几何规则与项目相同玻璃 recipe，不搬入其宿主 hooks。小模块拆分避免扩大已有大文件。整批实现后统一编译。

## 实现结果
- 操作样式修订：设置使用任务行、紧凑分段选择和独立开始 FAB；运行页中央暂停/继续 FAB，两侧较小结束/完成图标，放弃放在顶栏。避免成排矩形按钮。页内 FAB 使用规范的无 backdrop 材质回退，避免采样自身；主页玻璃采样不变。
- 参考 MyHyperModifier `4af554b` 的 Spotify 几何：计时胶囊默认 50dp，导航 54dp，共享 442dp 宽度上限与内侧 8dp 缩进，间距 6dp。
- 主页计时条共享导航页面的 backdrop，记录范围不包含计时条自身；两个无容器 IconButton 保留 48dp 触控目标，进度线每秒线性推进，暂停冻结。
- 任务右滑和详情底栏使用同一设置 Sheet；默认秒表可直接开始，倒计时提供预设和折叠自定义时长。启动期间阻止重复操作，成功收起 Sheet 后打开运行 Activity。
- 运行 Activity 用大号数字、进度环和状态标签展示计时；设置从运行页移除，统计与会话记录继续用 SettingsSection。
- 运行验证发现通知小图标使用应用自定义 Drawable 会使 SystemUI 加载失败，已换为静态矢量；前台服务初次加载改为等待完成。
- 构建、11 项测试与 API 36 模拟器交互验证通过，具体范围见 tasks.md。

## 项目组件统一修订
- 暂停、继续、结束注册在 material_symbols_map，通过项目生成器生成字体 Drawable 描述与 Compose 注册表，统一主题的 wght/GRAD/opsz。系统通知继续使用独立静态矢量图标。
- 设置 Sheet 使用 MiuixBottomSheet，复用根 Scaffold popup host 与已注册页面 backdrop，遵循高级材质、柔光玻璃和无采样回退策略。启动忙碌时通过统一组件的 allowDismiss 禁止关闭。
- 放弃、更正、删除确认框的两侧操作统一使用项目 Button，继承 MiuixDialogActions 的主次配色及等宽布局。
