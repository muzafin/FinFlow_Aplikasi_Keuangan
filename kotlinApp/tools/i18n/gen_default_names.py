"""Hasilkan core/i18n/DefaultNames.kt: semua terjemahan nama bawaan (kategori, dompet Kas) dari moko-resources.

Dipakai untuk mengenali nama bawaan yang belum diganti pengguna, agar tampil sesuai bahasa aktif.
Jalankan ulang setelah mengubah teks core_category_* atau core_default_wallet_name:
    python -I kotlinApp/tools/i18n/gen_default_names.py
"""
import html, io, os, re
here = os.path.dirname(os.path.abspath(__file__))
app = os.path.join(here, '..', '..', 'composeApp', 'src', 'commonMain')
res = os.path.join(app, 'moko-resources')
out = os.path.join(app, 'kotlin', 'com', 'clonical', 'kasiva', 'core', 'i18n', 'DefaultNames.kt')
pat = re.compile(r'<string name="(core_category_[a-z_]+|core_default_wallet_name)">(.*?)</string>')
names = {}
for lang in sorted(os.listdir(res)):
    p = os.path.join(res, lang, 'strings.xml')
    if not os.path.exists(p): continue
    for k, v in pat.findall(io.open(p, encoding='utf-8').read()):
        names.setdefault(k, set()).add(html.unescape(v))
# Nama lama sebelum moko (setup English memakai "Cash").
names['core_default_wallet_name'].update({'Kas', 'Cash'})
def kt(s): return '"' + s.replace('\\', '\\\\').replace('"', '\\"').replace('$', '\\$') + '"'
lines = ['// Dibuat oleh tools/i18n/gen_default_names.py — jangan diedit manual.',
         'package com.clonical.kasiva.core.i18n', '',
         '/** Semua terjemahan nama bawaan per kunci moko, untuk mengenali nama yang belum diganti pengguna. */',
         'internal val defaultNameVariants: Map<String, Set<String>> = mapOf(']
for k in sorted(names):
    lines.append(f'    {kt(k)} to setOf(' + ', '.join(kt(v) for v in sorted(names[k])) + '),')
lines.append(')')
io.open(out, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
print(out, len(names))
