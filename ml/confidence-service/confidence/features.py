"""The ONE place that defines the model's inputs.
Attendance (Java) must send exactly these fields. Order matters for training."""

FEATURES = [
    "in_geofence",              # 1 if phone was inside the work-site geofence
    "gps_accuracy_m",           # worst GPS accuracy while inside (metres)
    "minutes_in_zone",          # minutes between first and last in-zone evidence
    "wifi_connected",           # 1 if office Wi-Fi BSSID seen
    "ble_beacons_seen",         # distinct office beacons heard
    "movement_events",          # step / activity-change events while in zone
    "interaction_events",       # screen unlocks / app opens while in zone
    "minutes_after_shift_start",# when the first evidence arrived (negative = early)
    "arrival_deviation_min",    # |first evidence - this employee's usual arrival|
    "is_usual_device",          # 1 if device id matches the bound device
    "mock_location_flag",       # 1 if OS reported mock / fake location
    "present_days_last_14",     # 0..14, history of this employee
]

LABEL = "label"  # 1 = employee confirmed "I was there", 0 = rejected
