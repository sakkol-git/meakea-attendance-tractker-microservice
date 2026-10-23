# confidence-service
Scores "was this person really at work?" (0..1) from evidence features.
Models: logistic regression vs gradient boosting, calibrated. Labels = employee confirm/reject taps.

    python3 -m venv .venv && source .venv/bin/activate
    pip install -r requirements.txt
    python -m training.make_synthetic --rows 4000   # FAKE data, replace with real export
    python -m training.train
    python -m pytest -q
    uvicorn confidence.api:app --port 8107 --reload   # docs at /docs
