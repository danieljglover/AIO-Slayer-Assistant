#!/usr/bin/env python3
"""Authoring-only OSRS Wiki API evidence and equipment extraction (stdlib only).

Run from repository root. Raw revision bodies are cached outside the repository.
Fetching evidence does not certify that a source record has been semantically reviewed.
"""
import argparse
import concurrent.futures
import hashlib
import html
import json
import pathlib
import re
import time
import urllib.parse
import urllib.request
from collections import Counter
from datetime import datetime, timezone

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'src/main/data/slayer'
API = 'https://oldschool.runescape.wiki/api.php'
SLOTS = {'head':'HEAD','cape':'CAPE','neck':'AMULET','amulet':'AMULET','weapon':'WEAPON',
         '2h':'WEAPON','body':'BODY','shield':'SHIELD','legs':'LEGS','hands':'HANDS',
         'feet':'FEET','ring':'RING','ammo':'AMMO','ammunition':'AMMO'}


def read_sources(directory):
    return [(p, json.loads(p.read_text())) for p in sorted((SOURCE / directory).rglob('*.json'))]


def save(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=True) + '\n')


def title(value):
    return urllib.parse.unquote(value.split('/w/')[-1]).replace('_',' ').split('#')[0].strip()


def templates(text):
    stack = []
    result = []
    i = 0
    while i < len(text)-1:
        if text[i:i+2] == '{{':
            stack.append(i); i += 2
        elif text[i:i+2] == '}}' and stack:
            start = stack.pop(); result.append((start,i+2,text[start+2:i])); i += 2
        else: i += 1
    return sorted(result)


def parts(text):
    result=[]; start=0; curly=0; square=0; i=0
    while i < len(text):
        two=text[i:i+2]
        if two=='{{': curly+=1; i+=2; continue
        if two=='}}': curly-=1; i+=2; continue
        if two=='[[': square+=1; i+=2; continue
        if two==']]': square-=1; i+=2; continue
        if text[i]=='|' and curly==0 and square==0:
            result.append(text[start:i].strip()); start=i+1
        i+=1
    result.append(text[start:].strip())
    return result


def params(body):
    ps=parts(body); named={}; positional=[]
    for p in ps[1:]:
        if '=' in p:
            k,v=p.split('=',1); named[k.strip().lower()]=v.strip()
        else: positional.append(p)
    return ps[0].strip().lower(), named, positional


def clean(text):
    text=re.sub(r'<ref\b[^>]*>.*?</ref>|<ref\b[^>]*/>', '', text, flags=re.S|re.I)
    text=re.sub(r'<!--.*?-->', '', text, flags=re.S)
    for _ in range(5):
        nodes=templates(text)
        if not nodes: break
        # Replace innermost templates first; higher templates are reparsed next pass.
        for start,end,body in reversed(nodes):
            if '{{' in body: continue
            name,n,p=params(body)
            if name in ('plink','plinkp','item','ilink'): replacement=n.get('txt',p[0] if p else '')
            elif name in ('scp','skill','skill inline'): replacement=' '.join(reversed(p[:2]))
            elif name in ('yes','no','na'): replacement=name
            elif name in ('efn','refn','notelist','namedref'): replacement=''
            else: replacement=' '.join(p)
            text=text[:start]+replacement+text[end:]
    text=re.sub(r'\[\[([^\]|]+)(?:\|([^\]]+))?\]\]',lambda m:m[2] or m[1],text)
    text=re.sub(r'<[^>]+>', ' ', text)
    text=text.replace("'''",'').replace("''",'')
    return re.sub(r'\s+',' ',html.unescape(text)).strip()


def body(page):
    return page.get('revisions',[{}])[0].get('slots',{}).get('main',{}).get('content','')


def evidence(page):
    rev=page.get('revisions',[{}])[0]
    return {'url':'https://oldschool.runescape.wiki/w/'+urllib.parse.quote(page['title'].replace(' ','_'),safe="/()'"),
            'title':page['title'],'pageId':page.get('pageid',0),'revisionId':rev.get('revid',0),
            'timestamp':rev.get('timestamp',''),'missing':'missing' in page}


