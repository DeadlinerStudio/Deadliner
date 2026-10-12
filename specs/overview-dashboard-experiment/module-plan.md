# 总览实验版 Module Plan

## 模块拆分
- presentation：ui/overview/OverviewDashboardScreen.kt，小型根页、图表、管理入口。
- state：ui/overview/OverviewDashboardState.kt，生命周期内加载共享投影、错误重试和午夜刷新。
- navigation：OverviewActivity 保留完整统计宿主，ModernHost 接入根页；ArchiveActivity 复用。

## 平台映射
Android 首批；KMP 复用只读统计合同；iOS/HarmonyOS 未实施。

## 文件拆分策略
新增文件每个小于 1000 行，既有 ModernHost 仅替换接线。

## 风险点
专注管理等待其他功能交付；本周投影待共享层补充。旧独立入口进入根页后仍可进入完整统计。
