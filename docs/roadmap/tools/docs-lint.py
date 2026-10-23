#!/usr/bin/env python3
"""Portable HRFlow documentation checks. No third-party packages or application writes."""
import argparse
import json
import re
import sys
from pathlib import Path
from urllib.parse import unquote

ALLOWED={'java','sql','yaml','bash','json','ts','dart','xml','properties','text','mermaid'}
# Explicit user-requested canonical name; README is the folder entry-point contract.
EXCEPT={'README.md','Architecture.md'}
REQUIRED={'README.md','BUILD-GUIDE.md','VERIFY-AND-GATE.md','TROUBLESHOOTING.md'}
LABELS=['**Why:**','**Read first','**Before you start:**','**Do:**','**Expect:**',
        '**Common mistake:**','**Undo:**','**Senior habit:**',
        '**Check:**','**If it fails:**','**Status:**']
errors=[]
warnings=[]
def warn(p,n,r,m): warnings.append(f'{p}:{n}: {r}: {m}')
def err(p,n,r,m): errors.append(f'{p}:{n}: {r}: {m}')
def check(p):
    try: raw=p.read_bytes(); text=raw.decode('utf-8')
    except (OSError,UnicodeError) as ex: err(p,1,'encoding',str(ex)); return
    if b'\r' in raw: err(p,1,'LF','CR characters are forbidden')
    if not raw.endswith(b'\n') or raw.endswith(b'\n\n'): err(p,1,'newline','exactly one final newline')
    if len(text.splitlines())>500: err(p,501,'length','split files over 500 lines')
    if p.name not in EXCEPT and not re.fullmatch(r'[A-Z0-9]+(?:-[A-Z0-9]+)*\.md',p.name):
        err(p,1,'filename','use UPPER-KEBAB-CASE.md')
    lines=text.splitlines(); fence=None; headings=[]; inmer=[]; code=[]; previous_level=0
    if not lines or not lines[0].startswith('# '): err(p,1,'heading','H1 must be line 1')
    for i,line in enumerate(lines,1):
        if '\t' in line: err(p,i,'tabs','tabs are forbidden')
        if line.rstrip()!=line: err(p,i,'whitespace','trailing spaces')
        if line.startswith('```'):
            if fence is None:
                fence=line[3:]; code=[]
                if fence not in ALLOWED: err(p,i,'fence','unsupported or missing language')
            elif line=='```':
                if fence=='mermaid':
                    if not code or not re.match(r'(flowchart|sequenceDiagram|erDiagram|stateDiagram-v2)',code[0]):
                        err(p,i,'mermaid','unsupported diagram type')
                    if re.search(r'\b(style|classDef|linkStyle)\b','\n'.join(code)): err(p,i,'mermaid','no styling')
                    nodes=set(re.findall(r'\b(\w+)\[', '\n'.join(code)))
                    if len(nodes)>15: err(p,i,'mermaid','at most 15 nodes')
                    if re.search(r'\[[^"\]]', '\n'.join(code)): err(p,i,'mermaid','quote flowchart labels')
                    after=lines[i:i+4]
                    if not any(x=='Text version:' for x in after): err(p,i,'mermaid','follow diagram with Text version: and bullets')
                fence=None
            else: err(p,i,'fence','nested or malformed fence')
            continue
        if fence:
            code.append(line)
            if len(line)>100: err(p,i,'code-width','code line exceeds 100 characters')
            if fence=='bash' and line.startswith('$ '): err(p,i,'command','omit shell prompt')
            continue
        h=re.match(r'^(#{1,6}) (.*)',line)
        if h:
            level=len(h[1]);headings.append((level,i))
            if level>3: err(p,i,'heading','only H1 through H3')
            if level>previous_level+1: err(p,i,'heading','do not skip a level')
            previous_level=level
            if re.search(r'[`*_\[\]<>]',h[2]): err(p,i,'heading','heading must be plain text')
        if re.fullmatch(r'\s*(---+|___+|\*\*\*+)\s*',line): err(p,i,'rule','no horizontal rules/front matter')
        plain=re.sub(r'`[^`]*`','',line)
        if re.search(r'<\/?[A-Za-z][^>]*>',plain): err(p,i,'html','raw HTML/placeholders forbidden')
        if any(x in plain for x in ['[[','{{','==']): err(p,i,'markup','unsupported highlight/wiki/template markup')
        if re.search(r'\[\^[^\]]+\]',plain): err(p,i,'footnote','no footnotes')
        status_plain=re.sub(r'\*\*[^*]+\*\*','',plain)
        if re.search(r'\b(?:CONDITIONAL PASS|NOT RUN|PASS|FAIL)\b',status_plain):
            err(p,i,'status','gate status words must be bold')
        if line.endswith('\\'): err(p,i,'line-break','no forced prose line break')
        if '$' in plain: err(p,i,'prose','wrap dollar signs/math markers in backticks')
        if re.search(r'(?<!\w)#[A-Za-z]',plain) and not h: err(p,i,'prose','wrap hash words in backticks')
        if line.startswith('>') and line and not re.match(r'^> \*\*(Note|Warning|Decision|Evidence|Pitfall|Navigation):\*\*',line):
            err(p,i,'blockquote','use an approved bold callout label')
        li=re.match(r'^( *)([-*+]|\d+\.) (.*)',line)
        if li:
            indent,mark,body=li.groups()
            if mark not in {'-','1.'}: err(p,i,'list','only - and 1. markers')
            if len(indent)%4 or len(indent)>4: err(p,i,'list','four-space indent, at most two levels')
            if i>1 and lines[i-2].strip() and not re.match(r'^( *)(-|1\.) ',lines[i-2]):
                err(p,i,'list-space','blank line before a list')
            if body.startswith('[') and not re.match(r'\[ \] ',body) and re.match(r'\[[xX]\]',body):
                err(p,i,'task','tasks must remain unchecked in authored guide')
        if line.startswith('|'):
            cells=re.split(r'(?<!\\)\|',re.sub(r'`[^`]+`','CODE',line))[1:-1]
            if len(cells)>6: err(p,i,'table','at most six columns')
            if any(not c.strip() for c in cells): err(p,i,'table','empty cell; use em dash')
            if '[ ]' in line or '[x]' in line: err(p,i,'table','no task items in tables')
            if (i==1 or not lines[i-2].startswith('|')) and i<len(lines):
                if not re.match(r'^\|[ :\-|]+\|$',lines[i]): err(p,i,'table','missing header separator')
        for m in re.finditer(r'\[[^\]]+\]\(([^)]+)\)',line):
            dest=m[1]
            if dest.startswith('https://'): continue
            if '://' in dest or '#' in dest or ' ' in dest or not dest.endswith('.md'):
                err(p,i,'link','relative explicit .md only; no anchors, folders or source links');continue
            if not (p.parent/unquote(dest)).is_file(): err(p,i,'link',f'missing target {dest}')
    if fence: err(p,len(lines),'fence','unclosed fence')
    if sum(1 for level,_ in headings if level==1)!=1: err(p,1,'heading','exactly one H1')
    # Reject hard-wrapped paragraphs, but permit list continuations/tables and example syntax.
    prose=lambda s: bool(s.strip()) and not re.match(r'^(#|>|\||```|    |-|1\.)',s)
    inside=False
    code_lines=set()
    for ix, val in enumerate(lines):
        if val.startswith('```'):
            code_lines.add(ix); inside=not inside
        elif inside: code_lines.add(ix)
    for i in range(1,len(lines)):
        if i not in code_lines and i-1 not in code_lines and prose(lines[i-1]) and prose(lines[i]): err(p,i+1,'paragraph','one paragraph per line')
    if 'BUILD-GUIDE' in p.name:
        for m in re.finditer(r'^### (W\d{2}-S\d{2})[^\n]*\n(.*?)(?=^### |^## |\Z)',text,re.M|re.S):
            body=m[2]; line=text[:m.start()].count('\n')+1
            title=re.sub(r'\s*\(\d+[–-]\d+ min\)$','',m[0].splitlines()[0].split(' — ',1)[-1])
            if re.fullmatch(r'(implement|configure|set up|handle)\s+[\w -]+',title,re.I):
                warn(p,line,'vague-title',f'{m[1]} title may be vague; heuristic only')
            for label in LABELS:
                if label not in body: err(p,line,'step',f'{m[1]} missing {label}')
            duration=re.search(r'\((\d+)[–-](\d+) min\)',m[0].splitlines()[0])
            if not duration: err(p,line,'step','duration range required')
            elif not 30 <= int(duration[1]) <= int(duration[2]) <= 90:
                err(p,line,'duration','step range must stay between 30 and 90 minutes')
            if '**Run**' not in body and '**Not run**' not in body: err(p,line,'step','explicit execution status')
            if 'Committed with message' not in body: err(p,line,'step','step must end with commit task')
        for stone in re.split(r'^## Stone ',text,flags=re.M)[1:]:
            count=len(re.findall(r'^### W\d{2}-S\d{2}',stone,re.M))
            if not 2 <= count <= 6: err(p,1,'stone','each stone needs two to six steps')
            if not re.search(r'- \[ \] Tagged `w\d{2}-s\d{2}-green`',stone):
                err(p,1,'stone','each stone ends with green tag task')
    if p.name=='PATTERN-LESSON.md':
        for label in ['Admission test','Requirement','Baseline','Pain','Refactor','Class roles','Runnable example','Unit tests','Debugging','Trade-offs','When not to use','Spot it later','Pattern review']:
            if label not in text: err(p,1,'pattern',f'missing {label}')

