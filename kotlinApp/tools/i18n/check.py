"""Periksa moko-resources: setiap bahasa punya kunci yang sama dengan base dan placeholder %N$s yang sama.

Pakai: python -I kotlinApp/tools/i18n/check.py
"""
import io, os, re, sys
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'composeApp', 'src', 'commonMain', 'moko-resources')
pat = re.compile(r'<string name="([a-z0-9_]+)">(.*?)</string>', re.S)
ph = re.compile(r'%(\d+)\$s')
def load(lang):
    return dict(pat.findall(io.open(os.path.join(root, lang, 'strings.xml'), encoding='utf-8').read()))
base = load('base')
langs = sorted(d for d in os.listdir(root) if d != 'base' and os.path.isdir(os.path.join(root, d)))
bad = 0
for lang in langs:
    data = load(lang)
    for k in base.keys() - data.keys(): print(f'{lang}: kunci hilang {k}'); bad += 1
    for k in data.keys() - base.keys(): print(f'{lang}: kunci berlebih {k}'); bad += 1
    for k in base.keys() & data.keys():
        if set(ph.findall(base[k])) != set(ph.findall(data[k])): print(f'{lang}: placeholder beda {k}'); bad += 1
print(f'{len(base)} kunci, bahasa: base(id) + {", ".join(langs)}; masalah: {bad}')
sys.exit(1 if bad else 0)
