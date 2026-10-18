#!/usr/bin/env python3
"""Generate reviewed Hybrid assets and a visual audit from pinned, offline source SVGs.
Import downloaded upstream Kotlin/SVG using --import-sources DIR once; normal runs are offline.
"""
import argparse, copy, html, json, re, shutil, subprocess
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'third_party/interface-icons'
ANDROID = '{http://schemas.android.com/apk/res/android}'
SVG = '{http://www.w3.org/2000/svg}'
DIRECTIONAL = {'arrow_back','arrow_back_ios_new','chevron_left','chevron_right','undo','send','side_navigation'}
COMMANDS = {'MoveTo':'M','LineTo':'L','HorizontalTo':'H','VerticalTo':'V','QuadTo':'Q',
            'CurveTo':'C','ReflectiveQuadTo':'T','ReflectiveCurveTo':'S','ArcTo':'A','Close':'Z'}

CONTEXT_VARIANTS = {
    'ic_miuix_overview_previous': ('ChevronBackward', 'Demibold'),
    'ic_miuix_overview_next': ('ChevronForward', 'Demibold'),
    'ic_miuix_quick_expand': ('ExpandMore', 'Medium'),
}

def miuix_svg(text, name, weight='Regular'):
    start = re.search(r'val MiuixIcons\.(?:Glass\.)?' + weight + r'\.' + name + r': ImageVector', text)
    assert start, name
    block = text[start.start():].split('\nprivate var ', 1)[0]
    w = re.search(r'viewportWidth = ([\d.]+)f', block)[1]
    h = re.search(r'viewportHeight = ([\d.]+)f', block)[1]
    root = ET.Element('svg', xmlns='http://www.w3.org/2000/svg', viewBox=f'0 0 {w} {h}')
    groups = re.findall(r'group\((.*?)\)\s*\{', block, re.S)
    assert len(groups) <= 1, name
    attrs = dict(re.findall(r'(scaleX|scaleY|translationX|translationY) = (-?[\d.]+)f', groups[0] if groups else ""))
    group = ET.SubElement(root, 'g', transform=f"translate({attrs.get('translationX','0')} {attrs.get('translationY','0')}) scale({attrs.get('scaleX','1')} {attrs.get('scaleY','1')})")
    paths = re.findall(r'pathData = listOf\((.*?)\),\s*fill = SolidColor\(Color.Black\),\s*fillAlpha = ([\d.]+)f?,\s*pathFillType = PathFillType.(\w+)', block, re.S)
    assert paths, name
    for nodes, alpha, rule in paths:
        data = []
        for node, args in re.findall(r'PathNode\.(\w+)(?:\(([^)]*)\))?', nodes):
            relative = node.startswith('Relative')
            command = COMMANDS[node.removeprefix('Relative')]
            values = args.replace('f','').replace('true','1').replace('false','0').replace(',',' ')
            data.append((command.lower() if relative else command) + ' ' + values)
        ET.SubElement(group, 'path', d=' '.join(data), fill='black', opacity=alpha,
                      **{'fill-rule':'evenodd' if rule == 'EvenOdd' else 'nonzero'})
    return '<!-- Copyright 2026 compose-miuix-ui contributors; SPDX-License-Identifier: Apache-2.0 -->\n' + ET.tostring(root, encoding='unicode')

def vector_xml(root, mirrored, family):
    viewport = root.attrib.get('viewBox','0 0 256 256').split()
    vector = ET.Element('vector', {'xmlns:android':'http://schemas.android.com/apk/res/android',
        'android:width':'24dp','android:height':'24dp','android:viewportWidth':viewport[2],
        'android:viewportHeight':viewport[3], 'android:autoMirrored':str(mirrored).lower()})
    clips = {n.attrib['id']: n for n in root.iter() if n.tag.rsplit('}',1)[-1] == 'clipPath'}
    def append(node, parent):
        tag = node.tag.rsplit('}',1)[-1]
        if tag == 'g':
            attrs = {}
            transform = node.attrib.get('transform','')
            if transform:
                trans = re.search(r'translate\(([-\d.]+)[ ,]+([-\d.]+)\)',transform)
                scale = re.search(r'scale\(([-\d.]+)[ ,]+([-\d.]+)\)',transform)
                if trans:attrs.update({'android:translateX':trans[1],'android:translateY':trans[2]})
                if scale:attrs.update({'android:scaleX':scale[1],'android:scaleY':scale[2]})
            group = ET.SubElement(parent,'group',attrs)
            clip = node.get('clip-path')
            if clip:
                clip_id = re.search(r'#([^)]*)',clip)[1]
                path = list(clips[clip_id])[0]
                ET.SubElement(group,'clip-path',{'android:pathData':path.attrib['d'], 'android:fillType':'evenOdd'})
            for child in node:append(child,group)
        elif tag == 'path':
            if node.get('fill') == 'none':return
            attrs = {'android:pathData':node.attrib['d'],'android:fillColor':'#FF000000',
                     'android:fillType':'evenOdd' if node.get('fill-rule') == 'evenodd' else 'nonZero'}
            if node.get('opacity'):attrs['android:fillAlpha']=node.get('opacity')
            ET.SubElement(parent,'path',attrs)
        elif tag in ('title','desc','defs'):pass
        else:raise ValueError(f'Unsupported SVG element: {tag}')
    for child in root:append(child, vector)
    ET.indent(vector,space='    ')
    notice = 'MIUIX Apache-2.0' if family.startswith('miuix') else 'Phosphor Icons MIT'
    copyright = ('Copyright ' + ('2025' if family == 'miuix-regular' else '2026') + ' compose-miuix-ui contributors; ') if family.startswith('miuix') else ''
    return f'<!-- {copyright}Generated from pinned {notice} source; see third_party/interface-icons/NOTICE.md. -->\n' + ET.tostring(vector,encoding='unicode')+'\n'

