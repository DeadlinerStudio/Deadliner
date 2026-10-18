# Hybrid MIUIX icon sources

- MIUIX Glass icons (Copyright 2026 compose-miuix-ui contributors): compose-miuix-ui/miuix PR 423, lingqiqi5211/miuix commit `ec4c96963abf206ab2f2b0253a5efca71ba28043`, Apache-2.0. Only Regular (330) paths are extracted without shape changes. SVG translation/vertical flip reproduces the original ImageVector groups.
- MIUIX Regular Home / Add / Update (Copyright 2025 compose-miuix-ui contributors): miuix-icons-android 0.9.4-rc01 Regular, Apache-2.0.
- Phosphor Icons Fill / Regular: phosphor-icons/core commit `2b75f3ad12b420c9504ef05df8d2564a28f8500e`, MIT. Original fill paths and negative-space proportions are retained.
- The local `slash.svg` is a simple rounded exclusion slash. Composite entries in scripts/interface_icon_map.json combine pinned Phosphor paths; plus and exclamation marks reuse the inner contour of Fill's square-enclosed mark as a positive mark. Explicit clip geometry makes these marks readable against filled carriers. Composites are local adaptations, not upstream standalone icons.
- material-reference SVGs are rendered from the application's existing Material Symbols Rounded variable font at wght=330, opsz=24, GRAD=0 and each key's FILL state. These are review references only; they are not a third runtime icon pack. Existing Material Symbols license applies.

Runtime assets: app/src/main/res/drawable/hmi_*.xml. Source mapping and derivatives: scripts/interface_icon_map.json. Offline regeneration: python3 scripts/generate_interface_icons.py. Source font reference regeneration on macOS: swift scripts/render_material_symbol_references.swift /absolute/path/to/repo.

The preference changes interface symbols; brand logos, illustrations and system-required notification silhouettes are intentionally outside this icon pack.

2026-10-17: MIUIX Regular Add (ic_miuix_home_add.xml) and Update are sourced from the same pinned 0.9.4-rc01 source archive as Home. Quick-add ExpandMore uses the pinned Glass PR version. ic_lifi_new_conversation.xml is an application-owned redraw from the user-provided screenshot, not an extracted APK resource. VoiceAssistProxy.apk contained no such asset.

Context variants: Glass ChevronBackward/ChevronForward Demibold (500), Glass ExpandMore Medium (430), extracted from the same fixed PR 423 revision without path changes. Generated ic_miuix_overview_previous/next and ic_miuix_quick_expand use these variants independently of the default Regular mapping.

Hybrid optical calibration: each Phosphor semantic icon has one fixed, application-owned opticalScale in scripts/interface_icon_map.json, shared by all scenes and Compose/XML. Regular variants retain pinned upstream paths; scaling is applied only to the generated derivative after composing semantic marks. MIUIX paths and prior context-specific 500/430 weights remain unchanged. Review: specs/hybrid-icon-optics/review/index.html.