class Wiki:
    def __init__(self, cache, refresh=False):
        self.refresh=refresh
        self.cache=pathlib.Path(cache); self.cache.mkdir(parents=True,exist_ok=True)
        self.pages={}

    def batch(self, names):
        key=hashlib.sha256('|'.join(names).encode()).hexdigest()
        path=self.cache/(key+'.json')
        if path.exists() and not self.refresh: result=json.loads(path.read_text())
        else:
            query=urllib.parse.urlencode({'action':'query','prop':'revisions|info','rvprop':'ids|timestamp|content',
                 'rvslots':'main','titles':'|'.join(names),'redirects':1,'format':'json','formatversion':2,'maxlag':5})
            for attempt in range(5):
                try:
                    req=urllib.request.Request(API+'?'+query,headers={'User-Agent':'AIO-Slayer-Assistant authoring audit (bundled data; no runtime requests)'})
                    with urllib.request.urlopen(req,timeout=60) as response: result=json.load(response)
                    if 'error' in result: raise RuntimeError(str(result['error']))
                    save(path,result); break
                except Exception:
                    if attempt==4: raise
                    time.sleep(2**attempt)
        pages={p['title']:p for p in result.get('query',{}).get('pages',[])}
        aliases={p['from']:p['to'] for group in ('normalized','redirects') for p in result.get('query',{}).get(group,[])}
        out=dict(pages)
        for name in names:
            resolved=name; seen=set()
            while resolved in aliases and resolved not in seen:
                seen.add(resolved); resolved=aliases[resolved]
            if resolved in pages: out[name]=pages[resolved]
        return out

    def fetch(self,names):
        names=sorted(set(n for n in names if n and n not in self.pages))
        batches=[names[i:i+30] for i in range(0,len(names),30)]
        with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
            futures=[pool.submit(self.batch,b) for b in batches]
            for i,f in enumerate(futures):
                self.pages.update(f.result())
                print('Wiki batches',i+1,'/',len(batches),flush=True)
        return self.pages


def page_plan():
    refs={}; names={'Slayer Master','Mortimer','Mortimer/Strategies','Slayer equipment','Dwarf multicannon','Slayer training'}
    links_path=SOURCE/'advisor/page-links.json'
    links=json.loads(links_path.read_text()).get('records',{}) if links_path.exists() else {}
    for directory,key in [('masters','masterId'),('tasks','taskId'),('monsters','variantId'),('locations','locationId'),('strategies','strategyId'),('weapons','weaponId'),('items','itemKey'),('rewards','rewardId')]:
        for path,data in read_sources(directory):
            name=data.get('name','')
            if directory=='strategies': targets=[title(data['sourceUrl'])] if data.get('sourceUrl') else []
            elif directory=='rewards': targets=['Slayer Rewards']
            elif directory=='tasks': targets=['Slayer task/'+name]
            elif directory=='monsters':
                name=re.sub(r'\s*\([^)]*\)','',name).strip()
                targets=[name,name+'/Strategies']
            else: targets=[title(data['sourceUrl'])] if data.get('sourceUrl') else [name]
            targets=list(dict.fromkeys(links.get(str(path.relative_to(SOURCE)),[])+targets))
            refs[str(path.relative_to(SOURCE))]={'id':data.get(key),'requestedTitles':targets,'originalPageId':data.get('wikiPageId',0)}
            names.update(targets)
    for path in sorted((SOURCE/'strategies').glob('*.md')):
        text=path.read_text(); source=re.search(r'^sourceUrl: (.+)$',text,re.M); identity=re.search(r'^strategyId: (.+)$',text,re.M)
        targets=[title(source[1])] if source else []
        refs[str(path.relative_to(SOURCE))]={'id':identity[1] if identity else path.stem,'requestedTitles':targets,'originalPageId':0}
        names.update(targets)
    names.update(['Turael/Slayer assignments','Mazchna/Slayer assignments','Nieve/Slayer assignments','Duradel/Slayer assignments'])
    return refs,names


def item_links(value):
    out=[]
    for _,_,b in templates(value):
        name,n,p=params(b)
        if name in ('plink','plinkp','item') and p:
            item=p[0]
            if '#' in item or item.lower() in ('mitre','blessed boots','blessed coif','god blessing','vestment robe top','vestment robe legs','soul cape','cape of accomplishment (t)','god cape','god capes'):
                item=n.get('pic',item)
            item=title(item)
            if item and item not in out: out.append(item)
    if not out:
        for match in re.finditer(r'\[\[([^\]|]+)(?:\|[^\]]+)?\]\]',value):
            item=title(match[1])
            if not item.startswith(('File:','Image:')) and item not in out: out.append(item)
    return out