def original_svg(key):
    path = ASSETS / 'material-reference' / (key.replace('#','_')+'.svg')
    if not path.exists():
        raise FileNotFoundError(f'{path}: run scripts/render_material_symbol_references.swift first')
    return path.read_text()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--import-sources',type=Path);args=parser.parse_args()
    config=json.loads((ROOT/'scripts/interface_icon_map.json').read_text());rows=config['icons']
    ASSETS.mkdir(exist_ok=True,parents=True)
    for row in rows.values():
        family,name=row['family'],row['icon']
        if family=='material':continue
        out=ASSETS/family/(name+'.svg');out.parent.mkdir(exist_ok=True)
        if args.import_sources and not out.exists():
            source=args.import_sources/family/(name+('.kt' if family.startswith('miuix') else '.svg'))
            out.write_text(miuix_svg(source.read_text(),name) if family.startswith('miuix') else source.read_text())
        if row.get('overlay') and args.import_sources:
            overlay=ASSETS/'phosphor-fill'/(row['overlay']+'.svg')
            shutil.copyfile(args.import_sources/'phosphor-fill'/(row['overlay']+'.svg'),overlay)
    output=ROOT/'app/src/main/res/drawable'
    # Home Add uses HyperModifier's original Regular viewport, independently of generic Add.
    home_add = ASSETS / 'miuix-regular/Add.svg'
    (output/'ic_miuix_home_add.xml').write_text(vector_xml(ET.fromstring(home_add.read_text()), False, 'miuix-regular'))
    for resource, (name, weight) in CONTEXT_VARIANTS.items():
        source = ASSETS / 'miuix-glass' / (name + '_' + weight + '.svg')
        if args.import_sources and not source.exists():
            source.write_text(miuix_svg((args.import_sources/'miuix-glass'/(name+'.kt')).read_text(), name, weight))
        (output/(resource+'.xml')).write_text(vector_xml(ET.fromstring(source.read_text()), name in ('ChevronBackward', 'ChevronForward'), 'miuix-glass'))
    registry=[];cards=[];svgs={}
    for stale in output.glob('hmi_*.xml'):stale.unlink()
    review=ROOT/'specs/interface-icon-pack/review';review.mkdir(exist_ok=True)
    for key,row in rows.items():
        family,name=row['family'],row['icon'];resource='hmi_'+key.replace('#','_')
        if family=='material':continue
        root=ET.fromstring((ASSETS/family/(name+'.svg')).read_text())
        if row.get('cutoutCircle') or row.get('cutoutRect') or row.get('baseTransform'):
            children=list(root)
            for child in children:root.remove(child)
            attrs={}
            if row.get('baseTransform'):attrs['transform']=row['baseTransform']
            if row.get('cutoutCircle') or row.get('cutoutRect'):
                if row.get('cutoutCircle'):
                    x,y,r=row['cutoutCircle'];hole=f'M{x-r} {y}a{r} {r} 0 1 0 {2*r} 0a{r} {r} 0 1 0 {-2*r} 0Z'
                else:
                    x,y,right,bottom=row['cutoutRect'];hole=f'M{x} {y}H{right}V{bottom}H{x}Z'
                defs=ET.SubElement(root,'defs');clip=ET.SubElement(defs,'clipPath',id='clip-'+resource)
                ET.SubElement(clip,'path',d='M0 0H256V256H0Z '+hole,**{'fill-rule':'evenodd','clip-rule':'evenodd'})
                attrs['clip-path']='url(#clip-'+resource+')'
            group=ET.SubElement(root,'g',attrs)
            for child in children:group.append(child)
        if row.get('overlay'):
            overlay=ET.fromstring((ASSETS/'phosphor-fill'/(row['overlay']+'.svg')).read_text())
            if row['overlay'] in ('plus','exclamation-mark'):
                # Fill variants enclose the mark in a square. Reuse their exact inner contour
                # as a positive mark inside our existing semantic carrier (alarm/chat/bell).
                path=list(overlay)[0];data=path.attrib['d'];end=re.search('[zZ]',data).end()
                path.set('d',data[end:])
            transform=row.get('overlayTransform','' if row['overlay']=='slash' else 'translate(104 104) scale(0.60 0.60)')
            group=ET.SubElement(root,'g',{'transform':transform})
            for child in overlay:group.append(copy.deepcopy(child))
        scale = row.get('opticalScale', 1.0)
        if scale != 1.0:
            assert family.startswith('phosphor-'), key
            assert 0.8 <= scale <= 1.3, (key, scale)
            viewport = list(map(float, root.attrib['viewBox'].split()))
            cx, cy = viewport[0] + viewport[2]/2, viewport[1] + viewport[3]/2
            children = list(root)
            for child in children: root.remove(child)
            group = ET.SubElement(root, 'g', transform=f'translate({cx*(1-scale):.6f} {cy*(1-scale):.6f}) scale({scale} {scale})')
            for child in children: group.append(child)
        svg=ET.tostring(root,encoding='unicode')
        (output/(resource+'.xml')).write_text(vector_xml(root,key.split('#')[0] in DIRECTIONAL,family))
        registry.append(f'        "{key}" to R.drawable.{resource},')
        original=original_svg(key);svgs[key]=(original,svg)
        (review/(resource+'.svg')).write_text(svg)
        cards.append(f'<article><b>{html.escape(key)}</b><div>{original}{svg}</div><small>{html.escape(family+" / "+name)}</small><p>{html.escape(row["review"])}</p></article>')
    target=ROOT/'app/src/main/java/com/aritxonly/deadliner/ui/HybridIconRegistry.kt'
    target.write_text('''package com.aritxonly.deadliner.ui

import androidx.annotation.DrawableRes
import com.aritxonly.deadliner.R

/** Generated from scripts/interface_icon_map.json; review evidence lives in specs/interface-icon-pack/review. */
internal object HybridIconRegistry {
    private val icons = mapOf(
'''+ '\n'.join(registry)+'''
    )
    @DrawableRes fun resource(name: String, filled: Boolean = false): Int? =
        (if (filled) icons["$name#filled"] else null) ?: icons[name]
    val names: Set<String> get() = icons.keys
}
''')
    page='''<!doctype html><meta charset="utf-8"><title>Deadliner Hybrid icon review</title><style>
body{font:14px system-ui;margin:24px;background:#f6f6f8;color:#18181b}main{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:12px}article{background:white;border-radius:16px;padding:16px}article div{display:flex;gap:40px;margin:16px}svg{width:48px;height:48px}p{line-height:1.5}small{color:#64646a}input{padding:12px;width:300px;margin-bottom:20px}</style>
<h1>Material Symbols → Hybrid MIUIX</h1><p>每项左侧原图形，右侧候选。Material 基准直接从应用变量字体导出，wght=330，FILL 与调用状态一致。混合方案保留上游 Glass Regular 路径与 Phosphor Fill 实心比例。</p><input placeholder="搜索图标、来源或说明" oninput="document.querySelectorAll('article').forEach(e=>e.hidden=!e.textContent.toLowerCase().includes(this.value.toLowerCase()))"><main>'''+''.join(cards)+'</main>'
    (review/'index.html').write_text(page)
    # Contact sheets use vector conversion directly: no screenshots or device operations.
    entries=list(svgs.items())
    for offset in range(0,len(entries),36):
        chunks=['<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="960"><rect width="1200" height="960" fill="white"/>']
        for i,(key,(old,new)) in enumerate(entries[offset:offset+36]):
            x=(i%6)*200;y=(i//6)*160
            chunks.append(f'<text x="{x+10}" y="{y+20}" font-family="monospace" font-size="12">{html.escape(key)}</text>')
            for dx,source in [(12,old),(108,new)]:
                r=ET.fromstring(source);r.set('x',str(x+dx));r.set('y',str(y+34));r.set('width','64');r.set('height','64');chunks.append(ET.tostring(r,encoding='unicode'))
            family=rows[key]['family'].replace('miuix-','M.').replace('phosphor-fill','P.Fill')
            chunks.append(f'<text x="{x+10}" y="{y+124}" font-family="monospace" font-size="11">Material → {family}</text>')
            chunks.append(f'<text x="{x+10}" y="{y+143}" font-family="monospace" font-size="11">{rows[key]["icon"]}</text>')
        chunks.append('</svg>');s=review/f'contact-{offset//36+1}.svg';s.write_text(''.join(chunks))
        subprocess.run(['rsvg-convert','-o',str(s.with_suffix('.png')),str(s)],check=True)
    print('Generated',len(registry),'Hybrid resources and',len(cards),'review comparisons')

if __name__=='__main__':main()
