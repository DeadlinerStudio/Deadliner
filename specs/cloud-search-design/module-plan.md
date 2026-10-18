# 模块计划

## 模块拆分
SwipeableItemCard 管理统一图标 tint，背景颜色保持原语义；ModernListTopBarScrollCoordinator 控制搜索展开来源；CloudSyncSettings 与 CloudSyncOptions 展示集中设置；CloudSyncPreferences 保留既有互斥提供方，运行层沿用现有同步调度；BYOS/WebDAV 子页仅配置连接。

## 平台映射
Android Compose UI 与 Android-hosted 配置/同步协调；KMP 公共协议和数据库不变。

## 文件拆分策略
云同步页面与公共选项分文件，移除旧 SyncIntervalBottomSheet，不向大设置文件继续添加逻辑。

## 风险点
列表边界的 nested scroll 来源、配置保存意外切换模式、WorkManager 重新调度。
