# Widget Entry And Settings Editor Tasks

## 阶段 1：规格冻结

- [x] 明确小组件点击、实例列表、编辑返回栈与固定底栏的行为边界。
- [x] 冻结模块拆分与验证要求。

## 阶段 2：基础设施

- [x] 新增共享的小组件主界面启动组件。
- [x] 扩展编辑入口协议与已添加实例枚举。
- [x] 增加入口来源及实例排序的单元测试。

## 阶段 3：功能实现

- [x] 让 2×2 与 2×1 使用共享主界面启动操作。
- [x] 在小组件设置页展示可编辑实例 ID，并在恢复时更新。
- [x] 修正桌面编辑 Activity 的 task affinity 和保存返回行为。
- [x] 将保存按钮迁移到固定底部操作栏。

## 阶段 4：验证与回写

- [x] 执行规格校验、diff 检查和相关文件大小检查（仓库未提供统一扫描脚本）。
- [x] 运行单元测试与 Debug/Release 构建。
- [x] 签名并校验 Release APK。
- [x] 回写实现事实、验证结果和设备验证缺口。

## 阶段 5：小米桌面点击回归修复

- [x] 曾将小组件入口改为直达 `MainActivity` 以排查 Activity 启动限制；后续真机证据排除此根因并在阶段 6 恢复统一入口。
- [x] 为 Glance PendingIntent 注入 Android 14+ creator BAL activity options。
- [x] 更新 Intent 契约测试并重新执行全量构建、签名和飞书交付。

## 阶段 6：内容加载后点击回归修复

- [x] 根据真机现象恢复 `LauncherActivity` 统一入口并修正 Intent 契约测试。
- [x] 为 2×2 静态任务行、滚动集合项和 2×1 内容覆盖层绑定共享打开 Action。
- [x] 将 Android 14+ ActivityOptions 同步到 Glance `LazyColumn` 的 PendingIntentTemplate。
- [x] 重新执行规格校验、全量测试、构建、正式签名和飞书交付。

## 阶段 7：滚动列表渲染回归修复

- [x] 撤销小米桌面不兼容的实验性 `LazyColumn` ActivityOptions 模板。
- [x] 去除滚动集合项嵌套点击，仅保留任务行单层 fill-in Action。
- [x] 重新执行规格校验、全量测试、构建、正式签名和飞书交付。

## 阶段 8：PendingIntent 冲突与集合载荷修复

- [x] 将共享点击恢复为 Glance 标准 `LauncherActivity` class Action，移除固定 URI、flags 和 ActivityOptions。
- [x] 移除滚动任务项的逐行位图背景，改用轻量半透明颜色背景。
- [x] 为 Action 契约和任务行背景透明度补充单元测试，并验证 100 项集合 RemoteViews 存在且低于 1 MiB。
- [x] 重新执行规格校验、全量测试、构建、正式签名和飞书交付。

## 待真机日志定位

- [ ] 小米桌面：任务内容渲染完成后，点击仅出现按压反馈但未启动 `LauncherActivity`；连接复现设备后采集 Launcher、ActivityTaskManager、AppWidget 与应用进程日志，并核对宿主最终持有的 PendingIntent。
- [ ] 小米桌面：开启可滚动列表后任务项消失；连接复现设备后检查 AppWidgetHost、RemoteViews 集合适配器、事务大小与 apply 异常，暂停继续凭现象调整集合实现。

## 阶段 9：小组件文字层级

- [x] 确认 Glance 1.1.1 将 `FontWeight.Medium` 与 `FontWeight.Bold` 输出为相同的 `android:textStyle="bold"`。
- [x] 主信息改用系统 `sans-serif-black` 字体族，辅助信息改用 `sans-serif-medium`，避免依赖无差异的 Glance 字重映射。
- [x] 增加 RemoteViews 产物测试，确认强调与辅助字体族均被保留到宿主 TextView。

## 阶段 10：Xiaomi/HyperOS 原生宿主兼容层

- [x] 根据 Pixel 正常、Xiaomi 失败的对照结果冻结兼容分支边界与集合协议。
- [x] 增加 Xiaomi/Redmi/POCO 环境识别与原生 Activity PendingIntent 工厂。
- [x] 为 2×2 增加经典 `RemoteViewsService` + `ListView`，静态模式最多三项、滚动模式返回完整列表。
- [x] 为 Xiaomi 2×2/2×1 增加原生 RemoteViews 根布局，移除滚动区域上的重叠点击层。
- [x] 为 Glance 与原生兼容文字补齐显式字重，并增加宿主分流、Intent、列表数量和字体属性测试。
- [x] 完成规格校验、大文件扫描、全量单测与 Debug/Release 构建后回写验证结果。

## 阶段 10 验证结果

- `:app:testDebugUnitTest`：174 项测试通过，包含 RemoteViews 白名单 inflate、经典服务声明、字重属性、PendingIntent 身份隔离与列表数量覆盖。
- `:app:assembleDebug`、`:app:assembleRelease` 与 `lintVitalRelease` 通过；Release R8 和资源压缩通过。
- 规格校验与 `git diff --check` 通过；仓库未提供统一大文件扫描脚本，本次直接统计 Glance 模块，最大 Kotlin 文件 323 行。
- 合并 Manifest 已确认经典列表服务 `exported=true` 且受系统 `android.permission.BIND_REMOTEVIEWS` 权限保护。
- 当前 ADB 无设备连接；最终点击、滚动手势和 Xiaomi 系统字体显示仍需安装 Debug APK 真机确认。

