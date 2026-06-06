# Settings TopBar Blur Investigation

## Final Resolution

- [x] The bug is fixed.
- [x] The primary root cause was **background-source mismatch inside the backdrop capture chain**, not blur radius tuning and not the collapsible top bar behavior itself.
- [x] `useCurrentTopBarHeightForContentPadding` was investigated, but it was not the main cause of the rough / bright / mosaic blur quality difference.

### Final Root Cause

- [x] Known-good pages such as `Overview` use a blur chain where `layerBackdrop(backdrop)` records a subtree whose **real background is drawn by the page content itself**.
- [x] The problematic Settings path used an `AdaptiveMaterialScaffold` arrangement where the backdrop host and its background source were not equivalent to the known-good path.
- [x] In practice, this meant the Settings blur was sampling against a more synthetic / scaffold-owned base layer instead of the same content-owned background chain used by `Overview`.
- [x] That difference explains the observed symptoms:
  - [x] blur looked brighter than other pages,
  - [x] blur looked rougher / more blocky,
  - [x] saturated colors inside the blur band could collapse into mosaic-like patches.

### Fix Shape

- [x] Align the Settings top-bar blur path with the known-good content-owned background capture model.
- [x] Treat the backdrop host as a recorder of the page's real content/background subtree, instead of letting the scaffold layer manufacture the effective sampled base.

### Important Non-Cause

- [x] Dynamic top content padding (`useCurrentTopBarHeightForContentPadding`) can affect motion feel and whether content truly travels under the bar.
- [x] It was **not** the primary cause of the visual-quality mismatch discussed in this document.

## Goal

- [ ] Explain why the Settings top bar blur shows background content as if it were horizontally stretched / zoomed.
- [ ] Keep the existing collapse / snap / content padding behavior intact while debugging.
- [ ] Avoid changing blur parameters unless they are only used as temporary probes.

## Known Facts

- [x] The bug appears in Settings pages.
- [x] The collapse behavior itself is correct.
- [x] Content layout itself is correct.
- [x] The problem is inside the blurred representation of the background content.
- [x] The issue exists in both Material3 and MIUIX Settings pages.
- [x] The issue gets more obvious as `blurRadius` increases.
- [x] Main pages using the same advanced material blur do **not** show the same problem.

## Historical Hypotheses

### H1. Backdrop is sampling the wrong layer

- [x] The blur may be sampling an upstream full-width scroll layer instead of the final inset / clipped Settings content result.
- [ ] Expected symptom if true:
  - [x] In the blur area, section backgrounds appear to ignore horizontal padding.
  - [x] Text may collapse into rectangular color blocks because the sampled source is larger than the final rendered content area.

### H2. Settings content tree differs from Main content tree in a way that matters to `layerBackdrop`

- [x] Main pages may provide a more stable "final viewport" layer before blur sampling.
- [x] Settings pages may still expose a broader content layer to the backdrop.

### H3. Large TopAppBar collapse layout interacts badly with blurred backdrop sampling

- [ ] The issue may come from how the collapsible large top bar overlays the sampled content, not from the color logic.
- [ ] This remains possible because the bug is specific to Settings, where `CollapsingTopBarScaffold` is used.

## Hypotheses We Mostly Ruled Out

- [x] Blur parameter tuning alone is the root cause.
- [x] Tint color choice alone is the root cause.
- [x] Top bar container color logic alone is the root cause.
- [x] Simply moving horizontal padding around without knowing the sampled layer explains the bug.
- [x] Simply unifying the blur container implementation explains the bug.
- [x] Simply adding a list-level clip explains the bug.
- [x] Simply adding an offscreen-composited viewport on the Settings scroll container explains the bug.

## Diagnostic Strategy

### Experiment A. Layer tint tracing

Purpose:
- [x] Identify which content layer the top bar blur is actually sampling.

Method:
- [x] Temporarily assign very distinct debug background colors to several nested Settings layers.
- [x] Observe which color first appears inside the top bar blur when content crosses below it.

Suggested layers to tint independently:
- [x] The outer Settings scroll container.
- [ ] The immediate Settings content column / lazy list content area.
- [x] `SettingsSection` outer wrapper.
- [x] `SettingsSection` `Surface`.
- [x] A child row / item container inside `SettingsSection`.

