# 模块拆分计划

## 模块拆分
ui/category 新增可复用 CategoryDropdownMenu 与表单字段，注入分类列表与回调，复用 SoftGlassDropdownPopup。
ui/editor 替换分类字段并移除旧 Sheet 状态/弹出逻辑。
ui/quickadd 两种分类锚点使用同一菜单，保留删除分类与禁用状态。

## 平台映射
仅 Android 展示层变更，不修改 KMP 持久化和其他平台。

## 文件拆分策略
新增独立文件，不扩张大型组件文件，每个修改文件保持低于 1000 有效行。

## 风险点
类别名可能重复，选择必须用 UID。已选胶囊在清空后会消失，但菜单宿主必须持续保留至退场结束。管理分类回调要在组合应用并卸载 Dialog 后执行，避免窗口重叠；键盘和焦点在展开时收起。

## 分类视觉展示
现有 Dropdown 增加可选 leading 与文字颜色扩展，默认空值保持设置菜单布局。ui/category 统一图标装饰，使用 CategoryPresentationRegistry 保持图标包映射和分类色一致。QuickAddCategoryChip 独立承载图标、名称、分类底色和移除，不改变其他属性胶囊。
