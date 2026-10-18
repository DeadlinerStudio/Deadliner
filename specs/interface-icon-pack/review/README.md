# 图标图形与语义审查

## 基准与规则
逐项查看四张接触表及 HTML 对照页。左侧直接从应用的变量字体导出 wght=330 / GRAD=0 / opsz=24，填充项按 FILL=1 导出；右侧为最终 Hybrid 矢量。

遵循用户修订：基础操作语义一致可采用不同轮廓，优先官方 MIUIX；收藏、星标、书签等内容图标严格保留主体，空心/实心状态分别处理。补充库使用 Phosphor Fill，没有使用 Lucide。

## 覆盖
- 142 个语义/状态键：MIUIX Glass 53，MIUIX Regular 1，Phosphor（含组合）86（71 Fill、15 Regular），Android、house 保留原 Material 2 项。
- 实际生成 140 个 Hybrid 资源，统一由 Compose 和 XML 绘制桥使用；未知未来键保留 Material 回退，品牌图标走原资源。
- Phosphor Fill 组合保留新增加号、禁用斜线、手机内的更新/设置标志，不把附加信息丢掉。组合是本地派生，不冒充上游独立图标。

## 审图中拒绝或调整的候选
| 原语义 | 问题候选 | 最终处理 |
|---|---|---|
| 心形喜爱与五角星星标 | 按 Favorites 名称猜成星形 | 审图确认 Favorites/FavoritesFill 为心形；Starred 为五角星。实心五角星使用 Phosphor star |
| 书签 | Glass AddBookmark 自带加号 | 换为无加号的填充书签 |
| 专注计时/秒表 | Glass Timer 是沙漏 | 使用 Glass Stopwatch |
| 下箭头 | Glass ExpandMore 为四角展开标记 | 使用 Phosphor caret-down |
| 开源协议文档 | 把 contract 当作收缩操作 | 使用官方 Glass File 文档 |
| 群组/多人 | Glass Community 实为气泡 | 使用 Phosphor users 保留多人主体 |
| 两颗星 | seal-check 是徽章打勾 | 使用一大一小两个五角星组合 |
| 更新、生成、新增任务/提醒 | 单一主体会丢失附加符号 | 保留手机+箭头/齿轮、任务/闹钟/铃铛+加号组合 |
| 分享 | Glass Share 是向上分享 | 按用户基础图标规则接受同义分享，采用官方图标 |
| 基础编辑、日历、任务列表、书籍、设置 | 轮廓与 Material 不同 | 核对基本操作/对象含义后优先使用官方 MIUIX；不宣称像素相同 |

## 运行时约束
返回、左右导航、撤销、发送和逻辑侧栏按 RTL 镜像；心形、星形、媒体播放、文字/金额和其他物体不被全局镜像。调用方 tint、透明度、可访问性描述和点击区域保持独立。XML 图标切换后会失效重绘。

## 查看与复现
[index.html](index.html) 支持按名称、来源和审查说明搜索；contact-1.png 至 contact-4.png 为完整离线对照图。逐项结果也在 scripts/interface_icon_map.json 中。

生成脚本和固定版本记录见 third_party/interface-icons/NOTICE.md；本地编译与主机测试另见 ../tasks.md。离线审图不等于设备视觉验收，未操作主力机。

2026-10-17：分类 house 保留 Material 房屋，Home 只出现在主页清单语境；展开使用 Glass ExpandMore，更新入口使用 Regular Update。发送入口明确走 Material 字体，不受图标包影响。HyperModifier 源码对照：MiuixFloatingTabBarDefaults.DetachedIconSize=28dp；MyHyperModifierSettingsApp 的 SettingsNavigationItem ChevronForward=16dp。

VoiceAssistProxy.apk 离线 apktool 反编译仅有 com.miui.voiceassistProxy.activity.MainActivity 和通用 AppCompat 资源，MainActivity 启动系统 com.miui.voiceassist；没有截图中的对话图标。ic_lifi_new_conversation.xml 是按用户提供截图重绘的独立矢量，并非 APK 提取。

分类 home 最新映射为 Phosphor Fill house（Hybrid），Material 仍为 home；离线原始路径在 third_party/interface-icons/phosphor-fill/house.svg，图形和语义均为房屋。

局部字重：月份切换箭头用 Glass ChevronBackward/ChevronForward Demibold（500）；快速添加展开用 Glass ExpandMore Medium（430），固定 PR 423 的实际路径存放在 miuix-glass/*_Demibold.svg 与 ExpandMore_Medium.svg。仅对应位置切换，不影响其他箭头。

逐图标光学校准：86 个 Phosphor 语义键各有固定缩放值，别名一致、无场景系数；MIUIX 资产保持原样。重叠实心区域过重的基础操作使用同版本 Regular，收藏/星形/书签的对象与填充状态保留。完整前后对照见 ../../hybrid-icon-optics/review/index.html。
