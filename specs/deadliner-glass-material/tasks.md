# Deadliner Extensible Glass Material Tasks

## 阶段 1：规格冻结

- [x] 完成 `spec.md`
- [x] 完成 `module-plan.md`
- [x] 完成现有 MIUIX Blur 公共 API 与 API 等级自检

## 阶段 2：基础设施

- [x] 定义可扩展 Glass recipe 与默认导航材质
- [x] 实现共享高斯模糊和无 Backdrop 降级渲染
- [x] 实现 API 33+ 可配置圆角边缘折射 shader

## 阶段 3：功能实现

- [x] 添加独立浅色/深色 Glass Preview
- [x] 不改变尺寸地接入 `MiuixFloatingTabBar`
- [x] 完成整批材质改动后统一编译

## 阶段 4：验证与回写

- [x] 执行规格校验
- [x] 执行大文件扫描
- [x] 回写最终实现状态

## 阶段 5：折射与模糊夹层

- [x] 增强默认导航折射的边缘带宽、位移和可见度
- [x] 增加可配置的折射后微模糊与顶部完整形状柔化层
- [x] 保持内容层 alpha、底栏尺寸和交互不变
- [x] 更新 Preview 并统一编译验证

## 阶段 6：AndroidLiquidGlass 光学链重构

- [x] 对照公开 Lens shader 与 LiquidBottomTabs 效果顺序
- [x] 将 API 33+ 改为统一 Gaussian blur → circular lens → softening 链
- [x] 使用解析胶囊法线、全轮廓边缘光与可扩展色散参数
- [x] 更新 Preview、规格并完成 Kotlin 编译

## 阶段 7：折射清晰度与 MIUIX 半径映射

- [x] 将悬浮导航高斯半径映射为 MIUIX 当前连续值的 `0.4×`
- [x] 增强默认透镜高度、位移和 depth effect，并减少折射后柔化
- [x] 更新强折射 Preview 参数
- [x] 完成 Kotlin 编译、规格校验与尺寸回归检查

## 阶段 8：固化导出配方

- [x] 将用户预设四舍五入为稳定默认参数
- [x] 同步静态 recipe、动态 recipe 与恢复默认行为
- [x] 补充 recipe 映射测试并完成编译验证
