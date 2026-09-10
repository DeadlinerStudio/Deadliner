# Widget Scroll, Layout, and Refresh Tasks

## 阶段 1：规格冻结

- [x] 完成 `spec.md`
- [x] 完成 `module-plan.md`
- [x] 完成评审或自检

## 阶段 2：基础设施

- [x] 增加滚动实例配置及兼容存储
- [x] 增加编辑器退出后的刷新策略
- [x] 固化 4×6 默认几何参数

## 阶段 3：功能实现

- [x] 修复静态三行高度预算与底部安全余量
- [x] 移除任务卡片描边
- [x] 实现 2×2 可选 Glance LazyColumn
- [x] 大型改动完成后再统一编译

## 阶段 4：验证与回写

- [x] 执行规格校验
- [x] 执行大文件扫描
- [x] 回写最终实现状态

## 验证结果

- `:app:testDebugUnitTest`：199 项测试通过。
- `:app:assembleDebug`：构建通过。
- 规格目录校验通过；本次 Glance 模块最大文件 324 行，未触发 1000 行拆分阈值。
- 待真机确认：MIUI 桌面保存后的延迟刷新、4×6 视觉裁切和 LazyColumn 手势。

## 第二轮修复

- [x] 根据 Glance 1.1.1 源码确认活跃会话不会重启 `provideGlance`
- [x] 增加可观察的渲染快照与配置修订状态
- [x] 让通用刷新、配置保存和 KMP 更新先发布状态再更新宿主
- [x] 用 weight 重写 2×2 剩余高度分配，移除手算 viewport
- [x] 补充刷新状态与布局回归测试
- [x] 重新执行全量单测、Debug/Release 构建及规格校验
- [ ] 小米真机验证（当前 ADB 未连接设备）

## 第二轮验证结果

- `:app:testDebugUnitTest`：200 项测试通过。
- `:app:assembleDebug` 与 `:app:assembleRelease`：构建通过，Release R8 与 lintVital 通过。
- 规格校验和 `git diff --check` 通过；Glance 模块最大文件 324 行。
- 通过 Glance 1.1.1 源码验证 `collectAsState` 是活跃 composition 的官方数据观察方式。

## 第三轮视觉调整

- [x] 2×2 内容区四边 padding 统一为 12dp
- [x] 2×2 卡片背景统一应用 92% 不透明度
- [x] 执行全量测试、Release 构建与正式签名
- [x] 飞书发送
