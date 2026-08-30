# Android 分类功能

## 背景

共享 KMP 持久化层已经保存任务和习惯的 `categoryUid`，且 iOS、HarmonyOS 已实现分类选择、徽标展示与主页筛选。Android 尚未消费该能力；主页左上角筛选按钮也仍为空操作。

飞书业务原语将 `category_id`、`icon_semantic_id` 与 `color_token` 定义为跨端稳定契约。Android 不持久化平台资源名或 HEX，而是在渲染层将语义键映射到 Material Symbols 与 Compose `Color`。

## 目标

- 为任务和习惯的新增、编辑表单提供分类选择，保存至共享 KMP 编辑器。
- 在主页任务和习惯项目中显示分类徽标。
- 让主页左上角按钮打开自适应分类筛选 sheet/side panel，并对当前列表的任务和习惯同步筛选。
- 覆盖 8 个预设分类、20 个颜色 token、36 个图标语义键，并为未知键提供稳定降级显示。
- 为纯映射、筛选规则和 UI 状态建立单元测试。

## 非目标

- 本轮不在 Android 实现分类新建、编辑、删除或恢复预设的管理页面。
- 不变更 KMP 数据库 schema、同步协议或 iOS/HarmonyOS 的业务语义。
- 不改变归档、搜索、Widget 或统计页的筛选范围。

## 用户场景

1. 用户新增任务或习惯时可选择“工作”等分类，保存后在主页项目上看到带颜色的图标与名称。
2. 用户编辑已有任务或习惯时可更换或清除分类，返回主页后立即反映。
3. 用户从主页左上角打开分类面板，选择一个或多个分类或“未分类”，主页中的任务和习惯同时只显示匹配项；清空选择后恢复全部内容。

## 验收标准

- 任务和习惯新增/编辑均能加载、选择、保存与清除分类。
- 主页左侧分类按钮可操作，并以符合当前主题的分类 sheet 呈现。
- “全部”、多分类与“未分类”筛选规则与 HarmonyOS `CategoryFilterResolver` 一致；引用不存在分类的历史内容按“未分类”处理。
- 分类徽标不会影响任务状态颜色和进度语义。
- 语义图标和颜色的未知值分别降级到 `tag` 和 `blue_500`。
- `./gradlew testDebugUnitTest` 与 `./gradlew :app:compileDebugKotlin` 成功，且新增单元测试通过。

## 风险与约束

- 工作区已有用户的 `habit-redesign` 未提交改动，本轮不得覆盖或重置这些变更。
- 共享 KMP API 的 category model 使用 `uid`、`iconKey`、`colorHex` 命名；Android 渲染适配器必须把它们视为业务语义键/颜色 token，并兼容既有 HEX 值。
- 页面需同时适配 Material 与 Miuix/沉浸式主题，重用现有 `TopAppBar`、`BottomSheet` 和色彩 token，而不是引入平行设计系统。

## 实现结果

- 共享 KMP 的 `Category` catalog 被适配为 Android presentation model；`iconKey` 同时兼容业务语义键与既有 HEX 色值，并为未知值提供文档规定的降级。
- 任务和习惯创建表单都可选择或清除分类；任务详情中的分类行可编辑现有任务。
- 两种主页风格均可从左侧筛选入口打开多选分类 sheet；任务、习惯以及未知/删除分类引用均以同一 resolver 投影。
- 任务与习惯行展示独立的分类徽标，不复用分类颜色作为任务状态色。
