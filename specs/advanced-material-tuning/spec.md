# Advanced Material Continuous Tuning

## 背景

- 创建日期：2026-08-23
- 功能标识：`advanced-material-tuning`
- 现有高级材质只能从五个离散档位选择模糊半径，设置页预览使用普通图片模糊，不能同时说明顶部栏材质和悬浮胶囊玻璃的真实层次。

## 目标

- 在高级材质设置中提供 28–256px 的连续模糊半径滑块；默认值由用户导出预设四舍五入固化为 `92px`，旧五档仅继续承担插值锚点与快捷选择兼容。
- 将自定义半径持久化，并让全局 `AdvancedMaterialSpec`、顶部栏、导航材质及其他高级材质消费者实时使用。
- 饱和度以 128px 对应 `1.00` 为锚点，在现有五档半径/饱和度节点之间分段线性插值。
- 将设置预览改为同一 Backdrop 上的顶部高级材质模拟与胶囊玻璃材质预览。
- 保留旧 `advanced_material_level` 的读取兼容：首次没有自定义半径时，以旧档位半径迁移。
- 暂时展示“高级参数（实验）”区，允许精细调整基础材质的颜色/噪点/Tint，以及柔光玻璃的模糊、折射、高光、降级描边和后置柔化参数。
- 所有精细参数都持久化，并同时驱动 Preview 与实际悬浮底栏；提供一键恢复默认。
- 开放边缘高光的亮度灰度，以及柔光玻璃在浅色/深色模式下的独立 Tint 灰度，避免动态主题色干扰中性玻璃调试。
- 允许将当前模糊半径和全部精细参数导出为带 schema version 的 JSON 预设文件。
- 让柔光玻璃默认高光按明暗模式非对称：浅色模式增强轮廓区隔，深色模式降低高光存在感；高级参数仍可覆盖默认值。
- 默认隐藏高级微调面板；仅在当前应用进程内连续点击“关于 → 版本号”10 次后解锁显示。
- HyperOS 中性配色的设置项主/次文字使用不带透明度的 `#191919/#6F6F6F`（深色 `#F2F2F2/#A6A6A6`）；同时确保 `surfaceVariant` 与 `surfaceContainer` 不同值，避免 Material3 按颜色值反查内容色时误把容器标题映射为次文字色。其他现代品牌 palette 不变。

## 非目标

- 不删除 `AdvancedMaterialLevel` 或破坏引导页现有五档快捷选择；快捷选择需要同步写入自定义半径。
- 不改变高级材质开关、模糊算法、悬浮底栏尺寸或胶囊几何形状。
- 不开放 RenderEffect 链顺序或 BlendMode 这类会改变材质语义的结构参数。
- 本阶段不实现 JSON 导入，也不持久化或跨进程保存微调面板的解锁状态。

## 用户场景

1. 用户开启高级材质后拖动滑块，预览中的顶部栏模糊和胶囊玻璃立即随半径变化，松手后保存。
2. 用户将滑块放在 128px 时饱和度为 1.00；在其他半径时由相邻旧预设值平滑插值，不发生跳变。
3. 已有用户升级后继续获得原档位对应的视觉强度，之后可从该值开始连续调节。
4. 用户在高级参数区调整柔光玻璃模糊倍率或折射强度，Preview 立即响应，松手后实际悬浮底栏使用同一参数。
5. 用户试验后可一键恢复当前经过视觉确认的默认配方。
6. 用户调好配方后点击导出，通过 Android 系统文件选择器保存可读 JSON，便于回传或之后内置为预设。
7. 普通用户进入高级材质设置时看不到实验微调面板；在关于页连续点击版本号 10 次后返回即可看到，应用进程重启后重新隐藏。

## 验收标准

