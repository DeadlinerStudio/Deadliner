#!/usr/bin/env python3
"""Offline optical review: fixed per-icon scales, untouched MIUIX anchors."""
import json, html, subprocess
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
REVIEW = ROOT / 'specs/hybrid-icon-optics/review'
CURRENT = ROOT / 'specs/interface-icon-pack/review'
ROWS = json.loads((ROOT / 'scripts/interface_icon_map.json').read_text())['icons']

def glyph(path, x, y, size=52):
    root = ET.fromstring(path.read_text())
    root.set('x', str(x)); root.set('y', str(y))
    root.set('width', str(size)); root.set('height', str(size))
    return ET.tostring(root, encoding='unicode')

def save_svg(name, width, height, contents):
    source = f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}"><rect width="100%" height="100%" fill="#ffffff"/>' + ''.join(contents) + '</svg>'
    path = REVIEW / (name + '.svg'); path.write_text(source)
    subprocess.run(['rsvg-convert', '-o', str(path.with_suffix('.png')), str(path)], check=True)

def label(x, y, text, size=12):
    return f'<text x="{x}" y="{y}" font-family="sans-serif" font-size="{size}" fill="#222">{html.escape(text)}</text>'

keys = [k for k, r in ROWS.items() if r['family'].startswith('phosphor-')]
anchors = ['undo', 'delete', 'event', 'add', 'star', 'close']
for offset in range(0, len(keys), 36):
    parts = [label(16, 20, 'MIUIX reference (unchanged) / Phosphor BEFORE -> AFTER', 16)]
    for i, key in enumerate(anchors):
        parts += [glyph(CURRENT / ('hmi_' + key + '.svg'), 44 + i*195, 36), label(106+i*195, 67, key)]
    for i, key in enumerate(keys[offset:offset+36]):
        x=(i%6)*195+15; y=(i//6)*135+116; stem=key.replace('#','_')
        row=ROWS[key]
        parts += [label(x, y, key), glyph(REVIEW/'before'/(stem+'.svg'), x+8, y+12),
                  glyph(CURRENT/('hmi_'+stem+'.svg'), x+106, y+12),
                  label(x, y+86, f"x{row['opticalScale']:.2f} / {row['family'].removeprefix('phosphor-')}"),
                  label(x, y+104, row['icon'], 10)]
    save_svg('optics-'+str(offset//36+1), 1185, 930, parts)
# Concrete comparison matching the user's task-detail action bar.
parts=[label(18, 24, 'Task detail actions: before / after', 18)]
for row, y in enumerate([54, 150]):
    parts.append(label(18, y+38, 'Before' if row==0 else 'After', 15))
    for i, key in enumerate(['undo','move_to_inbox','delete','event']):
        path = REVIEW/'before'/(key+'.svg') if row==0 and key=='move_to_inbox' else CURRENT/('hmi_'+key+'.svg')
        parts += [glyph(path, 120+i*120, y, 56), label(112+i*120, y+78, key, 10)]
save_svg('detail-actions', 630, 255, parts)
page='''<!doctype html><meta charset="utf-8"><title>Hybrid icon optical review</title>
<style>body{font:15px system-ui;margin:24px;background:#f5f5f7;color:#222}img{display:block;max-width:100%;margin:16px 0;background:white;border-radius:16px}article{background:white;padding:12px;margin:8px 0;border-radius:12px}</style>
<h1>Hybrid MIUIX 逐图标光学校准</h1><p>MIUIX 保持原样；Phosphor 逐项缩放，所有场景共用。左侧为原实现，右侧为校准后；Regular 是固定上游字重路径，不使用透明度模拟。</p><img src="detail-actions.png">'''
for i in range((len(keys)+35)//36): page += f'<img src="optics-{i+1}.png">'
for key in keys:
    r=ROWS[key];page+=f'<article><b>{html.escape(key)}</b> ×{r["opticalScale"]:.2f} / {r["family"]}<p>{html.escape(r["opticalReview"])}</p></article>'
(REVIEW/'index.html').write_text(page)
print('Reviewed', len(keys), 'Phosphor mappings against unchanged MIUIX anchors')
