#!/usr/bin/env python3
"""
Serve the photo review page and apply card-image choices straight to Parse.

    python3 serve_review.py --brand 3S --series 26-27 --folder ~/Downloads/3S

Opens http://127.0.0.1:8765 . Clicking "Use as card" on any photo resizes it to
the agreed 1000x1000 square, uploads it, and updates that product's Image in
Parse immediately — no download, no second command.

Binds to localhost only. Build the page first with build_review.py.
"""

import argparse
import http.server
import importlib.util
import json
import os
import pathlib
import socketserver
import threading
import urllib.parse
import webbrowser

HERE = pathlib.Path(__file__).parent
spec = importlib.util.spec_from_file_location("attach_images", HERE / "attach_images.py")
AI = importlib.util.module_from_spec(spec)
spec.loader.exec_module(AI)

ROOT = HERE / "review"
STATE = {"series": None, "max_dim": 1000, "quality": 85}
BRANDS = {}            # brand -> {"folder": Path, "products": {name: objectId}}
LOCK = threading.Lock()


def load_products(brand):
    where = json.dumps({"Brand": brand, "Series": STATE["series"]})
    params = urllib.parse.urlencode({"where": where, "limit": "1000", "keys": "Name"})
    rows = AI.call("GET", "/classes/Products?%s" % params)["results"]
    return {row.get("Name"): row["objectId"] for row in rows}


def write_index():
    links = "".join(
        ('<a href="{b}/"><b>{b}</b><span>{n} products</span><span>pick covers from folders</span></a>'
         '<a href="manage?brand={b}&series={s}"><b>{b}</b>'
         '<span>{n} products</span><span>manage uploaded images</span></a>').format(
             b=b, n=len(v["products"]), s=STATE["series"])
        for b, v in BRANDS.items() if (ROOT / b / "index.html").exists())
    (ROOT / "index.html").write_text("""<!doctype html><meta charset="utf-8">
<title>Photo review — {series}</title>
<style>
 body{{margin:0;background:#F4F7F5;color:#171A23;
   font:15px/1.5 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif}}
 header{{background:#0B3D2E;color:#fff;padding:20px 26px}}
 header h1{{margin:0;font-size:19px;font-weight:600}}
 main{{padding:26px;display:flex;gap:14px;flex-wrap:wrap}}
 a{{background:#fff;border:1px solid #E2E8E4;border-radius:14px;padding:20px 26px;
   text-decoration:none;color:inherit;min-width:190px}}
 a:hover{{border-color:#0B3D2E}}
 a b{{display:block;font-size:17px}} a span{{color:#6B7A71;font-size:13px}}
</style>
<header><h1>Photo review — {series} collection</h1></header>
<main>{links}</main>""".format(series=STATE["series"], links=links), encoding="utf-8")


def fetch_products(brand, series):
    where = json.dumps({"Brand": brand, "Series": series})
    params = urllib.parse.urlencode({"where": where, "limit": "1000",
                                     "keys": "Name,Category,Order,Price,Image,Media",
                                     "include": "Media"})
    return AI.call("GET", "/classes/Products?%s" % params)["results"]


def shape(row):
    image = row.get("Image") or {}
    slides = []
    for media in (row.get("Media") or []):
        if isinstance(media, dict) and media.get("File"):
            slides.append({"id": media.get("objectId"),
                           "url": media["File"].get("url"),
                           "name": media["File"].get("name")})
    return {"id": row["objectId"], "name": row.get("Name"), "category": row.get("Category"),
            "order": row.get("Order"), "price": row.get("Price"),
            "cover": image.get("url"), "coverName": image.get("name"), "slides": slides}


def list_products(brand, series):
    rows = fetch_products(brand, series)
    rows.sort(key=lambda r: (r.get("Category") or "", r.get("Order") or 0))
    return [shape(r) for r in rows]


def one(brand, series, object_id):
    for row in fetch_products(brand, series):
        if row["objectId"] == object_id:
            return row
    raise ValueError("product no longer exists — reload the page")


def promote(req):
    """Make a slide the cover; the previous cover becomes a slide, so nothing is lost."""
    row = one(req["brand"], req["series"], req["id"])
    target = next((m for m in (row.get("Media") or [])
                   if isinstance(m, dict) and m.get("objectId") == req.get("mediaId")), None)
    if not target:
        raise ValueError("that image is no longer on this product — reload the page")

    old_cover = row.get("Image")
    new_media = [{"__type": "Pointer", "className": "Media", "objectId": m["objectId"]}
                 for m in (row.get("Media") or [])
                 if isinstance(m, dict) and m.get("objectId") != target["objectId"]]
    if old_cover:
        demoted = AI.call("POST", "/classes/Media",
                          {"Type": "image", "File": dict(old_cover, __type="File"),
                           "Product": row.get("Name")})
        new_media.append({"__type": "Pointer", "className": "Media",
                          "objectId": demoted["objectId"]})

    AI.call("PUT", "/classes/Products/%s" % row["objectId"],
            {"Image": dict(target["File"], __type="File"), "Media": new_media})
    try:
        AI.call("DELETE", "/classes/Media/%s" % target["objectId"])
    except AI.ParseError as exc:
        if exc.status != 404:
            raise
    return shape(one(req["brand"], req["series"], req["id"]))


