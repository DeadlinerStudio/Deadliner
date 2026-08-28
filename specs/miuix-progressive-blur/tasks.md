# Miuix Progressive Blur Migration Tasks

## 阶段 1：规格冻结

- [x] 完成 `spec.md`
- [x] 完成 `module-plan.md`
- [x] 完成 API 与现有顶栏路径自检

## 阶段 2：基础设施

- [x] 确认共享依赖、接口、数据结构

## 阶段 3：功能实现

- [x] 升级 Miuix artifact、兼容工具链并替换顶栏手写渐进模糊
- [x] 大型改动完成后统一编译

## 阶段 4：验证与回写

- [x] 执行规格校验
- [x] 检查修改的核心文件均低于 1000 行；项目未提供守卫脚本，已执行全量 debug 单测与 debug/release APK 构建
- [x] 回写最终实现状态
