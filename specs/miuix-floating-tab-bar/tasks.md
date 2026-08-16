# MIUIX Floating Tab Bar Tasks

## 阶段 1：规格冻结

- [x] 完成 `spec.md`
- [x] 完成 `module-plan.md`
- [x] 完成需求边界与现有导航实现自检

## 阶段 2：基础设施

- [x] 确认复用 `AdvancedMaterialSpec` 与 MIUIX blur，不新增依赖
- [x] 定义独立 Tab item contract，不绑定 Navigation Controller

## 阶段 3：功能实现

- [x] 实现 MIUIX 悬浮 Tab 生产组件
- [x] 提供横向与图标在上、文字在下两种布局
- [x] 实现浅色、深色交互 Preview
- [x] 完成整批修改后统一编译

## 阶段 4：验证与回写

- [x] 执行规格校验
- [x] 执行 Kotlin 大文件扫描并记录既有告警
- [x] 检查工作区，确保不覆盖并发的 `ic_back.xml` 修改
- [x] 回写最终实现状态

## 阶段 5：Compact 接入

- [x] 增加默认关闭且可持久化的现代悬浮底栏开关
- [x] 在现代设计设置中增加开关入口与中英文说明
- [x] 在 Compact 手机宽度分支接入 `Stacked` 悬浮底栏
- [x] 保持 Medium、Expanded、Material 3 与关闭状态的旧导航回退
- [x] 统一编译、规格校验、大文件扫描并回写结果

## 阶段 6：覆盖区遮罩

- [x] 将 AI 页的 `edgeFade` 提取为共享 UI Modifier
- [x] 在底栏覆盖区增加从底部背景色到 Tab 顶边 0 alpha 的遮罩
- [x] 按系统导航栏、Tab 高度与底部间距精确计算遮罩范围
- [x] 编译并验证最终实现
