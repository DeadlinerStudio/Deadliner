# DeadlineDetail Polish Module Plan

## 模块拆分
- DeadlineDetailActivity：统一编辑入口、保存后刷新和快照失效。
- DeadlineDetailScreen：页面布局与任务操作派发。
- DetailTaskMetaCard：紧凑编辑入口与同一尺寸规范的状态/分类标签。
- DetailTextAction：13sp 标签、16dp 图标与 48dp 点击高度的局部入口。
- DetailBottomActions：状态主操作、专注和锚定式玻璃 Dropdown；Dropdown 退出后派发二级操作，保留原全宽高级材质。
- DetailPlanCard：子任务列表、完成区、添加草稿与编辑弹窗。

## 平台映射
本轮仅 Android ui/detail，复用 ui/editor、ui/category 和现有 KMP TaskDetailViewModel。iOS/HarmonyOS 不改动。

## 文件拆分策略
从 DeadlineDetailScreen 移除旧 PlanCard/Composer/Row，将清单展示和瞬时交互放入 DetailPlanCard.kt；业务写入仍通过回调。

## 风险点
编辑回调必须刷新并失效返回转场快照。连续添加保留焦点。完成区展开状态不因列表更新重置。

## Dropdown 复用
SoftGlassDropdownPopup / SoftGlassDropdownOptions 增加可选 itemRole，默认 RadioButton 行为保持不变；详情操作菜单使用 Button，不显示选中勾选。
