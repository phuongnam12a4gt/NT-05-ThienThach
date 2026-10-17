from pathlib import Path
import urllib.request,urllib.parse,json,re,time,concurrent.futures,threading
ROOT=Path(__file__).resolve().parents[1]
VI=json.loads((ROOT/'tools/v2_strings_vi.json').read_text(encoding='utf-8'))
PATH=ROOT/'tools/v2_localizations.json'
DATA=json.loads(PATH.read_text(encoding='utf-8')) if PATH.exists() else {'vi':VI}
DATA['vi']=VI
LOCK=threading.Lock()
LANGS='en zh ja ko pt es hi id th ru de fr tr ar it pl fil ms ur'.split()
def batch(items,lang):
 text='\n'.join(f'@@{n:03d}@@ {value}' for n,(key,value) in enumerate(items))
 query={'client':'gtx','sl':'vi','tl':{'zh':'zh-CN','fil':'tl'}.get(lang,lang),'dt':'t','q':text}
 url='https://translate.googleapis.com/translate_a/single?'+urllib.parse.urlencode(query)
 for attempt in range(5):
  try:
   request=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0'})
   response=json.load(urllib.request.urlopen(request,timeout=35))
   output=''.join(part[0] for part in response[0] if part[0])
   matches=list(re.finditer(r'@+\s*(\d{3})\s*@+',output))
   if len(matches)!=len(items):raise ValueError('Translation markers lost')
   values={}
   for index,match in enumerate(matches):
    number=int(match.group(1));assert number==index
    value=output[match.end():matches[index+1].start() if index+1<len(matches) else len(output)].strip()
    assert value;values[items[index][0]]=value
   return values
  except Exception:
   if attempt==4:raise
   time.sleep(2+attempt*2)
def language(lang):
 pending=[(k,v) for k,v in VI.items() if k not in DATA.get(lang,{})]
 while pending:
  items=[];length=0
  for item in pending:
   if items and length+len(item[1])+12>1800:break
   items.append(item);length+=len(item[1])+12
  values=batch(items,lang)
  with LOCK:
   DATA.setdefault(lang,{}).update(values)
   PATH.write_text(json.dumps(DATA,ensure_ascii=False,indent=2),encoding='utf-8')
  pending=pending[len(items):]
 print(lang+': '+str(len(DATA.get(lang,{})))+' translated',flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
 for result in pool.map(language,LANGS):pass
print('DONE: '+str(len(VI))+' new strings in 20 languages',flush=True)
