#!/usr/bin/env python3
"""Generate Deadliner's Material Symbols resources.

The default pipeline writes tiny XML descriptors backed by the checked-in
official Material Symbols Rounded variable font, then refreshes the Kotlin
registry used by Compose. Pass `--legacy-svg` only to recover the retired SVG
pipeline for forensic comparisons.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import html
import http.client
import json
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MAP_PATH = ROOT / "scripts" / "material_symbols_map.json"
DRAWABLE_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable"
REGISTRY_PATH = ROOT / "app" / "src" / "main" / "java" / "com" / "aritxonly" / "deadliner" / "ui" / "MaterialSymbolRegistry.kt"
SVG_URL = "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/{symbol}/{variation}/24px.svg"

# Material Symbols' iOS back arrow has unusually generous bounds.  Keep this
# adjustment in the generator so every weight and future regeneration has the
# same visual size and optical alignment. Values are in vector viewport units.
SYMBOL_TRANSFORMS = {
    "arrow_back_ios_new": {"scale": 0.92, "translate_x": -30.0},
}


def resource_stem(symbol: str, filled: bool, weight: int) -> str:
    return f"msr_{symbol}{'_fill' if filled else ''}_wght{weight}"


def download_svg(symbol: str, filled: bool, weight: int) -> tuple[str, bool, int, str]:
    # Google Fonts calls its 400-weight outline "default". Other weights use
    # wghtNNN, and the fill suffix is concatenated without a separator.
    variation = "default" if weight == 400 and not filled else (
        "fill1" if weight == 400 else f"wght{weight}{'fill1' if filled else ''}"
    )
    request = urllib.request.Request(
        SVG_URL.format(symbol=symbol, variation=variation),
        headers={"User-Agent": "Deadliner Material Symbols generator"},
    )
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return symbol, filled, weight, response.read().decode("utf-8")
        except urllib.error.HTTPError:
            raise
        except (urllib.error.URLError, TimeoutError, OSError, http.client.HTTPException) as error:
            last_error = error
            time.sleep(0.4 * (attempt + 1))
    assert last_error is not None
    raise last_error


def svg_to_vector(svg: str, symbol: str) -> str:
    view_box = re.search(r'viewBox="([^"]+)"', svg)
    if not view_box:
        raise ValueError("SVG has no viewBox")
    min_x, min_y, width, height = (float(value) for value in view_box.group(1).split())
    paths = re.findall(r"<path\s+([^>]*?)\/>", svg)
    if not paths:
        raise ValueError("SVG has no paths")

    paths_data = []
    for attrs in paths:
        data = re.search(r'd="([^"]+)"', attrs)
        if not data:
            raise ValueError("SVG path has no path data")
        fill_rule = ' android:fillType="evenOdd"' if 'fill-rule="evenodd"' in attrs else ""
        paths_data.append((html.escape(data.group(1)), fill_rule))

    translate_x = -min_x
    translate_y = -min_y
    group_start = ""
    group_end = ""
    transform = SYMBOL_TRANSFORMS.get(symbol)
    if transform:
        scale = transform["scale"]
        adjusted_x = translate_x + transform.get("translate_x", 0.0)
        pivot_x = min_x + width / 2
        pivot_y = min_y + height / 2
        group_start = (
            f'  <group android:translateX="{adjusted_x:g}" android:translateY="{translate_y:g}">\n'
            f'    <group android:pivotX="{pivot_x:g}" android:pivotY="{pivot_y:g}" '
            f'android:scaleX="{scale:g}" android:scaleY="{scale:g}">'
        )
        group_end = "\n    </group>\n  </group>"
        path_indent = "      "
    elif translate_x or translate_y:
        group_start = (
            f'  <group android:translateX="{translate_x:g}" android:translateY="{translate_y:g}">'
        )
        group_end = "\n  </group>"
        path_indent = "    "
    else:
        path_indent = "  "
    path_xml = [
        f'{path_indent}<path android:fillColor="#FFFFFFFF" android:pathData="{path_data}"{fill_rule} />'
        for path_data, fill_rule in paths_data
    ]

    return "\n".join(
        [
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
            '    android:width="24dp"',
            '    android:height="24dp"',
            f'    android:viewportWidth="{width:g}"',
            f'    android:viewportHeight="{height:g}"',
            '    android:tint="?attr/colorControlNormal"',
            '    android:autoMirrored="true">',
            group_start.rstrip(),
            *path_xml,
            group_end,
            '</vector>',
            '',
        ]
    ).replace("\n\n\n", "\n")


def write_registry(icons: dict[str, dict]) -> None:
    entries = []
    definitions: dict[tuple[str, bool], dict] = {}
    for definition in icons.values():
        key = (definition["symbol"], bool(definition.get("fill")))
        existing = definitions.setdefault(key, definition)
        if existing.get("modern_symbol") != definition.get("modern_symbol"):
            raise ValueError(f"Conflicting modern symbols for {key}")
    for (symbol, filled), definition in sorted(definitions.items()):
        args = [f'"{definition["symbol"]}"']
        if filled:
            args.append("filled = true")
        if definition.get("modern_symbol"):
            args.append(f'modernName = "{definition["modern_symbol"]}"')
        resource = resource_stem(symbol, filled, 400)
        entries.append(f"        R.drawable.{resource} to MaterialSymbolDefinition({', '.join(args)}),")
    content = """package com.aritxonly.deadliner.ui

import androidx.annotation.DrawableRes
import com.aritxonly.deadliner.R

/** Generated by scripts/generate_material_symbols.py; do not edit by hand. */
internal data class MaterialSymbolDefinition(
    val name: String,
    val filled: Boolean = false,
    val modernName: String? = null,
)