Observed result:
- [x] Red appeared first: the outer Settings scroll container is sampled first.
- [x] Purple appeared immediately after slight scroll: the blur mixes the scroll container with the `SettingsSection` outer wrapper.
- [x] Yellow appeared only later: item rows are sampled after the outer layers.
- [x] Green did not dominate first: the `SettingsSection` `Surface` is not the earliest primary sampled layer.

Expected interpretations:
- [x] If the blur shows the outer full-width debug color first, then backdrop is sampling too high in the tree.
- [ ] If the blur shows `SettingsSection` surface color but still ignores side padding, then clipping / viewport composition is suspect.
- [ ] If the blur shows the innermost item layer directly, then we should inspect item-level composition and scroll container interaction.

### Experiment B. Compare Main and Settings at equivalent hierarchy points

Purpose:
- [x] Verify whether Main pages provide a more final, clipped viewport result to the backdrop.

Checklist:
- [x] Compare where `layerBackdrop(backdrop)` sits in Main vs Settings.
- [x] Compare where horizontal insets are applied in Main vs Settings.
- [x] Compare where clipping is applied in Main vs Settings.
- [x] Compare whether the first visible content under the top bar belongs to the same composited layer in both cases.

Findings:
- [x] Main pages usually expose a list-level viewport result before blur sampling.
- [x] Settings pages expose outer scroll container content before the `SettingsSection` final card result becomes dominant in blur.
- [x] Unifying the blur container implementation did not change the bug.
- [x] Moving Settings horizontal insets to the scroll container did not change the bug.
- [x] Adding a Settings list-level clip did not change the bug.
- [x] Adding an offscreen-composited Settings viewport did not change the bug.

### Experiment C. Collapse-specific interaction

Purpose:
- [ ] Verify whether the bug is tied to `CollapsingTopBarScaffold` itself.

Checklist:
- [ ] Reproduce on multiple Settings screens using the same scaffold.
- [ ] Check whether a non-section content block shows the same issue.
- [ ] Check whether the first `SettingsSection` behaves differently from lower sections.

### Experiment D. Backdrop hook-point isolation

Purpose:
- [ ] Verify whether the problem comes from where `layerBackdrop(backdrop)` is attached, rather than from the top bar blur container or the Settings viewport itself.

Checklist:
- [ ] Compare the exact `layerBackdrop(backdrop)` hook point used by Settings vs a known-good page.
- [ ] Check whether Settings blur still samples the outer scroll container when a more local content subtree owns the backdrop.
- [ ] Avoid touching collapse, snap, or top padding behavior while testing this.

Findings:
- [x] `LayerBackdrop` records the target node's `drawContent()` result into a `GraphicsLayer`; it is not a whole-screen snapshot.
- [x] Moving the Settings `layerBackdrop(backdrop)` hook point from the outer scaffold content box down onto `SettingsScrollColumn` / `SettingsLazyColumn` did not change the bug.
- [x] This makes it less likely that the problem is only "the hook point is one container too high".

### Experiment E. Per-section composition isolation

Purpose:
- [ ] Verify whether each `SettingsSection` needs to become its own final composited result before the parent backdrop records it.

Checklist:
- [ ] Apply offscreen composition only to `SettingsSection`, not the whole Settings viewport.
- [ ] Keep collapse, snap, and top padding behavior unchanged.
- [ ] Check whether the blur starts seeing the section's final card result instead of the wrapper-first chain.

Findings:
- [x] Applying offscreen composition to `SettingsSection` did not change the bug.
- [x] Replacing `SettingsSection`'s `Surface` with a simpler `clip + background + Column` implementation also did not change the bug.
- [x] This makes it less likely that the issue is caused by the section card container implementation itself.

### Experiment F. Item content simplification

Purpose:
- [ ] Verify whether the bug is tied to the shared `SettingItem` / Material `ListItem` content path.

Checklist:
- [ ] Replace the first visible Settings item with a minimal hand-written `Row + Text + Icon`.
- [ ] Keep the surrounding `SettingsSection`, top bar, blur parameters, and scroll container unchanged.

