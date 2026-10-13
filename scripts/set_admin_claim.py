#!/usr/bin/env python3
"""Give a BottleCast account the admin claim, or take it away.

    scripts/set_admin_claim.py --project <id> --uid <uid> [--revoke] [--apply] [--account <gcloud account>]

Without --apply it only looks the account up and prints what would change: its sign-in providers and
its custom claims, never its email or name. The rules and the app read `admin: true` from the ID
token, so the account sees the change after its token refreshes: sign out and in again, or wait up to
an hour.
"""
import argparse
import json

from firebase_rest import IDENTITY_API, access_token, call


def look_up(project, token, uid):
    users = call("POST", f"{IDENTITY_API}/projects/{project}/accounts:lookup", token, project,
                 json_body={"localId": [uid]}).get("users", [])
    if not users:
        raise SystemExit(f"no account with that uid in {project}")
    return users[0]


def describe(user):
    providers = [p["providerId"] for p in user.get("providerUserInfo", [])] or ["anonymous"]
    return f"providers {providers}, claims {json.loads(user.get('customAttributes') or '{}')}"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--project", required=True, help="the Firebase project id")
    parser.add_argument("--uid", required=True, help="the account's Firebase uid")
    parser.add_argument("--revoke", action="store_true", help="remove the claim instead of adding it")
    parser.add_argument("--apply", action="store_true", help="write for real")
    parser.add_argument("--account", help="the gcloud account to use (default: BOTTLECAST_GCLOUD_ACCOUNT or the active one)")
    args = parser.parse_args()

    token = access_token(args.account)
    user = look_up(args.project, token, args.uid)
    print(f"now: {describe(user)}")

    claims = json.loads(user.get("customAttributes") or "{}")
    if args.revoke:
        claims.pop("admin", None)
    else:
        claims["admin"] = True
    encoded = json.dumps(claims, separators=(",", ":"))
    if len(encoded) > 1000:
        raise SystemExit("custom claims must stay under 1000 characters")
    print(f"planned claims: {claims}")
    if not args.apply:
        print("dry run: nothing written, pass --apply to write")
        return

    call("POST", f"{IDENTITY_API}/projects/{args.project}/accounts:update", token, args.project,
         json_body={"localId": args.uid, "customAttributes": encoded})
    print(f"after: {describe(look_up(args.project, token, args.uid))}")


if __name__ == "__main__":
    main()
