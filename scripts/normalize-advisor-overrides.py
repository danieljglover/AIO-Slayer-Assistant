#!/usr/bin/env python3
"""Rebuild reviewed source overrides using authoring-time item evidence.

Run audit-advisor-wiki.py first. IDs are read from explicit wiki item records;
unknown labels remain absent, not guessed. This script never uses build output.
"""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/data/slayer'
items=json.loads((ROOT/'advisor/equipment-items.json').read_text())['items']
index={''.join(c for c in n.lower() if c.isalnum()):d for n,d in items.items()}
def get(name):return index.get(''.join(c for c in name.lower() if c.isalnum()),{})
def ids(*names):return list(dict.fromkeys(i for n in names for i in get(n).get('itemIds',[])))
def supply(name,*names,quantity=1,waiver=None):
 d={'name':name,'itemIds':ids(*(names or [name])),'quantity':quantity,'required':True,'stackable':any(get(n).get('stackable',False) for n in (names or [name]))}
 if waiver:d['waiverRequirement']=waiver
 return d
out={'items':{},'aliases':{},'optionFamilies':{},'tasks':{},'locations':{},'methods':{},'notes':['Only explicitly reviewed dependency and protection semantics are asserted here. Other item equip requirements require confirmation.']}
# Expand broad table labels into concrete options, retaining every item's own
# equip gates. Diary tiers are ordered from strongest to weakest, not equivalent.
gods=['Saradomin','Guthix','Zamorak','Armadyl','Ancient','Bandos']
for label,suffix in [('Blessed body'," d'hide body"),('Blessed chaps',' chaps'),('Blessed vambraces',' bracers')]:
 out['optionFamilies'][label]=[god+suffix for god in gods]
out['optionFamilies']['Blessed bracers']=out['optionFamilies']['Blessed vambraces']
out['optionFamilies']["Blessed d'hide body"]=out['optionFamilies']['Blessed body']
out['optionFamilies']["Blessed d'hide chaps"]=out['optionFamilies']['Blessed chaps']
out['optionFamilies']["Ardougne cloak"]=['Ardougne cloak '+str(tier) for tier in range(4,0,-1)]
out['optionFamilies']["Explorer's ring"]=["Explorer's ring "+str(tier) for tier in range(4,0,-1)]
out['aliases']['Cannonball']='Steel cannonballs'
reviewed_families=json.loads((ROOT/'advisor/equipment-families.json').read_text())
out['optionFamilies'].update(reviewed_families['optionFamilies'])
out['aliases'].update(reviewed_families.get('itemAliases',{}))
# Equipment family labels in authored strategy tables mean usable equivalent alternatives.
families={'Imbued god cape':['Imbued Saradomin cape','Imbued Guthix cape','Imbued Zamorak cape'],
'God blessing':['Holy blessing','Unholy blessing','Honourable blessing','Peaceful blessing','War blessing','Ancient blessing'],
'Blessed boots':[n+" d'hide boots" for n in ['Saradomin','Guthix','Zamorak','Armadyl','Ancient','Bandos']],
'Mitre':[n+' mitre' for n in ['Saradomin','Guthix','Zamorak']],
'Stole':[n+' stole' for n in ['Saradomin','Guthix','Zamorak']],
'Vestment cloak':[n+' cloak' for n in ['Saradomin','Guthix','Zamorak']],
'Vestment robe top':[n+' robe top' for n in ['Saradomin','Guthix','Zamorak']],
'Vestment robe legs':[n+' robe legs' for n in ['Saradomin','Guthix','Zamorak']]}
for name,names in families.items():
 out['items'][name]={'name':name,'itemIds':ids(*names),'slot':next((get(n).get('slot') for n in names if get(n).get('slot')),''),'levels':{},'requirements':[], 'requirementsKnown':False}
out['aliases'].update({'Blessing':'God blessing','Imbued god capes':'Imbued god cape','Cannonballs':'Cannonball','Blowpipe':'Toxic blowpipe','Prayer potions':'Prayer potion(4)','Prayer potion':'Prayer potion(4)','Super restores':'Super restore(4)','Super restore':'Super restore(4)','Antipoison':'Antipoison(4)','Antipoisons':'Antipoison(4)','Superantipoison':'Superantipoison(4)','Sharks':'Shark','High healing food':'Shark','Stamina potions':'Stamina potion(4)','Stamina potion':'Stamina potion(4)','Teleport to house tablet':'Teleport to house (tablet)','Extended antifire':'Extended antifire(4)','Divine super combat potion':'Divine super combat potion(4)','Divine super combat potions':'Divine super combat potion(4)','Divine ranging potion':'Divine ranging potion(4)','Divine ranging potions':'Divine ranging potion(4)','Efaritay\'s aid':'Efaritay\'s aid'})
# Explicit companion groups: AND across slots, OR across equivalent usable variants.
void_groups=[['Void melee helm','Void ranger helm','Void mage helm'],['Void knight top','Elite void top'],['Void knight robe','Elite void robe'],['Void knight gloves']]
for group in void_groups:
 for name in group:
  out['items'].setdefault(name,{})['requires']=[ids(*other) for other in void_groups if other is not group]
  out['items'][name]['levels']={s:42 for s in ['ATTACK','STRENGTH','DEFENCE','HITPOINTS','RANGED','MAGIC']}
  out['items'][name]['levels']['PRAYER']=22
  out['items'][name]['requirementsKnown']=True
  out['items'][name]['requirements']=[]
