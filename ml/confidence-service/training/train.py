"""Train, compare, calibrate, choose thresholds, save.
Run: python training/train.py --data data/evidence_labeled.csv"""
import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.calibration import CalibratedClassifierCV
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (average_precision_score, brier_score_loss,
                             precision_recall_curve, roc_auc_score)
from sklearn.model_selection import StratifiedKFold, cross_val_score, train_test_split
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

from confidence.features import FEATURES, LABEL

SEED = 42


def candidates():
    return {
        "logistic_regression": make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000, class_weight="balanced")),
        "gradient_boosting": HistGradientBoostingClassifier(max_depth=3, learning_rate=0.08, max_iter=150, random_state=SEED),
    }


def main(path: str, out_dir: str, target_precision: float):
    df = pd.read_csv(path)
    missing = set(FEATURES + [LABEL]) - set(df.columns)
    if missing:
        raise SystemExit(f"CSV is missing columns: {sorted(missing)}")
    if df[LABEL].nunique() < 2 or df[LABEL].value_counts().min() < 30:
        raise SystemExit("Need at least 30 examples of each label before training.")

    X, y = df[FEATURES], df[LABEL]
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.25, stratify=y, random_state=SEED)

    # 1) compare models with cross-validation on the training part only
    cv = StratifiedKFold(5, shuffle=True, random_state=SEED)
    scores = {n: float(cross_val_score(m, X_tr, y_tr, cv=cv, scoring="roc_auc").mean()) for n, m in candidates().items()}
    print("CV ROC-AUC:", {k: round(v, 4) for k, v in scores.items()})
    # prefer the simple, explainable model unless boosting is clearly better
    best = "gradient_boosting" if scores["gradient_boosting"] - scores["logistic_regression"] > 0.01 else "logistic_regression"
    print("chosen:", best)

    # 2) calibrate so 0.9 really means ~90%
    model = CalibratedClassifierCV(candidates()[best], method="sigmoid", cv=5)
    model.fit(X_tr, y_tr)

    # 3) honest evaluation on data the model never saw
    p = model.predict_proba(X_te)[:, 1]
    prec, rec, thr = precision_recall_curve(y_te, p)
    ok = np.where(prec[:-1] >= target_precision)[0]
    auto = float(thr[ok[0]]) if len(ok) else 0.95
    auto = max(auto, 0.80)  # never auto-infer below 0.80
    metrics = {
        "roc_auc": round(roc_auc_score(y_te, p), 4),
        "avg_precision": round(average_precision_score(y_te, p), 4),
        "brier": round(brier_score_loss(y_te, p), 4),
        "auto_precision": round(float(y_te[p >= auto].mean()), 4) if (p >= auto).any() else None,
        "auto_coverage": round(float((p >= auto).mean()), 4),
        "ask_share": round(float(((p >= 0.5) & (p < auto)).mean()), 4),
        "cv_auc": {k: round(v, 4) for k, v in scores.items()},
    }
    print("holdout:", metrics)

    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    joblib.dump(model, out / "model.joblib")
    meta = {
        "version": datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S") + f"-{best}",
        "algorithm": best,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "rows": int(len(df)),
        "features": FEATURES,
        "baseline": {c: float(X_tr[c].median()) for c in FEATURES},
        "thresholds": {"auto": round(auto, 3), "ask": 0.5},
        "metrics": metrics,
    }
    (out / "meta.json").write_text(json.dumps(meta, indent=2))
    print("saved ->", out)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data/evidence_labeled.csv")
    ap.add_argument("--out", default="models")
    ap.add_argument("--target-precision", type=float, default=0.95)
    a = ap.parse_args()
    main(a.data, a.out, a.target_precision)
