# confidence-service
Scores "was this person really at work?" (0..1) from evidence features.
Models: logistic regression vs gradient boosting, calibrated. Labels = employee confirm/reject taps.

    python3 -m venv .venv && source .venv/bin/activate
    pip install -r requirements.txt
    python -m training.make_synthetic --rows 4000   # FAKE data, replace with real export
    python -m training.train
    python -m pytest -q
    python run.py   # private service on 127.0.0.1:8107; docs at /docs

## Local environment and launcher

A blank .env is supplied locally and ignored by Git. Copy the desired .env.example values into it; exported process variables override file values. Install requirements.txt before running python run.py. The launcher reads the adjacent .env regardless of the current directory, binds to loopback by default, uses one worker and bounded concurrency/timeouts, disables access logs and ignores forwarded identity/address headers. Docker sets the bind address to 0.0.0.0 inside its network namespace; publish only on a private/loopback host interface.

This scoring API is still a prototype with no tenant authentication or Kafka integration. Do not expose it to untrusted networks or supply real employee data. Model paths and training behavior are unchanged. The Java baseline and complete service port list are in [service configuration](../../docs/SERVICE-CONFIGURATION.md).
