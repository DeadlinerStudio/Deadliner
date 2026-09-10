# Android 分类管理与小组件分类色 Tasks

## 阶段 1：规格冻结

- [x] 明确分类新增/编辑字段、入口和非目标。
- [x] 明确小组件分类色覆盖与语义色回退规则。
- [x] 确认复用 KMP `CategoryMutation` / `CategoryRepository`，无需修改 KMP 数据协议。
- [x] 冻结分类软删除、页面/Sheet 动画、深浅色与徽标层级优化范围。

## 阶段 2：基础设施

- [x] 扩展分类展示注册表的颜色、图标选项与 ARGB 解析。
- [x] 实现分类管理控制器及纯逻辑测试。
- [x] 给现有页面建立可重载的分类状态。
- [x] 扩展分类管理控制器的软删除编排与单元测试。

## 阶段 3：功能实现

- [x] 实现 MIUIX 风格分类管理列表与新增/编辑表单。
- [x] 接入分类选择、筛选与 Android Manifest。
- [x] 扩展小组件数据模型、数据源与竖线渲染。
- [x] 分类保存后立即触发数据刷新与小组件刷新。
- [x] 在编辑页加入删除确认，并为管理页/编辑页切换补齐动画。
- [x] 让分类 Sheet 完成退出动画后再移除，修正“未分类”语义色与重复文字。
- [x] 放大“全部分类”中的徽标文字、图标和内边距。
- [x] 移除管理页顶栏新增按钮，并统一 Sheet/管理页分类标签实现。
- [x] 去掉名称输入框外层 Section，补齐分类编辑页渐进顶栏与底栏高级材质。
- [x] 将分类编辑页切换为任务编辑器同款透明 Backdrop 页面壳，并让删除动作使用柔光玻璃按钮。
- [x] 为主页任务卡片接入独立紧凑分类标签规格。
- [x] 将主页紧凑标签字号降至备注字号以下，并消除徽标垂直内边距导致的行高差异。
- [x] 修复透明页面壳的 Backdrop 录制底色，让渐进顶栏、固定底栏和柔光按钮采样到页面内容。
- [x] 将分类编辑迁移为稳定的独立 Activity 材质宿主，并保留进入/返回过渡。
- [x] 抽取 MIUIX 风格分类删除确认弹窗，在管理列表每项增加独立删除按钮。
- [x] 按设置页根层级撤回独立编辑 Activity，改为稳定 Surface 外包裹的同 Activity `NavHost`。
- [x] 编辑页改用设置页同款 `CollapsingTopBarScaffold`，恢复分类列表原始视觉行高。
- [x] 根据 RenderThread 栈溢出日志撤回根层 Backdrop，恢复内容层录制与兄弟节点消费。
- [x] 将分类删除弹窗动作按钮尺寸与设置页 MIUIX 弹窗统一。
- [x] 为短分类编辑页补足顶栏高度的滚动余量，使内容进入柔光按钮与渐进顶栏的 Backdrop 采样区。
- [x] 将分类删除确认按钮改为 `errorContainer/onErrorContainer`，同时保留 MIUIX 等宽填充尺寸。

## 阶段 4：验证与回写

- [x] 补齐分类 mutation 与小组件颜色回退测试。
- [x] 执行规格校验与大文件扫描。
- [x] 执行全量单元测试、Debug、Release/R8、lintVitalRelease。
- [x] 正式签名 APK，校验证书摘要并发送飞书。
- [x] 按最终实现回写规格和任务状态。
- [x] 重新执行规格校验、大文件扫描、分类单测和 Debug Kotlin 编译并回写结果。
- [x] 重新执行静态检查、规格校验、分类单测和 Debug APK 构建并回写结果。
- [x] 执行分类回归测试、Release/R8 构建与正式签名校验。
- [x] 将最终签名 APK 发送到飞书并记录消息 ID。
- [x] 重新执行分类测试、Release/R8、正式签名校验和飞书交付。
- [x] 再次执行分类/材质测试、Release/R8、正式签名校验和飞书交付。
- [x] 验证设置页同款分类二级页宿主、列表行高与 Debug 构建。
- [x] 验证内容层 Backdrop、弹窗按钮、全量测试、Release/R8、正式签名与飞书交付。

### 2026-09-03 验证记录

