# 总览看板 · 定版开发包 v1.0

2026-10-13定版，供iOS/鸿蒙接续开发；Android为参考实现。

| 文件 | 用途 |
| --- | --- |
| [spec.md](spec.md) | 最终PRD；页面层级、月份规则、数据口径、AI持久化与验收标准 |
| [android-reference-trd.md](android-reference-trd.md) | Android实际模块/API、Core合同、生命周期、AI缓存和跨端接入 |
| [ui-design.md](ui-design.md) | 最终布局、字号、颜色、图表尺寸与系统栏行为 |
| [module-plan.md](module-plan.md) | 职责拆分与平台映射 |
| [tasks.md](tasks.md) | 实现与验证记录 |

产品PRD目录另附prototype.html独立原型、prototype.fragment.html可编辑源；ui-design.md在该目录保存为android-ui-reference.md。原型的数字/记录/AI/日期均为示例，示例日期为2026-10-12；真机使用设备当前本月。先读PRD再读TRD，冲突时以本版产品合同为准；历史版本已放入history/，不作现行要求。

范围已冻结：总览月历与双管理卡、任务统计卡、放大的趋势预览、完整上月AI洞察及独立二级页；不启动iOS/鸿蒙实现，不新增习惯总览。Android编译与单测通过情况见tasks.md，未完成的实机视觉验收不能从原型或构建结果推断。
