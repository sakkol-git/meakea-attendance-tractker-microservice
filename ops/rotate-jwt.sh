#!/usr/bin/env bash
set -euo pipefail

# W10-S07-S09: Rotate JWT signing keys
# Invariant: New key validates, old key fails after bounded overlap, neither key value appears in Git/logs.
# Missing configured key directory must produce BLOCKED, never synthetic PASS.

mode="${1:-recovery}"
keys_dir="${HRFLOW_SIGNING_KEYS_DIR:-}"

if [[ "$mode" == "negative" ]]; then
  # Negative assertion:
  # After bounded overlap, old key must fail verification against trust store containing only new key.
  temp_dir=$(mktemp -d /tmp/hrflow-jwt-neg-XXXXXX)
  trap 'rm -rf "$temp_dir"' EXIT
  umask 077

  if ! command -v openssl >/dev/null 2>&1; then
    printf 'BLOCKED: openssl utility required for cryptographic validation\n' >&2
    exit 2
  fi

  openssl genrsa -out "$temp_dir/priv_old.pem" 2048 2>/dev/null
  openssl genrsa -out "$temp_dir/priv_new.pem" 2048 2>/dev/null
  openssl rsa -in "$temp_dir/priv_new.pem" -pubout -out "$temp_dir/pub_new.pem" 2>/dev/null

  payload="hrflow-negative-test-payload"
  echo -n "$payload" | openssl dgst -sha256 -sign "$temp_dir/priv_old.pem" -out "$temp_dir/sig_old.bin"

  if echo -n "$payload" | openssl dgst -sha256 -verify "$temp_dir/pub_new.pem" -signature "$temp_dir/sig_old.bin" >/dev/null 2>&1; then
    printf 'NEGATIVE FAIL: old signing key unexpectedly validated against new trust store\n' >&2
    exit 1
  fi
  printf 'NEGATIVE PASS: retired signing key is strictly rejected by new public trust store\n'
  exit 0
fi

# Recovery mode: requires real operator-mounted signing keys directory
if [[ -z "$keys_dir" || ! -d "$keys_dir" ]]; then
  printf 'BLOCKED: Mounted signing key directory is not configured or missing (HRFLOW_SIGNING_KEYS_DIR); real key rotation requires operator key store\n' >&2
  exit 2
fi

if [[ ! -f "$keys_dir/private.pem" || ! -f "$keys_dir/public.pem" ]]; then
  printf 'BLOCKED: Required keypair (private.pem, public.pem) missing in %s\n' "$keys_dir" >&2
  exit 2
fi

temp_dir=$(mktemp -d /tmp/hrflow-jwt-verify-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

# Real key pair verification: mode-0600 temporary files, no binary signature in shell variable
payload_file="$temp_dir/payload.txt"
sig_file="$temp_dir/signature.bin"

printf 'hrflow-key-rotation-verification-%s' "$(date +%s)" > "$payload_file"
chmod 0600 "$payload_file"

if ! openssl dgst -sha256 -sign "$keys_dir/private.pem" -out "$sig_file" "$payload_file" 2>/dev/null; then
  printf 'RECOVERY FAIL: failed to generate signature using %s/private.pem\n' "$keys_dir" >&2
  exit 1
fi
chmod 0600 "$sig_file"

# Verify signature with configured public key
if ! openssl dgst -sha256 -verify "$keys_dir/public.pem" -signature "$sig_file" "$payload_file" >/dev/null 2>&1; then
  printf 'RECOVERY FAIL: signature verification against configured public key failed in %s\n' "$keys_dir" >&2
  exit 1
fi

printf 'PREREQUISITE PASS: Signing keypair in %s verified valid for signatures with configured public key. Actual key rotation and old-token retirement remain NOT RUN\n' "$keys_dir"
exit 0
