import os,re,sys
# gen.py <esm dir> <out.kt> key=IconName ...
d,out=sys.argv[1],sys.argv[2]
def to_path(tag,a):
    f=lambda k:float(a.get(k,0))
    if tag=='path': return a['d']
    if tag=='circle':
        cx,cy,r=f('cx'),f('cy'),f('r'); return f"M{cx-r} {cy}A{r} {r} 0 1 0 {cx+r} {cy}A{r} {r} 0 1 0 {cx-r} {cy}Z"
    if tag=='rect':
        x,y,w,h=f('x'),f('y'),f('width'),f('height'); rx=float(a.get('rx',a.get('ry',0)));ry=float(a.get('ry',rx))
        if rx==0: return f"M{x} {y}H{x+w}V{y+h}H{x}Z"
        return f"M{x+rx} {y}H{x+w-rx}A{rx} {ry} 0 0 1 {x+w} {y+ry}V{y+h-ry}A{rx} {ry} 0 0 1 {x+w-rx} {y+h}H{x+rx}A{rx} {ry} 0 0 1 {x} {y+h-ry}V{y+ry}A{rx} {ry} 0 0 1 {x+rx} {y}Z"
    if tag=='line': return f"M{f('x1')} {f('y1')}L{f('x2')} {f('y2')}"
    if tag=='ellipse':
        cx,cy,rx,ry=f('cx'),f('cy'),f('rx'),f('ry'); return f"M{cx-rx} {cy}A{rx} {ry} 0 1 0 {cx+rx} {cy}A{rx} {ry} 0 1 0 {cx-rx} {cy}Z"
    raise Exception(tag)
lines=['package com.logikawan.finflow.core.ui','','// Dihasilkan dari @hugeicons/core-free-icons 4.4.0 (MIT, lihat licenses/hugeicons-LICENSE.txt).','// Format: daftar (pathData, filled). Viewport 24, stroke 1.5, ujung bulat — sama seperti ikon Ollo.','internal val hugeIconPaths: Map<String, List<Pair<String, Boolean>>> = mapOf(']
for arg in sys.argv[3:]:
    key,name=arg.split('=')
    src=open(os.path.join(d,name+'Icon.js'),encoding='utf8').read()
    items=[]
    for tag,attrs in re.findall(r'\["(\w+)",\s*\{([^}]*)\}\]',src):
        a=dict(re.findall(r'(\w+):\s*"([^"]*)"',attrs))
        filled=a.get('fill','none')!='none'
        items.append('Pair("%s", %s)'%(to_path(tag,a),'true' if filled else 'false'))
    lines.append('    "%s" to listOf(%s),'%(key,', '.join(items)))
lines.append(')')
open(out,'w',encoding='utf8').write('\n'.join(lines)+'\n')
