#!/usr/bin/env python3
"""Upload a release bundle to Google Play through the Android Publisher API.

Auth is keyless: your gcloud login (which holds roles/iam.serviceAccountTokenCreator on the
publisher service account) mints a short-lived token for that service account, and Play Console
grants the service account its release permissions for this app.

    scripts/play_upload.py check
    scripts/play_upload.py release --track production --notes "What's new" [--name "Bug fixes"] [--rollout 0.2] [--yes]

`check`, and `release` without --yes, change nothing: they open an edit, report, then discard it.
`release --yes` uploads the bundle, puts it on the track, validates the edit and commits it.

Needs gcloud and bundletool on the PATH. Set PLAY_UPLOAD_ACCOUNT to the gcloud account to mint
the token with, when it isn't the active one.
"""
import argparse
import json
import os
import subprocess

from firebase_rest import run_quietly

PACKAGE = "com.bottlecast.app"
SERVICE_ACCOUNT = "play-publisher@bottlecast-prod.iam.gserviceaccount.com"
QUOTA_PROJECT = "bottlecast-prod"
# From the repo root, wherever the script is run from
DEFAULT_BUNDLE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                              "android/app/build/outputs/bundle/release/app-release.aab")

API = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}"
UPLOAD_API = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PACKAGE}"
TOKEN_API = f"https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/{SERVICE_ACCOUNT}:generateAccessToken"


def call(method, url, token, json_body=None, bundle=None, headers=()):
    # curl rather than urllib, as the python.org builds for macOS ship without a CA bundle
    cmd = ["curl", "-sS", "--max-time", "900", "-X", method, "-w", "\n%{http_code}",
           "-H", f"Authorization: Bearer {token}"]
    for header in headers:
        cmd += ["-H", header]
    stdin = None
    if json_body is not None:
        cmd += ["-H", "Content-Type: application/json", "--data-binary", "@-"]
        stdin = json.dumps(json_body)
    elif bundle is not None:
        cmd += ["-H", "Content-Type: application/octet-stream", "--data-binary", f"@{bundle}"]
    cmd.append(url)

    out = run_quietly(cmd, f"{method} {url.split('?')[0]}", stdin=stdin)
    body, _, status = out.rpartition("\n")
    payload = json.loads(body) if body.strip() else {}
    if not status.startswith("2"):
        error = json.dumps(payload.get("error", payload), indent=1)
        raise SystemExit(f"{method} {url.split('?')[0]} failed with HTTP {status}:\n{error}")
    return payload


def mint_token():
    account = os.environ.get("PLAY_UPLOAD_ACCOUNT")
    gcloud = ["gcloud", "auth", "print-access-token"] + ([f"--account={account}"] if account else [])
    # gcloud's errors can name the account, so they aren't repeated
    user_token = run_quietly(gcloud, "Getting an access token from gcloud (try `gcloud auth login`)", echo_error=False).strip()
    response = call(
        "POST", TOKEN_API, user_token,
        json_body={"scope": ["https://www.googleapis.com/auth/androidpublisher"], "lifetime": "3600s"},
        headers=[f"X-Goog-User-Project: {QUOTA_PROJECT}"],
    )
    return response["accessToken"]


def manifest_value(bundle, attribute):
    out = subprocess.run(
        ["bundletool", "dump", "manifest", f"--bundle={bundle}", f"--xpath=/manifest/@{attribute}"],
        capture_output=True, text=True, check=True,
    ).stdout
    return out.strip().splitlines()[-1]


def print_tracks(tracks):
    releases = [(t["track"], r) for t in tracks for r in t.get("releases", [])]
    for track, release in releases:
        print(f"  {track:<12} {release.get('status', ''):<11} "
              f"versionCodes={release.get('versionCodes', [])} name={release.get('name')!r}")
    if not releases:
        print("  (no releases on any track)")


def highest_version_code(tracks):
    return max(
        (int(code) for t in tracks for r in t.get("releases", []) for code in r.get("versionCodes", [])),
        default=0,
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("check", help="verify access and list each track's releases; changes nothing")
    release_parser = commands.add_parser("release", help="put the bundle on a track (a dry run unless --yes)")
    release_parser.add_argument("--bundle", default=DEFAULT_BUNDLE)
    release_parser.add_argument("--track", required=True, help="internal, alpha, beta or production")
    release_parser.add_argument("--notes", required=True, help="release notes, in the listing's default language")
    release_parser.add_argument("--name", help="release name shown in Play Console (default: \"<versionCode> (<versionName>)\")")
    release_parser.add_argument("--rollout", type=float, help="staged rollout fraction, e.g. 0.2 (default: everyone)")
    release_parser.add_argument("--yes", action="store_true", help="upload and commit for real")
    args = parser.parse_args()

    token = mint_token()
    edit_id = call("POST", f"{API}/edits", token, json_body={})["id"]
    committed = False
    try:
        language = call("GET", f"{API}/edits/{edit_id}/details", token)["defaultLanguage"]
        tracks = call("GET", f"{API}/edits/{edit_id}/tracks", token).get("tracks", [])
        print(f"{PACKAGE}, default language {language}, current releases:")
        print_tracks(tracks)
        if args.command == "check":
            return

        package = manifest_value(args.bundle, "package")
        version_code = int(manifest_value(args.bundle, "android:versionCode"))
        version_name = manifest_value(args.bundle, "android:versionName")
        if package != PACKAGE:
            raise SystemExit(f"{args.bundle} is for {package}, not {PACKAGE}")
        if version_code <= highest_version_code(tracks):
            raise SystemExit(f"versionCode {version_code} is not above the {highest_version_code(tracks)} already on Play")

        release = {
            "name": args.name or f"{version_code} ({version_name})",
            "versionCodes": [str(version_code)],
            "releaseNotes": [{"language": language, "text": args.notes}],
            "status": "inProgress" if args.rollout else "completed",
        }
        if args.rollout:
            release["userFraction"] = args.rollout
        rollout = f"{args.rollout:.0%} staged rollout" if args.rollout else "full rollout"
        print(f"\nplanned: {release['name']!r}, versionCode {version_code} ({version_name}) -> {args.track}, "
              f"{rollout}, notes ({language}): {args.notes!r}")
        if not args.yes:
            print("dry run: nothing uploaded, pass --yes to upload and commit")
            return

        uploaded = call("POST", f"{UPLOAD_API}/edits/{edit_id}/bundles?uploadType=media", token, bundle=args.bundle)
        if int(uploaded["versionCode"]) != version_code:
            raise SystemExit(f"Play read versionCode {uploaded['versionCode']}, expected {version_code}")
        call("PUT", f"{API}/edits/{edit_id}/tracks/{args.track}", token,
             json_body={"track": args.track, "releases": [release]})
        call("POST", f"{API}/edits/{edit_id}:validate", token, json_body={})
        call("POST", f"{API}/edits/{edit_id}:commit", token, json_body={})
        committed = True
        print(f"committed: {release['name']} is on {args.track}, pending any Play review")
    finally:
        if not committed:
            call("DELETE", f"{API}/edits/{edit_id}", token)


if __name__ == "__main__":
    main()
