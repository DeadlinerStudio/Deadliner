# Widget Entry And Settings Editor

## 背景

当前 Glance 2×2 与 2×1 小组件点击后无法稳定进入 Deadliner；小组件实例只能从桌面宿主进入编辑页，应用设置内无法查看或重新编辑已添加的实例。小米桌面通过独立编辑入口启动配置页时，保存后还可能把既有应用任务栈带到前台。

## 目标

- 所有现存 Glance 任务小组件点击主体后直接打开 Deadliner 主界面。
- 设置页列出当前已添加的 2×2、2×1 小组件及真实 AppWidget ID，并可进入对应实例的编辑页。
- 区分桌面宿主入口与应用设置入口；桌面编辑保存后返回桌面，设置内编辑保存后返回设置。
- 将编辑页保存操作固定在底部，沿用任务添加页的底部主操作区布局和当前 Material/MIUIX 设计系统适配。
- 保持 Pixel 与 Xiaomi/HyperOS 使用同一套 Glance 视觉结构；不以低保真原生 fallback 换取交互兼容。
- 小组件强调文字同时携带真实粗体字重；2×2 背景透明度使用 Glance 1.2 官方 background alpha，不再预处理半透明 Bitmap。

## 非目标

- 不恢复已清理的 RemoteViews 小组件。
- 不改变小组件任务数据、排序、滚动和外观配置规则。
- 不修改 iOS 或 HarmonyOS 小组件入口。

## 用户场景

- 用户点击桌面上的 2×2 或 2×1 小组件，Deadliner 主页面被打开或切回前台。
- 用户进入“设置 → 小组件”，看到每个已添加实例的类型与 ID，点击后编辑该实例。
- 用户从小米桌面长按进入编辑并保存，编辑页关闭后仍停留在桌面。
- 用户滚动长编辑表单时，保存按钮始终固定在屏幕底部。

## 验收标准

- 2×2 与 2×1 根卡片均使用 AppWidget 专用的显式启动 Intent，指向统一入口 `LauncherActivity`，保留首次启动与主界面分流语义。
- 小组件启动使用 Glance 标准的 `actionStartActivity<LauncherActivity>()`，由 Glance 按目标视图生成互不冲突的 PendingIntent；不再给所有节点复用同一 data URI，也不注入实验性 ActivityOptions。
- 数据内容不得只依赖根卡片点击：静态任务行、滚动集合项和 2×1 内容覆盖层都必须绑定同一个打开 Action。小米桌面下滚动集合使用标准 `LazyColumn` PendingIntentTemplate，避免带 ActivityOptions 的实验性集合模板导致列表不渲染。
- Pixel 与 Xiaomi 系宿主均使用原有 Glance 2×2/2×1 结构、圆角、间距、任务行与 `LazyColumn`，不再按设备品牌替换完整布局。
- 2×2 渐变位图保持完全不透明的源 Bitmap，并在 `GlanceModifier.background(imageProvider, alpha=0.92f)` 处声明宿主透明度；不得在渲染器里复制并回收为预乘透明 Bitmap。
- 强调文字必须设置 `FontWeight.Bold`，同时保留系统字体族层级。
- 启动 Intent 具备稳定且唯一于普通入口的 widget URI，并携带 `NEW_TASK`、`CLEAR_TOP` 与 `SINGLE_TOP` 任务栈语义。
- 设置页实例列表基于两个 Glance Provider 当前注册的 AppWidget ID，去重并稳定排序。
- 设置入口携带明确来源标记；标准配置或 MIUI URI 未携带该标记时均按桌面入口处理。
- 配置 Activity 使用独立 task affinity，避免桌面以 `NEW_TASK` 打开时复用应用主任务栈。
- 保存成功完成持久化和刷新请求后才关闭；设置入口返回设置，桌面入口返回宿主。
- 编辑内容预留底部栏空间，固定栏正确处理导航栏 inset。
- 单元测试、Debug/Release 编译和 Release 签名校验通过。

## 风险与约束

- 不同桌面传递配置 Intent 的 flags 不一致，返回行为需同时依靠来源协议和 Activity task affinity。
- 无连接设备时只能完成 Intent/策略测试与构建验证，小米桌面真实返回栈仍需设备验收。
- 当前工作区包含前序小组件功能的未提交改动，本轮只做增量修改，不覆盖或回退既有内容。

## 实现与验证结果

