# MIUIX 弹层材质实验室 Module Plan

## 模块拆分

1. 参数模型：在高级材质细调模型中加入 Dialog/Sheet 专属不透明度和绝对模糊半径，统一范围与默认实验值。
2. 持久化：沿用现有高级材质 SharedPreferences 读写链路，兼容没有新字段的旧安装。
3. 材质解析：统一 `rememberMiuixModalSurface` 接收弹层类型并解析对应参数；Dialog 使用纯高级材质主体加弱高光描边，Sheet 保持沉浸柔光玻璃，非沉浸分支使用普通纹理模糊。
4. 实验室 UI：独立 Composable 承载四个拖动条、数值展示、恢复默认和真实弹层预览，`LabSettingsScreen` 只负责接入全局状态。
5. 验证：模型与持久化使用 JVM/Robolectric 测试，包装组件通过编译与真机预览验证。

## 平台映射

- Android 参数模型：`app/src/main/java/com/aritxonly/deadliner/model/`
- Android 持久化：`app/src/main/java/com/aritxonly/deadliner/localutils/`
- Android 弹层包装：`app/src/main/java/com/aritxonly/deadliner/ui/base/`
- Android 实验室 UI：`app/src/main/java/com/aritxonly/deadliner/ui/settings/`
- Android 字符串与测试：`app/src/main/res/values*/`、`app/src/test/java/`
- iOS/HarmonyOS：本轮不修改；这些参数属于 Android MIUIX 渲染实验。

## 文件拆分策略

- 不把四个拖动条和两个预览弹层继续堆入 `LabSettings.kt`，新增 `MiuixModalMaterialLab.kt` 管理局部草稿与预览状态。
- `MiuixModalSurface.kt` 继续作为 Dialog/Sheet 唯一材质策略入口，不向各业务弹层散落透明度和半径常量。
- 复用现有 `AdvancedMaterialFineTuningPreferences`，不新建第二套外观存储。
- 核心文件保持低于 1000 行；若现有细调面板接近阈值，本轮不向其中加入实验室 UI。

## 风险点

- 沉浸分支的玻璃配方当前由全局半径乘倍率得到；弹层实验值需要以绝对半径覆盖，避免全局高级材质半径改变后实验值漂移。
- 非沉浸 `textureBlur` 也必须显式传入同一绝对半径，否则会继续使用 Miuix 默认半径。
- 背景不透明度只在存在 Backdrop 的材质分支生效；无 Backdrop 时保留不透明语义 Surface。
- Overlay 预览必须复用设置页已有 Scaffold popup host，不能增加嵌套 Scaffold。
- Dialog 的高级材质基底必须调用 Miuix 标准 `advancedTextureBlur`，避免手工链路遗漏 dp 密度换算、混色或重复噪声；其后禁止追加自定义折射 Shader，只允许不重新采样内容的弱渐变描边。
