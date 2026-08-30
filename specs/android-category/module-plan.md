# Android 分类功能 Module Plan

## 模块拆分

1. `ui/category/contract`：分类展示模型、颜色/图标解析、筛选状态和纯筛选 resolver；不依赖 Compose 页面状态。
2. `ui/category/components`：徽标、单选选择器、主页多选筛选面板与自适应容器。
3. `ui/editor`：把选中的 `categoryUid` 加入任务/习惯 draft，在保存时调用共享 editor 的分类更新接口。
4. `ui/main`：读取 KMP category catalog，投影主页任务/习惯、应用 filter，并将顶部按钮连接到筛选面板。

## 平台映射

| 能力 | iOS | HarmonyOS | Android |
| --- | --- | --- | --- |
| 业务筛选 | `CategoryFilterSheet` | `common/category/CategoryFilter` | `ui/category/CategoryFilter` |
| 表单赋值 | `CategoryPickerSheet` | `CategoryAssignmentField` | `CategoryAssignmentField` |
| 主页入口 | Home toolbar | `CategoryFilterSideBar` | `ModernListTopAppBar` / `MainSearchBar` + category sheet |
| 徽标渲染 | `CategoryBadgeView` | 任务/习惯 row badge | `CategoryBadge` |

## 文件拆分策略

- 图标/颜色映射放在独立 registry，避免塞进主页或编辑器文件。
- 选择器与筛选面板分开：前者负责单选和赋值，后者负责多选主页筛选。
- 现有核心 UI 文件接近复杂度上限时仅增加薄接线；新 UI 放在 `ui/category`。
- 不允许单个 Kotlin 核心文件超过 1000 有效行。

## 风险点

- KMP category catalog 的 model 属于依赖模块；已在首次编译中确认 `TaskEditorViewModel.updateCategory` 与 `HabitEditorViewModel.updateCategory` 可用。
- 主页现有 task/habit list 是不同的投影；筛选必须在两者的可见集合上以同一 resolver 运行。
- 现有工作区改动覆盖主页和 AddDDLActivity，合并时以最小上下文 patch 保留其未提交工作。
