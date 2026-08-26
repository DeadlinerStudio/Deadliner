# 沉浸体验 Tasks

## 阶段 1：规格冻结

- [x] 核对现有底栏、柔光玻璃、顶栏和偏好实现
- [x] 阅读 NexioSchedule 参考组件并记录许可证边界
- [x] 冻结沉浸体验生效范围、尺寸约束、迁移策略与交互验收标准
- [x] 创建重构前完整工作区回退提交 `c2f3c23`

## 阶段 2：基础设施

- [x] 新增无布局约束的柔光玻璃 Surface 与圆形图标按钮
- [x] 新增渐进顶栏背景容器与沉浸顶栏上下文
- [x] 新增连续 Tab 运动状态、目标解析和运动参数测试
- [x] 将旧偏好迁移为 `useImmersiveExperience`

## 阶段 3：功能实现

- [x] 底栏容器与选择胶囊切换到连续曲率 Capsule
- [x] 重构点击、按压、拖动、速度拉伸和准确吸附反馈
- [x] 让 Compact MIUIX 顶栏与底栏共享 Backdrop
- [x] 接入渐进模糊顶栏与主要圆形柔光玻璃按钮
- [x] 更新中英文设置标题与辅助文案

## 阶段 4：验证与回写

- [x] 运行偏好迁移和导航运动 JVM 回归测试
- [x] 统一运行 Android Kotlin 编译
- [x] 运行规格、格式与大文件检查
- [x] 回写最终实现与验证结果

## 阶段 5：全局渐进标题栏

- [x] 冻结全局生效范围、顶部覆盖和按钮材质渐显规则
- [x] 创建上一轮实现回退提交 `387e2b5`
- [x] 将沉浸体验状态提升到主题 CompositionLocal
- [x] 让通用 `TopAppBar` 托管渐进背景并移除主界面局部包装
- [x] 让柔光玻璃 Surface 分离材质 alpha 与不透明前景
- [x] 接入 Material3/MIUIX 折叠进度与主列表显式折叠状态
- [x] 补充进度映射测试并完成编译、规格和大文件验证

## 阶段 6：统一滚动显现与材质层次

- [x] 冻结 Small/Center 默认隐藏、滚动阈值、公共动画与单 Backdrop 规则
- [x] 创建上一轮实现回退提交 `e021405`
- [x] 新增不消费事件的通用顶栏滚动距离观察器
- [x] 将所有模式的默认按钮材质目标改为 0，并统一由公共顶栏执行动画
- [x] 让顶栏按钮复用父级渐进模糊基底，移除第二次 Backdrop 采样
- [x] 更新进度映射测试并完成编译、规格与大文件验证

## 阶段 7：分离顶栏遮罩与按钮 Backdrop

- [x] 冻结按钮保留 Backdrop、顶栏背景使用单层半透明渐变遮罩的规则
- [x] 从 `ProgressiveTopBarMaterial` 移除高级材质与 Backdrop 渲染
- [x] 恢复顶栏导航及 action 按钮的独立 Backdrop 柔光玻璃
- [x] 完成编译、规格与大文件验证

## 阶段 8：轻量渐进 Backdrop 模糊

- [x] 调研 NexioSchedule 的固定模糊半径与 RuntimeShader 渐隐结构
- [x] 冻结共享单一 Backdrop、顶栏轻量效果链和按钮完整玻璃链的边界
- [x] 将 `ProgressiveTopBarMaterial` 改为固定 4dp Blur + Shader alpha/tint 渐隐
- [x] 补充渐变参数与降级行为的 JVM 测试
- [x] 完成 Kotlin 编译、定向回归、规格、格式与大文件验证并回写结果

## 阶段 9：渐进模糊透明遮罩

- [x] 冻结遮罩仅叠加于真实 Backdrop、前景保持不透明的层次规则
- [x] 在渐进模糊之上加入轻量 Surface alpha 渐变
- [x] 补充遮罩参数测试并完成编译、定向回归与静态验证

## 阶段 10：沉浸导航与按压反馈重构

- [x] 创建上一阶段安全回退提交 `e45f52d`
- [x] 核对 NexioSchedule/AndroidLiquidGlass 的 Capsule、阻尼拖动、边缘位移与选择胶囊反馈
- [x] 冻结搜索拆分、Lifi 占位、无 Ripple 顶栏按钮和 Apache-2.0 Shapes 依赖边界
- [x] 为圆形柔光玻璃按钮实现全区域高亮与缩小反馈，并覆盖任意沉浸 navigationIcon
- [x] 新增主底栏 + 独立搜索按钮 Dock，并让搜索保持现有路由行为
- [x] 将主/选择胶囊切换为 Kyant continuous Capsule，并补齐参考组件的内距、溢出与边缘位移动画
- [x] 在灵感右侧加入 Lifi 占位项并保持其他设计模式的导航兼容
- [x] 补充交互/项目拆分测试，统一完成 Kotlin 编译、定向回归、规格与大文件验证

