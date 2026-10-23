from fastapi import FastAPI
from pydantic import BaseModel, Field

from .model import ConfidenceModel

app = FastAPI(title="HRFlow confidence service")
model = ConfidenceModel()


class Evidence(BaseModel):
    tenantId: str
    employeeId: str
    workDate: str
    in_geofence: int = Field(ge=0, le=1)
    gps_accuracy_m: float = Field(ge=0)
    minutes_in_zone: float = Field(ge=0)
    wifi_connected: int = Field(ge=0, le=1)
    ble_beacons_seen: int = Field(ge=0)
    movement_events: int = Field(ge=0)
    interaction_events: int = Field(ge=0)
    minutes_after_shift_start: float
    arrival_deviation_min: float = Field(ge=0)
    is_usual_device: int = Field(ge=0, le=1)
    mock_location_flag: int = Field(ge=0, le=1)
    present_days_last_14: int = Field(ge=0, le=14)


@app.get("/health")
def health():
    return {"status": "UP", "modelVersion": model.version}


@app.get("/model")
def info():
    return model.meta


@app.post("/score")
def score(e: Evidence):
    features = e.model_dump(exclude={"tenantId", "employeeId", "workDate"})
    out = model.score(features)
    return {"tenantId": e.tenantId, "employeeId": e.employeeId, "workDate": e.workDate, **out}