internal object MaterialSymbolRegistry {
    private val definitions = mapOf(
%s
    )

    fun definitionFor(@DrawableRes sourceId: Int): MaterialSymbolDefinition? = definitions[sourceId]
}
""" % "\n".join(entries)
    REGISTRY_PATH.parent.mkdir(parents=True, exist_ok=True)
    REGISTRY_PATH.write_text(content)


def migrate_legacy_references(icons: dict[str, dict]) -> None:
    replacements = {
        resource: resource_stem(definition["symbol"], bool(definition.get("fill")), 400)
        for resource, definition in icons.items()
    }
    for path in (ROOT / "app" / "src").rglob("*"):
        if not path.is_file() or path.suffix not in {".kt", ".java", ".xml"}:
            continue
        original = path.read_text()
        updated = original
        # A resource can be a prefix of another one (ic_add / ic_add_ddl_tile),
        # so replacement must always prefer the longest legacy name.
        for legacy, generated in sorted(replacements.items(), key=lambda item: len(item[0]), reverse=True):
            updated = updated.replace(f"R.drawable.{legacy}", f"R.drawable.{generated}")
            updated = updated.replace(f"@drawable/{legacy}", f"@drawable/{generated}")
        if updated != original:
            path.write_text(updated)
    for resource in replacements:
        legacy_path = DRAWABLE_DIR / f"{resource}.xml"
        if legacy_path.exists():
            legacy_path.unlink()


def repair_migrated_references(icons: dict[str, dict]) -> None:
    """Repair aliases produced by the pre-2026-08-27 prefix-order bug."""
    replacements = {
        resource: resource_stem(definition["symbol"], bool(definition.get("fill")), 400)
        for resource, definition in icons.items()
    }
    repairs: dict[str, str] = {}
    for legacy, generated in replacements.items():
        malformed = legacy
        for earlier, earlier_generated in replacements.items():
            malformed = malformed.replace(earlier, earlier_generated)
        if malformed != generated:
            repairs[malformed] = generated
    for path in (ROOT / "app" / "src").rglob("*"):
        if not path.is_file() or path.suffix not in {".kt", ".java", ".xml"}:
            continue
        original = path.read_text()
        updated = original
        for malformed, generated in sorted(repairs.items(), key=lambda item: len(item[0]), reverse=True):
            updated = updated.replace(f"R.drawable.{malformed}", f"R.drawable.{generated}")
            updated = updated.replace(f"@drawable/{malformed}", f"@drawable/{generated}")
        if updated != original:
            path.write_text(updated)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--weights", type=int, nargs="+", help="override the weights in the mapping file")
    parser.add_argument("--legacy-svg", action="store_true", help="use the retired per-symbol SVG pipeline")
    parser.add_argument("--jobs", type=int, default=2, help="parallel downloads (default: 2)")
    parser.add_argument("--clean", action="store_true", help="remove previously generated msr_*.xml before generating")
    parser.add_argument("--prune", action="store_true", help="remove obsolete generated msr_*.xml after a successful run")
    parser.add_argument(
        "--refresh-symbol",
        action="append",
        default=[],
        metavar="SYMBOL",
        help="redownload and regenerate one symbol even if its resources already exist (repeatable)",
    )
    parser.add_argument("--migrate-legacy-references", action="store_true", help="replace mapped ic_* references and delete their aliases")
    parser.add_argument("--repair-migrated-references", action="store_true", help="repair aliases from an older prefix-order migration")
    args = parser.parse_args()
    if not args.legacy_svg:
        from generate_material_symbol_drawables import main as generate_font_drawables

        generate_font_drawables()
        return 0
    manifest = json.loads(MAP_PATH.read_text())
    icons: dict[str, dict] = manifest["icons"]
    weights: list[int] = args.weights or manifest["weights"]

    combinations = {
        (definition.get("modern_symbol", definition["symbol"]), bool(definition.get("fill")), weight)
        for definition in icons.values()
        for weight in weights
    } | {
        (definition["symbol"], bool(definition.get("fill")), weight)
        for definition in icons.values()
        for weight in weights
    }
    if args.clean:
        for path in DRAWABLE_DIR.glob("msr_*.xml"):
            path.unlink()

    downloaded: dict[tuple[str, bool, int], str] = {}
    refresh_symbols = set(args.refresh_symbol)
    pending_combinations = {
        combo for combo in combinations
        if combo[0] in refresh_symbols or not (DRAWABLE_DIR / f"{resource_stem(*combo)}.xml").is_file()
    }
    errors: list[str] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.jobs) as executor:
        pending = {executor.submit(download_svg, *combo): combo for combo in pending_combinations}
        for future in concurrent.futures.as_completed(pending):
            combo = pending[future]
            try:
                symbol, filled, weight, svg = future.result()
                vector = svg_to_vector(svg, symbol)
                downloaded[symbol, filled, weight] = vector
                (DRAWABLE_DIR / f"{resource_stem(symbol, filled, weight)}.xml").write_text(vector)
            except (urllib.error.URLError, ValueError, TimeoutError) as error:
                errors.append(f"{combo}: {error}")

    if errors:
        print("Failed to download/convert Material Symbols:\n" + "\n".join(errors), file=sys.stderr)
        return 1

    if args.prune:
        expected = {
            DRAWABLE_DIR / f"{resource_stem(*combo)}.xml"
            for combo in combinations
        }
        for path in DRAWABLE_DIR.glob("msr_*.xml"):
            if path not in expected:
                path.unlink()

    write_registry(icons)
    if args.migrate_legacy_references:
        migrate_legacy_references(icons)
    if args.repair_migrated_references:
        repair_migrated_references(icons)
    print(f"Generated {len(downloaded)} Material Symbols variants for {len(icons)} mapped resources.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
