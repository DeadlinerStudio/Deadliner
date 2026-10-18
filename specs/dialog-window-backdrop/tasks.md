# Dialog Window Backdrop Tasks

## 阶段 1：规格冻结
- [x] 核对 MIUIX 0.9.4-rc01 Scaffold/DialogLayout/WindowDialog 源码。
## 阶段 2：基础设施
- [x] 窗口 recorder 覆盖根 Scaffold。
## 阶段 3：功能实现
- [x] 独立 WindowDialog、保留材质和退场生命周期。
## 阶段 4：验证与回写
- [x] 本地编译、主机交互回归、规格校验；禁止设备操作。

验证：Kotlin 编译通过；447 项主机测试通过，包含 Sheet 内 Dialog 采样源、独立窗口、退出释放、条件调用与深浅色回归。diff 与文件有效行数检查通过。未操作设备，未验证实际 GPU 模糊像素。
