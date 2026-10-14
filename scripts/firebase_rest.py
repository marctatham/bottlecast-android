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


def run_quietly(cmd, what, stdin=None, echo_error=True):
    """Runs a command whose arguments may hold a token or an account, so a failure never echoes them.

    A failed command's own exception prints its whole argument list, bearer token included, so it's
    replaced with a message naming only what failed and the tool's last line of error output.
    """
    try:
        return subprocess.run(cmd, input=stdin, capture_output=True, text=True, check=True).stdout
    except subprocess.CalledProcessError as error:
        lines = (error.stderr or "").strip().splitlines() if echo_error else []
        detail = f": {lines[-1]}" if lines else ""
        raise SystemExit(f"{what} failed with exit code {error.returncode}{detail}") from None


def access_token(account=None):
    account = account or os.environ.get("BOTTLECAST_GCLOUD_ACCOUNT")
    gcloud = ["gcloud", "auth", "print-access-token"] + ([f"--account={account}"] if account else [])
    # gcloud's errors can name the account, so they aren't repeated
    return run_quietly(gcloud, "Getting an access token from gcloud (try `gcloud auth login`)", echo_error=False).strip()


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

    out = run_quietly(cmd, f"{method} {url.split('?')[0]}", stdin=stdin)
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
