#!/usr/bin/env python3
"""Generate XML Drawable descriptors backed by Material Symbols Rounded font."""

from __future__ import annotations

import json
from pathlib import Path

from generate_material_symbols import write_registry


ROOT = Path(__file__).resolve().parents[1]
MAP_PATH = ROOT / "scripts" / "material_symbols_map.json"
DRAWABLE_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable"


def resource_stem(symbol: str, filled: bool) -> str:
    return f"msr_{symbol}{'_fill' if filled else ''}_wght400"


def descriptor(symbol: str, filled: bool) -> str:
    return f'''<?xml version="1.0" encoding="utf-8"?>
<com.aritxonly.deadliner.ui.MaterialSymbolDrawable
    xmlns:app="http://schemas.android.com/apk/res-auto"
    app:symbolName="{symbol}"
    app:symbolFilled="{str(filled).lower()}" />
'''


def main() -> None:
    manifest = json.loads(MAP_PATH.read_text())
    icons: dict[str, dict] = manifest["icons"]
    expected = {
        resource_stem(definition["symbol"], bool(definition.get("fill"))): definition
        for definition in icons.values()
    }
    for path in DRAWABLE_DIR.glob("msr_*.xml"):
        if path.stem not in expected:
            path.unlink()
    for stem, definition in expected.items():
        (DRAWABLE_DIR / f"{stem}.xml").write_text(
            descriptor(definition["symbol"], bool(definition.get("fill")))
        )
    write_registry(icons)
    print(f"Wrote {len(expected)} Material Symbol font drawables.")


if __name__ == "__main__":
    main()
