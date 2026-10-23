# Confidence prototype workflows

[DESIGN] This is a domain body of the [business-flow reference](../../business-flow.md). Evidence labels distinguish targets from current source; status applies to the whole named workflow. Source absence is bounded by the [inventory](_inventory.md).

## Workflows

### BF-032  Standalone confidence scoring — Status: IMPLEMENTED

- Purpose (business): [IMPL] Return a presence score/decision; do not create attendance. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score)
- Trigger / entry point: [IMPL] POST /score, Python handler score. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score)
- Actors and permissions: [IMPL] No authenticated actor/tenant binding exists in this private prototype. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score)
- Preconditions and inputs: [IMPL] All Evidence fields required; strings for IDs/date; constrained flags/counts and early-arrival float. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score)
- Main flow:

    1. [IMPL] Validate Evidence; remove tenant/employee/date from the feature dictionary. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score)
    1. [IMPL] Mock flag forces raw zero; otherwise use loaded model or missing-artifact rules fallback. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score)
    1. [IMPL] Compare raw probability to thresholds, round output afterward and echo caller identifiers. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [IMPL] Current artifact raw p >= 0.80 | AUTO_INFER; fallback auto threshold is 0.90 | ConfidenceModel.__init__/score; ml/confidence-service/models/meta.json; ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score |
    | [IMPL] ask <= p < auto / p < ask | ASK_EMPLOYEE / NO_ACTION; ask currently 0.50 | ConfidenceModel.score; ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score |
    | [IMPL] Mock location | Raw zero, one rule reason; current metadata yields NO_ACTION | ConfidenceModel.score; ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [IMPL] Invalid constrained field | FastAPI validation 422, no score | api.py Evidence; test_bad_input_rejected; ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score |
    | [IMPL] Caller supplies another tenant/date string | Accepted/echoed if otherwise valid; no identity validation | api.py Evidence/score; ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score |
    | [IMPL] Model/artifact evaluation error | No model-load or scoring recovery wrapper in source | ConfidenceModel.__init__/score; ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score |

- State transitions: [IMPL] Request → score response only; no employee/session state transition. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score)
- Data effects: [IMPL] No PostgreSQL write, session insertion or outbox. Reads loaded pipeline/meta from process memory. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [IMPL] No Kafka producer/consumer; HTTP demo is not the designed inference integration. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [IMPL] No persistent effect, idempotency key or attendance-side dedup; the handler evaluates the loaded model independently for each request. (ml/confidence-service/confidence/api.py#score)
- Security: [IMPL] No authentication or trusted tenant binding in the Python API; loopback launcher only limits exposure. (ml/confidence-service/confidence/api.py#Evidence; ml/confidence-service/confidence/api.py#score; ml/confidence-service/run.py#server_options)
- QA scenarios: QA-063, QA-064; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Standalone source exists for this limited workflow; no HR integration is inferred. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score) [UNKNOWN] Metadata-selected probabilities are not evidence of reliable real-world employee presence; model provenance/consent unknown. (ml/confidence-service/confidence/api.py#score; ml/confidence-service/confidence/model.py#ConfidenceModel.score)

### BF-033  Model health and metadata inspection — Status: IMPLEMENTED

- Purpose (business): [IMPL] Inspect the loaded prototype model and process health. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)
- Trigger / entry point: [IMPL] GET /health and GET /model. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)
- Actors and permissions: [IMPL] No authentication; private launcher binding is exposure reduction, not tenant authorization. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)
- Preconditions and inputs: [IMPL] N/A: no request body. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)
- Main flow:

    1. [IMPL] Load model once when api.py imports the application. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)
    1. [IMPL] Return UP plus modelVersion from health. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)
    1. [IMPL] Return the full in-memory metadata from info. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [IMPL] Missing model/meta pair | Use rules-only defaults | ConfidenceModel.__init__; ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__ |
    | [IMPL] Artifact changes on disk | No hot-reload code; process retains loaded state | api.py module model variable; ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__ |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [IMPL] Corrupt/incompatible artifact | Load exception propagates; no fallback on this path | ConfidenceModel.__init__; ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__ |
    | [IMPL] Health UP | Does not test DB/Kafka or attendance correctness | api.py health; ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__ |

- State transitions: [IMPL] No HR state; process startup → loaded model or exception. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)
- Data effects: [IMPL] Reads model.joblib/meta.json if both exist; no metadata redaction or DB query. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [IMPL] N/A: no integration publication. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: N/A: metadata/health are read-only; [IMPL] artifacts load once without a hot-reload lock/protocol. (ml/confidence-service/confidence/api.py#model; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)
- Security: [IMPL] No authentication or trusted tenant binding in the Python API; loopback launcher only limits exposure. (ml/confidence-service/confidence/api.py#Evidence; ml/confidence-service/confidence/api.py#score; ml/confidence-service/run.py#server_options)
- QA scenarios: QA-065, QA-066; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Standalone source exists for this limited workflow; no HR integration is inferred. (ml/confidence-service/confidence/api.py#health; ml/confidence-service/confidence/api.py#info; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)

### BF-034  Synthetic data and model training — Status: IMPLEMENTED

- Purpose (business): [IMPL] Build a local model artifact from labeled features. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates)
- Trigger / entry point: [IMPL] Python CLI modules make_synthetic/train; neither training command was run in this task. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates)
- Actors and permissions: [IMPL] Local developer/operator; data consent and real training provenance UNKNOWN. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates)
- Preconditions and inputs: [IMPL] CSV with FEATURES and label; at least two labels and thirty samples per represented label; synthetic defaults four thousand rows. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates)
- Main flow:

    1. [IMPL] Optionally generate clearly fake feature/label rows. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates)
    1. [IMPL] Split data, compare five-fold candidates and prefer boosting only above 0.01 AUC improvement. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates)
    1. [IMPL] Calibrate, select threshold with floor 0.80 and write joblib then metadata. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [IMPL] Missing columns / inadequate label counts | SystemExit before fitting | train.py main; ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates |
    | [IMPL] Model threshold choice | Chosen/evaluated with the same test split; independent final policy evaluation absent | train.py main; ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [IMPL] Crash between artifact writes | No pair-level transactional publish/rollback | train.py main; ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates |
    | [IMPL] Deploy new files while serving | No model hot reload; restart/rollout procedure missing | api.py module model variable; ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates |

- State transitions: [IMPL] Input CSV → model.joblib + meta.json; no employee state or label-ingestion consumer. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates)
- Data effects: [IMPL] Writes chosen output directory after mkdir; training pair write is sequential, not a proven atomic swap. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [IMPL] Confirmation-label ingestion and scheduled training are designed, not implemented. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [IMPL] Training writes joblib then metadata without a pair-level swap or writer lock; parallel training/output replacement has no coordination. (ml/confidence-service/training/train.py#main)
- Security: [IMPL] CLI reads a local CSV and writes local artifacts; no per-tenant filtering or file access control layer is implemented. [UNKNOWN] Real-data consent and trusted model/artifact publication policy are not established. (ml/confidence-service/training/train.py#main; ml/confidence-service/confidence/model.py#ConfidenceModel.__init__)
- QA scenarios: QA-067, QA-068; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Standalone source exists for this limited workflow; no HR integration is inferred. (ml/confidence-service/training/make_synthetic.py#make; ml/confidence-service/training/train.py#main; ml/confidence-service/training/train.py#candidates)
