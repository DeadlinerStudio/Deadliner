# 模块计划

## 模块拆分

- InterfaceIconPack / InterfaceIconPreferences：枚举、持久化、Flow 与 XML 重绘通知。
- HybridIconRegistry：经过审查的语义键到离线 VectorDrawable 资源映射。
- MaterialSymbolStyle / MaterialSymbolDrawable：统一选择绘制后端；现有调用方保留资源标识。
- scripts/interface_icon_map.json 与生成器：固定 MIUIX / Phosphor Fill 源版，转换 Regular 路径及圆角描边，生成注册表、许可与审图页。
- Theme 与 DesignColorSettings：全局 CompositionLocal 和下拉选项，沿用已有主题和 Dropdown。
- specs/interface-icon-pack/review：逐项图形与语义对照，避免按名称盲目映射。

每个核心文件少于 1000 有效行。仅 Android 应用层，保留跨平台业务与系统资产。

## 平台映射
仅 Android UI、XML Drawable 与应用设置；跨平台领域模型和其他客户端不变。

## 文件拆分策略
设置存储、绘制桥接、注册表、转换器与审图证据分文件，每个核心文件低于 1000 有效行。

## 风险点
实心图标的负形、复合符号可读性、星标与心形状态、RTL 镜像、XML 失效通知、上游版权与固定版本。

InterfaceIconMetrics 集中管理按语境的 Hybrid 尺寸；Material 绘制桥提供明确的原始字体入口供发送操作使用。

设置的 SettingItem 和 SettingsDetailTextButtonItem 从原超限文件移至 SettingsRows，样式与回调保持一致。

mainListHomePainter 显式持有底栏清单标识，两种图标包共用；通用 home 键与分类资源使用原 Material home，避免语境冲突。