- `AppearancePreferences` 保存经过 28–256px 限制的 `advancedMaterialBlurRadius`。
- SharedPreferences 半径新键缺失时：若存在旧 `advanced_material_level` 键则保留该旧档位半径，否则使用固化后的新默认；存在半径键时优先读取并按默认配方版本迁移。
- 插值精确经过五个既有节点：28/1.05、60/1.02、128/1.00、196/0.98、256/0.95，且 128px 明确为 1.00。
- 设置页移除离散 RadioGroup，显示连续 Slider、当前整数半径与插值后的饱和度。
- 引导页保留快捷档位，选择后同时更新档位和自定义半径。
- Preview 背景只注册一次 `LayerBackdrop`；顶部使用与正式 TopAppBar 相同的 `advancedTextureBlur`，底部胶囊使用 `deadlinerGlassMaterial`。
- Preview 直接使用 `res/drawable/some_background.webp` 作为被采样背景，不叠加会放大模糊观感的人造高对比装饰。
- 高级材质关闭时两块预览安全降级为不透明度更高的普通表面。
- 基础材质开放：饱和度倍率 0.75–1.25、噪点 0–0.20、亮度 -0.20–0.20、对比度 0.75–1.25、顶部/导航 Tint alpha 0.20–1.00。
- 柔光玻璃开放：模糊倍率 0.10–1.00、噪点倍率 0–2、Tint alpha 倍率 0.50–1.50、折射开关/高度/位移/深度/色散、0–1.00 高光 alpha、0.10–2.00 深色高光倍率、降级描边宽度与明暗 alpha、后置柔化开关与半径。
- 边缘高光灰度范围为 0–1，默认 1.0；柔光玻璃 Tint 在浅色/深色模式下分别使用独立 0–1 灰度，默认为 0.9900225/0.11889533。
- 固化后的默认预设使用附件的稳定四舍五入值：主模糊 92；基础饱和度倍率 1.05、噪点 0.095、亮度 0、对比度 1、顶部/导航 Tint 均为 0.75；玻璃模糊/噪点/Tint 倍率 0.25/1/0.90，浅/深 Tint 灰度 0.99/0.12；折射 18dp/18dp/0.60、色散 1dp；高光 0.95、灰度 1、深色倍率 0.20；降级边缘 0.5dp/0.46/0.08；后置柔化 0.5dp。
- 默认配方版本升级时，缺失键直接使用新默认；已有值仅在精确等于上一版默认时迁移，非默认值视为用户手调并保留。
- 微调面板可见性的唯一入口为关于页版本号的第 10 次点击；静态常量不得绕过该门控。
- HyperOS 的主文字 token 为不透明 `#191919/#F2F2F2`，次文字 token 为不透明 `#6F6F6F/#A6A6A6`；Vivo、Honor、Oppo 映射保持不变。
- HyperOS `surfaceContainer` 保持 MIUIX 原值（浅色白色、深色 `#242424`），`surfaceVariant` 使用浅色 `#ECECEC`、深色 `#333333`，两种模式都不得与 `surfaceContainer` 相等。
- 通用 `SettingsSection` 的 `surfaceContainer` 内容必须通过 Material3 `contentColorFor` 解析为 `onSurface`；不得给“去线留白设计”或任何单独设置项添加标题颜色覆盖。
- 多代旧安装按默认配方版本逐字段迁移；用户手动调过且不等于对应上一版默认的参数不得被覆盖。
- Slider 拖动期间仅更新本地 Preview，松手后再批量写入 SharedPreferences；开关和恢复默认即时持久化。
- 高级参数面板由单一进程内解锁状态控制；隐藏 UI 不影响已保存配方和正式材质渲染。
- JSON 导出根节点包含 `schemaVersion=1`、`type=deadliner_advanced_material`、`blurRadius`与 `fineTuning`；本阶段只导出，不实现导入。
- Kotlin 编译、插值单元测试、规格校验与大文件扫描通过。

## 风险与约束

- 拖动期间不应每个像素都写 SharedPreferences；本地状态驱动预览，松手时持久化。
- 当前 `SettingsComponents.kt` 已超过 1000 行，本功能不得继续扩张该文件；滑块与预览实现留在独立的 `AdvancedMaterialSettings.kt` 或新文件。
- RuntimeShader 真实折射仅在 API 33+ 可见，Preview 在低版本继续展示 MIUIX 高斯模糊降级。
- 当前工作区包含任务卡、Glance Widget、baseline profile 等并发修改，本功能只做局部补丁并保留其他差异。
- `GlobalUtils.kt` 已在本功能前超过 1000 行；持久化键解析与写入必须放到新 helper，`GlobalUtils` 只保留最小的读/写接线。

