#!/usr/bin/env python3
"""Back up BottleCast's bottles, and move the old ones to the slice 2 shape.

    scripts/migrate_messages.py backup  --project <id> [--account <gcloud account>]
    scripts/migrate_messages.py migrate --project <id> [--account <gcloud account>] [--apply]
    scripts/migrate_messages.py restore --project <id> --from <backup folder> [--apply]

Every command first backs up the messages, the live Firestore rules and the indexes to
~/bottlecast-backups/<project>/<UTC time>/ (--out moves it, never into the repo). Without --apply,
`migrate` and `restore` only plan: they list each bottle's id, what happens to it and its length,
never its text or its writer.

An old bottle is exactly {messageContent, userId}. Migrating it adds the slice 2 fields: id,
createdAt (the document's createTime), lang "und", status "afloat", a random rand, penName null,
sealable false (its writer never chose), reportCount and findCount 0, and foundAt null. Bottles that
already have a status are left alone, so it's safe to run again. REST writes don't go through the
rules, so it doesn't matter whether the new rules are deployed yet.

`restore` puts each bottle in a backup back exactly as it was. Bottles cast since are left alone.

Production needs the owner's explicit yes, after a backup and a dry run (CLAUDE.md).
"""
import argparse
import json
import re
import secrets
import sys
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import quote, urlencode

from firebase_rest import FIRESTORE_API, RULES_API, access_token, call, documents_path

REPO = Path(__file__).resolve().parent.parent
DEFAULT_OUT = Path.home() / "bottlecast-backups"
MESSAGE_ID = re.compile(r"^[A-Za-z0-9]{20}$")
V2_FIELDS = ["id", "createdAt", "lang", "status", "rand", "penName", "sealable", "reportCount", "findCount", "foundAt"]


def list_messages(project, token):
    messages, page = [], None
    while True:
        query = {"pageSize": 300} | ({"pageToken": page} if page else {})
        response = call("GET", f"{FIRESTORE_API}/{documents_path(project)}/messages?{urlencode(query)}", token, project)
        messages += response.get("documents", [])
        page = response.get("nextPageToken")
        if not page:
            return messages


def write_private(path, data):
    path.write_text(json.dumps(data, indent=1))
    path.chmod(0o600)


def back_up(project, token, out_root):
    out_root = out_root.expanduser().resolve()
    if out_root == REPO or REPO in out_root.parents:
        raise SystemExit(f"{out_root} is inside the repo; back up somewhere else (--out)")
    folder = out_root / project / datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    folder.mkdir(parents=True, mode=0o700)
    for parent in [out_root, out_root / project]:
        parent.chmod(0o700)

    messages = list_messages(project, token)
    write_private(folder / "messages.json", messages)

    release = call("GET", f"{RULES_API}/projects/{project}/releases/cloud.firestore", token, project, allow_missing=True)
    ruleset = call("GET", f"{RULES_API}/{release['rulesetName']}", token, project) if release else None
    write_private(folder / "rules.json", {"release": release, "ruleset": ruleset})

    indexes = call("GET", f"{FIRESTORE_API}/projects/{project}/databases/(default)/collectionGroups/-/indexes",
                   token, project)
    write_private(folder / "indexes.json", indexes)

    print(f"backed up {len(messages)} messages, the live rules and {len(indexes.get('indexes', []))} indexes "
          f"to {folder}")
    return messages


def classify(document):
    """Returns ('v2' | 'legacy' | 'skip', reason)."""
    fields = document.get("fields", {})
    doc_id = document["name"].rsplit("/", 1)[-1]
    if "status" in fields:
        return "v2", "already has a status"
    if set(fields) != {"messageContent", "userId"}:
        return "skip", f"unexpected fields {sorted(fields)}"
    if any("stringValue" not in fields[key] for key in fields):
        return "skip", "messageContent or userId isn't a string"
    if not fields["messageContent"]["stringValue"].strip():
        return "skip", "blank text"
    if not MESSAGE_ID.match(doc_id):
        return "skip", "id isn't 20 alphanumerics"
    return "legacy", "migrate"


def v2_values(document):
    doc_id = document["name"].rsplit("/", 1)[-1]
    return {
        "id": {"stringValue": doc_id},
        "createdAt": {"timestampValue": document["createTime"]},
        "lang": {"stringValue": "und"},
        "status": {"stringValue": "afloat"},
        "rand": {"doubleValue": secrets.randbits(53) / 2**53},
        "penName": {"nullValue": None},
        "sealable": {"booleanValue": False},
        "reportCount": {"integerValue": "0"},
        "findCount": {"integerValue": "0"},
        "foundAt": {"nullValue": None},
    }


