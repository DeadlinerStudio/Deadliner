# 小组件背景透明度自定义 Module Plan

## 模块拆分

- preferences：透明度字段、范围限制、alpha 转换、默认兼容、实例持久化与清理。
- configuration：复用现有滑块行，支持百分比单位，预览背景同步透明度。
- ui：两类 Glance 小组件读取配置并传给现有背景 Image。

## 平台映射

Android 仅修改 app 的 widgets/glance 与中英文资源；iOS、HarmonyOS 不在此次范围内。

## 文件拆分策略

沿用现有数据、编辑、渲染模块，不引入巨型文件。所有受影响 Kotlin 文件应低于 1000 行有效代码。

## 风险点

透明度与 alpha 方向相反，集中转换，确保 100% 对应 alpha 0。配置缺失须按尺寸回退。保持背景 Image 在全透明时仍有点击 Action，内容层不应用整体 alpha。