- 两种 Glance 小组件通过统一的 `LauncherActivity` 入口进入应用，并通过 Glance 的 activity options 为 Android 14+ PendingIntent 创建端显式开启后台 Activity 启动授权；配置页通过来源标记和空 task affinity 隔离桌面编辑返回栈。
- 设置页会在进入及 `ON_RESUME` 时重新枚举两个 Provider 的实例，并展示类型、ID 和编辑入口。
- 编辑页复用添加任务页的 `DeadlineBottomActions`，保存按钮固定在底部并由现有双设计系统组件适配 MIUIX。
- 155 项 Debug 单元测试、Debug 构建、Release/R8 构建与 lintVitalRelease 均通过；规格校验和 `git diff --check` 通过。
- 仓库未提供规格所述的大文件扫描脚本/后缀清单；本轮相关文件已直接统计，最大文件 323 行，未触发 1000 行限制。
- 小米设备进一步实测表明加载阶段可以正常进入应用，而任务内容渲染完成后点击只剩按压反馈；根因修正为内容层未继承根卡片 Action，尤其 Glance `LazyColumn` 集合项需要独立 fill-in Action，而不是 Activity 入口本身失效。
- 回归修复后 163 项 Debug 单元测试零失败，Debug、Release/R8、`lintVitalRelease`、规格校验与签名证书校验通过。
- 内容点击修复已恢复 `LauncherActivity`，并把共享 Action 下沉到 2×2 静态/滚动内容以及 2×1 内容覆盖层；163 项 Debug 单元测试零失败，Debug、Release/R8 与 `lintVitalRelease` 通过。
- 小米设备实测发现带 ActivityOptions 的实验性 `LazyColumn` 模板会令滚动列表内容消失；滚动路径已改回标准集合模板并去除集合项嵌套点击，只在可见任务行保留单层 fill-in Action。
- 再次实测表明仅回退集合模板仍不足：滚动集合中的逐行位图背景会随任务数量放大 RemoteViews 传输体积，可能让小米宿主直接丢弃列表；任务行因此改为轻量半透明纯色背景。点击同时恢复 Glance 标准 class Action，避免根卡片与内容节点因固定 URI 和 `FLAG_UPDATE_CURRENT` 共用 PendingIntent。
- 结构修复后新增真实 Glance 组合测试：100 条滚动任务能够生成 `ListView`，RemoteViews Parcel 低于 1 MiB；连同 Action 与透明度契约共 163 项测试零失败，Debug、Release/R8、`lintVitalRelease`、正式签名和飞书交付均通过。
- 当前无 ADB 设备连接，新修复仍需安装签名包后做小米桌面最终真机验收。
- 后续真机反馈确认 Glance 1.1.1 的内联 `RemoteCollectionItems`、多层点击与字体族别名组合在 Pixel 正常、在 Xiaomi/HyperOS 仍失败；阶段 10 改为按宿主品牌启用经典 RemoteViews 兼容渲染，不再继续依赖同一条 Glance 集合链路试错。
- 阶段 10 的 Xiaomi 原生 fallback 证明交互链路可用，但真机反馈显示其圆角与内容视觉不可接受，因此只保留为一次诊断结论并在阶段 11 完整撤销。
- 阶段 11 恢复统一 Glance 布局，升级到稳定版 Glance 1.2.0，并使用官方 background alpha API 实现 92% 背景透明度；以此隔离阶段 10 fallback 与原先手工半透明 Bitmap 两类变量。
- 阶段 11 的 `:app:testDebugUnitTest` 共 166 项零失败，覆盖不透明背景源图、Glance 官方 background alpha、可点击根卡片与 100 项 `LazyColumn`/`ListView` 集合同时存在，以及字体字重映射。
- `:app:assembleDebug`、`:app:assembleRelease` 与 `lintVitalRelease` 通过；Release R8、资源压缩和 `git diff --check` 通过。Glance 相关 Kotlin 文件最大 323 行。
- 当前无 ADB 设备连接；阶段 11 的统一 Glance 外观、点击、滚动手势和 Xiaomi 系统字体显示仍需安装 Debug APK 真机确认。
- 阶段 11 真机复验显示 Xiaomi 上字体因 `sans-serif-black` 与 `Bold` 叠加而过粗，自动展开的透明 background Image 与多层重复 clickable 仍未恢复点击；阶段 12 改为适中字族，并将唯一整卡 Action 直接绑定到显式背景 Image，内容层不再重复声明点击。
- 阶段 12 将主信息降为 `sans-serif-medium` + 400、辅助信息降为 `sans-serif` + 400，仅徽标和计数保留 700；点击使用显式背景 Image 的单一整卡 Action 和按实例唯一的 Launcher Intent。
- 阶段 12 共 166 项 Debug 单元测试零失败，Debug、Release/R8、资源压缩、`lintVitalRelease`、规格校验和 `git diff --check` 均通过；真机结论仍待本阶段 APK 复验。
- 阶段 12 真机确认字体与点击已恢复，但 Xiaomi/HyperOS 仍不显示 Glance 1.2 在 API 32+ 使用的内联 `RemoteCollectionItems`，并能观察到周期性整卡闪动。
- 阶段 13 保留 Glance 卡片、显式背景 Image 与半透明效果，只把可滚动区域改为标准 `RemoteViewsService` + `ListView`；同时对相同快照去重并移除配置保存后的多次重试更新。
