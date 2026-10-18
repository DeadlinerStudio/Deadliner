# DeadlineDetail Polish Tasks

## 阶段 1：规格冻结
- [x] 用户审核交互原型，记录范围与约束。

## 阶段 2：基础设施
- [x] 确认统一任务编辑入口与分类标签可复用。
- [x] 确定详情页和计划组件边界。

## 阶段 3：功能实现
- [x] 接入编辑弹层，整合分类展示。
- [x] 实现紧凑清单、折叠完成区、连续添加和子任务编辑弹窗。

## 阶段 4：验证与回写
- [x] 完成实现批次后统一本地编译（compileDebugKotlin）。
- [x] 校验规格、文件大小、diff 与中英文 XML 资源。
- [x] 记录验证结果与限制；不操作物理设备。

## 验证结果
- `:app:compileDebugKotlin` 通过；现有编辑器主机测试 12 项通过，无失败。
- Core AAR 已由构建同步并校验；最终编译/测试使用 `-x :app:syncDeadlinerCoreKmp --offline` 复用该产物。
- 规格校验、改动范围 diff 空白检查、中英文资源解析通过。
- 仓库未提供 `scripts/check_large_core_files.py` / `core_suffixes.txt`，采用等价有效代码行扫描：详情页 569 行，计划组件 262 行，均低于 1000 行。
- 未运行设备测试、安装、截屏；真实渲染与键盘布局尚未在设备/模拟器上验证。
- 全工作区 diff 检查发现 GlassMaterialModifier.kt 原有 EOF 空行，本轮未改动该文件。

## 截图反馈修正验证
- 编辑入口改用与分类管理一致的 msr_edit 图标，标题行上方留白减小，编辑/添加入口字号改为 13sp。
- 状态、分类和未分类共用 DetailBadge 尺寸规范。
- 底栏改为主操作、专注、更多；原有次要操作移入更多面板，并在面板关闭后派发。
- 添加展开后使用同排输入框、确认图标和关闭图标；支持连续添加与空白禁用。
- `:app:testDebugUnitTest --tests com.aritxonly.deadliner.ui.detail.DeadlineDetailLayoutTest -x :app:syncDeadlinerCoreKmp --offline` 通过，3 项布局/交互测试无失败；同时通过 Kotlin 编译。
- 本地主机原生渲染已检查：`app/build/outputs/detail-review/detail.png`、`meta.png`、`composer.png`；验证紧凑编辑位置、标签等高、原型底栏和同排输入操作。
- 未操作任何设备；设备系统字体、动态色及实际 IME 仍需在允许的测试环境中验证。

## 布局与材质补充反馈验证
- 基础信息恢复原有 Card / 字体 / 18dp 内边距 / 分隔线，保留紧凑编辑入口和等高胶囊标签。
- 详细计划标题与完整备注统一使用 Material titleMedium / onSurface。
- 底栏恢复原 advancedTextureBlur、backdrop、tint 和不透明回退，保持新版按钮布局。
- 更多改用现有 SoftGlassDropdownPopup，操作语义为 Button；退出后派发确认弹窗等后续操作。
- Kotlin 编译、详情布局/交互测试及 GlassDropdown / CategoryDropdown 回归通过，共 26 项。
- 已检查本地页面渲染 detail.png 和 dropdown.png；未操作物理设备，真实材质采样和键盘行为仍未在设备上验证。
