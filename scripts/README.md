# Scripts

Python tools for jobs the app doesn't do itself. They need Python 3 and `curl`, and they sign in with your `gcloud` login, so there are no keys to keep. Run them from the repo root. None of them changes anything without its write flag, and `--help` lists each one's options.

| Script | What it does | Writes for real with |
|---|---|---|
| `set_admin_claim.py` | Gives an account the admin claim, or takes it away | `--apply` |
| `migrate_messages.py` | Backs up the bottles, and moves old ones to the slice 2 shape | `--apply` |
| `play_upload.py` | Uploads a release bundle to Google Play | `release --yes` |

`firebase_rest.py` holds the helpers the Firebase scripts share. `play_upload.py` also needs `bundletool`, and its header explains how it signs in to Play.

## Making an account an admin

Admins get **Moderation** in the Shore's ⋮ menu, and the Firestore rules let them pull, remove and restore reported bottles. Both check for `admin: true` among the account's custom claims. The Firebase console can't set custom claims, so `set_admin_claim.py` sets them through Firebase Auth's REST API.

### Before you start

- Sign `gcloud` in (`gcloud auth login`) with an account that can manage the project's users. If that isn't the active gcloud account, pass `--account <gcloud account>` or set `BOTTLECAST_GCLOUD_ACCOUNT`.
- The account must already exist in that project, so sign in with it once in the app that uses the project: a debug build for the debug project, the Play app for production.
- Find the project id. It's `project_info.project_id` in `app/google-services.json` (debug) or `app/src/release/google-services.json` (production).

### Steps

1. Do a dry run. It writes nothing, and prints the account's sign-in providers, its claims now and the claims it would write:

   ```bash
   python3 scripts/set_admin_claim.py --project <project id> --email <account email>
   ```

   Check it's the account you expect. Keep the output: its `now:` line records the claims before the change.
2. Run the same command with `--apply` added. The script writes the claim, reads the account back and prints its claims after.
3. To take the claim away, add `--revoke`: a dry run first, then with `--apply`.

You can pass `--uid <uid>` instead of `--email`. The uid is in the Firebase console, under Authentication → Users. The script never prints the email, the uid or a token.

In production, treat it like any other production write: keep the dry run's output as your backup, and read it before you apply.

### When the app picks it up

The app reads the claim from the account's ID token, so the change shows once the token refreshes: within an hour, or straight away when the account signs in again. The app has no sign-out yet. To sign in again sooner, clear the app's storage: Settings → Apps → BottleCast (BottleCast-Debug for a debug build) → Storage & cache → Clear storage. Then sign in from **Cast a bottle** → **Agree and set sail** → **Sign in with Google**.
