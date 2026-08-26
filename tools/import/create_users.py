#!/usr/bin/env python3
"""Create dealer accounts from a CSV (Name, username, password, email).

Skips usernames that already exist, so it is safe to re-run. Writes a report CSV
with the outcome for every row. Run with --commit; without it you get a dry run.
"""
import argparse, csv, json, os, sys, urllib.error, urllib.parse, urllib.request
from concurrent.futures import ThreadPoolExecutor

ENV = dict(l.strip().split("=", 1) for l in open(os.path.expanduser("~/.the3s_upload.env"))
           if "=" in l and not l.startswith("#"))
BASE = ENV["PARSE_SERVER_URL"].rstrip("/")
H = {"X-Parse-Application-Id": ENV["PARSE_APP_ID"], "X-Parse-Master-Key": ENV["PARSE_MASTER_KEY"],
     "Content-Type": "application/json"}


def existing_usernames():
    """Every username already in _User (paged)."""
    out, skip = set(), 0
    while True:
        q = urllib.parse.urlencode({"limit": "1000", "skip": str(skip), "keys": "username", "order": "createdAt"})
        page = json.load(urllib.request.urlopen(urllib.request.Request(BASE + "/users?" + q, headers=H), timeout=120))["results"]
        if not page:
            break
        out |= {(u.get("username") or "").strip() for u in page}
        skip += len(page)
        if len(page) < 1000:
            break
    return out


def create(row):
    body = {"username": row["username"], "password": row["password"], "Markup": 0, "MarkupType": "percent"}
    if row.get("Name"):
        body["Name"] = row["Name"]
    if row.get("email"):
        body["email"] = row["email"]
    req = urllib.request.Request(BASE + "/users", data=json.dumps(body).encode(), headers=H, method="POST")
    try:
        obj = json.load(urllib.request.urlopen(req, timeout=90))
        return row["username"], "created", obj.get("objectId", "")
    except urllib.error.HTTPError as e:
        detail = e.read().decode()[:120]
        return row["username"], "taken" if "taken" in detail or "exists" in detail else "failed:%d" % e.code, detail
    except Exception as e:
        return row["username"], "failed", str(e)[:120]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("csv_path")
    ap.add_argument("--report", default="user_import_report.csv")
    ap.add_argument("--workers", type=int, default=8)
    ap.add_argument("--commit", action="store_true")
    args = ap.parse_args()

    rows = [{k: (v or "").strip() for k, v in r.items()}
            for r in csv.DictReader(open(os.path.expanduser(args.csv_path), encoding="utf-8", errors="replace"))]
    rows = [r for r in rows if r.get("username") and r.get("password")]
    have = existing_usernames()
    todo = [r for r in rows if r["username"] not in have]
    print("CSV rows: %d | already in the DB: %d | to create: %d"
          % (len(rows), len(rows) - len(todo), len(todo)))
    if not args.commit:
        print("\nDry run. Re-run with --commit to create the accounts.")
        return

    done = []
    with ThreadPoolExecutor(max_workers=args.workers) as ex:
        for n, res in enumerate(ex.map(create, todo), 1):
            done.append(res)
            if n % 100 == 0 or n == len(todo):
                print("  %d/%d" % (n, len(todo)))
    with open(args.report, "w", newline="") as f:
        w = csv.writer(f); w.writerow(["username", "status", "detail"]); w.writerows(done)
    counts = {}
    for _, status, _ in done:
        counts[status] = counts.get(status, 0) + 1
    print("\nDone. " + ", ".join("%s: %d" % kv for kv in sorted(counts.items())))
    print("Report: %s" % args.report)


if __name__ == "__main__":
    main()
