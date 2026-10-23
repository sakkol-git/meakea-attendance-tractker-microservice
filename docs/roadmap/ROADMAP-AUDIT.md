# Roadmap audit V2

## Measurement method and limits

Measured the incoming working tree, not a reconstructed plan: 182 step blocks in twelve existing week folders. Snapshot includes original ID, title, duration, body, stone and path. Each row below is traceable to that original ID through STEP-ID-MAP.md after decomposition.

D means stated duration exceeds 90; B means more than two artifact categories or more than three explicit file paths; C means multiple concept families in the work paragraph; E means absent/non-observable Expect; N means no negative/failure assertion in its stone work; I means no inspect instruction; T means a named technical term absent from glossary/primers; U means no exact Undo; V means a vague title starts with implement/configure/set up/handle.

B/C/T/V are conservative text-review signals, not measured execution time or proof that every named concept is new to the developer. Artifact families are tests, SQL, application, config, client, operations and contract. V is a warning heuristic, never a reason to delete a valid step. D uses stated upper duration; zero D failures does not make broad 60-minute estimates credible. Generic troubleshooting links alone do not prove a failure test.

## Per-week scorecard part one

| Week | Steps | D | B | C | E |
| --- | --- | --- | --- | --- | --- |
| 01 | 15 | 0 | 12 | 9 | 0 |
| 02 | 17 | 0 | 16 | 12 | 0 |
| 03 | 15 | 0 | 14 | 8 | 0 |
| 04 | 15 | 0 | 14 | 9 | 0 |
| 05 | 15 | 0 | 14 | 10 | 0 |
| 06 | 15 | 0 | 14 | 9 | 0 |
| 07 | 15 | 0 | 14 | 8 | 0 |
| 08 | 15 | 0 | 12 | 7 | 0 |
| 09 | 15 | 0 | 14 | 9 | 0 |
| 10 | 15 | 0 | 13 | 11 | 0 |
| 11 | 15 | 0 | 14 | 9 | 0 |
| 12 | 15 | 0 | 14 | 11 | 0 |

## Per-week scorecard part two

| Week | N | I | T | U | V warning |
| --- | --- | --- | --- | --- | --- |
| 01 | 0 | 0 | 0 | 15 | 0 |
| 02 | 0 | 0 | 0 | 17 | 0 |
| 03 | 0 | 0 | 0 | 15 | 0 |
| 04 | 0 | 0 | 0 | 15 | 0 |
| 05 | 0 | 0 | 0 | 15 | 0 |
| 06 | 0 | 0 | 0 | 15 | 0 |
| 07 | 0 | 0 | 0 | 15 | 0 |
| 08 | 0 | 0 | 1 | 15 | 0 |
| 09 | 0 | 0 | 0 | 15 | 0 |
| 10 | 0 | 0 | 0 | 15 | 0 |
| 11 | 0 | 0 | 0 | 15 | 0 |
| 12 | 0 | 0 | 0 | 15 | 0 |

## Baseline capacity and stated estimates

| Week | A step h | B step h | Limit each h | Reading and reserve |
| --- | --- | --- | --- | --- |
| 01 | 10 | 7 | 16.8 | All additional work must fit remaining capacity |
| 02 | 12 | 7 | 16.8 | All additional work must fit remaining capacity |
| 03 | 10 | 7 | 16.8 | All additional work must fit remaining capacity |
| 04 | 7.5 | 5.25 | 14.28 | All additional work must fit remaining capacity |
| 05 | 10 | 7 | 16.8 | All additional work must fit remaining capacity |
| 06 | 10 | 7 | 16.8 | All additional work must fit remaining capacity |
| 07 | 10 | 7 | 16.8 | All additional work must fit remaining capacity |
| 08 | 7.5 | 5.25 | 14.28 | All additional work must fit remaining capacity |
| 09 | 10 | 7 | 16.8 | All additional work must fit remaining capacity |
| 10 | 10 | 7 | 16.8 | All additional work must fit remaining capacity |
| 11 | 10 | 7 | 16.8 | All additional work must fit remaining capacity |
| 12 | 10 | 7 | 16.8 | All additional work must fit remaining capacity |

## Ranked fifteen worst steps

Ranking is score descending, then artifact breadth and work-text length; it prioritizes review rather than pretends to measure actual coding hours.

| Rank | Old ID | Original intent | Failing signals | Why review first |
| --- | --- | --- | --- | --- |
| 1 | W12-S01 | Record exact release inputs | B,C,U | 6 artifact families, 3 concept families; missing bounded undo |
| 2 | W01-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 3 | W02-S14 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 4 | W03-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 5 | W04-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 6 | W05-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 7 | W06-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 8 | W07-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 9 | W08-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 10 | W09-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 11 | W10-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 12 | W11-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 13 | W12-S12 | Close only the reviewed gap | B,C,U | 5 artifact families, 4 concept families; missing bounded undo |
| 14 | W11-S01 | Run every two-tenant route case | B,C,U | 5 artifact families, 3 concept families; missing bounded undo |
| 15 | W08-S01 | Compare OpenAPI and live responses | B,T,U | 5 artifact families, 1 concept families; missing bounded undo |

## Every original step scored

An em dash means no failing signal. Original wording/expected behavior remain in the pre-pass snapshot and Git history; this ledger does not mark implementation done.

