# Android UI 优化 Module Plan

## 模块拆分
- ui/material/glass：采样空间与折射边界。
- ui/MaterialSymbolStyle：330 字重统一入口。
- model/intro/main：退出简洁主页、兼容旧偏好。
- ui/settings：Dropdown 与 MIUIX 路由。
- ui/detail：来源几何与详情展开动画。

## 平台映射
仅 Android app/src/main；其他平台保持现状。

## 文件拆分策略
新增导航/详情动画独立文件，不扩大已有超大文件。

## 风险点
导航状态恢复、返回优先级、卡片窗口坐标与全屏坐标换算；批量实现后统一编译。
