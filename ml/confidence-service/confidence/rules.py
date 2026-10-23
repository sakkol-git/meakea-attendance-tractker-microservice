"""Rules score = the fallback when no trained model exists yet (cold start)
and the safety net when the model is unavailable. Returns 0..1."""

def rules_score(f: dict) -> float:
    if f["mock_location_flag"]:
        return 0.05
    s = 0.0
    s += 0.30 * f["in_geofence"]
    s += 0.15 * f["wifi_connected"]
    s += 0.15 * min(f["ble_beacons_seen"], 3) / 3
    s += 0.15 * min(f["movement_events"], 20) / 20
    s += 0.10 * min(f["interaction_events"], 10) / 10
    s += 0.05 * f["is_usual_device"]
    s += 0.10 * min(f["present_days_last_14"], 10) / 10
    return round(min(s, 1.0), 4)
