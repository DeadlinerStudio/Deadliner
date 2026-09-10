# Cross-platform Widget Refresh Coverage

## 背景

Deadliner 的任务/习惯小组件分别由 Android Glance、iOS WidgetKit 和 HarmonyOS FormExtension 承载。当前三端刷新入口分散：Android 依赖 StateFlow、Activity `onResume/onStop` 与 provider 周期，iOS 主要依赖 WidgetKit Timeline，HarmonyOS 有 `WidgetUpdater` 与 Ability 前后台回调，但 KMP 数据库提交并没有一个统一、可追踪的刷新请求。

## 目标

- 在 DeadlinerCore commonMain 定义 `expect updateWidget(reason)` 平台契约。
- 分类、任务、习惯及其记录的数据库事务成功提交后，由 KMP 层触发一次 `DATA_MUTATION` 请求。
- 用户下拉刷新完成数据合并/重读后触发 `USER_REFRESH` 请求。
- 三端应用进入前台时触发 `APP_FOREGROUND` 请求。
- 三端保留平台原生定时刷新：Android AppWidget 周期、iOS WidgetKit Timeline、HarmonyOS Form `updateDuration`。
- 平台宿主对短时间内的重复请求去抖，失败不反向影响数据库事务或页面刷新。

## 非目标

- 不让 KMP commonMain 直接依赖 Glance、WidgetKit、FormKit 或 ArkTS Context。
- 不保证系统精确按固定分钟唤醒；后台时机仍服从各平台调度策略。
- 不重写小组件数据计算、UI 或按实例外观配置。
- 不为不影响任务/习惯小组件的数据表（例如 AI memory）强制刷新。

## 用户场景

1. 用户创建、编辑、完成、恢复或删除任务后，桌面小组件在平台允许的最短时间内更新。
2. 用户打卡、撤销习惯记录或编辑习惯后，包含习惯统计的 iOS/HarmonyOS 小组件更新。
3. 用户下拉刷新并合并远端数据后，小组件刷新为合并后的状态。
4. 用户从后台回到应用，系统时间导致的“临近/逾期”文案得到刷新。
5. 用户长期不打开应用时，平台原生定时刷新仍提供兜底覆盖。

## 验收标准

- commonMain 存在带原因枚举的 `expect fun updateWidget(...)`，Android/iOS/ohosArm64/macosArm64 均有 actual。
- `CategoryRepository`、`TaskRepository` 与 `HabitRepository` 只在成功提交事务后发出数据变更请求；失败事务不触发。
- Android KMP actual 进入现有 `TaskWidgetRefreshCoordinator`，150ms 内的重复请求合并；Application 级前台切换只刷新一次，不再在 Activity `onStop` 刷新。
- iOS KMP actual 通过进程内通知交给统一协调器，协调器调用 `WidgetCenter.reloadAllTimelines()`；`.active` 生命周期和 15 分钟 Timeline 均覆盖。
- HarmonyOS KMP actual 通过 native/NAPI 回调请求现有 `WidgetUpdater.updateAllWidgets(context)`；Ability `onForeground` 与 Form 周期更新均覆盖。
- 下拉刷新入口在 Android、iOS、HarmonyOS 均发出 `USER_REFRESH` 请求。
- Android 单测与 Debug APK、DeadlinerCore common/平台编译、iOS scheme 构建、HarmonyOS 编译在可用环境中通过；无法执行的设备级验证需在任务文档注明。

## 风险与约束

- iOS 工作区已有未提交修改，`DeadlinerApp.swift` 等重叠文件只能做最小 patch，禁止覆盖用户内容。
- HarmonyOS native 回调可能来自非 ArkTS 线程，NAPI 桥必须使用线程安全函数或等价安全调度，不能跨线程直接调用 JS。
- KMP 的刷新请求属于 best-effort 副作用：任何平台刷新异常都不得回滚已成功的数据事务。
- 所有协调器必须具备去抖/合并能力，避免批量同步或连续打卡造成刷新风暴。
- 新增核心文件目标小于 350 行、硬阈值 1000 行。
