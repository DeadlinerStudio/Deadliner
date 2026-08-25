# Deadliner Extensible Glass Material

## 背景

- 创建日期：2026-08-22
- 功能标识：`deadliner-glass-material`
- 需求来源：在保留 Deadliner 现有 MIUIX 高斯模糊质感的前提下，为悬浮底栏增加类似液态玻璃的边缘折射，并把材质能力从单一组件中抽离成可扩展模块。

## 目标

- 提供不参与测量、不改变调用方尺寸的 Compose Glass Modifier。
- 保留现有 `AdvancedMaterialSpec`、`LayerBackdrop`、高斯模糊、Tint 和噪点能力。
- 悬浮导航 recipe 将当前 MIUIX 高级材质预设半径按固化默认 `0.25×` 映射到玻璃链，继续响应用户选择但保留更多背景细节。
- Android 13 及以上通过单一 Backdrop 效果链完成高斯模糊、胶囊透镜折射与全材质柔化；Android 12 和缺少 Backdrop 时安全降级。
- 将模糊、折射、边缘光学和降级策略拆成稳定 recipe，便于未来复用于顶部栏、浮动按钮、弹窗与选中胶囊。
- 先提供独立 Preview，再以现有尺寸接入 MIUIX 悬浮底栏。

## 非目标

- 不改变悬浮底栏当前 54dp 高度、最大宽度、内外边距、图标文字布局或拖动交互。
- 不在本阶段为 Material 3、顶部栏、弹窗或选中胶囊启用折射。
- 不追求逐帧流体模拟、形状变形或强烈 RGB 色散。
- 不修改 MIUIX Blur 依赖源码，也不新增第三方图形依赖。

## 用户场景

1. 高级材质开启且运行在 Android 13 及以上时，用户在底栏胶囊边缘看到明显但连续的背景透镜畸变，中央仍保持柔和高斯模糊。
2. Android 12、Preview 无 RuntimeShader 或 Backdrop 缺失时，用户仍看到可读的模糊或半透明容器以及克制边缘光。
3. 开发者可通过 recipe 调整边缘宽度、折射强度、光学透明度和降级描边，而无需修改导航组件布局代码。

## 验收标准

- `deadlinerGlassMaterial` 不添加 `padding`、`size` 或其他 Layout Modifier，接入前后底栏测量尺寸一致。
- API 33+ 的中心与边缘使用同一份 Backdrop 纹理，按 `color controls → Gaussian blur → lens refraction → softening blur` 连续处理，不再把独立半透明折射环覆盖在模糊外侧。
- Android 13+ 使用 MIUIX Blur 公开的 `drawBackdrop` 与 `runtimeShaderEffect`，不依赖库的 internal API。
- Android 12 自动使用高斯模糊加轻微渐变边缘光；缺少 Backdrop 时使用半透明 Tint 加同等降级边缘光。
- 固化默认使用 1dp 色散；透镜使用圆形截面映射将 18dp 最大采样位移集中在 18dp 内边缘带，向中央快速连续衰减。
- 悬浮导航默认透镜为 18dp 高度、18dp 位移和 0.60 depth effect；配合更清晰的半径映射，使边缘跨越文字或色块时产生可辨认的弯折。
- 独立 Preview 提供跨过玻璃边缘的高对比背景，以便观察高斯模糊和折射差异。
- MIUIX 悬浮底栏接入后保持当前 54dp 高度、1.5dp 内间距与 4dp 横向外边距。
- 默认导航 recipe 使用“0.25× 主高斯模糊 → 完整胶囊透镜 → 0.5dp 折射后柔化”的单链顺序；Tint 作为材质表面绘制且导航内容最后绘制。
- 边缘光覆盖完整轮廓，并在各边保留基础亮度后叠加方向渐变，避免退化成单边高光。
- 默认光学高光在浅色模式使用 0.95 alpha；深色模式乘 0.20，得到 0.19 的轮廓。降级路径对应使用 0.46/0.08 alpha。
- Kotlin 编译、规格校验与核心文件大小扫描通过；并发工作区改动不被覆盖。

## 风险与约束