def remove(req):
    """Unlink a slide and delete its Media row. Covers are never removed this way."""
    row = one(req["brand"], req["series"], req["id"])
    keep = [{"__type": "Pointer", "className": "Media", "objectId": m["objectId"]}
            for m in (row.get("Media") or [])
            if isinstance(m, dict) and m.get("objectId") != req.get("mediaId")]
    AI.call("PUT", "/classes/Products/%s" % row["objectId"], {"Media": keep})
    if req.get("mediaId"):
        try:
            AI.call("DELETE", "/classes/Media/%s" % req["mediaId"])
        except AI.ParseError as exc:
            if exc.status != 404:                 # already deleted is fine
                raise
    return shape(one(req["brand"], req["series"], req["id"]))


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **kw):
        super().__init__(*a, directory=str(ROOT), **kw)

    def log_message(self, fmt, *args):
        pass          # the interesting events are printed explicitly below

    def do_GET(self):
        if self.path in ("/favicon.ico", "/apple-touch-icon.png"):
            self.send_response(204)
            self.end_headers()
            return
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path == "/api/list":
            q = urllib.parse.parse_qs(parsed.query)
            return self._json(200, {"products": list_products(q.get("brand", [""])[0],
                                                              q.get("series", [""])[0])})
        if parsed.path.rstrip("/").endswith("manage"):
            body = (HERE / "manage.html").read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        super().do_GET()

    def _json(self, code, payload):
        body = json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        if self.path in ("/api/promote", "/api/remove"):
            try:
                length = int(self.headers.get("Content-Length", 0))
                req = json.loads(self.rfile.read(length) or b"{}")
                with LOCK:
                    product = (promote if self.path.endswith("promote") else remove)(req)
                print("  %-12s %-30s %-8s %s" % (req.get("brand"), req.get("product"),
                                                 self.path.split("/")[-1], req.get("id")))
                return self._json(200, {"ok": True, "product": product})
            except BaseException as exc:      # nothing may kill the server
                return self._json(500, {"error": str(exc)})
        if self.path != "/api/set-card":
            return self._json(404, {"error": "not found"})
        try:
            length = int(self.headers.get("Content-Length", 0))
            req = json.loads(self.rfile.read(length) or b"{}")
            product = req.get("product")
            brand = req.get("brand")
            photo = pathlib.Path(req.get("path", ""))

            entry = BRANDS.get(brand)
            if not entry:
                return self._json(400, {"error": "unknown brand %r" % brand})
            object_id = entry["products"].get(product)
            if not object_id:
                return self._json(404, {"error": "product %r not found in Parse" % product})
            folder = entry["folder"].resolve()
            if not photo.exists() or folder not in photo.resolve().parents:
                return self._json(400, {"error": "photo not inside the photo folder"})

            with LOCK:
                uploaded = AI.upload_file(photo, STATE["max_dim"], STATE["quality"], True)
                AI.call("PUT", "/classes/Products/%s" % object_id,
                        {"Image": dict(uploaded, __type="File")})
            print("  %-12s %-28s <- %s" % (brand, product, photo.name))
            return self._json(200, {"ok": True, "product": product,
                                    "file": uploaded.get("name"), "url": uploaded.get("url")})
        except BaseException as exc:                   # surfaced in the page
            return self._json(500, {"error": str(exc)})


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--app", action="append", required=True, metavar="BRAND=FOLDER",
                    help="repeatable, e.g. --app 3S=~/Downloads/3S")
    ap.add_argument("--series", required=True)
    ap.add_argument("--port", type=int, default=8765)
    ap.add_argument("--max-dim", type=int, default=1000)
    ap.add_argument("--quality", type=int, default=85)
    args = ap.parse_args()

    STATE.update(series=args.series, max_dim=args.max_dim, quality=args.quality)
    for spec_str in args.app:
        brand, _, folder = spec_str.partition("=")
        path = pathlib.Path(os.path.expanduser(folder))
        if not (ROOT / brand / "index.html").exists():
            raise SystemExit("Build it first:  python3 build_review.py --brand %s "
                             "--series %s --folder %s" % (brand, args.series, folder))
        BRANDS[brand] = {"folder": path, "products": load_products(brand)}
    write_index()
    count = sum(len(v["products"]) for v in BRANDS.values())

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("127.0.0.1", args.port), Handler) as httpd:
        url = "http://127.0.0.1:%d/" % args.port
        print("Review server — %s collection, %d products" % (args.series, count))
        for b, v in BRANDS.items():
            print("   %-12s %3d products   %s" % (b, len(v["products"]), v["folder"]))
        print("Card images upload at %dx%d, quality %d" % (args.max_dim, args.max_dim, args.quality))
        print("\n  %s\n\nClicking 'Use as card' updates Parse immediately. Ctrl-C to stop." % url)
        webbrowser.open(url)
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nstopped")


if __name__ == "__main__":
    main()
