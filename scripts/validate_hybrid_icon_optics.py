#!/usr/bin/env python3
"""Validate per-icon optical contracts and actual raster bounds without an Android device."""
import hashlib, json, struct, subprocess, zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ROWS = json.loads((ROOT/'scripts/interface_icon_map.json').read_text())['icons']

def alpha_bounds(png):
    assert png[:8] == b'\x89PNG\r\n\x1a\n'
    offset=8; data=[]; w=h=0
    while offset < len(png):
        length=struct.unpack('>I',png[offset:offset+4])[0];kind=png[offset+4:offset+8]
        chunk=png[offset+8:offset+8+length];offset+=length+12
        if kind==b'IHDR':
            w,h,depth,color,_,_,interlace=struct.unpack('>IIBBBBB',chunk)
            assert depth==8 and color==6 and interlace==0
        if kind==b'IDAT':data.append(chunk)
    raw=zlib.decompress(b''.join(data));stride=w*4;previous=bytearray(stride);position=0
    left=w;top=h;right=bottom=0
    for y in range(h):
        mode=raw[position];row=bytearray(raw[position+1:position+1+stride]);position+=stride+1
        for i in range(stride):
            a=row[i-4] if i>=4 else 0;b=previous[i];c=previous[i-4] if i>=4 else 0
            if mode==1:predict=a
            elif mode==2:predict=b
            elif mode==3:predict=(a+b)//2
            elif mode==4:
                p=a+b-c;pa,pb,pc=abs(p-a),abs(p-b),abs(p-c)
                predict=a if pa<=pb and pa<=pc else b if pb<=pc else c
            else:assert mode==0;predict=0
            row[i]=(row[i]+predict)&255
        for x in range(w):
            if row[x*4+3]>8:
                left=min(left,x);top=min(top,y);right=max(right,x+1);bottom=max(bottom,y+1)
        previous=row
    return left,top,right,bottom,w,h

def validate():
    signatures={};count=0
    for key,row in ROWS.items():
        if not row['family'].startswith('phosphor-'):
            assert 'opticalScale' not in row, key
            continue
        count+=1;scale=row['opticalScale']
        assert .8<=scale<=1.3 and row['opticalReview'].strip(), key
        signature=json.dumps({k:row.get(k) for k in ('family','icon','overlay','cutoutCircle','cutoutRect','baseTransform','overlayTransform')},sort_keys=True)
        assert signature not in signatures or signatures[signature]==scale, key
        signatures[signature]=scale
        path=ROOT/'specs/interface-icon-pack/review'/('hmi_'+key.replace('#','_')+'.svg')
        png=subprocess.check_output(['rsvg-convert','-w','256','-h','256',str(path)])
        l,t,r,b,w,h=alpha_bounds(png)
        assert r>l and b>t, ('empty',key)
        # Expanded glyphs must not lose their outer contour or compound marks to viewport clipping.
        if scale>1:
            assert l>0 and t>0 and r<w and b<h, ('clipped',key,(l,t,r,b))
        assert max(r-l,b-t)/w <= .98, ('oversized',key)
    assert ROWS['star#filled']['family']=='phosphor-fill'
    assert ROWS['check_circle#filled']['family']=='phosphor-fill'
    assert ROWS['favorite']['icon']=='Favorites' and ROWS['favorite#filled']['icon']=='FavoritesFill'
    print(f'PASS: {count} per-icon calibrations; alias/state contracts and actual raster bounds')

if __name__=='__main__':validate()
