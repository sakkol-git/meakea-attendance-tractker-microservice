#!/usr/bin/env python3
"""Portable HRFlow documentation checks. No third-party packages or application writes."""
import argparse
import re
import sys
from pathlib import Path
from urllib.parse import unquote

ALLOWED={'java','sql','yaml','bash','json','ts','dart','xml','properties','text','mermaid'}
# Explicit user-requested canonical name; README is the folder entry-point contract.
EXCEPT={'README.md','Architecture.md','Architecture.MD'}
REQUIRED={'README.md','BUILD-GUIDE.md','VERIFY-AND-GATE.md','TROUBLESHOOTING.md'}
LABELS=['**Why:**','**Read first','**Before you start:**','**Do:**','**Expect:**',
        '**Check:**','**If it fails:**','**Status:**']
errors=[]
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
            if '://' in dest or '#' in dest or ' ' in dest or not dest.lower().endswith('.md'):
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
            for label in LABELS:
                if label not in body: err(p,line,'step',f'{m[1]} missing {label}')
            if not re.search(r'\(\d+[–-]\d+ min\)',m[0].splitlines()[0]): err(p,line,'step','duration range required')
            if '**Run**' not in body and '**Not run**' not in body: err(p,line,'step','explicit execution status')
            if 'Committed with message' not in body: err(p,line,'step','step must end with commit task')
        for stone in re.split(r'^## Stone ',text,flags=re.M)[1:]:
            if not re.search(r'- \[ \] Tagged `w\d{2}-s\d{2}-green`',stone):
                err(p,1,'stone','each stone ends with green tag task')
    if p.name=='PATTERN-LESSON.md':
        for label in ['Admission test','Requirement','Baseline','Pain','Refactor','Class roles','Runnable example','Unit tests','Debugging','Trade-offs','When not to use','Spot it later','Pattern review']:
            if label not in text: err(p,1,'pattern',f'missing {label}')

def main():
    ap=argparse.ArgumentParser();ap.add_argument('paths',nargs='+');args=ap.parse_args()
    files=[]
    for arg in args.paths:
        p=Path(arg)
        files.extend(sorted(f for f in p.rglob('*') if f.suffix.lower()=='.md') if p.is_dir() else [p])
    for p in files: check(p)
    roots=[Path(x)/'roadmap' for x in args.paths if Path(x).is_dir() and (Path(x)/'roadmap').is_dir()]
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
    if errors:
        print('\n'.join(errors));print(f'FAIL: {len(errors)} errors');return 1
    print(f'PASS: {len(files)} Markdown files; zero errors');return 0
if __name__=='__main__': sys.exit(main())