for set_names in [["Guthan's helm","Guthan's platebody","Guthan's chainskirt","Guthan's warspear"], ["Dharok's helm","Dharok's platebody","Dharok's platelegs","Dharok's greataxe"]]:
 for name in set_names:
  if get(name):out['items'].setdefault(name,{})['requires']=[ids(other) for other in set_names if other!=name and ids(other)]
# A greataxe also has a standalone slow-weapon role. Apply full Dharok dependencies only
# to its armour; forcing the full set on the punish weapon would reject a valid TD switch.
out['items'].pop("Dharok's greataxe",None)
for name in ['Crystal helm','Crystal body','Crystal legs']:
 out['items'].setdefault(name,{})['requires']=[ids('Bow of Faerdhinen','Bow of Faerdhinen (c)','Crystal bow')]
# Correct explicit equip gates where lead prose also mentions assembling the item.
for name,levels in {'Slayer helmet':{'DEFENCE':10},'Slayer helmet (i)':{'DEFENCE':10},'Twisted bow':{'RANGED':85},'Rune crossbow':{'RANGED':61},'Ancestral robe top':{'MAGIC':75,'DEFENCE':65},'Ancestral robe bottom':{'MAGIC':75,'DEFENCE':65},'Ancestral hat':{'MAGIC':75,'DEFENCE':65},'Avernic defender':{'ATTACK':70,'DEFENCE':70},'Toxic blowpipe':{'RANGED':75},'Bandos chestplate':{'DEFENCE':65},'Bandos tassets':{'DEFENCE':65},'Torva full helm':{'DEFENCE':80},'Torva platebody':{'DEFENCE':80},'Torva platelegs':{'DEFENCE':80}}.items():
 out['items'].setdefault(name,{}).update({'levels':levels,'requirementsKnown':True,'requirements':[]})
# Task protection alternatives are explicit; no arbitrary weapon or armour fallback can
# satisfy them. Vampyre equipment depends on species, Karuulm boots depend on location.
helm=['Slayer helmet (i)','Slayer helmet']
protection={'aberrant-spectres':('Nose peg or Slayer helmet',['Nose peg']+helm),'banshees':('Earmuffs or Slayer helmet',['Earmuffs']+helm),'dust-devils':('Facemask or Slayer helmet',['Facemask']+helm),'smoke-devils':('Facemask or Slayer helmet',['Facemask']+helm),'wall-beasts':('Spiny helmet or Slayer helmet',['Spiny helmet']+helm),'basilisks':("Mirror shield or V's shield",['Mirror shield',"V's shield"]),'cockatrice':("Mirror shield or V's shield",['Mirror shield',"V's shield"]),'harpie-bug-swarms':('Bug lantern (light before fighting)',['Lit bug lantern']),'skeletal-wyverns':('Wyvern breath shield',['Elemental shield','Mind shield','Dragonfire shield','Ancient wyvern shield','Dragonfire ward']),'fossil-island-wyverns':('Wyvern breath shield',['Elemental shield','Mind shield','Dragonfire shield','Ancient wyvern shield','Dragonfire ward'])}
for task,(label,names) in protection.items():out['tasks'][task]={'requiredItems':[supply(label,*names)]}
for task in ['vampyres','wyrms','drakes','hydras']:out['tasks'][task]={'requiredItems':[]}
out['tasks']['metal-dragons']={'requiredItems':[supply('Anti-dragon shield or super antifire','Anti-dragon shield','Dragonfire shield','Super antifire potion','Extended super antifire')]}
out['tasks']['sea-snakes']={'requiredItems':[supply('Antipoison','Antipoison(4)','Superantipoison(4)')]}
out['tasks']['cave-slime']={'requiredItems':[{'name':'Lit bullseye lantern','itemIds':[4550],'quantity':1,'required':True,'stackable':False}]}
out['items'].setdefault('Lit bug lantern',{})['itemIds']=[7053]
for path in (ROOT/'locations').glob('*.json'):
 d=json.loads(path.read_text())
 if 'karuulm' in d['locationId']:
  boots=supply('Karuulm heat protection','Boots of stone','Boots of brimstone','Granite boots',waiver='Elite Kourend & Kebos Diary reward claimed')
  out['locations'][d['locationId']]={'requiredItems':[boots]}
# Mandatory standard task tools absent from equipment name catalogues are resolved from
# existing pinned supply IDs by the compiler; these are retained by the original task data.
(ROOT/'advisor/overrides.json').write_text(json.dumps(out,indent=2,ensure_ascii=True)+'\n')
print('Wrote',len(out['items']),'item overrides,',len(out['tasks']),'task protection overrides and',len(out['locations']),'location protections')