def equipment_tables(page):
    text=body(page); result=[]
    for start,end,b in templates(text):
        name,n,p=params(b)
        if name!='recommended equipment': continue
        heading=[clean(m[2]) for m in re.finditer(r'^(={2,6})\s*(.*?)\s*\1\s*$',text[:start],re.M)]
        tabs=re.findall(r'(?:<tabber>|\|-\|)\s*([^\n={}]+)\s*=',text[:start],re.I)
        context=heading[-2:]+([clean(tabs[-1])] if tabs else [])
        slots={}; switches=[]; raw=[]
        for key,value in n.items():
            match=re.fullmatch(r'([a-z]+)(\d*)',key)
            if not match: continue
            slot,rank=match[1],int(match[2] or '1'); names=item_links(value)
            if slot in SLOTS:
                slots.setdefault(SLOTS[slot],[]).append((rank,names,clean(value)))
            elif slot in ('special','spec'): switches.extend(names)
        for slot,rows in list(slots.items()):
            slots[slot]=[]
            for rank,names,description in sorted(rows):
                slots[slot].extend({'name':item,'wikiPriority':rank} for item in names)
                if not names and description and description.lower() not in ('none','n/a','any blessing'): raw.append(slot+': '+description)
        style=clean(n.get('style','')).upper()
        if 'MAGIC' in style or 'MAGE' in style: style='MAGIC'
        elif any(v in style for v in ('RANGED','RANGE','CHINCHOMPA','BOWFA','TBOW')): style='RANGED'
        elif 'MELEE' in style: style='MELEE'
        if style not in ('MELEE','RANGED','MAGIC'):
            weapon=' '.join(o['name'] for o in slots.get('WEAPON',[])).lower()
            if any(v in weapon for v in ('staff','wand','sceptre','trident','shadow')): style='MAGIC'
            elif any(v in weapon for v in ('bow','ballista','blowpipe','chinchompa','atlatl')): style='RANGED'
        if style not in ('MELEE','RANGED','MAGIC'):
            context_text=' '.join(context).lower()
            style='RANGED' if any(w in context_text for w in ('ranged','range','bow','crossbow')) else 'MAGIC' if any(w in context_text for w in ('mage','magic','barrage','shadow')) else 'MELEE'
        result.append({'index':len(result)+1,'name':' / '.join(context) or style.title()+' equipment',
                       'style':style,'wikiStyle':clean(n.get('style','')),'equipment':slots,'switchNames':list(dict.fromkeys(switches)),
                       'unresolvedCells':raw,'evidence':[evidence(page)]})
    return result


