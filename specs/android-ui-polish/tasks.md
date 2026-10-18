# Android UI 优化 Tasks

## 阶段 1：规格冻结
- [x] 检查依赖、当前改动、设备限制和官方 API。

## 阶段 2：基础设施
- [x] 玻璃采样与图标策略。

## 阶段 3：功能实现
- [x] 简洁模式退出、设置选择/导航、详情展开。

## 阶段 4：验证与回写
- [x] 本地编译、主机单测、文件规模扫描；不运行 connected 测试。

## 验证结果
- `:app:compileDebugKotlin :app:testDebugUnitTest -x :app:syncDeadlinerCoreKmp --offline`：通过，95 个测试套件 / 350 个测试，零失败。Core 在首轮构建已同步，后续跳过重复构建。
- 新增覆盖：玻璃采样边界、旧样式键迁移、设置重复导航/返回栈。
- `git diff --check` 与规格验证通过。
- 仓库未提供 `scripts/check_large_core_files.py` / `core_suffixes.txt`，改用有效行扫描检查全部变更 Kotlin 文件：没有超过 1000 行的文件。
- MIUIX API 对照官方 `4a6b750b578880146876e4ab77097d9b01702413` 源码与本地 `0.9.4-rc01` sources.jar。
- 未运行 connected 测试、APK 安装、ADB 操作或设备截图。材质和动画的视觉验收尚待模拟器完成。

## 实现备注
- 三处选择采用 WindowDropdownPreference：背景策略、显示大小、桌面小组件间距。
- 设置采用 NavDisplay + MiuixDefault，栈按字符串保存恢复，避免重复内容键；SettingsActivity 关闭预测返回。
- MIUIX 导航内联 API 需要 JVM 21，Android Java 编译目标同步至 21。
- 详情在列表与搜索结果中捕获来源卡片窗口坐标，扩展到详情；旋转/分屏后放弃过期坐标。
- 删除简洁主页/向导/模式选择入口，旧键统一解析为 MIUIX；共用列表组件继续供现代主页使用。