## 阶段 11：底栏比例、无选择状态与彩色玻璃 FAB

- [x] 冻结主 Tab/搜索图标比例、浅色阴影、搜索无选择状态和沉浸 FAB 定位规则
- [x] 缩小主 Tab 图标与文字，同时保持独立搜索按钮识别度
- [x] 统一增强主 Capsule 与搜索圆钮在浅色模式下的环境阴影
- [x] 为主 TabBar 增加显式选择可见性，搜索激活时隐藏胶囊与选中内容态
- [x] 新增彩色柔光玻璃 FAB，并在沉浸 Compact Scaffold 中居中定位
- [x] 补充状态/参数测试，完成 Kotlin 编译、定向回归、规格与大文件验证

## 阶段 12：按压缩小与 Lifi 占位路由

- [x] 冻结选择图标缩小比例和 Lifi 空白页面行为
- [x] 将主 Tab 按压内容反馈改为仅图标缩小，文字保持稳定
- [x] 将 Lifi 改为 enabled 导航项并接入穷尽的 `MainSection.LIFI` 空页面
- [x] 补充运动参数/路由回归，完成编译、规格和文件体积验证

## 阶段 13：沉浸顶部文字 Tab

- [x] 核对 Overview、Add 与公共 TabRow 的 Backdrop/主题接入边界
- [x] 冻结 42dp 双 Capsule、纯文字、无 Ripple 与非沉浸回退规格
- [x] 新增可复用 `ImmersiveTextTabRow` 与可测试的选择索引参数
- [x] 在公共 `TabRow` 中仅对 MIUIX 沉浸模式切换新组件
- [x] 完成 Kotlin 编译、定向回归、规格与文件体积验证

## 阶段 14：渐进顶栏测量与 Detail 柔光 FAB

- [x] 冻结完整 topBar slot 实测、状态栏/最小内容区与 75% 消散位置的高度规则
- [x] 将渐进材质提升到 Scaffold 顶栏宿主，并让 `TopAppBar` 保留无宿主回退
- [x] 将 Surface 遮罩改为顶部 0.90、75% 高度归零的连续渐变
- [x] 将柔光玻璃 FAB 调整为 64dp，并接入 Detail 页的沉浸分支
- [x] 保证 Detail FAB 使用 `primary/onPrimary` 且可以读取页面 Backdrop
- [x] 补充高度/渐变/FAB 语义测试，完成编译、规格和文件体积验证

## 阶段 15：控件中性色与玻璃阴影增强

- [x] 冻结 Material 活力辅助色与 MIUIX 控件 neutral secondary 的边界
- [x] 在 MIUIX ColorScheme 工厂恢复官方中性 secondary/secondaryVariant
- [x] 将主页和 Detail FAB 加号统一增大到 30dp
- [x] 新增柔光玻璃双层阴影 Token，并接入底栏、文字 Tab、FAB 与圆形按钮
- [x] 补充颜色、阴影和尺寸回归，完成编译、规格与文件体积验证

## 阶段 16：沉浸导航图形与渐进覆盖微调

- [x] 冻结 FAB、单色底栏图标、AI 标题和渐进顶栏覆盖参数
- [x] 将沉浸 FAB 从 64dp 收至 58dp并保持 30dp 加号
- [x] 为导航契约增加沉浸专用图标，接入 home/overview/search `_imm` 与 capture/lifi 回退图标
- [x] 将沉浸底栏启用项统一为同一前景色，不再用透明度区分选择状态
- [x] 将灵感改为无 fill 的 `ic_capture`、清单改为 `ic_home*`，Lifi 底栏标题改为 AI
- [x] 增强顶栏 Surface 透明度渐变，并将自动材质覆盖高度缩短 6dp
- [x] 补充图标解析、尺寸和渐进参数回归，完成编译、规格与文件体积验证

## 阶段 17：无方向柔影与裁切修正

- [x] 冻结轻量四向弥散、零偏移和容器尺寸不变的阴影规则
- [x] 将 elevation 光源阴影替换为零偏移 DropShadow，移除中间 GraphicsLayer
- [x] 下调外层与近缘阴影 alpha/radius，保持浅色白底可辨但不形成重黑边
- [x] 更新通用阴影 Token 回归，完成编译、规格与文件体积验证
