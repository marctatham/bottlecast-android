"""Shared helpers for the scripts that call Firebase's REST APIs with your gcloud login.

Pass --account (or set BOTTLECAST_GCLOUD_ACCOUNT) when the active gcloud account isn't the one that
can reach the project. Nothing here prints a token.
"""
import json
import os
import subprocess

FIRESTORE_API = "https://firestore.googleapis.com/v1"
RULES_API = "https://firebaserules.googleapis.com/v1"
IDENTITY_API = "https://identitytoolkit.googleapis.com/v1"


def access_token(account=None):
    account = account or os.environ.get("BOTTLECAST_GCLOUD_ACCOUNT")
    gcloud = ["gcloud", "auth", "print-access-token"] + ([f"--account={account}"] if account else [])
    return subprocess.run(gcloud, capture_output=True, text=True, check=True).stdout.strip()


def call(method, url, token, project, json_body=None, allow_missing=False):
    """Calls a Google REST API and returns its JSON, or None for a 404 when allow_missing is set."""
    # curl rather than urllib, as the python.org builds for macOS ship without a CA bundle
    cmd = ["curl", "-sS", "--max-time", "120", "-X", method, "-w", "\n%{http_code}",
           "-H", f"Authorization: Bearer {token}", "-H", f"X-Goog-User-Project: {project}"]
    stdin = None
    if json_body is not None:
        cmd += ["-H", "Content-Type: application/json", "--data-binary", "@-"]
        stdin = json.dumps(json_body)
    cmd.append(url)

    out = subprocess.run(cmd, input=stdin, capture_output=True, text=True, check=True).stdout
    body, _, status = out.rpartition("\n")
    payload = json.loads(body) if body.strip() else {}
    if status == "404" and allow_missing:
        return None
    if not status.startswith("2"):
        error = json.dumps(payload.get("error", payload), indent=1)
        raise SystemExit(f"{method} {url.split('?')[0]} failed with HTTP {status}:\n{error}")
    return payload


def documents_path(project):
    return f"projects/{project}/databases/(default)/documents"
