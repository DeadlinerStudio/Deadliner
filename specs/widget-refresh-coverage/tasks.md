# Cross-platform Widget Refresh Coverage Tasks

## 阶段 1：规格冻结

- [x] 梳理三端现有小组件更新、前台生命周期和定时策略
- [x] 冻结 KMP reason contract、平台桥与 best-effort 语义
- [x] 明确 iOS 脏工作区与 HarmonyOS NAPI 线程安全约束
- [x] 建立 Android 回退点 `7fa932c`

## 阶段 2：基础设施

- [x] 新增 common expect contract 和四个 target actual
- [x] 组合 repository sync/widget notifier，并覆盖分类、任务、习惯事务
- [x] 在手动刷新和同步完成路径发出对应 reason
- [x] 增加 notifier/触发时机测试

## 阶段 3：功能实现

### Android

- [x] 注册 KMP actual handler 到 `TaskWidgetRefreshCoordinator`
- [x] 将前台刷新提升到 Application 生命周期并移除 `onStop` 重复刷新
- [x] 验证两个 Glance provider 的周期兜底

### iOS

- [x] 新增 WidgetKit 刷新协调器并监听 KMP 通知
- [x] 在初次 appear、scene active 与下拉刷新接入请求
- [x] 验证 Timeline 15 分钟策略

### HarmonyOS

- [x] 实现 OHOS actual、C ABI 与线程安全 NAPI callback
- [x] 将 callback 接到 `WidgetUpdater`，并为连续请求去抖
- [x] 在任务/习惯下拉刷新接入 `USER_REFRESH`
- [x] 验证 `onForeground` 与 Form 周期兜底

## 阶段 4：验证与回写

- [x] 执行 DeadlinerCore 四目标编译、macOS 公共测试、Android 单测/APK 与 HarmonyOS HAP 编译
- [x] 编译 iOS 本次涉及文件；全 scheme 剩余失败来自既有 `RichMainView.swift` 的 `TabRole.prominent` 错误
- [x] 执行四仓 `git diff --check` 和核心新增文件行数扫描
- [x] 校验规格目录并回写最终实现差异
- [x] 记录设备/系统调度相关的剩余手测项

## 验证记录

- DeadlinerCore：Android、iOS Simulator、ohosArm64、macosArm64 编译及 `macosArm64Test` 通过。
- Android：`testDebugUnitTest` 与 `assembleDebug` 通过。
- HarmonyOS：KMP SO 部署、CMake/Ninja、ArkTS 与 Debug HAP 打包通过；仅保留工程既有警告。
- iOS：默认 scheme 先被既有 Icon Composer 资源的 `actool` 崩溃拦截；验证构建临时排除 `.icon` 后，本次涉及文件通过编译，随后被既有 `RichMainView.swift` 两处错误拦截。
- 规格校验与四仓 `git diff --check` 通过；新增核心文件均远低于 1000 行。

## 剩余手测

- 在 Android、iOS、HarmonyOS 真机分别验证分类/任务/习惯写入、下拉刷新和回前台后的桌面卡片内容。
- 观察各系统在省电/后台限制下的定时刷新；系统调度为 best-effort，不承诺精确分钟触发。
- iOS 工作区的 Icon Composer 与 `RichMainView.swift` 既有构建问题修复后，再补一次无排除参数的完整 scheme 构建。
