#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
changed=[]

# Keep exactly one supabase-js loader on the protected TopoDroid import page.
path=ROOT/'topodroid-import.html'
text=path.read_text(encoding='utf-8')
tag='<script src="https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2"></script>'
if text.count(tag)>1:
    first=text.find(tag)
    head=text[:first+len(tag)]
    tail=text[first+len(tag):].replace(tag,'')
    text=head+tail
    path.write_text(text,encoding='utf-8')
    changed.append(str(path.relative_to(ROOT)))

# External Drive link opens a new tab and must not retain window.opener.
path=ROOT/'topodroid.html'
text=path.read_text(encoding='utf-8')
old='target="_blank">Drive arhiv</a>'
new='target="_blank" rel="noopener noreferrer">Drive arhiv</a>'
if old in text:
    path.write_text(text.replace(old,new,1),encoding='utf-8')
    changed.append(str(path.relative_to(ROOT)))

print('Updated: '+(', '.join(changed) if changed else 'none; already clean'))
