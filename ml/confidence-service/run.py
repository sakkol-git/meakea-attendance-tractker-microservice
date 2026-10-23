"""Local/private launcher; process environment overrides the service .env file."""

import os
from pathlib import Path

import uvicorn
from dotenv import load_dotenv


def positive_integer(name: str, default: int) -> int:
    value = int(os.environ.get(name, str(default)))
    if value < 1:
        raise ValueError(f"{name} must be positive")
    return value


def server_options() -> dict:
    port = positive_integer("CONFIDENCE_PORT", 8107)
    if port > 65535:
        raise ValueError("CONFIDENCE_PORT must be at most 65535")
    log_level = os.environ.get("CONFIDENCE_LOG_LEVEL", "info").lower()
    if log_level not in {"critical", "error", "warning", "info", "debug", "trace"}:
        raise ValueError("CONFIDENCE_LOG_LEVEL is invalid")
    return {
        "host": os.environ.get("CONFIDENCE_HOST", "127.0.0.1"),
        "port": port,
        "log_level": log_level,
        "workers": positive_integer("CONFIDENCE_WORKERS", 1),
        "limit_concurrency": positive_integer("CONFIDENCE_MAX_CONCURRENCY", 100),
        "timeout_keep_alive": positive_integer("CONFIDENCE_KEEP_ALIVE_SECONDS", 5),
        "timeout_graceful_shutdown": positive_integer("CONFIDENCE_SHUTDOWN_SECONDS", 20),
        "proxy_headers": False,
        "server_header": False,
        "access_log": False,
    }


def main() -> None:
    load_dotenv(Path(__file__).with_name(".env"), override=False)
    uvicorn.run("confidence.api:app", **server_options())


if __name__ == "__main__":
    main()