def patch(project, token, document, field_paths, fields):
    """Writes `fields` under the mask (a masked field missing from `fields` is deleted), if unchanged."""
    query = [("updateMask.fieldPaths", path) for path in field_paths]
    query.append(("currentDocument.updateTime", document["updateTime"]))
    url = f"{FIRESTORE_API}/{quote(document['name'], safe='/()')}?{urlencode(query)}"
    call("PATCH", url, token, project, json_body={"fields": fields})


def is_v2(document):
    fields = document.get("fields", {})
    expected = {"id": "stringValue", "createdAt": "timestampValue", "lang": "stringValue", "status": "stringValue",
                "rand": "doubleValue", "penName": "nullValue", "sealable": "booleanValue",
                "reportCount": "integerValue", "findCount": "integerValue", "foundAt": "nullValue"}
    return all(kind in fields.get(name, {}) for name, kind in expected.items())


def confirm(project):
    typed = input(f"Type the project id ({project}) to write: ").strip()
    if typed != project:
        raise SystemExit("not confirmed; nothing written")


def migrate(project, token, messages, apply):
    plan = [(document, *classify(document)) for document in messages]
    counts = {kind: sum(1 for _, k, _ in plan if k == kind) for kind in ["legacy", "v2", "skip"]}
    print(f"\n{len(plan)} messages: {counts['legacy']} to migrate, {counts['v2']} already v2, "
          f"{counts['skip']} skipped")
    for document, kind, reason in plan:
        text = document.get("fields", {}).get("messageContent", {}).get("stringValue", "")
        print(f"  {document['name'].rsplit('/', 1)[-1]:<24} {kind:<7} created {document['createTime']}  "
              f"{len(text)} chars  ({reason})")

    legacy = [document for document, kind, _ in plan if kind == "legacy"]
    if not apply or not legacy:
        print("\ndry run: nothing written, pass --apply to migrate" if legacy else "\nnothing to migrate")
        return
    confirm(project)
    for document in legacy:
        patch(project, token, document, V2_FIELDS, v2_values(document))

    migrated = {document["name"] for document in legacy}
    unfinished = [d["name"].rsplit("/", 1)[-1] for d in list_messages(project, token)
                  if d["name"] in migrated and not is_v2(d)]
    if unfinished:
        raise SystemExit(f"these didn't end up in the v2 shape: {unfinished}")
    print(f"migrated {len(legacy)} messages, and checked each one")


def restore(project, token, backup_folder, apply):
    backed_up = {d["name"]: d for d in json.loads((Path(backup_folder) / "messages.json").read_text())}
    current = {d["name"]: d for d in list_messages(project, token)}
    plan = [(current[name], saved) for name, saved in backed_up.items() if name in current]
    print(f"\n{len(plan)} of the {len(backed_up)} backed-up messages still exist and would be put back")
    for document, _ in plan:
        print(f"  {document['name'].rsplit('/', 1)[-1]}")
    if not apply or not plan:
        print("\ndry run: nothing written, pass --apply to restore")
        return
    confirm(project)
    for document, saved in plan:
        saved_fields = saved.get("fields", {})
        paths = sorted(set(saved_fields) | set(document.get("fields", {})))
        patch(project, token, document, paths, saved_fields)
    print(f"restored {len(plan)} messages")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("command", choices=["backup", "migrate", "restore"])
    parser.add_argument("--project", required=True, help="the Firebase project id")
    parser.add_argument("--account", help="the gcloud account to use (default: BOTTLECAST_GCLOUD_ACCOUNT or the active one)")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help=f"where backups go (default: {DEFAULT_OUT})")
    parser.add_argument("--from", dest="backup_folder", help="the backup folder to restore (restore only)")
    parser.add_argument("--apply", action="store_true", help="write for real")
    args = parser.parse_args()
    if args.command == "restore" and not args.backup_folder:
        parser.error("restore needs --from <backup folder>")

    token = access_token(args.account)
    messages = back_up(args.project, token, args.out)
    if args.command == "migrate":
        migrate(args.project, token, messages, args.apply)
    elif args.command == "restore":
        restore(args.project, token, args.backup_folder, args.apply)


if __name__ == "__main__":
    sys.exit(main())
