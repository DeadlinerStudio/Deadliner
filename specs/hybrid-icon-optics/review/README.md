# 校准审查

index.html 展示全部 86 项前后对照；detail-actions.png 对应用户截图中的四个底部操作。optics-1/2/3.png 上方 MIUIX 基准保持原样，每项左侧为原实现，右侧为最终校准。

每个 opticalScale 都是固定逐图标值，没有场景系数。Regular 是固定 Phosphor 上游原始路径；原始 SVG 在 third_party/interface-icons 中，不使用透明度改变重量。before/ 保存校准前 SVG 审查基线。

离线重现：python3 scripts/generate_interface_icons.py；python3 scripts/render_hybrid_icon_optics.py；python3 scripts/validate_hybrid_icon_optics.py。最后一个命令实际光栅化全部 86 个矢量并检查边界与别名/状态约束，不需要设备。
