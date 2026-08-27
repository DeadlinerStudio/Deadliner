# 主清单导航重构 Tasks

## 阶段 1：规格冻结

- [x] 提交重构前完整工作区为 `907c18e`
- [x] 对照 HarmonyOS/iOS 主清单布局与滚动语义
- [x] 核对 MIUIX TopAppBar、ScrollBehavior 与 SearchBar 官方 API
- [x] 冻结生效范围、非目标、模块边界与验收标准

## 阶段 2：基础设施

- [x] 新增可测试的主清单 Chrome policy
- [x] 为 Vivid 安装不改变设计系统路由的 MIUIX Theme bridge
- [x] 新增三段式 MIUIX 主清单 TopAppBar
- [x] 复用顶部文字 Tab 并接入任务/习惯切换
- [x] 接入任务可折叠、习惯固定展开的 ScrollBehavior
- [x] 为任务 LazyColumn 新增通用首部 slot

## 阶段 3：功能实现

- [x] 新增随任务列表滚动消失的 MIUIX SearchBar 入口
- [x] 使用共享容器动画展开/关闭现有搜索结果页
- [x] 为沉浸 Dock 增加独立 action 契约
- [x] 将右侧搜索按钮替换为彩色柔光玻璃 `+`
- [x] 移除沉浸 Compact 主页重复 FAB，并保持其他布局原行为

## 阶段 4：验证与回写

- [x] 运行新增 policy/navigation JVM 测试
- [x] 运行完整 Debug JVM 测试与 Kotlin 编译
- [x] 运行规格校验与 `git diff --check`
- [x] 检查所有修改 Kotlin 文件行数及 1000 行门禁
- [x] 回写实现、验证和已知限制

## 阶段 5：导航图标与柔影修正

- [x] 固定常规 Material3/Vivid 导航图标测量框，阻断 SVG intrinsic size 撑高栏体
- [x] 归一 Home、Capture、AI、Search 的 Vector 有效图形边界
- [x] 为沉浸导航契约增加独立 visual scale，统一五个图标视觉重量
- [x] 移除包住主胶囊阴影的拖动 GraphicsLayer，改用无裁切的 layout offset
- [x] 扩散并弱化近缘阴影，更新阴影参数回归测试

## 阶段 6：顶栏滚动与有色材质修订

- [x] 冻结 24dp 渐进材质尾部裁切、Detail FAB 回退与彩色玻璃验收规则
- [x] 把搜索入口从任务 LazyColumn 迁入 MIUIX TopAppBar bottomContent
- [x] 实现搜索先折叠、标题后折叠的 nested scroll 协调器
- [x] 将搜索源与结果目标提升到同一共享转场作用域
- [x] 为柔光玻璃增加显式色彩保真 tint 策略并接入 MainActivity 彩色加号
- [x] Detail 页恢复统一 Material3 FAB
- [x] 增加 24dp 渐进尾部裁切 token 和高度回归测试
- [x] 运行完整 Debug JVM 测试、规格校验、diff check 与 1000 行门禁

## 阶段 7：交互回归修正

- [x] 冻结搜索间距、图标基准、顶栏按钮阴影、搜索页复用和刷新手势优先级
- [x] 搜索入口改为水平 8dp、垂直 0dp，并同步搜索槽高度
- [x] 清单与概览统一视觉尺寸，放大 AI/Lifi，保持 Capture 不变
- [x] 为 TopAppBar 圆形柔光玻璃按钮接入紧凑阴影 token
- [x] 展开搜索复用原 Search Tab 的 MainSearchBar 页面
- [x] 将 Chrome nested scroll 下沉到 LazyColumn，并让下拉先展开标题与搜索再触发刷新
- [x] 运行完整 Debug JVM 测试、规格校验、diff check 与 1000 行门禁

## 阶段 8：MIUIX 预设色与主页 FAB 契约

- [x] 冻结亮暗 MIUIX 主色、Material 派生强调色与 FAB seed 规则
- [x] 用 Material Kolor 从 MIUIX 主色生成 preset secondary/tertiary
- [x] 统一解析 Preset、SeedColor、SystemDynamic 的主页 FAB container
- [x] 让常规主页 FAB 与沉浸底栏加号共享 container，并固定白色 plus 前景
- [x] 更新颜色来源矩阵、预设色与深色控件前景回归测试
- [x] 运行完整 Debug JVM 测试、规格校验、diff check 与 1000 行门禁

