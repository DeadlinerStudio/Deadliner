# 可切换界面图标包

## 背景
Symbols 已将资源 ID / 动态名称与 Material 字体绘制解耦，但界面仍固定使用 Material Symbols，少量直接 MIUIX 图标绕过入口。

## 目标
在界面外观 → 设计与色彩中提供 Material Symbols / Hybrid MIUIX 下拉选项，即时生效并持久化。默认保持 Material Symbols。Hybrid 优先使用 PR 423 的 Glass Regular（330），没有合适图形时使用 MIUIX Regular，再使用 Phosphor 补充（按图标选择 Fill / Regular，并做固定光学校准）。

## 非目标
不重新启用官方玻璃材质实验，不修改品牌标识、插画、系统规定的通知小图标或第三方 logo。不升级整个 MIUIX 库。

## 用户场景
切换设置即可预览整页和其他界面的图标变化；重启后保持选择；资源 ID、动态名称和应用内 XML Drawable 使用同一映射。

## 验收标准
完整枚举已有 Symbols 及动态键，检查使用语境、原图与候选图形，留下逐项映射理由和可视化对照。不能把心形映射为星形、反馈映射为告警、调色盘映射为主题衣架；填充状态和 RTL 方向保持有意义，缺失映射有明确回退。品牌资产不受切换影响。两种图标包编译及关键布局、状态恢复测试通过。

## 风险与约束
固定 PR 源码版本，保留 Apache-2.0 / Phosphor MIT 许可；图标保持单色，由调用方提供 tint。XML 已存在 Drawable 需在选择变化时失效重绘；不操作主力机，仅本地编译、主机测试与离线矢量审图。

## 已实施与验证
入口为设计与色彩 → 界面图标，两个选项即时生效并保存到既有 app_settings。Hybrid 共覆盖 142 个语义/状态键：53 个 Glass、1 个 MIUIX Regular、86 个 Phosphor（71 Fill、15 Regular，含保留附加语义的派生组合）、2 个原图保留（Android、house）。实际生成 140 个替代资源，品牌与原有定制资产不进入 Symbols 映射。

按用户最新规则，基础图标允许语义一致的轮廓变化，优先官方；收藏、星标、书签严格保留对象，空心/实心配对独立。逐项审查证据见 review/README.md 和 review/index.html。原图由应用字体直接导出 330 字重，而非猜测或仅按图标名称选择。

本地 Kotlin 编译及全部 417 个主机测试通过（110 套件、零失败/错误）。新回归覆盖完整注册表、动态键、严格内容锁定、方向性、全部矢量/复合裁剪资源加载、偏好恢复、即时 Compose 后端切换和既有 XML 透明度/失效重绘。修改核心文件低于 1000 有效行；未操作设备，最终实机观感待复核。

## 2026-10-17 图标细调
MIUIX Home 仅用于主页清单；分类 home 按图标包选用 Material home 或 Phosphor Fill house。Hybrid 首页独立加号 28dp（HyperModifier 独立操作尺寸），总览箭头为原尺寸 50%，设置及 Lifi 轮播箭头 16dp。快速添加展开使用 Glass ExpandMore；两个发送入口强制使用 Material 上箭头；更新入口使用 MIUIX Regular Update。点击区域不变。
附件 VoiceAssistProxy.apk 离线反编译后确认仅为启动代理，不包含新对话图标；该入口按用户截图重绘单色矢量，明确标注为参考重绘而非提取。

本轮最终 Kotlin 编译和全部 418 个主机测试通过（110 套件），零失败；新增回归验证 Hybrid 下发送箭头保留 Material 后端，并覆盖两个独立矢量的加载绘制。未操作任何设备。

## 主页与分类图标语境补充
底栏清单在两种图标包下始终直接使用 MiuixIcons.Home（Regular 330），分类 home 资源指向 msr_home，Material 使用原字体 home，Hybrid 使用 Phosphor Fill house。快速添加展开按钮仍为 44dp 点击区域，图形由 12dp 提升至 14.4dp（120%，两侧 padding 从 16dp 改为 14.8dp）。

## 局部字重
Hybrid 总览右上角月份切换箭头使用 PR 423 Glass Demibold（500），快速添加展开使用 Glass Medium（430）；其他语境与 Material 图标不变。使用固定上游对应字重路径，保留已有尺寸与点击区域。
