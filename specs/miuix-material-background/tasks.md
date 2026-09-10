# MIUIX Single Design System and Monet Background Strategy Tasks

## 阶段 1：规格冻结

- [x] 完成 `spec.md`
- [x] 完成 `module-plan.md`
- [x] 完成评审或自检（已确认主题根、偏好持久化和所有 `AppDesignSystem` 使用点）

## 阶段 2：基础设施

- [x] 引入 `BackgroundColorStrategy`，完成旧偏好一方向迁移
- [x] 固定根主题为 MIUIX，并把 Material/Monet 收敛为背景策略输入
- [x] 删除基础组件和导航中的双分支装配

## 阶段 3：功能实现

- [x] 更新设置与首次引导文案/交互
- [x] 更新迁移和主题单测
- [x] 大型改动完成后再统一编译

## 阶段 4：验证与回写

- [x] 执行规格校验
- [ ] 执行大文件扫描（项目未包含 skill 所引用的检查脚本）
- [ ] Release 签名、包校验并发送飞书（前两项完成；飞书外发被安全策略拦截，需用户再次确认）
- [x] 回写最终实现状态