- RuntimeShader 在 Android 上要求 API 33；项目 minSdk 31，因此必须保留 API 31–32 降级路径。
- API 33+ 使用一个 Backdrop 节点串联 BlurEffect 与 RuntimeShader；折射只覆盖最大 380×54dp 的底栏，避免多个独立 Backdrop 层重复采样和产生脱节边缘。
- 折射后柔化是同一 RenderEffect 链中的 0.5dp 小半径 BlurEffect，可由 recipe 关闭。
- Shader 使用可配置圆角矩形 SDF 计算边缘距离与法线，默认解析为胶囊；Backdrop 扩展采样通过 effect padding 完成，不得转化为布局 padding。
- Preview 的 RuntimeShader 支持取决于渲染环境；降级 Preview 仍需可用，真实折射需在 API 33+ 设备或模拟器复核。
- 当前工作区存在任务卡、Glance Widget、baseline profile 等并发修改，本功能只修改新材质目录、底栏材质调用和对应规格/Preview。

## 实现与验证记录

- 新增独立 `ui/material/glass` contract、renderer、AGSL 与 Preview 文件；底栏只调用共享材质 Modifier。
- 折射层使用圆角矩形 SDF，默认半高圆角保持胶囊几何；边缘带宽、位移、透明度、光学高光、降级边缘和 geometry 均由 recipe 配置。
- 材质层序由 `GlassLayeringSpec` 控制；导航默认在透镜后对完整形状做 0.5dp 轻度 Gaussian softening。
- 底栏继续使用工作区当前的 54dp 高度、1.5dp 内间距、4dp 横向外边距，材质 Modifier 不包含 Layout Modifier。
- `:app:compileDebugKotlin` 于 2026-08-22 通过；AGSL 的真实视觉效果仍需 API 33+ 设备或支持 RuntimeShader 的 Preview 环境复核。
- 新增材质 Kotlin 文件均不超过 204 行；大文件扫描只报告仓库既有的超限文件，本功能未修改或扩大这些文件。
- `deadliner-glass-material` 与 `miuix-floating-tab-bar` 规格校验通过，`git diff --check` 无格式错误。
- 第三轮参考 `Kyant0/AndroidLiquidGlass` 的公开 Apache-2.0 实现，采用 rounded-rectangle SDF、解析法线与 circular lens profile，并将 API 33+ 渲染改为连续的 blur → lens 链。
- 第三轮默认透镜高度为 14dp、折射量为 10dp、depth effect 为 0.16；Preview 曾提供 17/13dp 强折射版本作视觉对照。
- RuntimeShader 光学链继续保留高级材质的亮度、对比度、饱和度、噪点与 Tint；API 31–32 继续使用现有 `advancedTextureBlur` 降级。
- 第五轮将导航 recipe 的 `GlassBlurSpec.radiusMultiplier` 从 `0.5f` 下调为 `0.4f`，因此 Crystal Clear/Light/Soft/Hazy/Deep 分别约映射为 11.2/24/51.2/78.4/102.4px；API 33+ 与 API 31–32 降级路径使用相同映射。
- 第六轮保留 `0.4f` 作为默认倍率，并新增 `floatingNavigation(fineTuning)` 动态 recipe，使高级参数面板可调模糊、折射、高光、降级描边与后置柔化；实际底栏与设置 Preview 共用该解析器。
- 第七轮为 Shader 新增高光灰度 uniform，并在光学路径外叠加受同一 alpha/灰度控制的 0.5dp 克制边缘光，使调参变化可见；降级描边同步使用该灰度。
- 第四轮将默认透镜提升到 18dp/16dp/0.24，并把折射后柔化收敛到 0.35dp；旧的 14dp/10dp/0.16 参数不再作为导航默认值。
- 第九轮将用户导出配方四舍五入固化为默认：主半径 92px、玻璃倍率 0.25、18dp/18dp/0.60 折射、1dp 色散、0.95 高光、深色倍率 0.20 与 0.5dp 后置柔化；前述轮次保留为历史记录。
- `:app:compileDebugKotlin` 于 2026-08-23 再次通过；仅报告仓库既有的 compile SDK 与 `GlobalUtils.kt` 警告。
- 第八轮将明暗高光改为非对称默认：浅色增强轮廓区隔，深色减弱发亮感；实验参数仍可分别调节基础高光和深色倍率。
