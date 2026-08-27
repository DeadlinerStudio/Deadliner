# MIUIX 预设颜色

## 背景

现代设计同时承载 MIUIX 与 Material 组件。当前非动态取色只能依赖单一种子生成 Material 色板，缺少一套稳定、鲜明且能同时覆盖两套组件语义的预设方案；沉浸主页 FAB 也直接复用了 `primary`，玻璃合成后在明暗主题下均缺少预期的色彩表现。

## 目标

- 为现代（MIUIX）设计新增可明确选择和持久化的“预设颜色”来源。
- 采用 MIUIX 官方非 Monet 主色：浅色 `#3482FF`、深色 `#277AF7`。
- 使用独立的 Material 风格 secondary 与 tertiary，让 Material 内容更鲜活，同时保留当前设备中性色表面策略。
- Material 的活力 secondary/tertiary 不直接覆盖 MIUIX 的 neutral secondary 角色；MIUIX Switch、Checkbox 等控件轨道继续使用官方中性灰语义。
- 预设颜色的深色模式中，Material 与 MIUIX Checkbox 的选中勾、Switch 的选中 thumb 均为纯白。
- 预设颜色生效时，沉浸主页 FAB 使用固定色 `#FF6D6D`，并保持图标对比度与柔光玻璃质感。

## 非目标

- 不改变系统动态取色及用户种子色生成结果。
- 不替换 HyperOS、HONOR、OPPO、vivo 的中性色表面预设。
- 不为单个设置项或业务页面单独指定 Checkbox/Switch 颜色。
- 不改变鲜活（Material3）设计的默认颜色来源选择。

## 用户场景

- 用户在现代设计下选择“预设颜色”，获得固定且跨设备一致的多角色配色。
- 用户切换浅色/深色模式后，主色遵循 MIUIX 官方值，辅助色保持协调且控件选中内容清晰可见。
- 用户切回动态取色或任意种子色后，颜色与 FAB 自动恢复对应方案。

## 验收标准

- 颜色来源模型可区分 `SystemDynamic`、`SeedColor`、`Preset`，旧数据可无损迁移。
- 预设浅色 primary 为 `#3482FF`，预设深色 primary 为 `#277AF7`；两者 `onPrimary` 均为白色。
- 预设深色 Material ColorScheme 的 `onPrimary` 与 `onPrimaryContainer` 为白色，MIUIX ColorScheme 的对应前景语义同样为白色。
- Material ColorScheme 保留珊瑚 secondary 与青绿 tertiary；MIUIX ColorScheme 的 secondary/secondaryVariant 使用 MIUIX 官方明暗中性灰，不得把未选中控件轨道染成 Material 辅助色。
- 现代设计颜色选择器显示独立预设入口，鲜活设计不显示该入口。
- 预设颜色 + 沉浸模式下，主页 FAB 基础 tint 为 `#FF6D6D`；其他颜色来源仍使用主题主色。
- 定向单元测试、Debug Kotlin 编译通过，既有沉浸体验测试无回归。

## 风险与约束

- MIUIX 与 Material 控件默认颜色读取不同，必须在主题/基础组件语义层统一，不能在设置项层覆盖。
- 颜色来源此前由 `seed_color` 是否为空隐式推断；新增字段需兼容旧版本偏好。
- 柔光玻璃会对 tint 施加透明度与背景采样，固定 FAB 色必须以主题语义传入而非直接替换整个玻璃材质。

## 实现与验证记录

- MIUIX 主题工厂只从预设 Token 映射 primary、容器与内容角色；`secondary/secondaryVariant` 及 disabled 对应值复用 MIUIX 官方明暗中性色，避免 Material 珊瑚辅助色污染 Switch/Checkbox 未选中轨道。
- 回归测试明确验证浅色 MIUIX secondary 为 `#E6E6E6`、深色为 `#505050`，并验证它们与 Material secondary 不同；Java 22 下完整 Debug JVM 测试 86 项通过。
