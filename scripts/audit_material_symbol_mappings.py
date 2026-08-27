#!/usr/bin/env python3
"""Audit Material Symbols mappings against the pre-refactor 400 vectors.

The input side is read straight from Git so it remains available after the
generated 400 resource has replaced the working-tree drawable. Each vector is
rendered to a transparent PNG and compared by ImageMagick. A score of 0 means
the old icon and the selected Material Symbols Rounded 400 icon are pixel
identical at the audit resolution.

Requirements: rsvg-convert and ImageMagick's compare command.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import tempfile
import xml.etree.ElementTree as element_tree
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MAP_PATH = ROOT / "scripts" / "material_symbols_map.json"
DRAWABLE_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable"
ANDROID = "{http://schemas.android.com/apk/res/android}"
DEFAULT_CATALOG = Path(
    os.environ.get("ANDROID_SDK_ROOT", os.environ.get("ANDROID_HOME", ""))
) / "icons" / "material" / "materialsymbolsrounded"


def git_vector(revision: str, resource: str) -> str | None:
    path = f"app/src/main/res/drawable/{resource}.xml"
    # A user may have staged an icon before this refactor. The index preserves
    # that exact baseline even after the generated working-tree XML replaces it.
    for source in (f":{path}", f"{revision}:{path}"):
        completed = subprocess.run(
            ["git", "show", source],
            cwd=ROOT,
            text=True,
            capture_output=True,
        )
        if completed.returncode == 0:
            return completed.stdout
    return None


def android_vector_to_svg(vector: str) -> str:
    root = element_tree.fromstring(vector)
    width = root.get(f"{ANDROID}viewportWidth")
    height = root.get(f"{ANDROID}viewportHeight")
    if not width or not height:
        raise ValueError("Vector has no viewport")

    def children(node: element_tree.Element) -> str:
        output: list[str] = []
        for child in node:
            tag = child.tag.rsplit("}", 1)[-1]
            if tag == "path":
                path_data = child.get(f"{ANDROID}pathData")
                if path_data:
                    fill_rule = "evenodd" if child.get(f"{ANDROID}fillType") == "evenOdd" else "nonzero"
                    output.append(f'<path d="{path_data}" fill="black" fill-rule="{fill_rule}"/>')
            elif tag == "group":
                transforms = []
                translate_x = child.get(f"{ANDROID}translateX")
                translate_y = child.get(f"{ANDROID}translateY")
                scale_x = child.get(f"{ANDROID}scaleX")
                scale_y = child.get(f"{ANDROID}scaleY")
                if translate_x or translate_y:
                    transforms.append(f"translate({translate_x or 0} {translate_y or 0})")
                if scale_x or scale_y:
                    transforms.append(f"scale({scale_x or 1} {scale_y or 1})")
                output.append(f'<g transform="{" ".join(transforms)}">{children(child)}</g>')
        return "".join(output)

    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" '
        f'width="{width}" height="{height}">{children(root)}</svg>'
    )


def canonical_path_data(vector: str) -> str:
    """Matches the Android Studio catalog's original Material Symbols paths."""
    return "\x1f".join(
        re.sub(r"[\s,]+", "", path)
        for path in re.findall(r'android:pathData="([^"]+)', vector)
    )


def catalog_index(catalog: Path) -> dict[str, list[str]]:
    if not catalog.is_dir():
        raise ValueError(
            f"Material Symbols catalog not found at {catalog}. "
            "Pass --catalog $ANDROID_SDK_ROOT/icons/material/materialsymbolsrounded."
        )
    index: dict[str, list[str]] = {}
    for path in catalog.glob("*/*_24.xml"):
        key = canonical_path_data(path.read_text())
        if key:
            index.setdefault(key, []).append(path.parent.name)
    return index


def render(svg: str, output: Path) -> None:
    subprocess.run(
        ["rsvg-convert", "--width", "480", "--height", "480", "--output", str(output)],
        input=svg,
        text=True,
        check=True,
    )


def difference(left: Path, right: Path) -> float:
    completed = subprocess.run(
        ["compare", "-metric", "RMSE", str(left), str(right), "null:"],
        text=True,
        capture_output=True,
    )
    match = re.search(r"\(([-+0-9.eE]+)\)", completed.stderr)
    if not match:
        raise RuntimeError(f"Could not read ImageMagick output: {completed.stderr}")
    return float(match.group(1))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--revision", default="HEAD", help="Git revision containing original ic_*.xml files")
    parser.add_argument("--max-score", type=float, default=0.005, help="largest score considered shape-equivalent")
    parser.add_argument("--catalog", type=Path, default=DEFAULT_CATALOG, help="Android SDK Material Symbols Rounded catalog")
    parser.add_argument("--discover", action="store_true", help="find exact 400-path candidates in the SDK catalog")
    args = parser.parse_args()
    manifest = json.loads(MAP_PATH.read_text())
    if args.discover:
        index = catalog_index(args.catalog)
        for resource in manifest["icons"]:
            source = git_vector(args.revision, resource)
            candidates = index.get(canonical_path_data(source), []) if source else []
            verdict = "EXACT" if candidates else "NO_EXACT"
            print(f"{verdict}\t{resource}\t{'|'.join(candidates) or '-'}")
        return 0

    results: list[tuple[float, str, str]] = []
    skipped: list[str] = []

    with tempfile.TemporaryDirectory(prefix="deadliner-symbol-audit-") as directory:
        temporary = Path(directory)
        for resource, definition in manifest["icons"].items():
            source = git_vector(args.revision, resource)
            if source is None:
                skipped.append(resource)
                continue
            target = (DRAWABLE_DIR / f"msr_{definition['symbol']}{'_fill' if definition.get('fill') else ''}_wght400.xml").read_text()
            old_png = temporary / f"{resource}-old.png"
            new_png = temporary / f"{resource}-new.png"
            render(android_vector_to_svg(source), old_png)
            render(android_vector_to_svg(target), new_png)
            results.append((difference(old_png, new_png), resource, definition["symbol"]))

    for score, resource, symbol in sorted(results, reverse=True):
        verdict = "MATCH" if score <= args.max_score else "REVIEW"
        print(f"{verdict}\t{score:.6f}\t{resource}\t{symbol}")
    for resource in skipped:
        print(f"NO_BASELINE\t-\t{resource}\t-")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
