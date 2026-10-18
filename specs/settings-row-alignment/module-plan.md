# 模块计划

## 模块拆分
SettingsItemDefaults 读取 Dropdown 官方内边距；SettingsComponents/SettingsRows 共用内边距；CloudSyncOptions 切换标准详情开关；CloudSyncSettings 去除入口装饰图标。

## 平台映射
Android Compose 设置 UI，数据与其他平台不变。

## 文件拆分策略
独立边距来源文件，复用现有组件而不扩充大组件文件。

## 风险点
外层重复 padding、两种详情开关重载、单选尾部控件和 RTL 排列。

主页通过 SettingItem.horizontalPadding=16dp 局部覆盖；默认20dp供子页使用。SettingsNavigationChevron 为统一导航标记，SettingsDetailTextButtonItem.navigates 控制导航样式，原有语义图标在导航行放在标题前。
