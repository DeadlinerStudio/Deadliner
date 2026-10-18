# 点击反馈裁切 Tasks

## 阶段 1：规格冻结
- [x] 核对入口与 MIUIX/Material indication、默认 shape。
## 阶段 2：基础设施
- [x] 修复共用控件与形状匹配。
## 阶段 3：功能实现
- [x] Lifi 与全应用入口审计修复，记录安全情况。
## 阶段 4：验证与回写
- [x] 本地编译、主机绘制/交互回归、规格及文件大小检查，无设备操作。

验证：Kotlin 编译及 450 项主机测试通过。新增 Native Graphics 像素回归覆盖 Lifi 轮播、记忆按钮及 Material 连体选项原生轮廓；审计记录为 95 处 Foundation + 103 处 Material。diff 与有效行数检查通过，未执行设备操作。
