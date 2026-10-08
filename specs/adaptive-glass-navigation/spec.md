# 自适应柔光玻璃导航与沉浸体验统一

## 背景

现有沉浸体验偏好控制悬浮底栏和渐进顶栏，宽窗口仍使用旧导航轨。
用户要求先提交全部工作区修改，再删除关闭状态及相关链路。

## 目标

- 清单导航使用固定版本的 MiuixIcons.Home；Overview 使用用户提供的 Chart Pie Fill SVG 路径。
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
- 页面内容独立捕获 backdrop，导航、FAB、弹窗和渐进顶栏不得进入自身采样树。
- 删除偏好后没有 useImmersiveExperience、ImmersiveExperiencePreferences 或 LocalImmersiveExperienceEnabled 生产代码引用。
- 删除前存在包含所有工作区修改的 Git 提交。

## 风险与约束

- NexioSchedule 侧边栏来源 revision 0e131d5fc75c55e3e6d2b84500b843f36ce4d65a，AGPL-3.0，移植文件保留标注和许可证。
- 复用 Deadliner SoftGlassSurface 替换上游独立材质层；路由回调由 ModernHost 提供。
- 动画进度在 layout/draw 阶段读取，避免页面树逐帧重组。
- 新文件有效代码低于 1000 行；既有超长文件不得增长。