## 实现与验证记录

- 已新增连续 Float 偏好：无新键时从旧档位半径迁移，引导页选档会原子同步旧档位与新半径。
- 已用纯模型实现 28–256px 限制、五节点分段线性饱和度插值，其中 128px 精确对应 1.00。
- 设置页 Slider 拖动时仅更新本地预览，松手时持久化；顶部模糊与底部胶囊共享同一 `LayerBackdrop`。
- “柔光玻璃”预览直接复用 `deadlinerGlassMaterial` 和悬浮导航 recipe，因此展示的有效玻璃模糊半径为全局半径的 0.4。
- 预览背景已切换为未着色的 `some_background.webp`，并移除额外高对比圆点，避免紧凑 Preview 尺度放大模糊强度的错觉。
- 已新增 `AdvancedMaterialFineTuning` 纯模型与独立 SharedPreferences helper，所有数值读取后都经过有限值和范围校验。
- `createAdvancedMaterialSpec` 成为主题和 Preview 的统一材质构造入口，`DeadlinerGlassRecipes.floatingNavigation` 为 Preview 与实际悬浮底栏生成同一动态 recipe。
- 高级参数面板已由进程内 `AdvancedMaterialFineTuningAccess` 单一状态控制；版本号第 10 次点击解锁，重启应用后恢复隐藏，已存配方不受影响。
- 2026-08-23：新增精细参数范围测试和玻璃 recipe 映射测试；Kotlin 编译与三组定向 JVM 测试通过。
- 高光灰度已同时驱动 Shader rim、API 33+ 0.5dp 克制边缘光与低版本降级描边；高光 alpha 调节上限扩展到 1.00，深色倍率下限扩展到 0.10，便于覆盖更宽的视觉调试范围。
- 柔光玻璃 Preview 与实际悬浮底栏已从动态表面色切换为用户调节的浅色/深色中性灰度，各自再应用导航 Tint alpha。
- “导出 JSON 预设”使用 Android `CreateDocument(application/json)` 写入用户选择的 URI；纯 Kotlin encoder 与 schema 单元测试已通过。
- 2026-08-23：高光/Tint 灰度、JSON 导出与动态 recipe 整批 Kotlin 编译及三组定向测试通过。
- 2026-08-23：Kotlin 编译与 `AdvancedMaterialTuningTest` 通过；仅保留 `GlobalUtils.kt` 内既有弃用 API 与恒真条件警告。
- 2026-08-23：柔光玻璃高光默认值改为浅色增强、深色收敛，并为仍使用上一版默认值的偏好增加一次性兼容迁移。
- 2026-08-23：HyperOS 中性 palette 的 `background`、`surface`、`surfaceContainerLow/Lowest` 已统一为浅色 `#F3F3F3`、深色 `#000000`；背景/默认参数、玻璃 recipe、JSON 回归测试及 Kotlin 编译通过。
- 2026-08-23：边缘高光 Slider/模型上限开放到 1.00，深色高光倍率下限开放到 0.10；默认值不变，边界单元测试、Kotlin 编译和规格校验通过。
- 2026-08-23：用户导出预设已按 92px 与稳定四舍五入参数固化为 `AdvancedMaterialFineTuning.Default`；默认配方版本升至 3，并以逐字段方式迁移旧默认、保留自定义值。
- 2026-08-23：实验微调入口改为关于页版本号 10 次点击的进程内解锁；HyperOS 设置文字改用不透明主/次 token。“恢复默认”同步恢复 92px 主模糊和完整精调配方。默认值、偏好迁移、解锁计数、颜色、JSON 与玻璃 recipe 共 17 项定向测试及 Kotlin 编译通过。
- 2026-08-23：确认 HyperOS 的 `surfaceVariant` 与 `surfaceContainer` 使用相同色值时，Material3 `contentColorFor` 会优先命中 `surfaceVariant`，导致通用设置卡片继承灰色 `onSurfaceVariant`；已在 palette 根部将 variant 去别名为浅色 `#ECECEC`、深色 `#333333`，未修改任何单独设置项。语义内容色回归测试、Kotlin 编译、规格、格式与大文件检查通过。
