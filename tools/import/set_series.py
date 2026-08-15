#!/usr/bin/env python3
"""
Publish a season by pointing Basic.CurrentSeries at it.

    python3 set_series.py              # show current value and per-series counts
    python3 set_series.py 26-27        # switch the app to the 26-27 collection

The app reads Basic.CurrentSeries on every launch and shows only products whose
Series matches, so this is the on/off switch for a collection. Nothing is
deleted; switching back restores the previous season instantly.
"""

import json
import os
import pathlib
import sys
import urllib.error
import urllib.parse
import urllib.request


def load_env():
    path = pathlib.Path(os.path.expanduser("~/.the3s_upload.env"))
    if not path.exists():
        sys.exit("Missing ~/.the3s_upload.env — see tools/import/README.md")
    env = {}
    for line in path.read_text().splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            env[k.strip()] = v.strip()
    return env


ENV = load_env()
BASE = ENV["PARSE_SERVER_URL"].rstrip("/")
HEADERS = {
    "X-Parse-Application-Id": ENV["PARSE_APP_ID"],
    "X-Parse-Master-Key": ENV["PARSE_MASTER_KEY"],
}


def call(method, path, data=None):
    req = urllib.request.Request(BASE + path, method=method)
    for k, v in HEADERS.items():
        req.add_header(k, v)
    body = None
    if data is not None:
        body = json.dumps(data).encode()
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, body) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        raise SystemExit("Parse error %s: %s" % (e.code, e.read().decode()[:300]))


def count(series):
    params = urllib.parse.urlencode({
        "where": json.dumps({"Series": series, "isEnabled": True}),
        "limit": "0",
        "count": "1",
    })
    return call("GET", "/classes/Products?%s" % params).get("count", 0)


def main():
    basic = call("GET", "/classes/Basic?limit=1")["results"]
    if not basic:
        sys.exit("No Basic settings row found.")
    row = basic[0]
    current = row.get("CurrentSeries")

    all_series = sorted({
        p.get("Series") for p in call("GET", "/classes/Products?limit=1000&keys=Series")["results"]
        if p.get("Series")
    })

    if len(sys.argv) == 1:
        print("Current series: %s\n" % current)
        print("Enabled products per series:")
        for s in all_series:
            print("  %-8s %5d %s" % (s, count(s), "  <- live" if s == current else ""))
        print("\nTo publish a season:  python3 set_series.py <series>")
        return

    target = sys.argv[1]
    live = count(target)
    if live == 0:
        sys.exit("Refusing to switch: no enabled products have Series=%r." % target)

    print("Current series: %s" % current)
    print("New series:     %s  (%d enabled products)" % (target, live))
    if input("Switch the live app to this collection? [y/N] ").strip().lower() != "y":
        sys.exit("Cancelled.")

    call("PUT", "/classes/Basic/%s" % row["objectId"], {"CurrentSeries": target})
    print("Done. The app now shows the %s collection." % target)
    print("Rollback: python3 set_series.py %s" % current)


if __name__ == "__main__":
    main()
