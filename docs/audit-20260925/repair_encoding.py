from pathlib import Path
import re, json, shutil, difflib
ROOT=Path(__file__).resolve().parents[2]
OUT=Path(__file__).parent
mapping={chr(i):i for i in range(128,256)}
for i in range(128,256):
    try: mapping[bytes([i]).decode('cp1252')]=i
    except UnicodeDecodeError: pass
def repair(s):
    for _ in range(4):
        result=[]; i=0
        while i<len(s):
            b=mapping.get(s[i],0)
            n=2 if 0xC2<=b<=0xDF else 3 if 0xE0<=b<=0xEF else 4 if 0xF0<=b<=0xF4 else 0
            if n and i+n<=len(s):
                try:
                    seq=bytes(mapping[c] for c in s[i:i+n])
                    fixed=seq.decode('utf-8')
                    result.append(fixed); i+=n; continue
                except (KeyError,UnicodeDecodeError): pass
            result.append(s[i]); i+=1
        new=''.join(result)
        if new==s: break
        s=new
    return s
changes=[]; diffs=[]
for p in (ROOT/'src/main/resources/templates').rglob('*.html'):
    old=p.read_bytes().decode('utf-8'); new=repair(old)
    if old==new: continue
    rel=p.relative_to(ROOT)
    changes.append(str(rel))
    diffs.extend(difflib.unified_diff(old.splitlines(True),new.splitlines(True),fromfile=str(rel),tofile=str(rel)))
    backup=OUT/'originals'/rel; backup.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(p,backup)
    p.write_bytes(new.encode('utf-8'))
if changes:
    (OUT/'encoding.patch').write_text(''.join(diffs),encoding='utf-8')
    (OUT/'encoding-files.json').write_text(json.dumps(changes,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(changes,ensure_ascii=False,indent=2))
