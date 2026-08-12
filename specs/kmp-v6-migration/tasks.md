# Deadliner v6 KMP Migration Tasks

## 阶段 1：规格冻结

- [x] 完成 `spec.md`
- [x] 完成 `module-plan.md`
- [x] 冻结 Android v5 基线提交 `9a4b5d6` 并创建 `refactor/kmp`
- [x] 盘点 Android ViewModel/Lifi 与 Core 现有能力
- [x] 完成规格校验与自检

## 阶段 2：基础设施

- [x] 确认共享依赖、接口、数据结构现状
- [x] 让 Android 显式依赖 `.deadliner-core/kmp/deadliner-core.aar`
- [x] 统一本地 Core 路径与 runscript/Gradle 开关
- [x] 以 Intro lifecycle wrapper 建立 Android 薄适配规则
- [ ] 定义 Room -> SQLDelight 数据迁移映射与回滚策略

## 阶段 3：功能实现

- [x] 迁移 Intro wizard 状态机
- [ ] 接入 Capture 共享 ViewModel
- [ ] 接入 Task list/detail/editor/archive/search 共享 ViewModel
- [ ] 接入 Habit list、Overview、Settings 共享 ViewModel
- [ ] 用共享 Lifi API 替换 Android `AIUtils` 业务逻辑
- [ ] 完成 Room -> SQLDelight 数据迁移并停止双写
- [ ] 删除 Android 重复的业务 ViewModel/repository/AI 实现
- [ ] 大型改动完成后再统一编译

## 阶段 4：验证与回写

- [ ] 执行规格校验
- [ ] 执行大文件扫描
- [ ] 运行 Core common/Android 测试与 `runscript` 构建部署
- [ ] 运行 Android 单元测试与 debug 构建
- [ ] 验证 v5 数据升级、Lifi、任务、习惯、捕获、归档、搜索、概览和设置
- [ ] 回写最终实现状态