Findings:
- [x] Replacing the first visible `SettingItem` with a minimal hand-written row did not change the bug.
- [x] This makes it less likely that `SettingItem` / `ListItem` is the root cause.

### Experiment G. Diagnostic ownership and host comparison

Purpose:
- [ ] Verify whether the rough / mosaic blur quality is caused by blur-container ownership or by the Settings runtime environment.

Checklist:
- [x] Build an A/B diagnostic inside Settings using identical backdrop content.
- [x] Compare a built-in top bar material container against an external blur container with the same backdrop.
- [x] Temporarily align `SettingsActivity` host conditions more closely with known-good pages:
  - [x] Switch from `DeadlinerAppCompatActivity` to `DeadlinerComponentActivity`.
  - [x] Add an explicit `Theme.Deadliner` declaration in the manifest.
- [x] Extract the diagnostic into a shared composable so it can be rendered in both Settings and a known-good page (`Overview`).

Findings:
- [x] `Built-in container` and `External container` looked equally rough inside Settings.
- [x] Switching `SettingsActivity` to `DeadlinerComponentActivity` did not improve blur quality.
- [x] Explicitly applying `Theme.Deadliner` to `SettingsActivity` did not improve blur quality.
- [x] The diagnostic is now shared and rendered in both Settings and `Overview` for a cross-host comparison.
- [x] Rendering `MainSettingsScreen` inside `OverviewActivity` still produced the same rough blur quality as `SettingsActivity`.
- [x] This strongly suggests the problem follows the Settings screen subtree, not the activity host.

## Result Checklist

### If Experiment A shows the outer scroll container color

- [x] Conclusion: backdrop sampling is too high in the content tree.
- [ ] Next step: move or isolate the sampled layer without changing collapse logic.

### If Experiment A shows the `SettingsSection` surface color directly

- [ ] Conclusion: sampled layer is closer to correct, but viewport clipping / inset preservation is still wrong.
- [ ] Next step: inspect how the section enters composition relative to the top bar overlay.

### If Experiment A shows item-level colors but blur still looks stretched

- [ ] Conclusion: issue is likely in how the sampled layer is composed or transformed before blur, not just which node is sampled.
- [ ] Next step: inspect `CollapsingTopBarScaffold` + `AdaptiveMaterialScaffold` interaction more closely.

## Guardrails

- [ ] Do not change collapse behavior while debugging this issue.
- [ ] Do not change content padding semantics while debugging this issue.
- [ ] Do not change `blurRadius`, `noiseCoefficient`, or blend parameters except as temporary probes.
- [ ] Keep all diagnostics reversible and localized.

## Final Takeaways

- [x] The bug was not caused by blur parameter tuning.
- [x] The bug was not caused by `SettingsSection`, `SettingItem`, `SettingsLazyColumn`, or `CollapsingTopBarScaffold` alone.
- [x] The bug was not explained by `Activity` host class or navigation host differences.
- [x] The decisive difference was the **background provenance inside the backdrop capture path**.
- [x] Future blur regressions should first compare:
  - [x] where `layerBackdrop(backdrop)` is attached,
  - [x] whether the backdrop host is transparent or manufactures its own base layer,
  - [x] whether the captured background is supplied by the real content subtree or by a scaffold/container wrapper.

## Quick Audit Notes

- [x] `OverviewActivity` is the known-good reference: it uses `AdaptiveMaterialScaffold(containerColor = Transparent)` and the visible page background is drawn by the real content subtree.
- [x] `AddDDLActivity` already follows the same safe pattern: transparent scaffold container, content subtree paints its own page background.
- [x] `Settings` was the page where this mismatch actually manifested and is now the reference case for this class of bug.
- [x] `CaptureScreen`, `WikiScreen`, `FeedbackScreen`, and `DeadlineDetailScreen` also use `AdaptiveMaterialScaffold`, but they did not reproduce the same quality issue during this investigation.
- [ ] If a similar blur-quality mismatch appears elsewhere in the future, audit those pages first for the same "scaffold-owned background vs content-owned background" difference before changing blur parameters.
