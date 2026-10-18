# Lifi AI 页面交互原型

直接用浏览器打开 `lifi-ai.html`，它包含全部样式、脚本和 Lifi 标识，无需网络或安装依赖。

源文件入口为 `index.html`。需要本地服务时：

```sh
python3 -m http.server 8765 --bind 127.0.0.1 --directory design/lifi-ai-prototype
```

修改模块后执行 `python3 design/lifi-ai-prototype/export.py` 更新单文件版本。

## 体验

- 右侧六个场景：初次见面、读取审批、任务、习惯、持续处理、失败重试。
- 快捷示例先填入输入框，由用户发送；Enter 发送，Shift+Enter 换行。
- 任务与习惯卡可编辑、确认或拒绝；读取审批后生成安排建议。
- 处理中点击发送按钮内的方块可停止；切换底部导航再返回可继续。
- 顶部历史按钮查看只读记录；新对话归档上一段；输入区上方管理记忆。
- 顶部切换浅深色和手机/宽屏预览。窄屏隐藏设计说明与场景控制，保留完整应用布局。

## 范围

这是一份设计模拟，所有内容仅存在于本次页面内存；刷新恢复示例。没有连接真实 AI、任务仓库、日历或用户记忆。历史与记忆是 Android 补齐提案。任务之外的主导航仅用于演示离开/返回 Lifi。

CSS 表达柔和玻璃材质意图；原生实现应复用 Deadliner 的 SoftGlassSurface、渐进顶栏和悬浮导航。

设计依据与后续模块方案位于 `specs/lifi-ai-redesign/`。