## 阶段 11：恢复统一 Glance 视觉并修正透明度实现

- [x] 根据真机反馈确认原生 fallback 的交互正常但视觉不可接受，并将故障边界收敛到手工半透明 Bitmap 之后。
- [x] 撤销 Xiaomi/Redmi/POCO 原生布局、经典列表服务、品牌分流、专用 Intent 与对应测试资源。
- [x] 升级稳定版 Glance 1.2.0，2×2 背景改用官方 `background(alpha=0.92f)`。
- [x] 恢复背景渲染器输出不透明 Bitmap，保留 Glance 圆角、内容与 LazyColumn 结构。
- [x] 增加背景源位图不透明及 modifier alpha 契约测试。
- [x] 执行规格校验、大文件检查、全量测试与 Debug/Release 构建并回写结果。

## 阶段 11 验证结果

- Xiaomi/Redmi/POCO 专用 RemoteViews fallback、经典列表服务、原生布局与品牌判断已完全删除，两类小组件重新共享 Glance 视觉结构。
- 2×2 背景位图保持不透明，由 Glance 1.2.0 的图片背景 alpha 以 0.92 应用透明度；2×1 背景不变。
- 166 项 Debug 单元测试零失败；Debug、Release/R8、资源压缩与 `lintVitalRelease` 均通过。
- `git diff --check` 通过；Glance 相关 Kotlin 文件最大 323 行，未触发 1000 行限制。
- 当前没有 ADB 设备，仍需在 Xiaomi/HyperOS 桌面真机复验最终视觉、点击和滚动。

## 阶段 12：显式点击层与适中字重

- [x] 记录阶段 11 的 Xiaomi 真机结果：字体过粗且卡片点击仍无响应。
- [x] 将 2×2 自动 background alpha 展开改成显式底层 `Image(alpha=0.92f)`，整卡 Action 直接绑定到该 Image。
- [x] 去除 2×2 静态行和 2×1 内部的重复 clickable，只保留整卡 Action；滚动列表项保留 fill-in Action。
- [x] 整卡与滚动项共享按 AppWidget ID 唯一的显式 Launcher Intent，替换小米宿主可能缓存的旧 class Action。
- [x] 主字体由 black+Bold 调整为 medium+Normal，仅徽标和计数保留 Bold。
- [x] 增加 RemoteViews 点击节点数量、层级和字体契约测试。
- [x] 执行规格校验、大文件扫描、全量测试与 Debug/Release 构建并回写结果。

## 阶段 12 验证结果

- 两种尺寸均复用显式背景 Image + 内容层结构；非滚动布局只有一个整卡点击节点，滚动集合项保留标准 fill-in Action。
- 启动 Action 使用 `deadliner://widget/open/<appWidgetId>` 唯一身份，并携带 `NEW_TASK | CLEAR_TOP | SINGLE_TOP`。
- 主信息输出为 `sans-serif-medium`、font weight 400，辅助信息为 `sans-serif`、400；仅徽标与计数输出 700。
- 166 项 Debug 单元测试零失败；Debug、Release/R8、资源压缩和 `lintVitalRelease` 均通过。
- 规格校验与 `git diff --check` 通过；仓库仍无统一大文件扫描脚本，直接统计的 Glance Kotlin 最大文件 323 行。
- 当前无 ADB 设备；Xiaomi/HyperOS 的最终点击、滚动与字重仍需使用本阶段 Debug APK 真机验收。

## 阶段 13：服务型滚动集合与刷新去重

- [x] 记录阶段 12 真机结果：字体和点击正常，Xiaomi 可滚动列表仍为空且小组件偶发闪动。
- [x] 仅将可滚动内容区改为 Glance `AndroidRemoteViews` 内嵌 `ListView`，使用标准 `RemoteViewsService` 提供任务行。
- [x] 保持 Glance 卡片背景、圆角、Header 和半透明效果，不恢复按品牌分流的完整 fallback。
- [x] 为服务列表增加按实例唯一 adapter Intent、行 fill-in 点击和数据变化通知。
- [x] 对相同任务快照去重；移除配置保存后的延迟二次更新与全量刷新。
- [x] 增加服务声明、列表 inflate/数据数量、适配器身份和渲染状态去重测试。
- [x] 执行规格校验、大文件扫描、全量测试与 Debug/Release 构建并回写结果。

## 阶段 13 验证结果

- 2×2 静态模式继续使用原有 Glance 内容；开启滚动后仅内容区嵌入系统 `ListView`，外层圆角、Header、显式背景 Image 与 0.92 半透明保持不变。
- 集合 adapter Intent 按 AppWidget ID 唯一，行点击使用可变 PendingIntent 模板与 fill-in Intent；服务受系统 `android.permission.BIND_REMOTEVIEWS` 权限保护。
- 相同任务快照不再递增渲染 revision 或触发更新；配置保存由“立即、延迟、全量”多次更新收敛为一次实例更新，并单独通知滚动集合。
- `:app:testDebugUnitTest`：172 项测试零失败；`:app:assembleDebug`、`:app:assembleRelease`、Release R8、资源压缩与 `lintVitalRelease` 均通过。
- 规格校验与 `git diff --check` 通过；仓库未提供统一大文件扫描脚本，直接统计的 Glance Kotlin 最大文件 323 行，新集合实现最大文件 128 行。
- 当前未连接 ADB 设备；Xiaomi/HyperOS 的滚动列表显示、行点击和闪动频率仍需安装本阶段 Debug APK 真机验收。
