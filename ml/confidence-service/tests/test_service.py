from fastapi.testclient import TestClient

from confidence.api import app

client = TestClient(app)

PRESENT = dict(tenantId="t1", employeeId="e1", workDate="2026-10-16", in_geofence=1, gps_accuracy_m=10,
               minutes_in_zone=240, wifi_connected=1, ble_beacons_seen=3, movement_events=15,
               interaction_events=7, minutes_after_shift_start=1, arrival_deviation_min=3,
               is_usual_device=1, mock_location_flag=0, present_days_last_14=13)
PHONE_LEFT = {**PRESENT, "movement_events": 0, "interaction_events": 0, "arrival_deviation_min": 40,
              "present_days_last_14": 7}
SPOOFED = {**PRESENT, "mock_location_flag": 1}


def test_health():
    assert client.get("/health").json()["status"] == "UP"


def test_present_is_auto_inferred():
    r = client.post("/score", json=PRESENT).json()
    assert r["decision"] == "AUTO_INFER" and r["confidence"] > 0.9


def test_phone_left_alone_is_not_auto_inferred():
    r = client.post("/score", json=PHONE_LEFT).json()
    assert r["decision"] != "AUTO_INFER" and r["confidence"] < 0.9


def test_mock_location_always_blocked():
    r = client.post("/score", json=SPOOFED).json()
    assert r["decision"] == "NO_ACTION" and r["confidence"] == 0.0


def test_bad_input_rejected():
    assert client.post("/score", json={**PRESENT, "in_geofence": 5}).status_code == 422