| Original ID | Signals | Artifact families | Review disposition |
| --- | --- | --- | --- |
| W01-S01 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W01-S02 | C,U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W01-S03 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W01-S04 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W01-S05 | B,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W01-S06 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W01-S07 | B,C,U | test,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W01-S08 | B,U | test,application,config,client,operations | Add exact recovery; split coupled test/change work; retain intent |
| W01-S09 | B,U | test,application,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W01-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W01-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W01-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W01-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W01-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W01-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W02-S01 | B,C,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W02-S02 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W02-S03 | B,C,U | test,application,config,operations,contract | Add exact recovery; split coupled test/change work; retain intent |
| W02-S04 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W02-S05 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W02-S06 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W02-S07 | B,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W02-S08 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W02-S09 | B,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W02-S10 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W02-S11 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W02-S12 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W02-S13 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W02-S14 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W02-S15 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W02-S16 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W02-S17 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W03-S01 | B,C,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W03-S02 | B,C,U | test,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W03-S03 | B,C,U | test,config,client,contract | Add exact recovery; split coupled test/change work; retain intent |
| W03-S04 | B,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W03-S05 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W03-S06 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W03-S07 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W03-S08 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W03-S09 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W03-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W03-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W03-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W03-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W03-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W03-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W04-S01 | B,C,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W04-S02 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W04-S03 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W04-S04 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W04-S05 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W04-S06 | B,U | test,config,client,contract | Add exact recovery; split coupled test/change work; retain intent |
| W04-S07 | B,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W04-S08 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W04-S09 | B,U | test,SQL,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W04-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W04-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W04-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W04-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W04-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W04-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W05-S01 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W05-S02 | B,C,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W05-S03 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W05-S04 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W05-S05 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W05-S06 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W05-S07 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W05-S08 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W05-S09 | B,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W05-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W05-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W05-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W05-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W05-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W05-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W06-S01 | B,C,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W06-S02 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W06-S03 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W06-S04 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W06-S05 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W06-S06 | B,C,U | test,SQL,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W06-S07 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W06-S08 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W06-S09 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W06-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W06-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W06-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W06-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W06-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W06-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W07-S01 | B,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W07-S02 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W07-S03 | B,U | test,config,client,operations,contract | Add exact recovery; split coupled test/change work; retain intent |
| W07-S04 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W07-S05 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W07-S06 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W07-S07 | B,C,U | test,application,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W07-S08 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W07-S09 | B,C,U | test,application,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W07-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W07-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W07-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W07-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W07-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W07-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W08-S01 | B,T,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W08-S02 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W08-S03 | B,U | test,config,client,operations | Add exact recovery; split coupled test/change work; retain intent |
| W08-S04 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W08-S05 | B,C,U | test,SQL,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W08-S06 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W08-S07 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W08-S08 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W08-S09 | B,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W08-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W08-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W08-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W08-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W08-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W08-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W09-S01 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W09-S02 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W09-S03 | B,U | test,SQL,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W09-S04 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W09-S05 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W09-S06 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W09-S07 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W09-S08 | B,U | test,SQL,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W09-S09 | B,C,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W09-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W09-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W09-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W09-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W09-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W09-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W10-S01 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S02 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S03 | B,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S04 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S05 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S06 | B,C,U | test,config,client,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S07 | C,U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W10-S08 | B,U | test,application,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S09 | B,U | test,application,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W10-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W10-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W10-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W11-S01 | B,C,U | test,application,config,operations,contract | Add exact recovery; split coupled test/change work; retain intent |
| W11-S02 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W11-S03 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W11-S04 | B,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W11-S05 | B,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W11-S06 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W11-S07 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W11-S08 | B,U | test,application,config | Add exact recovery; split coupled test/change work; retain intent |
| W11-S09 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W11-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W11-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W11-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W11-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W11-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W11-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |
| W12-S01 | B,C,U | test,SQL,application,config,operations,contract | Add exact recovery; split coupled test/change work; retain intent |
| W12-S02 | B,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W12-S03 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W12-S04 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W12-S05 | B,C,U | test,SQL,config | Add exact recovery; split coupled test/change work; retain intent |
| W12-S06 | B,C,U | test,config,client,operations | Add exact recovery; split coupled test/change work; retain intent |
| W12-S07 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W12-S08 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W12-S09 | B,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W12-S10 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W12-S11 | B,C,U | test,config,client | Add exact recovery; split coupled test/change work; retain intent |
| W12-S12 | B,C,U | test,SQL,application,config,contract | Add exact recovery; split coupled test/change work; retain intent |
| W12-S13 | B,C,U | test,SQL,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W12-S14 | B,C,U | test,config,operations | Add exact recovery; split coupled test/change work; retain intent |
| W12-S15 | U | test,config | Add exact recovery; split coupled test/change work; retain intent |

## Baseline metrics

Average upper duration: 57.53 minutes. Maximum: 60. Steps over 90: 0. Missing Expect: 0. Missing Undo: 182.

All fourteen Q-items lack a complete introducing-step plus enforcing-step plus named gate-checkbox chain in the old suite. Existing mentions of security/outbox/CI are useful but not equivalent to enforceable coverage. Phase 3 adds shared assets; phase 4 maps atomic actions; phase 5 closes quality gate gaps.