def check_v2(root):
    """Cross-file roadmap invariants. Scope/file/concept estimates remain human-reviewed."""
    dashboard=root/'YOU-ARE-HERE.md'
    text=dashboard.read_text() if dashboard.exists() else ''
    caps=re.findall(r'\*\*Capacity inputs:\*\* A=(\d+(?:\.\d+)?); B=(\d+(?:\.\d+)?); fraction=(\d+(?:\.\d+)?); light=(\d+(?:\.\d+)?)',text)
    if len(caps)!=1:
        err(dashboard,1,'capacity-input','one numeric capacity input block required'); return
    try:
        ca,cb,fraction,light=map(float,caps[0])
        if min(ca,cb)<=0 or not 0<fraction<=1 or not 0<light<=1: raise ValueError()
    except ValueError:
        err(dashboard,1,'capacity-input','positive hours and fractions in (0,1] required'); return
    weeks=sorted(p for p in root.iterdir() if p.is_dir() and re.match(r'^(0[1-9]|1[0-2])-',p.name))
    all_ids=set()
    for week in weeks:
        ids=[]; minutes={'A':0,'B':0}; stones=0
        for guide in sorted(week.glob('BUILD-GUIDE*.md'),key=lambda x:(x.name!='BUILD-GUIDE.md',x.name)):
            source=guide.read_text()
            for m in re.finditer(r'^### (W\d{2}-S\d{2})[^\n]*\n(.*?)(?=^### |^## |\Z)',source,re.M|re.S):
                sid,body=m[1],m[2];ids.append(sid)
                if sid[:3]!='W'+week.name[:2]: err(guide,1,'step-ids','week prefix does not match folder')
                duration=re.search(r'\((\d+)[–-](\d+) min\)',m[0].splitlines()[0])
                who=re.search(r'\*\*Why:\*\* (A and B|A|B)\b',body)
                if not who: err(guide,1,'capacity-owner',f'{sid} needs explicit A/B owner')
                elif duration:
                    for owner in ('A','B'):
                        if owner in who[1]:minutes[owner]+=int(duration[2])
            for stone in re.split(r'^## Stone ',source,flags=re.M)[1:]:
                stones+=1; last=re.findall(r'^### (W\d{2}-S\d{2})',stone,re.M)
                tag=re.search(r'- \[ \] Tagged `(w\d{2}-s\d{2}-green)`',stone)
                if last and tag and tag[1]!=last[-1].lower()+'-green':
                    err(guide,1,'stone-tag','tag must match final step in stone')
        if not 5<=stones<=8:err(week,1,'stone-count','week must have five to eight stones')
        if len(ids)!=len(set(ids)) or [int(x[-2:]) for x in ids]!=list(range(1,len(ids)+1)):
            err(week,1,'step-ids','unique sequential step IDs required per week')
        if all_ids.intersection(ids):err(week,1,'step-ids','duplicate global step ID')
        all_ids.update(ids)
        verify=week/'VERIFY-AND-GATE.md';gate=verify.read_text() if verify.exists() else ''
        if not re.search(r'^## Enterprise gate$',gate,re.M):err(verify,1,'enterprise-gate','Enterprise gate section required')
        if not re.search(r'^## Minimum Viable Gate$',gate,re.M):err(verify,1,'minimum-gate','Minimum Viable Gate section required')
        qtasks=re.findall(r'^- \[ \] (Q\d{2}):',gate,re.M)
        if not 3<=len(qtasks)<=6:err(verify,1,'enterprise-gate','three to six explicit Q-item tasks required')
        readme=week/'README.md';overview=readme.read_text() if readme.exists() else ''
        declared=re.search(r'\*\*Planned hours:\*\* A=(\d+(?:\.\d+)?); B=(\d+(?:\.\d+)?)',overview)
        extras=re.search(r'\*\*Extra hours:\*\* A=(\d+(?:\.\d+)?); B=(\d+(?:\.\d+)?)',overview)
        if not declared or not extras:err(readme,1,'capacity','planned and extra owner hours required');continue
        limits=[ca*fraction,cb*fraction]
        if week.name[:2] in ['04','08']:limits=[x*light for x in limits]
        for j,owner in enumerate(['A','B']):
            value=float(declared[j+1]);expected=minutes[owner]/60+float(extras[j+1])
            if value>limits[j]+0.001:err(readme,1,'capacity',f'{owner} planned {value:g} exceeds {limits[j]:g}')
            if abs(value-expected)>0.011:err(readme,1,'capacity-total',f'{owner} README {value:g} differs from steps+extras {expected:g}')
    for name in ['STEP-ID-MAP.md','DRAFT-ID-MAP.md']:
        path=root/name
        if not path.exists():err(path,1,'step-map','ID map required');continue
        old_ids=set();rows=0
        for n,line in enumerate(path.read_text().splitlines(),1):
            cells=[x.strip() for x in line.split('|')[1:-1]]
            if len(cells)<2 or not re.fullmatch(r'W\d{2}-S\d{2}',cells[0]):continue
            rows+=1
            if cells[0] in old_ids:err(path,n,'step-map','duplicate old ID')
            old_ids.add(cells[0]);targets=re.findall(r'W\d{2}-S\d{2}',cells[1])
            if not targets or any(t not in all_ids for t in targets):err(path,n,'step-map','every mapped target must resolve')
        if not rows:err(path,1,'step-map','no mapping rows')
        baseline=root.parent/'validation'/'ROADMAP-V1-BASELINE.json'
        opening=root.parent/'validation'/'ROADMAP-V2-PAIR-12-DRAFT.json'
        if baseline.is_file():
            try:
                records=json.loads(baseline.read_text())
                expected={x['id'] for x in records}
                if name=='DRAFT-ID-MAP.md' and opening.is_file():
                    expected={x['id'] for x in records if x['week']>2}
                    for week,items in json.loads(opening.read_text()).items():
                        expected.update(f'W{int(week):02}-S{i:02}' for i in range(1,len(items)+1))
                if old_ids!=expected:err(path,1,'step-map','all baseline old IDs must have exactly one row')
            except (ValueError,KeyError,TypeError):err(path,1,'step-map','baseline ID inventory is invalid')
    coverage=root/'QUALITY-COVERAGE.md'
    if not coverage.exists():err(coverage,1,'quality-coverage','coverage matrix required');return
    seen=set()
    for n,line in enumerate(coverage.read_text().splitlines(),1):
        cells=[x.strip() for x in line.split('|')[1:-1]]
        if not cells or not re.fullmatch(r'Q\d{2}',cells[0]):continue
        q=cells[0]
        if q in seen:err(coverage,n,'quality-coverage','duplicate Q row')
        seen.add(q)
        if len(cells)<4:err(coverage,n,'quality-coverage','week, steps and gate required');continue
        if not re.fullmatch(r'0?[1-9]|1[0-2]',cells[1]):err(coverage,n,'quality-week','introducing week 01–12 required')
        refs=re.findall(r'W\d{2}-S\d{2}',cells[2])
        if not refs or any(t not in all_ids for t in refs):err(coverage,n,'quality-steps','enforcing IDs must all resolve')
        link=re.search(r'\[[^\]]+\]\(([^)]+\.md)\)',cells[3])
        target=root/link[1] if link else None
        if target is None or not target.is_file() or not re.search(r'^- \[ \] '+q+r':',target.read_text(),re.M):
            err(coverage,n,'quality-gate','linked file must contain the literal Q checkbox')
    if seen!={f'Q{i:02}' for i in range(1,15)}:err(coverage,1,'quality-coverage','all fourteen Q-items required')


