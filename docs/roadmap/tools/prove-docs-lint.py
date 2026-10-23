#!/usr/bin/env python3
"""Exercise required V2 linter failures on isolated temporary document copies."""
import argparse
import json
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

REPO=Path(__file__).resolve().parents[3]
LINTER=REPO/'docs/roadmap/tools/docs-lint.py'

def run(root):
    p=subprocess.run([sys.executable,str(LINTER),str(root)],capture_output=True,text=True)
    return p.returncode,p.stdout+p.stderr

def replace(path,old,new):
    text=path.read_text()
    if old not in text:raise AssertionError(f'fixture fragment absent: {old}')
    path.write_text(text.replace(old,new,1))

def row_change(path,index,new):
    text=path.read_text();line=next(x for x in text.splitlines() if x.startswith('| Q01 |'))
    cells=line.split('|');cells[index]=' '+new+' ';replace(path,line,'|'.join(cells))

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--report');args=ap.parse_args()
    code,output=run(REPO/'docs')
    if code:print(output);return 1
    rows=[]
    cases=[]
    for label in ['Why','Do','Expect','Common mistake','Undo','Senior habit','Check','If it fails','Status']:
        cases.append(('missing '+label,'step',lambda r,l=label:replace(r/'roadmap/01-FOUNDATION/BUILD-GUIDE.md','**'+l+':**','FIELD REMOVED')))
    cases.extend([
        ('duration over 90','duration',lambda r:replace(r/'roadmap/01-FOUNDATION/BUILD-GUIDE.md','(30–30 min)','(91–91 min)')),
        ('duplicate ID','step-ids',lambda r:replace(r/'roadmap/01-FOUNDATION/BUILD-GUIDE.md','### W01-S02','### W01-S01')),
        ('nonsequential ID','step-ids',lambda r:replace(r/'roadmap/01-FOUNDATION/BUILD-GUIDE.md','### W01-S02','### W01-S99')),
        ('unresolved map target','step-map',lambda r:replace(r/'roadmap/STEP-ID-MAP.md','| W01-S01 | '+next(x.split('|')[2].strip() for x in (r/'roadmap/STEP-ID-MAP.md').read_text().splitlines() if x.startswith('| W01-S01 |'))+' |','| W01-S01 | W99-S01 |')),
        ('missing old map row','step-map',lambda r:(r/'roadmap/STEP-ID-MAP.md').write_text('\n'.join(x for x in (r/'roadmap/STEP-ID-MAP.md').read_text().splitlines() if not x.startswith('| W01-S01 |'))+'\n')),
        ('missing enterprise gate','enterprise-gate',lambda r:replace(r/'roadmap/01-FOUNDATION/VERIFY-AND-GATE.md','## Enterprise gate','## Removed section')),
        ('missing minimum gate','minimum-gate',lambda r:replace(r/'roadmap/01-FOUNDATION/VERIFY-AND-GATE.md','## Minimum Viable Gate','## Removed section')),
        ('capacity overflow','capacity',lambda r:replace(r/'roadmap/01-FOUNDATION/README.md','**Planned hours:** A=13.75','**Planned hours:** A=99')),
        ('capacity arithmetic mismatch','capacity-total',lambda r:replace(r/'roadmap/01-FOUNDATION/README.md','**Planned hours:** A=13.75','**Planned hours:** A=13')),
        ('missing Q introducing week','quality-week',lambda r:row_change(r/'roadmap/QUALITY-COVERAGE.md',2,'missing')),
        ('missing Q enforcing ID','quality-steps',lambda r:row_change(r/'roadmap/QUALITY-COVERAGE.md',3,'W99-S01')),
        ('missing literal Q gate','quality-gate',lambda r:replace(r/'roadmap/01-FOUNDATION/VERIFY-AND-GATE.md','- [ ] Q01:','- [ ] Removed:')),
        ('vague title warning','vague-title',lambda r:replace(r/'roadmap/01-FOUNDATION/BUILD-GUIDE.md','Inventory the dirty workspace','Implement authentication')),
    ])
    for name,rule,mutate in cases:
        with tempfile.TemporaryDirectory(prefix='hrflow-doc-proof-') as temp:
            root=Path(temp)/'docs';shutil.copytree(REPO/'docs',root,ignore=shutil.ignore_patterns('__pycache__','node_modules','.DS_Store'))
            shutil.copy2(REPO/'README.md',Path(temp)/'README.md')
            baseline_rc,baseline_out=run(root)
            if baseline_rc:raise AssertionError('clean temp fixture must pass: '+baseline_out)
            mutate(root);rc,out=run(root)
            good=(rc==0 and 'WARNING:' in out and 'vague-title' in out) if rule=='vague-title' else (rc!=0 and ': '+rule+':' in out)
            rows.append({'fixture':name,'rule':rule,'expected_exit':'zero with warning' if rule=='vague-title' else 'nonzero','actual_exit':rc,'pass':good})
            print(('PASS' if good else 'FAIL')+': '+name+f' (exit {rc})')
            if not good:print(out)
    result={'real_docs_exit':code,'real_docs_result':output.strip(),'fixtures':rows}
    if args.report:Path(args.report).write_text(json.dumps(result,indent=2)+'\n')
    return 0 if all(x['pass'] for x in rows) else 1
if __name__=='__main__':sys.exit(main())
