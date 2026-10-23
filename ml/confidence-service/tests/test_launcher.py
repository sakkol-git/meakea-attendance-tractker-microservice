from pathlib import Path

import pytest

import run


def test_private_defaults_and_bounded_resources(monkeypatch):
    for key in list(run.os.environ):
        if key.startswith("CONFIDENCE_"):
            monkeypatch.delenv(key)
    options = run.server_options()
    assert options["host"] == "127.0.0.1"
    assert options["port"] == 8107
    assert options["workers"] == 1
    assert options["limit_concurrency"] == 100
    assert not options["proxy_headers"]
    assert not options["access_log"]


@pytest.mark.parametrize("value", ["0", "-1", "65536", "not-a-port"])
def test_invalid_port_is_rejected(monkeypatch, value):
    monkeypatch.setenv("CONFIDENCE_PORT", value)
    with pytest.raises(ValueError):
        run.server_options()


def test_main_loads_adjacent_env_without_overriding_process(monkeypatch):
    calls = {}
    monkeypatch.setattr(run, "load_dotenv", lambda path, override: calls.update(path=path, override=override))
    monkeypatch.setattr(run.uvicorn, "run", lambda app, **options: calls.update(app=app, options=options))
    monkeypatch.setenv("CONFIDENCE_PORT", "18107")
    run.main()
    assert calls["path"] == Path(run.__file__).with_name(".env")
    assert calls["override"] is False
    assert calls["app"] == "confidence.api:app"
    assert calls["options"]["port"] == 18107


def test_real_env_file_does_not_override_exported_port(monkeypatch, tmp_path):
    (tmp_path / ".env").write_text("CONFIDENCE_PORT=18108\n")
    monkeypatch.setattr(run, "__file__", str(tmp_path / "run.py"))
    monkeypatch.setenv("CONFIDENCE_PORT", "18107")
    captured = {}
    monkeypatch.setattr(run.uvicorn, "run", lambda app, **options: captured.update(options))
    run.main()
    assert captured["port"] == 18107
