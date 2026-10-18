# 柔光玻璃 Dropdown Module Plan

## 模块拆分
- WindowGlassBackdropHost：设置窗口内容共享采样与坐标原点。
- SoftGlassDropdownPreference：MIUIX 设置行、窗口生命周期与选项回调。
- SoftGlassDropdownPopup：窗口定位、过渡与标准柔光材质。
- SoftGlassDropdownOptions：跟手胶囊、行语义与手势。
- GlassDropdownGeometry：定位、命中与胶囊边界纯计算及主机测试。

## 平台映射
仅 Android ui/base 与 ui/settings 及 SettingsActivity；其他平台不变。

## 文件拆分策略
窗口采样、定位、交互分文件，保持每个核心文件少于 1000 有效行。

## 风险点
跨窗口坐标、滑出取消、空选项、字体缩放、滚动与拖动选择、窗口重建。

## 来源展开修订
- DropdownSourceValue：设置行与弹层共享的当前值/箭头布局，源布局占位隐藏。
- GlassDropdownMotion：来源边界到面板边界的可逆插值、内容交接透明度及 24dp 目标圆角。
- Preference 冻结右侧来源矩形，Popup 关闭完成才通知来源恢复。面板靠来源尾部对齐并覆盖来源，受安全区域约束。

## 悬停与选择分离
Options 只用真实交互维护预览背景，持久选中项文字/勾选独立。选择回调携带 keepExpanded，Preference 按点击或拖动决定是否关闭。Motion 的来源文本与菜单文本采用互斥透明度，防止反向交接时重影。

## 交互简化修订
Options 仅使用 selectable 与常规 verticalScroll，删除自定义滑动指针处理和命中模型；选择回调恢复为单索引。Popup 复用 MIUIX RemovePlatformDialogDefaultEffects 清理平台窗口动画，组件继续唯一管理自身动画。

## 单一来源文字修订
Preference 持有共享 Animatable，原 DropdownSourceValue 直接参与淡出/恢复，Popup 不再复制来源文字。Motion 增加面板透明度，收回到零时窗口完全透明。主机回归覆盖两端原节点身份与边界稳定，以及零进度透明度连续。

## 官方深色截图对齐
DeadlinerGlassRecipes.dropdown 持有 DropdownCornerRadius，并保留 dialog 的模糊、折射强度与边缘微调。Motion/Popup 引用同一几何常量。Options 负责 14sp 字号/20sp 行高、48dp 最小点击高度、20dp 总留白和全宽列；增加字体、行高、勾选尾侧对齐及镜头/轮廓一致回归。
