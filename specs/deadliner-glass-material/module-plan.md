# Deadliner Extensible Glass Material Module Plan

## 模块拆分

- `contract`：`GlassMaterialRecipe`、`GlassBlurSpec`、`GlassRefractionSpec`、`GlassLayeringSpec`、`GlassEdgeOpticsSpec`、`GlassGeometrySpec` 与可选交互状态，描述材质与层序而不依赖导航组件。
- `renderer`：`deadlinerGlassMaterial` 在 API 33+ 以单一 Backdrop 节点串联色彩处理、高斯模糊、透镜与柔化；API 31–32/无 Backdrop 走现有降级层。
- `shader`：独立保存胶囊 SDF、解析边缘法线、circular lens profile、可选色散、噪点与全轮廓边缘光的 AGSL 源码和 uniform 名称。
- `preview`：构造跨越胶囊边界的色块与文字背景，展示浅色、深色和独立材质效果。
- `integration`：`MiuixFloatingTabBar` 只解析 MIUIX 颜色并调用材质 Modifier，继续负责布局与交互。

## 平台映射

- iOS：本阶段不修改；未来 recipe 可映射到 SwiftUI Material、distortionEffect 或 Metal shader。
- HarmonyOS：本阶段不修改；未来 recipe 可映射到 ArkUI backgroundBlurStyle 与自定义 shader/effect。
- Android：新增 `ui/material/glass/`，使用 Compose、MIUIX Blur 公共 API 和 Android 13+ AGSL。

## 文件拆分策略

- 单个核心文件尽量不超过 1000 行有效代码
- 若必须逃逸，先申请开发者批准
- 数据 contract、shader 源码、renderer 和 Preview 分文件，避免形成单一图形巨型文件。
- 不继续扩张 `MiuixFloatingTabBar.kt`；接入仅删除现有内联材质构造并调用共享 Modifier。

## 风险点

- `drawBackdrop` 内 RenderEffect 的链序必须保证主高斯模糊先于透镜，0.55dp 柔化后于透镜，导航内容最后绘制。
- AGSL 输出完整的已模糊 Backdrop 而非半透明折射环，并将采样坐标限制在内容纹理范围，避免透明黑边和折射描边感。
- 圆角矩形 SDF 必须基于实际像素尺寸；默认半高圆角生成胶囊，其他组件可通过 geometry recipe 指定圆角，且不能改变布局。
- API 33 以下不能创建 RuntimeShader，能力判断必须发生在 renderer 内。
