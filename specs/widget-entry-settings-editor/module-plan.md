# Widget Entry And Settings Editor Module Plan

## 模块拆分

- `widgets/glance/interaction`：生成指向统一 `LauncherActivity` 入口的标准 Glance Action，供两种尺寸复用。
- `widgets/glance/ui`：把共享打开 Action 绑定到根卡片、静态内容层、滚动集合项与 2×1 覆盖层；滚动集合使用小米桌面可正常渲染的标准模板。
- `widgets/glance/theme`：只生成原始不透明渐变位图；透明度由 Glance background modifier 承担。
- `widgets/glance/configuration`：解析编辑来源、枚举实例、创建设置内编辑 Intent，并协调配置 Activity 的完成行为。
- `ui/settings`：展示实例 ID 列表并在页面恢复时刷新。
- `widgets/glance/configuration` UI：滚动表单与固定底部操作栏。

## 平台映射

- Android：本规格全部实现于 `app/src/main/java/com/aritxonly/deadliner/widgets/glance` 与 `ui/settings`。
- iOS：WidgetKit 的 URL 入口和配置机制保持不变。
- HarmonyOS：FormExtension 的路由与配置机制保持不变。

## 文件拆分策略

- 新增独立的交互 Intent 工厂，避免在 2×2、2×1 页面重复 flags 与统一入口目标。
- 将实例模型/合并规则放入配置基础设施，不把 AppWidgetManager 查询细节写进 Compose 设置页。
- 保持 `TaskWidgetConfigurationScreen.kt` 只负责编辑 UI；Activity 负责入口来源和保存完成策略。
- 所有核心文件维持在 1000 行有效代码以内，无需大文件逃逸。

## 风险点

- `actionStartActivity` 使用显式且按 AppWidget ID 唯一的启动 Intent；整卡只保留一个 Action 后，唯一 URI 不再与多层 clickable 竞争，并可强制宿主替换旧 PendingIntent 身份。
- Glance 集合不会继承祖先 RemoteViews 点击；`LazyColumn` 项必须显式提供 fill-in Action。小米桌面对带 ActivityOptions 的实验性集合模板兼容性不佳，不能为了根卡片的启动选项替换标准模板。
- 滚动集合不能为每一项携带位图背景；大量 `RemoteCollectionItems` 会放大 RemoteViews/Binder 载荷，任务行使用半透明纯色背景，根卡片才保留渲染位图。
- 不保留按品牌分流的第二套布局，避免圆角、排版、颜色和后续配置功能发生双轨漂移。
- Glance 1.2 background alpha 应只改变背景图像节点透明度；点击 Action、内容层和 `LazyColumn` 结构保持与透明度调整前一致。
- Xiaomi 宿主不能依赖 Glance 对 image background 与 Action 的隐式归一化顺序；2×2 使用显式底层 `Image(alpha)` 并把整卡 Action 直接绑定到该可见 Image，静态内容不再声明 Action，滚动集合仅在列表项保留必要的 fill-in Action。
- 字重不得同时依赖极粗字体族和 Bold 样式；主信息使用系统 medium 字族配合 Normal，避免 Glance 把 Medium 再翻译为 bold，状态徽标与计数才使用 Bold。
- Glance 1.2 在 API 32+ 强制把 `LazyColumn` 翻译为内联 `RemoteCollectionItems`，没有公开的强制 service 模式；可滚动区域通过 `AndroidRemoteViews` 嵌入标准 `ListView`，由持久数据库驱动的 `RemoteViewsService` 提供行，卡片其余部分继续使用 Glance。
- 列表适配器 Intent 必须按 AppWidget ID 唯一，数据变化后调用 `notifyAppWidgetViewDataChanged`；行只携带小型状态背景和 fill-in Intent，不把整卡降级为第二套原生布局。
- 内存渲染状态对相同快照不得递增 revision；应用前台但数据未变化时不重新发送 RemoteViews。配置保存只失效一次配置并更新目标实例一次，不再延迟重试和全量刷新。
- 设置页生命周期恢复时应重新查询实例，避免删除/新增小组件后显示旧 ID。
- 固定底栏和 Scaffold 提供的 bottom padding 不能重复消费导航栏 inset。