def main():
    ap=argparse.ArgumentParser();ap.add_argument('paths',nargs='+');args=ap.parse_args()
    files=[]
    for arg in args.paths:
        p=Path(arg)
        files.extend(sorted(f for f in p.rglob('*') if f.suffix.lower()=='.md') if p.is_dir() else [p])
    for p in files: check(p)
    roots=sorted({Path(x)/'roadmap' for x in args.paths if Path(x).is_dir() and (Path(x)/'roadmap').is_dir()} | {Path(x) for x in args.paths if Path(x).is_dir() and Path(x).name=='roadmap' and (Path(x)/'YOU-ARE-HERE.md').exists()})
    for root in roots:
        weeks=sorted(p for p in root.iterdir() if re.match(r'^(0[1-9]|1[0-2])-',p.name) and p.is_dir())
        # Preparation commits explicitly declare weekly authoring incomplete until pair generation finishes.
        progress=(root/'PROGRESS.md').read_text() if (root/'PROGRESS.md').exists() else ''
        if 'Weekly authoring complete: yes.' in progress and len(weeks)!=12:
            err(root,1,'weeks','exactly 12 week folders required at completion')
        for week in weeks:
            for n in REQUIRED:
                if not (week/n).exists(): err(week,1,'week-files',f'missing {n}')
            ids=[]
            for f in sorted(week.glob('BUILD-GUIDE*.md'), key=lambda x: (x.name!='BUILD-GUIDE.md',x.name)):
                ids+=re.findall(r'^### (W\d{2}-S\d{2})',f.read_text(),re.M)
            if len(ids)!=len(set(ids)): err(week,1,'step-ids','duplicate step ID')
            if ids and [int(x[-2:]) for x in ids]!=list(range(1,len(ids)+1)):
                err(week,1,'step-ids','steps must be sequential across guide parts')
        for folder in [root]+[p for p in root.rglob('*') if p.is_dir() and p.name!='__pycache__']:
            if not (folder/'README.md').exists(): err(folder,1,'folder-index','README.md required')
    for root in roots: check_v2(root)
    for warning in warnings: print('WARNING: '+warning)
    if errors:
        print('\n'.join(errors));print(f'FAIL: {len(errors)} errors');return 1
    print(f'PASS: {len(files)} Markdown files; zero errors; {len(warnings)} warnings');return 0
if __name__=='__main__': sys.exit(main())
