# 点击反馈裁切审计记录

覆盖 Android Compose 95 处 Foundation 点击/选择/长按入口和 103 处 Material 回调入口。

MIUIX 原生 Button/Card/IconButton 的内部点击位于 squircleSurface 中；只修复应用侧传入 modifier 更外侧的反馈。排除 Glance 桌面宿主、系统窗口和没有按压层的遮罩。

| 文件（包内路径） | Foundation 入口数 | 结论 |
| --- | ---: | --- |
| SettingsActivity.kt | 1 | 已核查：矩形选择列表、文字操作或透明遮罩；无独立圆角底色 |
| calendar/CalendarImportDialogs.kt | 2 | 已核查：矩形选择列表、文字操作或透明遮罩；无独立圆角底色 |
| capture/ui/InspirationLightCard.kt | 1 | 修复：灵感卡自身 24dp 裁切（父 SwipeableItemCard 也有裁切） |
| ui/agent/AIOverlay.kt | 1 | 无默认按压层：遮罩/自有选择高亮/玻璃宿主反馈，保持原逻辑 |
| ui/agent/AIOverlayPresentation.kt | 1 | 修复：快捷建议 18dp 裁切 |
| ui/archive/ArchiveComponents.kt | 1 | 修复：圆形快捷按钮裁切 |
| ui/base/SoftGlassDropdownOptions.kt | 1 | 无默认按压层：遮罩/自有选择高亮/玻璃宿主反馈，保持原逻辑 |
| ui/category/CategoryComponents.kt | 3 | 修复：分类字段共享表单 squircle 裁切；分类列表行父级设置容器裁切 |
| ui/category/CategoryEditorScreen.kt | 2 | 已有相同 shape 的 clip 或外层形状容器，反馈位于裁切内部 |
| ui/category/CategoryManagementScreen.kt | 2 | 矩形列表行，外轮廓由父 Card/SettingsSection 裁切；内部行不额外圆角 |
| ui/detail/DeadlineDetailScreen.kt | 1 | 父级 MIUIX Card/Material Surface/SwipeableItemCard/SoftGlassSurface 已裁切内容 |
| ui/editor/DeadlineEditorComponents.kt | 2 | 修复：表单共享 squircleSurface 透明裁切；提醒行位于父级卡片内 |
| ui/focus/FocusGlobalBar.kt | 1 | 父级 MIUIX Card/Material Surface/SwipeableItemCard/SoftGlassSurface 已裁切内容 |
| ui/focus/FocusManagementTargetPicker.kt | 2 | 修复：任务目标复用 MIUIX squircle 掩膜；纯文本标签保持矩形点击区 |
| ui/focus/FocusRecentSessions.kt | 1 | 父级 MIUIX Card/Material Surface/SwipeableItemCard/SoftGlassSurface 已裁切内容 |
| ui/focus/FocusTimerControls.kt | 1 | 已有相同 shape 的 clip 或外层形状容器，反馈位于裁切内部 |
| ui/focus/FocusTimerSetupSheet.kt | 2 | 父级 MIUIX Card/Material Surface/SwipeableItemCard/SoftGlassSurface 已裁切内容 |
| ui/intro/ClassicWizard.kt | 1 | 已有相同 shape 的 clip 或外层形状容器，反馈位于裁切内部 |
| ui/lifi/LifiAIPage.kt | 1 | 修复：记忆按钮使用独立已裁切组件；侧栏历史列表为矩形 |
| ui/lifi/LifiComposer.kt | 1 | 父级 MIUIX Card/Material Surface/SwipeableItemCard/SoftGlassSurface 已裁切内容 |
| ui/lifi/LifiConversation.kt | 1 | 修复：过程卡片 20dp 裁切 |
| ui/lifi/LifiHistorySheet.kt | 1 | 修复：历史卡片父级 22dp 裁切 |
| ui/lifi/LifiWelcome.kt | 3 | 修复：22dp 轮播/16dp 记忆按钮/圆形页码点击前裁切 |
| ui/main/Components.kt | 3 | 修复：任务卡片外层点击 clip；其余卡片原有同形状 clip |
| ui/main/SwipeableTaskCard.kt | 1 | 父级 MIUIX Card/Material Surface/SwipeableItemCard/SoftGlassSurface 已裁切内容 |
| ui/main/modern/ModernHost.kt | 2 | 无默认按压层：遮罩/自有选择高亮/玻璃宿主反馈，保持原逻辑 |
| ui/main/shared/MainDisplay.kt | 1 | 无默认按压层：遮罩/自有选择高亮/玻璃宿主反馈，保持原逻辑 |
| ui/main/shared/MainSearchResultsContent.kt | 2 | 修复：新建习惯/习惯结果卡片均使用 24dp 外层裁切 |
| ui/main/simplified/HabitComponents.kt | 3 | 修复：日期点击先 clip 再 clickable；习惯卡片原有同形状 clip |
| ui/main/simplified/MorePanelCard.kt | 1 | 矩形列表行，外轮廓由父 Card/SettingsSection 裁切；内部行不额外圆角 |
| ui/main/simplified/ProfileEditor.kt | 1 | 已有相同 shape 的 clip 或外层形状容器，反馈位于裁切内部 |
| ui/material/glass/SoftGlassSurface.kt | 1 | 无默认按压层：遮罩/自有选择高亮/玻璃宿主反馈，保持原逻辑 |
| ui/navigation/ImmersiveTextTabRow.kt | 1 | 已有相同 shape 的 clip 或外层形状容器，反馈位于裁切内部 |
| ui/navigation/MiuixFloatingTabBar.kt | 1 | 已有相同 shape 的 clip 或外层形状容器，反馈位于裁切内部 |
| ui/navigation/TabletNavSideBar.kt | 3 | 已有相同 shape 的 clip 或外层形状容器，反馈位于裁切内部 |
| ui/overview/FocusManagementCards.kt | 3 | 父级 MIUIX Card/Material Surface/SwipeableItemCard/SoftGlassSurface 已裁切内容 |
| ui/overview/FocusManagementScreen.kt | 1 | 修复：悬浮玻璃快捷卡外层 clip 与玻璃 shape 相同 |
| ui/overview/OverviewMonthCalendar.kt | 1 | 已有相同 shape 的 clip 或外层形状容器，反馈位于裁切内部 |
| ui/quickadd/QuickAddModelDownloadStatus.kt | 1 | 已核查：矩形选择列表、文字操作或透明遮罩；无独立圆角底色 |
| ui/quickadd/QuickAddOverlay.kt | 4 | 无默认按压层：遮罩/自有选择高亮/玻璃宿主反馈，保持原逻辑；修复：属性按钮 16dp；图标 CircleShape；空白关闭遮罩无反馈 |
| ui/quickadd/QuickAddProperties.kt | 5 | 已有相同 shape 的 clip 或外层形状容器，反馈位于裁切内部 |
| ui/settings/AboutSettings.kt | 3 | 版本小标签胶囊裁切；普通设置行使用父级 SettingsSection 裁切 |
| ui/settings/AppIconSettings.kt | 1 | 矩形列表行，外轮廓由父 Card/SettingsSection 裁切；内部行不额外圆角 |
| ui/settings/AppearanceSettings.kt | 3 | 矩形列表行，外轮廓由父 Card/SettingsSection 裁切；内部行不额外圆角 |
| ui/settings/BehaviorSettings.kt | 1 | 矩形列表行，外轮廓由父 Card/SettingsSection 裁切；内部行不额外圆角 |
| ui/settings/CloudSyncSettings.kt | 2 | 矩形列表行，外轮廓由父 Card/SettingsSection 裁切；内部行不额外圆角 |
| ui/settings/DesignColorSettings.kt | 3 | 修复：16dp/20dp 风格和配色预览卡片外层裁切 |
| ui/settings/MainSettings.kt | 3 | 修复：推荐卡片 28dp；普通设置行由 SettingsSection 统一裁切 |
| ui/settings/SettingsComponents.kt | 7 | 矩形列表行，外轮廓由父 Card/SettingsSection 裁切；内部行不额外圆角；无默认按压层：遮罩/自有选择高亮/玻璃宿主反馈，保持原逻辑 |
| ui/settings/SettingsRows.kt | 2 | 矩形列表行，外轮廓由父 Card/SettingsSection 裁切；内部行不额外圆角 |
| ui/settings/ThemeColorPicker.kt | 1 | 已有相同 shape 的 clip 或外层形状容器，反馈位于裁切内部 |
| widgets/glance/configuration/TaskWidgetConfigurationScreen.kt | 1 | 无默认按压层：遮罩/自有选择高亮/玻璃宿主反馈，保持原逻辑 |

Material 静态形状：对应组件 Defaults 或调用方显式 shape；动态连体形状共 5 处（习惯编辑 4 处、搜索范围 1 处）保持原生表面与内部反馈。逐入口位置见 audit.json。

共享表单保留原 squircleBackground 颜色路径，在外侧增加透明 squircleSurface 掩膜，避免改变填充策略。