- `validate_feature_spec.py specs/category-management-widget-colors` 通过。
- 12 个 `ui.category` 单元测试通过；`assembleDebug` 通过并包含 Debug Kotlin 编译。
- 技能引用的 `scripts/check_large_core_files.py` 仍不在当前工作区；已用 Kotlin 文件行数扫描替代。本轮分类组件最大 335 行，活动页 125 行，均低于 1000 行；未扩大既有超限文件的职责。
- Debug APK 已安装到在线设备；设备处于安全锁屏，真机动画、深浅色和触控验证仍待人工解锁后确认。
- 管理页顶栏新增按钮已移除；Sheet 与管理页现共用 `CategoryBadge`，统一为 12dp 圆角、正文级小号文字和 16dp 图标。
- 分类名称输入框已脱离 Section；编辑页改为在顶栏和底栏后方滚动的 LazyColumn，并把滚动状态接入现有渐进材质链路。
- 12 个 `ui.category` 单元测试再次零失败，`assembleDebug`、`git diff --check` 和规格校验通过。
- 分类编辑页已从通用设置页壳切换到与添加任务页相同的 `AdaptiveMaterialScaffold` 透明 Backdrop；顶栏、固定底栏共享 Backdrop，删除动作通过 `TopBarIconButton` 获得滚动驱动的柔光玻璃容器。
- 主页任务卡片单独启用紧凑分类标签：Miuix 使用 `footnote1`、Material 3 使用 `labelSmall`，图标缩为 12dp；Sheet、管理页和习惯卡片继续使用默认规格。
- 12 个分类单元测试零失败；`assembleRelease`、Release R8、`lintVitalRelease`、规格校验和 `git diff --check` 通过。
- 签名 APK：`Deadliner-v5.0.0-category-material-badge-fix-signed.apk`；APK SHA-256 为 `7077a093822151324d51b27db5867b3f330dc5309efa16fb25ef92901ef958b6`，APK Signature Scheme v3，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`。
- ADB 当前无连接设备，渐进模糊与柔光玻璃的本轮真机视觉复核未执行。
- 飞书交付成功：机器人发送至已验证本人账号 `Aritx Zhou`，消息 ID `om_x100b66bb16513d00b488f7e3ddd0cbc`。
- 主页任务卡片紧凑标签最终使用 Material `labelSmall`、12dp 图标与 0dp 垂直内边距，文字不大于备注所用 `bodySmall`，避免标签撑高第二行。
- 依据 Miuix `0.9.4-rc01` Backdrop 契约，透明 `AdaptiveMaterialScaffold` 在录制回调中先绘制当前设计系统的不透明 Surface，再绘制页面内容；渐进顶栏、固定底栏和柔光按钮继续共享该 Backdrop。
- 新增 3 个 Backdrop 录制底色选择测试；最终 178 个 Debug 单元测试零失败，Release Kotlin、R8、`lintVitalRelease`、规格校验和 `git diff --check` 通过。
- 最终签名 APK：`Deadliner-v5.0.0-category-badge-backdrop-fix-signed.apk`；APK SHA-256 为 `61da5bd1c165bc70505f0f29dfbb656b9aac1c4c765de9a6dd992435149ef363`，APK Signature Scheme v3，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，16 KiB 对齐校验通过。
- 最终包已由机器人发送至本人账号 `Aritx Zhou`，消息 ID `om_x100b66bbf9a094acb22af7baa6831df`。
- 曾尝试把分类编辑迁移到独立 `CategoryEditorActivity`，但真机反馈高级材质仍未恢复；该中间方案已撤回，最终改为复用设置页同款根 Surface、`NavHost` 与页面 Scaffold 层级。
- 管理列表每一项已增加独立删除按钮；列表与编辑页统一复用归档页同款 MIUIX 删除确认结构（标题、摘要、等宽操作区和 Miuix 文本按钮）。
- 本轮 178 个 Debug 单元测试零失败；Release Kotlin、R8、`lintVitalRelease`、规格校验和 `git diff --check` 通过。
- 本轮签名 APK：`Deadliner-v5.0.0-category-editor-glass-delete-fix-signed.apk`；APK SHA-256 为 `1bd17a700a935314b8964f32e3b488594ca8f9caab3dc1c63afed81d11481e8d`，APK Signature Scheme v3，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，16 KiB 对齐校验通过。
- 本轮签名包已由机器人发送至本人账号 `Aritx Zhou`，消息 ID `om_x100b66a47cdfd0a8ddb00a5d677a08c`。
- 根据真机反馈撤回独立编辑 Activity：分类管理 Activity 现与设置页一致地继承 `DeadlinerAppCompatActivity`，并在根部持有页面级模糊宿主、不透明 Surface 和 `NavHost`；编辑页改用 `CollapsingTopBarScaffold`，不再维护自定义透明页面壳。
- 分类管理列表仍保留 48dp 删除点击目标，同时将行上下留白从 14dp 收回到 6dp，恢复新增删除按钮前约 60dp 的视觉行高。
- 本轮 178 个 Debug 单元测试零失败，`assembleDebug`、规格校验和 `git diff --check` 通过；ADB 无在线设备，高级材质真机视觉仍需安装后确认。
- 设置页同款宿主修订的 Release/R8 与 `lintVitalRelease` 已通过；签名包 `Deadliner-v5.0.0-category-settings-host-row-height-fix-signed.apk` 使用 APK Signature Scheme v3，SHA-256 为 `b4e5520a9b6e96aa92fdaf6c9959a8b9e6fd02685629d31ed1d5ed16d81f8dac`，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，16 KiB 对齐校验通过。
- 最新签名包已由机器人发送至本人账号 `Aritx Zhou`，消息 ID `om_x100b66a57ec348a0b4b2865e5d02fb5`。
- 分类 Activity 现于 `Surface/NavHost` 外层录制唯一根 `LayerBackdrop`，管理页和编辑页的 `CollapsingTopBarScaffold` 显式消费它而不再在导航转场内部嵌套录制；根 backdrop 同时注册给页面级弹窗宿主。
- 分类删除确认已移除较小的 `MiuixTextButton` 分支，管理页与编辑页统一使用设置页同款等宽填充动作按钮。
- 根 Backdrop 相关 6 项基础测试与 12 项分类测试零失败；最终全量 181 项 Debug 单元测试零失败，Release Kotlin、R8、`lintVitalRelease`、规格校验和 `git diff --check` 通过。
- 最终签名 APK：`Deadliner-v5.0.0-category-root-backdrop-dialog-fix-signed.apk`；APK SHA-256 为 `dae36a898f72b4d898bdd05999b29f5b94431f66726bab997f68cb823a272116`，APK Signature Scheme v3，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，16 KiB 对齐校验通过。
- 最终签名包已由机器人发送至本人账号 `Aritx Zhou`，消息 ID `om_x100b66a53367b904de3fc379b58ae78`。
- 真机 `logcat_2026-09-03_15-07-57.txt` 显示 RenderThread `SIGSEGV`、512 层 `RenderNode::prepareTreeImpl`/`SkiaDisplayList::prepareListAndChildren` 重复调用，直接原因是根 `layerBackdrop` 包含了引用同一 Backdrop 的顶栏消费者。
- 已撤回 Activity 根录制与跨页面共享 Backdrop 参数；页面恢复在 Scaffold 内容 Box 内录制，顶栏、底栏、柔光动作和 MIUIX 弹窗继续通过 CompositionLocal/页面模糊宿主消费，避免形成渲染树自引用。
- 崩溃修复后 178 项 Debug 单元测试零失败，`assembleDebug`、Release Kotlin、R8、`lintVitalRelease`、规格校验与 `git diff --check` 通过；ADB 当前没有在线设备，因此未能自动执行真机启动回归。
- 崩溃修复签名 APK：`Deadliner-v5.0.0-category-backdrop-stack-overflow-fix-signed.apk`；APK SHA-256 为 `eea760413c689b0279f2c37603c423a881ed0f418bac2ec8abcb58ee4e199766`，APK Signature Scheme v3，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，16 KiB 对齐校验通过。
- 崩溃修复包已由机器人发送至本人账号 `Aritx Zhou`，消息 ID `om_x100b66a64d3288a0c3f4d831bba21a1`。
- 分类编辑页保留安全的内容层 Backdrop，并在底部增加一个顶栏高度的滚动余量；这让短表单中的实际内容可以移动到顶栏及删除按钮下方，不再只采样均匀 Surface。
- 分类删除弹窗确认按钮保留 MIUIX Dialog 的等宽填充布局，配色改为 Material 语义色 `errorContainer/onErrorContainer`。
- 本轮定向分类/基础 UI 单元测试与 `assembleDebug` 通过。
- 本轮最终 178 项 Debug 单元测试零失败，Release Kotlin、R8、`lintVitalRelease`、规格校验和 `git diff --check` 通过；ADB 无在线设备，因此模糊效果仍需安装后真机视觉确认。
- 最终签名 APK：`Deadliner-v5.0.0-category-visible-blur-error-dialog-fix-signed.apk`；APK SHA-256 为 `78f1d138b0742963c6ce71168e168ab0e9ab7658f97d0539a40065fb663532f9`，APK Signature Scheme v3，证书 SHA-256 为 `3f3f7b8270647a10d6e04080bc4805f1065d619db8a74ec75cf5b84b1e6c8406`，16 KiB 对齐校验通过。
- 最终签名包已由机器人发送至本人账号 `Aritx Zhou`，消息 ID `om_x100b66a61146cca4c299c1dc1ebbee7`。
