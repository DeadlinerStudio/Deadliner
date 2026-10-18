# 模块计划

## 模块拆分
interface_icon_map.json 保存每个 Phosphor 图标的固定 opticalScale 及校准理由；生成器在组合完成后以视口中心缩放；审图表展示 MIUIX 基准与 Phosphor 前后对比。

## 平台映射
Android 离线矢量资产与注册表，现有 Compose/XML 桥接直接复用，不添加场景依赖。

## 文件拆分策略
测量、校准、审图脚本与运行时分离，不增加运行时测量或绘制包装器。

## 风险点
实心/空心状态、窄图形、附加符号裁剪、别名一致性、旧 MIUIX 资产被误改。
