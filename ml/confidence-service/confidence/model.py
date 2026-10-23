import json
from pathlib import Path

import joblib
import numpy as np
import pandas as pd

from .features import FEATURES
from .rules import rules_score

MODEL_DIR = Path(__file__).resolve().parent.parent / "models"


class ConfidenceModel:
    def __init__(self, model_dir: Path = MODEL_DIR):
        self.pipeline = None
        self.meta = {"version": "rules-only", "thresholds": {"auto": 0.90, "ask": 0.50}}
        model_file, meta_file = model_dir / "model.joblib", model_dir / "meta.json"
        if model_file.exists() and meta_file.exists():
            self.pipeline = joblib.load(model_file)
            self.meta = json.loads(meta_file.read_text())

    @property
    def version(self) -> str:
        return self.meta["version"]

    def _p(self, df: pd.DataFrame) -> np.ndarray:
        return self.pipeline.predict_proba(df[FEATURES])[:, 1]

    def score(self, f: dict) -> dict:
        """Returns confidence, decision, top reasons."""
        t = self.meta["thresholds"]
        if f["mock_location_flag"]:  # hard rule beats the model
            conf, reasons, source = 0.0, [("mock_location_flag", -1.0)], "rule"
        elif self.pipeline is None:
            conf, reasons, source = rules_score(f), [], "rules"
        else:
            row = pd.DataFrame([f])[FEATURES]
            conf = float(self._p(row)[0])
            reasons = self._explain(row, conf)
            source = "model"
        decision = "AUTO_INFER" if conf >= t["auto"] else "ASK_EMPLOYEE" if conf >= t["ask"] else "NO_ACTION"
        return {
            "confidence": round(conf, 4),
            "decision": decision,
            "source": source,
            "modelVersion": self.version,
            "reasons": [{"feature": n, "effect": round(v, 3)} for n, v in reasons],
        }

    def _explain(self, row: pd.DataFrame, conf: float, top: int = 3):
        """Occlusion: set one feature to its training baseline, see how the
        probability moves. Works for any model, easy to explain to a supervisor."""
        base = self.meta.get("baseline", {})
        effects = []
        for name in FEATURES:
            alt = row.copy()
            alt[name] = base.get(name, 0.0)
            effects.append((name, conf - float(self._p(alt)[0])))
        effects.sort(key=lambda x: abs(x[1]), reverse=True)
        return effects[:top]
