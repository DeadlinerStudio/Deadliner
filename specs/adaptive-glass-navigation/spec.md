# 自适应柔光玻璃导航与沉浸体验统一

## 背景

现有沉浸体验偏好控制悬浮底栏和渐进顶栏，宽窗口仍使用旧导航轨。
用户要求先提交全部工作区修改，再删除关闭状态及相关链路。

## 目标

- 清单导航使用固定版本的 MiuixIcons.Home；Overview 使用用户提供的 Chart Pie Fill SVG 路径。
- 底部 Tab 按数量收拢并居中：纵向每项最大 80dp、整条最大 380dp（与 HyperModifier 一致）；横向每项最大 104dp，加胶囊两侧 6dp 内边距。
- Compact（<600dp）使用堆叠悬浮底栏；Medium（600–839dp）使用图标与文字并列悬浮底栏；Expanded（>=840dp）使用可收展柔光玻璃侧边栏。
- 移除开关、AppearancePreferences 字段、旧偏好读写及迁移逻辑、关闭状态 UI 分支。
- 所有窗口统一渐进顶栏；保留高级材质关闭、无 backdrop、旧系统的正常材质降级。

## 非目标

不修改任务业务规则、核心库接口或其他平台代码；不移除高级材质偏好。

## 用户场景

折叠屏内屏使用横向悬浮导航；平板横屏侧栏展开并为内容让位，竖屏默认折叠，展开覆盖内容并支持点击遮罩收起。
旧用户即使保存了关闭沉浸体验，也直接进入统一设计。

## 验收标准

- Android debug 编译与相关导航/材质单元测试通过。
- 模拟器检查 Compact / Medium / Expanded 和展开、折叠、竖屏遮罩。
- 语义浮光及渐进顶栏覆盖整窗，标题和页面内容单独避让侧栏；饼图按 0.86 光学缩放，侧栏复用每个图标的缩放。
- 页面内容独立捕获 backdrop，导航、FAB、弹窗和渐进顶栏不得进入自身采样树。
- 删除偏好后没有 useImmersiveExperience、ImmersiveExperiencePreferences 或 LocalImmersiveExperienceEnabled 生产代码引用。
- 删除前存在包含所有工作区修改的 Git 提交。

## 风险与约束

- NexioSchedule 侧边栏来源 revision 0e131d5fc75c55e3e6d2b84500b843f36ce4d65a，AGPL-3.0，移植文件保留标注和许可证。
- 复用 Deadliner SoftGlassSurface 替换上游独立材质层；路由回调由 ModernHost 提供。
- 动画进度在 layout/draw 阶段读取，避免页面树逐帧重组。
- 新文件有效代码低于 1000 行；既有超长文件不得增长。

## 验证结果

- 删除前全工作区提交：210e4a2（140 文件）。
- :app:assembleDebug 构建成功；导航、材质、列表策略相关 52 项单元测试全部通过。
- Pixel_Tablet 模拟器查看 1280dp 横屏展开/折叠、900dp 竖屏展开遮罩、700dp 横向底栏、500dp 纵向底栏、概览路由和暗色主题。
- 本轮新组件未超过 1000 有效代码行；扫描发现既有超长 SettingsComponents、AIOverlay 和生成绑定。SettingsComponents 从 1128 行降为 1096 行，GlobalUtils 同样缩减，没有新增超长文件。
- 规格校验、diff 检查通过；高级材质关闭及低 API shader 降级逻辑保留，未在低 API 设备进行实机验证。

## 图标细调

饼图 SVG 的 0.85 填充透明度归一到 1，底栏 tint 不再透出背景造成深浅差异。灵感图标从 1.0 到 1.02，Lifi 从 0.9 到 0.945，分别相对当前尺寸增加 2% 和 5%；底栏与侧栏共享这套尺寸。
