#!/usr/bin/env python3
"""Exercise the first auth slice through the local gateway with a seeded test password."""
import http.cookiejar
import json
import os
import urllib.error
import urllib.request
import uuid
from urllib.parse import urlsplit
from pathlib import Path

from jsonschema import Draft202012Validator

BASE = os.environ.get("HRFLOW_GATEWAY", "http://localhost:8080/api/v1")
PASSWORD = os.environ.get("HRFLOW_DEV_PASSWORD")
if not PASSWORD:
    raise SystemExit("Set HRFLOW_DEV_PASSWORD to the local seed password before running the smoke test.")

contract = json.loads((Path(__file__).resolve().parents[1] / "contracts/openapi.json").read_text())
cookies = http.cookiejar.CookieJar()
browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))


def call(path, payload=None, headers=None, client=None, method=None):
    request = urllib.request.Request(
        BASE + path,
        data=None if payload is None else json.dumps(payload).encode(),
        headers={"Accept": "application/json", **({"Content-Type": "application/json"} if payload is not None else {}), **(headers or {})},
        method=method or ("GET" if payload is None else "POST"),
    )
    try:
        with (client or urllib.request.build_opener()).open(request) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw else None, response.headers
    except urllib.error.HTTPError as error:
        raw = error.read()
        return error.code, json.loads(raw) if raw else None, error.headers


def expect(condition, message):
    if not condition:
        raise AssertionError(message)


def schema(name, value):
    Draft202012Validator(contract["components"]["schemas"][name]).validate(value)


def main():
    gateway = urlsplit(BASE)
    with urllib.request.urlopen(f"{gateway.scheme}://{gateway.netloc}/actuator/health") as response:
        health, body = response.status, json.loads(response.read())
    expect(health == 200 and body["status"] == "UP", "gateway health")
    code, jwks, _ = call("/.well-known/jwks.json")
    expect(code == 200, "JWKS route")
    schema("Jwks", jwks)
    login = {"tenant": "demo", "email": "owner@example.test", "password": PASSWORD}
    code, native, _ = call("/auth/login", login)
    expect(code == 200, "native login")
    schema("AuthTokens", native)
    expect("refreshToken" in native, "native refresh token")
    code, profile, _ = call("/auth/me", headers={"Authorization": "Bearer " + native["accessToken"]})
    expect(code == 200 and profile["tenantId"], "verified profile")
    schema("Profile", profile)
    original = native["refreshToken"]
    code, rotated, _ = call("/auth/refresh", {"refreshToken": original})
    expect(code == 200 and rotated["refreshToken"] != original, "refresh rotation")
    code, problem, headers = call("/auth/refresh", {"refreshToken": original})
    expect(code == 401 and headers.get_content_type() == "application/problem+json", "reuse denied")
    schema("Problem", problem)
    expect(call("/auth/refresh", {"refreshToken": rotated["refreshToken"]})[0] == 401,
           "successor revoked after reuse")
    expect(call("/auth/login", {**login, "password": "wrong"})[0] == 401, "wrong password")
    expect(call("/auth/login", {**login, "tenant": "other"})[0] == 401, "wrong tenant")
    expect(call("/auth/login", {**login, "tenant": "Invalid!"})[0] == 422,
           "tenant validation")
    expect(call("/auth/refresh", {"refreshToken": "short"})[0] == 422,
           "refresh validation")
    expect(call("/auth/me", headers={"Authorization": "Bearer " + native["accessToken"] + "x"})[0] == 401,
           "tampered JWT rejected")
    expect(call("/auth/me", headers={"X-Tenant-Id": profile["tenantId"]})[0] == 401,
           "forged tenant header rejected")
    unknown = {**login, "email": f"unknown-{uuid.uuid4().hex}@example.test"}
    for _ in range(5):
        expect(call("/auth/login", unknown)[0] == 401, "unknown account failure")
    expect(call("/auth/login", unknown)[0] == 429, "distributed login throttle")
    origin = {"Origin": "http://localhost:3000"}
    expect(call("/auth/csrf", headers=origin, client=browser)[0] == 204, "browser CSRF bootstrap")
    expect(call("/auth/login", login, headers=origin, client=browser)[0] == 403, "browser CSRF required")
    expect(call("/auth/login", login, headers={"Origin": "https://attacker.example"}, client=browser)[0] == 403,
           "hostile origin rejected")
    csrf = next(cookie.value for cookie in cookies if cookie.name == "XSRF-TOKEN")
    browser_headers = {**origin, "X-XSRF-TOKEN": csrf}
    code, signed_in, _ = call("/auth/login", login, headers=browser_headers, client=browser)
    expect(code == 200 and "refreshToken" not in signed_in, "browser cookie login")
    expect(call("/auth/refresh", headers=browser_headers, client=browser, method="POST")[0] == 200,
           "browser refresh")
    expect(call("/auth/logout", headers=browser_headers, client=browser, method="POST")[0] == 204,
           "browser logout")
    expect(call("/auth/refresh", headers=browser_headers, client=browser, method="POST")[0] == 401,
           "browser session revoked")
    expect(call("/auth/me")[0] == 401, "missing access token")
    print("Gateway auth smoke passed: health, contract, native/browser login, tenant, CSRF, refresh reuse, logout.")


if __name__ == "__main__":
    main()
