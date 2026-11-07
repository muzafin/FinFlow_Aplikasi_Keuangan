"""Tambah/perbarui string di semua bahasa moko-resources sekaligus.

Pakai: python -I kotlinApp/tools/i18n/add_strings.py strings.json
strings.json: {"kunci": {"base": "...", "en": "...", "ms": "...", "de": "...", "fr": "...", "ja": "...", "es": "...", "hi": "...", "zh": "..."}}
Teks ditulis apa adanya (escape & < > otomatis). Kunci diurutkan.
"""
import html, io, json, os, re, sys
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'composeApp', 'src', 'commonMain', 'moko-resources')
pat = re.compile(r'<string name="([a-z0-9_]+)">(.*?)</string>', re.S)
new = json.load(io.open(sys.argv[1], encoding='utf-8'))
langs = sorted(d for d in os.listdir(root) if os.path.isdir(os.path.join(root, d)))
for key, texts in new.items():
    missing = set(langs) - set(texts)
    if missing: sys.exit(f'{key}: bahasa hilang {sorted(missing)}')
for lang in langs:
    path = os.path.join(root, lang, 'strings.xml')
    data = dict(pat.findall(io.open(path, encoding='utf-8').read()))
    for key, texts in new.items():
        data[key] = html.escape(texts[lang], quote=False)
    with io.open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('<?xml version="1.0" encoding="UTF-8"?>\n<resources>\n')
        for k in sorted(data): f.write(f'    <string name="{k}">{data[k]}</string>\n')
        f.write('</resources>\n')
print(f'{len(new)} kunci ditulis ke {len(langs)} bahasa')
