import json,pathlib,base64,unicodedata
source=pathlib.Path('C:/Users/THINKPAD789/Downloads/places_VN_Đà Nẵng.json');data=json.loads(source.read_text(encoding='utf-8-sig'))
def norm(s):return ''.join(c for c in unicodedata.normalize('NFD',s) if unicodedata.category(c)!='Mn').replace('đ','d').replace('Đ','D').lower()
with pathlib.Path('docs/revision-20260927/places-import.tsv').open('w',encoding='utf-8') as f:
 for x in data:
  name=x.get('name') or '';address=x.get('address') or '';area=x.get('locality') or x.get('district') or x.get('province') or ''
  fields=[x['place_id'],name,address,area,x.get('primary_type') or '',x.get('phone') or '',x.get('google_maps_url') or '',str(x.get('lat') or ''),str(x.get('lng') or ''),x.get('business_status') or 'UNSPECIFIED',x.get('updated_at') or x.get('crawled_at') or '',norm(name)+' | '+norm(area)+' | '+norm(address)]
  f.write('\t'.join(base64.b64encode(v.encode()).decode() for v in fields)+'\n')
