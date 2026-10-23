"""Creates data/evidence_labeled.csv with FAKE data so you can build and demo
the pipeline before real labels exist. Replace it with the real export later
(same columns). Run: python training/make_synthetic.py --rows 4000"""
import argparse
from pathlib import Path

import numpy as np
import pandas as pd

from confidence.features import FEATURES, LABEL


def make(n: int, seed: int = 42) -> pd.DataFrame:
    rng = np.random.default_rng(seed)
    present = rng.random(n) < 0.80              # ground truth
    phone_left = (~present) & (rng.random(n) < 0.35)  # phone stayed at office, person did not
    near = present | phone_left                  # phone physically at office

    d = pd.DataFrame(index=range(n))
    d["in_geofence"] = (near & (rng.random(n) < 0.97)) | (~near & (rng.random(n) < 0.04))
    d["gps_accuracy_m"] = np.where(near, rng.gamma(2.0, 8, n), rng.gamma(2.0, 25, n)).round(1)
    d["minutes_in_zone"] = np.where(near, rng.gamma(4, 40, n), rng.gamma(1, 5, n)).round(0)
    d["wifi_connected"] = near & (rng.random(n) < 0.85)
    d["ble_beacons_seen"] = np.where(near, rng.poisson(2.2, n), rng.poisson(0.1, n))
    # key signal: a person moves and touches the phone, a forgotten phone does not
    d["movement_events"] = np.where(present, rng.poisson(14, n), np.where(phone_left, rng.poisson(0.5, n), rng.poisson(2, n)))
    d["interaction_events"] = np.where(present, rng.poisson(6, n), np.where(phone_left, rng.poisson(0.3, n), rng.poisson(1, n)))
    d["minutes_after_shift_start"] = rng.normal(2, 9, n).round(0)
    d["arrival_deviation_min"] = np.abs(rng.normal(0, np.where(present, 6, 25), n)).round(0)
    d["is_usual_device"] = rng.random(n) < np.where(present, 0.98, 0.90)
    d["mock_location_flag"] = (~present) & (rng.random(n) < 0.04)
    d["present_days_last_14"] = np.clip(rng.normal(np.where(present, 12, 8), 2.5, n), 0, 14).round(0)

    y = present.copy()
    flip = rng.random(n) < 0.03                  # people mis-tap: 3% label noise
    y = np.where(flip, ~y, y)

    d = d[FEATURES].astype(float)
    for c in ["in_geofence", "wifi_connected", "is_usual_device", "mock_location_flag"]:
        d[c] = d[c].astype(int)
    for c in ["ble_beacons_seen", "movement_events", "interaction_events", "present_days_last_14"]:
        d[c] = d[c].astype(int)
    d[LABEL] = y.astype(int)
    return d


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--rows", type=int, default=4000)
    ap.add_argument("--out", default="data/evidence_labeled.csv")
    a = ap.parse_args()
    Path(a.out).parent.mkdir(parents=True, exist_ok=True)
    df = make(a.rows)
    df.to_csv(a.out, index=False)
    print(f"wrote {len(df)} rows -> {a.out}; positive rate {df[LABEL].mean():.2f}")