def parse_item(page):
    text=body(page); inf=None; bonuses=None
    for _,_,b in templates(text):
        name,n,p=params(b)
        if name=='infobox item' and inf is None: inf=n
        if name=='infobox bonuses' and bonuses is None: bonuses=n
    if not inf: return None
    ids=[]; rejected=[]
    for key,value in inf.items():
        if not re.fullmatch(r'id\d*',key): continue
        suffix=key[2:]; version=clean(inf.get('version'+suffix,'')); label=clean(inf.get('name'+suffix,inf.get('name',page['title'])))
        invalid=version.strip() == '0' or bool(re.search(r'\b(uncharged|empty|broken|damaged|mangled|inactive|depleted|noted|unfired|ornament kit)\b',version+' '+label,re.I))
        for raw in re.findall(r'\b\d+\b',value):
            item_id=int(raw)
            if 0<item_id<100000:
                (rejected if invalid else ids).append(item_id)
    levels={}
    # Only the lead identifies equip requirements; manufacturing/charging sections are excluded.
    lead=re.split(r'^==',text,maxsplit=1,flags=re.M)[0]
    for _,_,b in templates(lead):
        name,n,p=params(b)
        if name=='scp' and len(p)>=2 and p[0].upper() in ('ATTACK','STRENGTH','DEFENCE','RANGED','MAGIC','PRAYER','SLAYER','HITPOINTS') and p[1].isdigit():
            levels[p[0].upper()]=max(levels.get(p[0].upper(),0),int(p[1]))
    plain=clean(lead)
    requirement_sentences=[]
    for sentence in re.split(r'(?<=[.!?])\s+',plain):
        if re.search(r'\b(require|requires|requiring|wield|equip|wear)\b',sentence,re.I):
            requirement_sentences.append(sentence)
            for level,skill in re.findall(r'\b(\d{1,2})\s+(?:in\s+)?(Attack|Strength|Defence|Ranged|Magic|Prayer|Slayer|Hitpoints)\b',sentence,re.I):
                levels[skill.upper()]=max(levels.get(skill.upper(),0),int(level))
            for skill,level in re.findall(r'\b(Attack|Strength|Defence|Ranged|Magic|Prayer|Slayer|Hitpoints)\s+(?:level\s+)?(?:of\s+)?(\d{1,2})\b',sentence,re.I):
                levels[skill.upper()]=max(levels.get(skill.upper(),0),int(level))
            for level,group in re.findall(r'\b(\d{1,2})\s+((?:(?:Attack|Strength|Defence|Ranged|Magic|Prayer|Hitpoints)(?:,?\s+(?:and\s+)?)?){2,})',sentence,re.I):
                for skill in re.findall(r'Attack|Strength|Defence|Ranged|Magic|Prayer|Hitpoints',group,re.I):
                    levels[skill.upper()]=max(levels.get(skill.upper(),0),int(level))
    requirements=[]
    for sentence in re.split(r'(?<=[.!?])\s+',plain):
        if re.search(r'\b(wear|wield|equip)\b',sentence,re.I) and re.search(r'\b(quest|completed|completion|minigame|favour|diary)\b',sentence,re.I):
            requirements.append(sentence)
    slot=SLOTS.get(clean((bonuses or {}).get('slot','')).lower(),'')
    # The page may discuss other weapons/creation prerequisites. Preserve parsed levels as
    # candidate metadata; only explicit no-requirements or manually reviewed entries are known.
    known=bool(re.search(r'(?:no|does not have any) (?:skill |level |combat )?requirements? to (?:wear|equip|wield)',plain,re.I))
    return {'name':page['title'],'itemIds':list(dict.fromkeys(ids)),'rejectedItemIds':list(dict.fromkeys(rejected)),
            'slot':slot,'stackable':clean(inf.get('stackable','')).lower()=='yes',
            'levels':levels,'requirements':requirements,'requirementsKnown':known,'requirementEvidence':requirement_sentences,
            'evidence':[evidence(page)]}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cache',default='/tmp/aio-slayer-wiki-cache')
    parser.add_argument('--evidence-only',action='store_true',help='Refresh page evidence and grids without changing reviewed item metadata')
    parser.add_argument('--refresh',action='store_true',help='Query current revisions instead of reusing the authoring cache')
    args=parser.parse_args(); wiki=Wiki(args.cache,args.refresh)
    refs,names=page_plan(); wiki.fetch(names)
    # Monster/main-page strategy transclusions and newer assignment monsters need their own pages.
    follow=set()
    for requested,p in list(wiki.pages.items()):
        if requested=='Mortimer':
            follow.update(re.findall(r'\[\[(?:Slayer task/|Slayer_task/)([^\]|]+)',body(p)))
    # Item names from all authored equipment slots and weapons, plus current wiki grids.
    tables={}; item_names=set()
    for requested,p in sorted(wiki.pages.items()):
        if requested!=p['title']: continue
        ts=equipment_tables(p)
        if ts:
            tables[requested]=ts
            for t in ts:
                item_names.update(o['name'] for opts in t['equipment'].values() for o in opts)
                item_names.update(t['switchNames'])
    for _,s in read_sources('strategies'):
        for m in s.get('methods',[])+s.get('styleOptions',[]):
            for options in m.get('equipment',{}).get('slots',{}).values():
                for option in options:
                    if len(option)<90 and not re.search(r'\b(or|such as|any|setup|gear)\b',option,re.I): item_names.add(option)
    extra=SOURCE/'advisor/item-aliases.json'
    if extra.exists(): item_names.update(json.loads(extra.read_text()).get('fetchTitles',[]))
    for directory in ('weapons','items'):
        item_names.update(d['name'] for _,d in read_sources(directory))
    wiki.fetch(item_names)
    items={}
    for requested in sorted(item_names):
        page=wiki.pages.get(requested)
        parsed=parse_item(page) if page else None
        if parsed: items[requested]=parsed
    report={}
    for path,ref in refs.items():
        pages=[wiki.pages[n] for n in ref['requestedTitles'] if n in wiki.pages]
        record=dict(ref)
        record['evidence']=[evidence(p) for p in pages]
        record['status']='evidence-fetched-not-reviewed'
        record['missingTitles']=[p['title'] for p in pages if 'missing' in p]
        record['equipmentTableCount']=sum(len(equipment_tables(p)) for p in pages)
        record['headings']={p['title']:[clean(m[2]) for m in re.finditer(r'^(={2,6})\s*(.*?)\s*\1\s*$',body(p),re.M)] for p in pages if 'missing' not in p}
        authored=json.loads((SOURCE/path).read_text()) if path.endswith('.json') else {}
        existing=[p for p in pages if 'missing' not in p]
        if ref.get('originalPageId') and existing:
            record['pageIdMatches']=any(p.get('pageid')==ref['originalPageId'] for p in existing)
        if path.startswith('tasks/') and existing:
            inf=next((params(b)[1] for _,_,b in templates(body(existing[0])) if params(b)[0]=='infobox slayer'),{})
            current={}
            for master in ('turael','spria','mazchna','vannaka','chaeldar','konar','nieve','duradel','krystilia','mortimer'):
                value=clean(inf.get(master,''))
                match=re.match(r'(\d+)(?:\s*-\s*(\d+))?',value)
                if match: current[master]=[int(match[1]),int(match[2] or match[1])]
            record['taskInfoboxPresent']=bool(inf)
            if inf:
                record['currentTaskInfoboxAmounts']=current
                record['assignmentDifferences']={m:{'source':authored.get('amountByMaster',{}).get(m),'wikiTaskInfobox':current.get(m)} for m in sorted(set(current)|set(authored.get('amountByMaster',{}))) if authored.get('amountByMaster',{}).get(m)!=current.get(m)}
            levels=[int(pos[1]) for _,_,b in templates(inf.get('skillreq','')) for name,named,pos in [params(b)] if name=='scp' and len(pos)>1 and pos[0].lower()=='slayer' and pos[1].isdigit()]
            if levels:record['slayerLevelComparison']={'source':authored.get('slayerLevel',0),'wiki':min(levels),'variantLevels':levels}
        if path.startswith('monsters/') and existing:
            wiki_ids=set()
            for _,_,b in templates(body(existing[0])):
                name,named,pos=params(b)
                if name=='infobox monster':
                    wiki_ids.update(int(i) for k,v in named.items() if re.fullmatch(r'id\d*',k) for i in re.findall(r'\b\d+\b',v))
            record['sourceNpcIdsAbsentFromPage']=sorted(set(authored.get('npcIds') or [])-wiki_ids)
            record['meaningOfNpcDiff']='Candidate drift only: related versions, quest states and title disambiguation require semantic review.'
        report[path]=record
    out=SOURCE/'advisor'
    save(out/'wiki-audit.json',{'fetchedAt':datetime.now(timezone.utc).isoformat(),'records':report,
         'additionalEvidence':[evidence(wiki.pages[n]) for n in ('Slayer Master','Mortimer','Mortimer/Strategies') if n in wiki.pages],
         'meaning':'Revision retrieval establishes provenance only. Semantic completeness is not certified.'})
    save(out/'wiki-equipment.json',{'pages':tables,'meaning':'Current wiki equipment table order; individual item pages resolve IDs. Context matching requires review.'})
    if not args.evidence_only:
        existing=json.loads((out/'equipment-items.json').read_text()) if (out/'equipment-items.json').exists() else {}
        retained=existing.get('items',{})
        for name,item in items.items():
            previous=retained.get(name,{})
            old_revisions=[e.get('revisionId') for e in previous.get('evidence',[])]
            new_revision=item.get('evidence',[{}])[0].get('revisionId')
            if previous.get('requirementsKnown') and new_revision in old_revisions:
                # Reviewed item metadata and equivalence groups survive a same-revision
                # evidence refresh. A changed revision requires another equip-gate review.
                retained[name]=previous
            else: retained[name]=item
        existing.update({'items':retained,'unresolvedNames':sorted(item_names-set(retained))})
        save(out/'equipment-items.json',existing)
    print('Source records:',len(refs),'wiki pages:',len({p['title'] for p in wiki.pages.values()}),'equipment tables:',sum(map(len,tables.values())), 'resolved item names:',len(items),'unresolved:',len(item_names-set(items)),flush=True)


if __name__=='__main__': main()
