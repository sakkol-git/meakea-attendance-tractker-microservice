// Usage: node docs/validation/check-mermaid.mjs <temporary dependency directory> <diagrams.json>
// Install mermaid and jsdom outside the application; this checks parser syntax, not visual layout.
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {pathToFileURL} from 'node:url';
if(process.argv.length!==4) throw new Error('Expected dependency directory and diagrams.json');
const require=createRequire(path.resolve(process.argv[2],'package.json'));
const {JSDOM}=require('jsdom');
const dom=new JSDOM('<!doctype html><html><body></body></html>');
globalThis.window=dom.window;
globalThis.document=dom.window.document;
Object.defineProperty(globalThis,'navigator',{value:dom.window.navigator,configurable:true});
const {default:mermaid}=await import(pathToFileURL(require.resolve('mermaid')));
mermaid.initialize({startOnLoad:false,securityLevel:'strict'});
const diagrams=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));
let failures=0;
for(let i=0;i<diagrams.length;i++) {
  try { await mermaid.parse(diagrams[i].source); process.stdout.write(`PASS ${i+1} ${diagrams[i].file}\n`); }
  catch(e) { failures++; process.stderr.write(`FAIL ${i+1} ${e.message}\n`); }
}
process.stdout.write(`${diagrams.length-failures}/${diagrams.length} Mermaid diagrams parsed\n`);
process.exitCode=failures?1:0;
