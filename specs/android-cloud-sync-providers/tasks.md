# Android 云与同步 Provider 设置 Tasks

## 阶段 1：规格冻结

- [x] 明确 BYOS 使用 Core 既有的 V2 服务端协议，不新增服务端能力。
- [x] 拆分配置、运行时、调度与设置 UI 的职责。

## 阶段 2：基础设施

- [x] 增加 Android provider 与 BYOS 配置偏好设置。
- [x] 将当前 provider 接入 KMP runtime 与 WorkManager。

## 阶段 3：功能实现

- [x] 新增“云与同步”入口页与 BYOS 配置页。
- [x] 将原 WebDAV 首页入口迁移为云与同步，并保持备份页跳转可用。

## 阶段 4：验证与回写

- [x] 执行规格校验；仓库未提供 `check_large_core_files.py`，已用行数核对新增文件与改动文件。既有 `GlobalUtils.kt` 超过阈值，因此新增配置已拆入 62 行的 `CloudSyncPreferences.kt`。
- [x] 执行 `./gradlew :app:assembleRelease`，随后在最终拆分后执行 `./gradlew :app:compileReleaseKotlin`，均成功。
- [x] 回写实际实现、验证结果与提交范围。

## 阶段 1：规格冻结

- [ ] 完成 `spec.md`
- [ ] 完成 `module-plan.md`
- [ ] 完成评审或自检

## 阶段 2：基础设施

- [ ] 确认共享依赖、接口、数据结构

## 阶段 3：功能实现

- [ ] 按模块分批实现
- [ ] 大型改动完成后再统一编译

## 阶段 4：验证与回写

- [ ] 执行规格校验
- [ ] 执行大文件扫描
- [ ] 回写最终实现状态
