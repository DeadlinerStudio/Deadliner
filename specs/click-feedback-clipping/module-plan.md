# 点击反馈裁切 Module Plan

## 模块拆分
1. Lifi 与各页面：点击前裁切同形状，复用已有 radius/shape。
2. 共用表单、玻璃及 Material 适配：审计 modifier 顺序和默认形状。
3. 审计记录与主机验证：列出已修复/已有外层或父级裁切/无反馈等入口。

## 平台映射
Android Compose UI，排除 Glance 宿主与系统窗口。

## 文件拆分策略
小范围 modifier 修改，审计文档独立保存，核心文件不超过 1000 有效行。

## 风险点
错误的 clip 顺序依然会泄漏；固定 CircleShape 不一定与 Material Chip/FAB 默认外观一致。
