#!/usr/bin/env python3
"""
Bulk-import a season's categories and products into the The3S Parse database.

Credentials come from ~/.the3s_upload.env and are never read from the repo:
    PARSE_SERVER_URL=https://parseapi.back4app.com
    PARSE_APP_ID=...
    PARSE_MASTER_KEY=...

Usage:
    python3 import_season.py --series 26-27                 # dry run: validate only
    python3 import_season.py --series 26-27 --commit        # actually write
    python3 import_season.py --series 26-27 --commit --categories-only

Nothing is deleted or overwritten. New rows are created with isEnabled=True and
the given Series, so they stay invisible in the app until Basic.CurrentSeries
is switched to that series.
"""

import argparse
import csv
import json
import mimetypes
import os
import pathlib
import sys
import urllib.error
import urllib.parse
import urllib.request

HERE = pathlib.Path(__file__).parent
VALID_BRANDS = {"3S", "Trophyverse", "Verbose"}


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
    for key in ("PARSE_SERVER_URL", "PARSE_APP_ID", "PARSE_MASTER_KEY"):
        if not env.get(key):
            sys.exit("%s missing from ~/.the3s_upload.env" % key)
    return env


class Parse:
    def __init__(self, env):
        self.url = env["PARSE_SERVER_URL"].rstrip("/")
        self.headers = {
            "X-Parse-Application-Id": env["PARSE_APP_ID"],
            "X-Parse-Master-Key": env["PARSE_MASTER_KEY"],
        }

    def _send(self, method, path, data=None, content_type="application/json"):
        req = urllib.request.Request(self.url + path, method=method)
        for k, v in self.headers.items():
            req.add_header(k, v)
        body = None
        if data is not None:
            body = data if isinstance(data, bytes) else json.dumps(data).encode()
            req.add_header("Content-Type", content_type)
        try:
            with urllib.request.urlopen(req, body) as resp:
                return json.loads(resp.read().decode())
        except urllib.error.HTTPError as e:
            raise SystemExit("Parse error %s on %s %s: %s"
                             % (e.code, method, path, e.read().decode()[:300]))

    def query(self, cls, where=None, limit=1000, keys=None):
        params = {"limit": str(limit)}
        if where:
            params["where"] = json.dumps(where)
        if keys:
            params["keys"] = keys
        return self._send("GET", "/classes/%s?%s" % (cls, urllib.parse.urlencode(params)))["results"]

    def create(self, cls, obj):
        return self._send("POST", "/classes/%s" % cls, obj)

    def upload_file(self, path: pathlib.Path):
        ctype = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
        safe = urllib.parse.quote(path.name)
        return self._send("POST", "/files/%s" % safe, path.read_bytes(), ctype)


def read_csv(path):
    if not path.exists():
        return []
    with path.open(newline="", encoding="utf-8-sig") as fh:
        return [row for row in csv.DictReader(fh) if any((v or "").strip() for v in row.values())]


def as_number(value, field, row_no, errors):
    text = (value or "").strip().replace(",", "").replace("₹", "")
    if not text:
        return None
    try:
        number = float(text)
    except ValueError:
        errors.append("row %d: %s is not a number (%r)" % (row_no, field, value))
        return None
    return int(number) if number.is_integer() else number


def as_bool(value):
    return (value or "").strip().lower() in ("true", "yes", "1", "y")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--series", required=True, help='e.g. 26-27')
    ap.add_argument("--commit", action="store_true", help="write to Parse (default: dry run)")
    ap.add_argument("--categories-only", action="store_true")
    ap.add_argument("--products-only", action="store_true")
    args = ap.parse_args()

    parse = Parse(load_env())
    categories = read_csv(HERE / "categories.csv")
    products = read_csv(HERE / "products.csv")

    existing = parse.query("Category", limit=1000, keys="Name,Brand")
    known = {(c.get("Brand"), c.get("Name")) for c in existing}
    errors, warnings = [], []

    # ---- validate categories ----
    planned_categories = []
    if not args.products_only:
        for i, row in enumerate(categories, start=2):
            name, brand = row.get("Name", "").strip(), row.get("Brand", "").strip()
            if not name or brand not in VALID_BRANDS:
                errors.append("categories row %d: Name required, Brand must be one of %s"
                              % (i, ", ".join(sorted(VALID_BRANDS))))
                continue
            if (brand, name) in known:
                warnings.append("categories row %d: %s / %s already exists — skipping" % (i, brand, name))
                continue
            icon = (row.get("icon_file") or "").strip()
            icon_path = HERE / "icons" / icon if icon else None
            if icon and not icon_path.exists():
                errors.append("categories row %d: icon not found: icons/%s" % (i, icon))
            planned_categories.append((name, brand, icon_path if icon else None))
            known.add((brand, name))

    # ---- validate products ----
    planned_products = []
    if not args.categories_only:
        for i, row in enumerate(products, start=2):
            name = (row.get("Name") or "").strip()
            brand = (row.get("Brand") or "").strip()
            category = (row.get("Category") or "").strip()
            if not name:
                errors.append("products row %d: Name is required" % i)
                continue
            if brand not in VALID_BRANDS:
                errors.append("products row %d: Brand %r must be one of %s"
                              % (i, brand, ", ".join(sorted(VALID_BRANDS))))
            elif (brand, category) not in known:
                errors.append("products row %d: category %r does not exist for brand %s "
                              "(add it to categories.csv)" % (i, category, brand))

            image = (row.get("image_file") or "").strip()
            image_path = HERE / "images" / image if image else None
            if not image:
                warnings.append("products row %d: %s has no image" % (i, name))
            elif not image_path.exists():
                errors.append("products row %d: image not found: images/%s" % (i, image))

            obj = {
                "Name": name,
                "Brand": brand,
                "Category": category,
                "Detail": (row.get("Detail") or "").strip(),
                "Series": args.series,
                "isEnabled": True,
                "isNew": as_bool(row.get("isNew")),
            }
            for field in ("Price", "Order", "MOQ", "MasterPacking", "MasterSerial"):
                value = as_number(row.get(field), field, i, errors)
                if value is not None:
                    obj[field] = value
            serial = (row.get("Serial") or "").strip()
            if serial:
                obj["Serial"] = serial
            planned_products.append((obj, image_path if image else None))

    # ---- report ----
    print("Series:            %s" % args.series)
    print("Categories to add: %d" % len(planned_categories))
    print("Products to add:   %d" % len(planned_products))
    for w in warnings:
        print("  warn: %s" % w)
    if errors:
        print("\n%d error(s) — nothing was written:" % len(errors))
        for e in errors:
            print("  %s" % e)
        sys.exit(1)
    if not args.commit:
        print("\nDry run OK. Re-run with --commit to write.")
        return

    # ---- write ----
    for name, brand, icon_path in planned_categories:
        obj = {"Name": name, "Brand": brand}
        if icon_path:
            obj["Icon"] = dict(parse.upload_file(icon_path), __type="File")
        parse.create("Category", obj)
        print("category  + %s / %s" % (brand, name))

    for n, (obj, image_path) in enumerate(planned_products, 1):
        if image_path:
            obj["Image"] = dict(parse.upload_file(image_path), __type="File")
        parse.create("Products", obj)
        print("[%d/%d] product + %s (%s)" % (n, len(planned_products), obj["Name"], obj["Brand"]))

    print("\nDone. These rows are live in Parse but still invisible in the app,")
    print("because Basic.CurrentSeries has not been changed yet.")
    print("To publish:  python3 set_series.py %s" % args.series)


if __name__ == "__main__":
    main()
