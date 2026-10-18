# MIUIX glass experiment

Source: https://github.com/compose-miuix-ui/miuix/pull/423
Commit: ec4c96963abf206ab2f2b0253a5efca71ba28043
License: Apache-2.0, copyright headers retained.

Vendored material dependency closure from miuix-glass/src/commonMain, compiled against the app’s MIUIX 0.9.4-rc01. The unpublished PR is not consumed via an unpinned dependency. No navigation, popup, or icon components are forked.

Local adaptation: GlassStyles.kt is mechanically split into a public forwarding object and four internal token holders. Every token value is retained; no shader or optical constant is modified. upstream-sha256.json records original file hashes. The split is reproducible by grouping the 35 annotated GlassStyle properties in source order, ten per holder.

App integration lives in OfficialMiuixGlassMaterial.kt. Existing layout, backdrop capture, shadows, presses, and transitions remain app-owned. Disable the experimental preference to restore the current implementation. After an official artifact becomes available, remove this source directory and use the verified release.