## 阶段 9：Preset role 分层修正

- [x] 冻结“仅 primary/onPrimary 固定、其余 Material role 全自动生成”的严格契约
- [x] 保持 preset factory 的 container role 不被覆写
- [x] 删除已废弃 Toggle 颜色 helper 的单测引用
- [x] 增加完整 ColorScheme role 等价与白色 onPrimary 契约测试
- [x] 运行完整 Debug JVM 测试、规格校验、diff check 与 1000 行门禁

## 阶段 10：沉浸搜索系统返回稳定性

- [x] 根据崩溃堆栈定位共享边界反向重测的负尺寸异常
- [x] 将 SearchBar 共享边界切换为不改变子布局约束的 scale 模式
- [x] 运行完整 Debug JVM 测试和 Kotlin 编译

## 阶段 11：Preset role 层级与 seed 玻璃保真

- [x] 冻结“仅 primary/onPrimary 固定、Detail 使用 container role”与浅色 seed 玻璃验收规则
- [x] 切换 preset 到 Material Kolor 2021 TonalSpot，仅覆盖 primary 与白色 onPrimary
- [x] 显式固定 Detail FAB 的 primaryContainer/onPrimaryContainer 语义
- [x] 新增 `SeedChromatic` 玻璃 profile 并用于沉浸底栏 FAB
- [x] 增加 preset role 差异与 seed tint 保真回归测试
- [x] 运行完整 Debug JVM 测试、Kotlin 编译、规格校验、diff check 与 1000 行门禁

## 阶段 12：顶栏材质节奏与沉浸操作精修

- [x] 冻结更强 seed 覆盖、专用 plus 图标、Tab 材质降级与搜索转场契约
- [x] 提高浅色 `SeedChromatic` 覆盖并更新数值回归
- [x] 新增 `ic_plus_imm` 并仅替换沉浸底栏新增动作
- [x] 提前顶栏材质显现阈值，让顶部 Tab 与按钮共享进度
- [x] 为 MIUIX 顶部 Tab 增加半透明 container 降级态
- [x] 缩小并减弱搜索入口 hint，把共享转场改为顶对齐 FillWidth
- [x] 运行针对性与完整 Debug JVM 测试、Kotlin 编译、规格与文件门禁

## 阶段 13：主页撞色、浮层容器与普通导航图标

- [x] 冻结 75% seed 有效覆盖、撞色主页按钮、surfaceContainer 浮层与普通图标规则
- [x] 持久化并条件展示“撞色主页按钮”，调整沉浸体验设置排序
- [x] 将现代预设主页 FAB 默认改为 `#FFFF6D6D`，关闭撞色时回退 preset primary
- [x] 把亮暗 `SeedChromatic` 默认 recipe 的最终 seed 覆盖统一到约 75%
- [x] 统一 Dialog/Sheet 的 `surfaceContainer` 浮层容器色策略
- [x] 恢复非沉浸灵感 `ic_draw_*`，仅放大普通 AI/Lifi 图标
- [x] 更新回归测试并运行完整 Debug JVM 测试、规格、diff check 与 1000 行门禁

## 阶段 14：沉浸 Home 与 Capture 图标校准

- [x] 核对 MIUIX Icons 官方 `Home.Light` 路径、viewport 与许可证
- [x] 将沉浸主页资源替换为 `Home.Light` 并固定 visual scale 1.0
- [x] 为沉浸灵感路径增加约 0.5dp 同色圆角描边
- [x] 运行完整 Debug JVM 测试、规格校验与 diff check

## 阶段 15：统一柔影与搜索页动作

- [x] 冻结统一中等 shadow token、自动 IME 聚焦和归档入口契约
- [x] 移除按钮/Tab shadow style 分叉并接入统一 token
- [x] 在搜索页 expanded 后请求焦点并显示 IME
- [x] 在搜索页右上角增加归档入口并复用 `ArchiveActivity`
- [x] 更新回归测试并运行完整 Debug JVM 测试、规格、diff check 与文件门禁
