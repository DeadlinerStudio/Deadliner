# 滚轮日期与时间选择器 Module Plan

## 模块拆分

- model / localutils：持久化 useWheelDateTimePicker；GlobalUtils 保留代理接口。
- ui/picker/DateTimePickerRules：年月日范围、最小分钟与 UTC 编码纯函数。
- ui/picker/MaterialDateTimePickers：独立保留 Material 入口和限制。
- ui/picker/DateTimePickers：FragmentManager 对应请求注册与关闭管理。
- ui/picker/DateTimePickerHost：Activity 主题内挂载弹窗；MiuixDateTimePicker：NumberPicker 日期、时间内容。
- ui/settings/BehaviorSettings：交互方式开关，不使用品牌名作为主文案。

## 平台映射

Android app/src/main/java/com/aritxonly/deadliner 对应目录；iOS / HarmonyOS 保持现有实现。

## 文件拆分策略

不向 GlobalUtils / AddDDLActivity 添加 picker 业务；UI、路由、规则、Material 平台桥分开。

## 风险点

日期过渡不触发关闭；取消不写入；闰年和月份改变时夹紧日；opaque/no-backdrop 材质 fallback 保留。

## 连续滚动与动画

DeadlinerNumberPicker 为固定版本 MIUIX NumberPicker 的有署名本地适配，修复首次绘制测量依赖。MiuixDateTimePicker 使用连续月、日、小时、分钟索引保证进位和借位，固定高度容器与单份内容透明度过渡提供步骤切换。规则层增加索引转换，测试覆盖跨月、跨年、闰年、午夜、多圈和负纪元。

## 滚动即时联动与切换流畅度

固定手势起点，在数字跨越中线时立即同步选择，父级回传不重启动画；拖动与惯性阶段均实时进位。日期/时间切换改为固定玻璃表面高度、单份滚轮依次淡出淡入，避免尺寸动画与双树绘制。

## 同屏选择与即时发布修订

PickerPresentation 定义屏幕/字体缩放布局规则和同步值发布器；DeadlinerNumberPicker 从拖动增量及惯性动画逐帧直接发布，保持固定手势锚点。DateTime 请求在手机上分为日期、时间两页，宽屏两组横排同屏；小屏、矮屏或字体放大保留分步。移除上下两组的 CombinedColumn 布局。AlertDialog 提供可选最大宽度，默认仍为 420dp，宽屏组合选择器为 640dp。数字使用 Normal 字重。
