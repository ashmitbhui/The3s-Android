#!/usr/bin/env python3
"""
Attach photos from a folder to products already imported into Parse.

    python3 attach_images.py ~/Downloads/"table clock" --brand 3S --series 26-27          # dry run
    python3 attach_images.py ~/Downloads/"table clock" --brand 3S --series 26-27 --commit

Handles both folder shapes:

  nested   <item#> <Product Name> <Category>/IMG_1234.jpg   (several photos per product)
  flat     001-versace.png, versace.jpg, 1.jpg              (one photo per product)

The first photo becomes the product's Image (used on cards and as the first slide).
Any additional photos are created as Media rows and linked, which is what the
product page's image slider reads.

Matching is by leading item number first, then by product name. Anything that
cannot be matched confidently is reported and skipped — nothing is guessed.
"""

import argparse
import json
import mimetypes
import os
import pathlib
import re
import sys
import unicodedata
import urllib.error
import urllib.parse
import urllib.request

IMAGE_EXT = {".jpg", ".jpeg", ".png", ".webp", ".heic"}


def load_env():
    path = pathlib.Path(os.path.expanduser("~/.the3s_upload.env"))
    if not path.exists():
        sys.exit("Missing ~/.the3s_upload.env")
    env = {}
    for line in path.read_text().splitlines():
        if line.strip() and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            env[k.strip()] = v.strip()
    return env


ENV = load_env()
BASE = ENV["PARSE_SERVER_URL"].rstrip("/")
HEADERS = {
    "X-Parse-Application-Id": ENV["PARSE_APP_ID"],
    "X-Parse-Master-Key": ENV["PARSE_MASTER_KEY"],
}


class ParseError(RuntimeError):
    """HTTP error from Parse. A normal exception so callers (incl. the review
    server) can handle it instead of the process dying."""

    def __init__(self, status, path, body):
        super().__init__("Parse error %s on %s: %s" % (status, path, body[:300]))
        self.status = status
        self.path = path
        self.body = body


def call(method, path, data=None, content_type="application/json"):
    req = urllib.request.Request(BASE + path, method=method)
    for k, v in HEADERS.items():
        req.add_header(k, v)
    body = None
    if data is not None:
        body = data if isinstance(data, bytes) else json.dumps(data).encode()
        req.add_header("Content-Type", content_type)
    try:
        with urllib.request.urlopen(req, body, timeout=120) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        raise ParseError(e.code, path, e.read().decode())


def norm(text):
    text = unicodedata.normalize("NFKD", str(text)).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]", "", text.lower())


# Subfolders holding clean catalogue cut-outs rather than lifestyle shots.
STUDIO_HINT = re.compile(r"white\s*_?back\s*_?ground|white\s*bg|shoot|cut\s*out", re.IGNORECASE)


def looks_like_cutout(path, tolerance=18, coverage=0.85):
    """True when the photo's border is essentially plain white/near-white.

    Folder names are unreliable (many clean pack-shots sit outside a
    'White background' folder), so classify from the pixels: sample the
    border ring and check how much of it is near-white.
    """
    try:
        with open_image(path) as im:
            im = im.convert("RGB")
            im.thumbnail((160, 160))
            w, h = im.size
            px = im.load()
            ring = []
            for x in range(w):
                ring.append(px[x, 0]); ring.append(px[x, h - 1])
            for y in range(h):
                ring.append(px[0, y]); ring.append(px[w - 1, y])
            light = sum(1 for r, g, b in ring
                        if r >= 255 - tolerance and g >= 255 - tolerance and b >= 255 - tolerance)
            return light / len(ring) >= coverage
    except Exception:
        return False


def sort_photos(photos):
    """Studio cut-outs first (they are the catalogue shots), then lifestyle photos."""
    def rank(p):
        return (0 if is_cutout(p) else 1, p.name.lower())
    return sorted(photos, key=rank)


_CUTOUT_CACHE = {}


def is_cutout(path):
    """Cut-out if the folder says so, or if the pixels say so."""
    key = str(path)
    if key not in _CUTOUT_CACHE:
        _CUTOUT_CACHE[key] = (any(STUDIO_HINT.search(part) for part in path.parts[:-1])
                              or looks_like_cutout(path))
    return _CUTOUT_CACHE[key]


VARIANT_DIR = re.compile(
    r"(shoot|live|white\s*back|background|cut\s*out|studio|image|photo|pic|final|edit|untitled)",
    re.IGNORECASE)


def collect(folder: pathlib.Path):
    """-> list of (item_no|None, name_hint, section, [image paths]) per product.

    Product folders look like '<item#> <Name> <Category>' and may nest their
    photos one level deeper ('White background/', '* Shoot/', '* Live Images/').
    `section` is the top-level folder the group sits under (for libraries filed
    by category, such as Verbose's 'Notebooks/', 'Pens/'), or None at the root.
    """
    groups = []

    def scan(base, section):
        for d in sorted(x for x in base.iterdir() if x.is_dir()):
            m = re.match(r"\s*(\d+)\s+(.*)", d.name)
            photos = [p for p in d.rglob("*")
                      if p.is_file() and p.suffix.lower() in IMAGE_EXT
                      and not p.name.startswith(".")]
            here = section if section is not None else d.name
            subdirs = [x for x in d.iterdir() if x.is_dir()]
            direct = [x for x in d.iterdir()
                      if x.is_file() and x.suffix.lower() in IMAGE_EXT
                      and not x.name.startswith(".")]
            # a folder is one product when it is numbered, holds photos itself,
            # or only contains shoot/live/white-background variants of one item
            variants_only = bool(subdirs) and all(VARIANT_DIR.search(x.name) for x in subdirs)
            # never swallow numbered product folders sitting one level deeper
            has_numbered = any(re.match(r"\s*\d+\s+", x.name) for x in subdirs)
            if has_numbered and not m:
                scan(d, here)
                continue
            if m:
                if photos:
                    groups.append((int(m.group(1)), m.group(2), section, sort_photos(photos)))
            elif not m and subdirs and direct and not variants_only:
                # a category folder ('shirts/') holding product folders AND a few
                # loose photos: each loose file is its own product, not a slide
                for x in sorted(direct):
                    groups.append((None, x.stem, here, [x]))
                scan(d, here)
            elif photos and (direct or variants_only or not subdirs):
                groups.append((None, d.name, section, sort_photos(photos)))
            else:
                scan(d, here)   # a grouping folder such as 'Notebooks' or 'WW'

    scan(folder, None)

    loose = sorted(p for p in folder.iterdir()
                   if p.is_file() and p.suffix.lower() in IMAGE_EXT
                   and not p.name.startswith("."))
    for p in loose:
        m = re.match(r"\s*(\d+)\b[-_ ]*(.*)", p.stem)
        item_no = int(m.group(1)) if m else None
        name_hint = (m.group(2) if m and m.group(2) else p.stem)
        groups.append((item_no, name_hint, None, [p]))
    return groups


def norm_category(text):
    return re.sub(r"[^a-z0-9]", "", (text or "").lower()).rstrip("s")


def category_for(section, categories):
    """Map a folder like 'Tshirts' or 'Verbose photo frames' to a real category."""
    if not section:
        return None
    key = norm_category(section)
    by_key = {norm_category(c): c for c in categories}
    if key in by_key:                     # exact first: 'shirts' must not become T-Shirts
        return by_key[key]
    for ck in sorted(by_key, key=len, reverse=True):
        if key in ck or ck in key:
            return by_key[ck]
    return None


NOISE_WORDS = {
    "bag", "bags", "carry", "canvas", "tote", "notebook", "notebooks", "diary",
    "pen", "pens", "cap", "caps", "mug", "bottle", "shoot", "live", "image",
    "images", "photo", "photos", "white", "background", "final", "new", "copy",
    "mini", "set", "combo", "tshirt", "tshirts", "shirt", "shirts", "jacket",
    "jackets", "coaster", "coasters", "trophy", "trophies", "clock", "clocks",
    "frame", "frames", "watch", "watches", "magnet", "magnets", "stand",
    "weight", "keychain", "chain", "key", "desk", "manager", "mousepad",
    "drinkware", "lifestyle", "travel", "essentials", "promotional", "paper",
    "fridge", "mobile", "products", "product", "oxford", "formal", "pu",
}


def name_variants(name_hint):
    """Progressively shorter forms of a folder name, best guess first."""
    words = re.split(r"\s+", (name_hint or "").strip())
    seen, out = set(), []
    for cut in range(len(words), 0, -1):
        candidate = " ".join(words[:cut])
        if candidate and candidate.lower() not in seen:
            seen.add(candidate.lower())
            out.append(candidate)
        if words[cut - 1].lower() not in NOISE_WORDS:
            break          # stop at the first meaningful word from the right
    return out


def strip_category_words(name_hint, categories):
    """Folder names often end with the category — 'Audi Photo Frames' -> 'Audi'."""
    text = name_hint
    for cat in sorted(categories, key=len, reverse=True):
        # whole word only: 'Shirts' must not eat the tail of 'Tshirt'
        pattern = re.compile(r"(?<![A-Za-z])" + re.escape(cat.rstrip("s")) + r"s?\s*$", re.IGNORECASE)
        text = pattern.sub("", text).strip()
    return text or name_hint


def open_image(path):
    """Open any photo as a PIL image, upright, converting HEIC via macOS sips when needed.

    Cameras record portrait shots as landscape pixels plus an EXIF orientation flag;
    without applying it here the photo is baked in sideways.
    """
    from PIL import Image, ImageOps
    try:
        im = Image.open(path)
        im.load()
        return ImageOps.exif_transpose(im)
    except Exception:
        import subprocess, tempfile
        tmp = pathlib.Path(tempfile.mkdtemp()) / (path.stem + ".png")
        subprocess.run(["sips", "-s", "format", "png", str(path), "--out", str(tmp)],
                       capture_output=True, check=True)
        im = Image.open(tmp)
        im.load()
        return ImageOps.exif_transpose(im)


def prepare(path: pathlib.Path, max_dim, quality, square=True):
    """Normalise a photo for the app. Returns (bytes, filename, content-type).

    Source photos are full-resolution camera files of mixed shapes and several MB
    each. With square=True every photo is fitted inside a max_dim x max_dim white
    canvas, so all product images share one aspect ratio and the grid and slider
    line up regardless of whether a shot was portrait or landscape.
    """
    if max_dim <= 0:
        ctype = mimetypes.guess_type(path.name)[0] or "image/jpeg"
        return path.read_bytes(), path.name, ctype

    from PIL import Image
    import io

    with open_image(path) as im:
        has_alpha = im.mode in ("RGBA", "LA") or (im.mode == "P" and "transparency" in im.info)
        if has_alpha:
            im = im.convert("RGBA")

        if square:
            im.thumbnail((max_dim, max_dim), Image.LANCZOS)
            canvas = Image.new("RGB", (max_dim, max_dim), (255, 255, 255))
            canvas.paste(im, ((max_dim - im.width) // 2, (max_dim - im.height) // 2),
                         im if has_alpha else None)
            im = canvas
        else:
            if max(im.size) > max_dim:
                scale = max_dim / max(im.size)
                im = im.resize((max(1, round(im.width * scale)),
                                max(1, round(im.height * scale))), Image.LANCZOS)
            if has_alpha:
                buf = io.BytesIO()
                im.save(buf, "PNG", optimize=True)
                return buf.getvalue(), path.stem + ".png", "image/png"

        buf = io.BytesIO()
        im.convert("RGB").save(buf, "JPEG", quality=quality, optimize=True, progressive=True)
        return buf.getvalue(), path.stem + ".jpg", "image/jpeg"


def uploaded_name_for(path: pathlib.Path, square=True):
    """The filename prepare() would produce, so an already-uploaded photo can be spotted."""
    stem = re.sub(r"[^A-Za-z0-9._-]", "_", path.stem)
    return stem + (".jpg" if square else path.suffix.lower())


def upload_file(path: pathlib.Path, max_dim, quality, square=True):
    data, name, ctype = prepare(path, max_dim, quality, square)
    safe = urllib.parse.quote(re.sub(r"[^A-Za-z0-9._-]", "_", name))
    return call("POST", "/files/%s" % safe, data, ctype)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("folder")
    ap.add_argument("--brand", required=True)
    ap.add_argument("--series", required=True)
    ap.add_argument("--commit", action="store_true")
    ap.add_argument("--max-extra", type=int, default=4,
                    help="extra photos per product to attach as Media (default 4)")
    ap.add_argument("--max-dim", type=int, default=1000,
                    help="square container size in px (default 1000; 0 = keep original)")
    ap.add_argument("--quality", type=int, default=85, help="JPEG quality (default 85)")
    ap.add_argument("--no-square", action="store_true",
                    help="keep original aspect ratio instead of padding to a square")
    ap.add_argument("--slides-only", action="store_true",
                    help="add Media slides but never touch the existing card Image "
                         "(keeps covers you picked on the review page)")
    ap.add_argument("--only-missing", action="store_true",
                    help="only touch products that have no card Image yet; products "
                         "with a cover (hand-picked or not) are left exactly as they are")
    ap.add_argument("--choices", default=None,
                    help="card_choices.json downloaded from the review page: "
                         "product name -> photo to use as the card image")
    args = ap.parse_args()

    folder = pathlib.Path(os.path.expanduser(args.folder))
    if not folder.is_dir():
        sys.exit("Not a folder: %s" % folder)

    where = json.dumps({"Brand": args.brand, "Series": args.series})
    params = urllib.parse.urlencode({"where": where, "limit": "1000",
                                     "keys": "Name,Category,Order,Image"})
    products = call("GET", "/classes/Products?%s" % params)["results"]
    if not products:
        sys.exit("No products found for Brand=%s Series=%s" % (args.brand, args.series))

    categories = {p.get("Category") for p in products if p.get("Category")}
    by_order = {p.get("Order"): p for p in products if p.get("Order") is not None}
    by_name = {}
    for p in products:
        by_name.setdefault(norm(p.get("Name")), []).append(p)

    choices = {}
    if args.choices:
        choices = json.loads(pathlib.Path(os.path.expanduser(args.choices)).read_text())
        print("Loaded %d card-image choice(s) from %s" % (len(choices), args.choices))

    groups = collect(folder)
    matched, unmatched, ambiguous = [], [], []
    used = set()

    for item_no, name_hint, section, photos in groups:
        product = None
        section_category = category_for(section, categories)
        if item_no is not None and item_no in by_order:
            product = by_order[item_no]
            # '289 Volvo Bags' inside Bags/ must be a Bags product; if #289 is
            # something else the number is a photo-list index, not the Order.
            if section_category and product.get("Category") != section_category:
                product = None
        if product is None:
            def in_section(cs):
                # the folder a photo sits in tells us the category. A name match
                # from a different category is never right (a "Volvo" in Bags/
                # must not become the cover of the "Volvo" cap), so drop those
                # rather than fall back to them.
                if not section_category:
                    return cs
                return [c for c in cs if c.get("Category") == section_category]

            candidates = []
            for variant in name_variants(strip_category_words(name_hint, categories)):
                candidates = in_section(by_name.get(norm(variant), []))
                if candidates:
                    break
            if not candidates and section_category:
                # 'Mercedes' inside Tshirts/ when the only Mercedes T-shirt is
                # 'Mercedes Benz': accept a unique same-category name that starts
                # with the folder name as a whole word
                stem = norm(strip_category_words(name_hint, categories))
                if len(stem) >= 3:
                    starts = [p for p in products
                              if p.get("Category") == section_category
                              and norm(p.get("Name")).startswith(stem)]
                    if len({p["objectId"] for p in starts}) == 1:
                        candidates = starts
            if not candidates:
                import difflib
                pool = [n for n, ps in by_name.items()
                        if not section_category
                        or any(p.get("Category") == section_category for p in ps)]
                close = difflib.get_close_matches(
                    norm(strip_category_words(name_hint, categories)), pool, n=1, cutoff=0.86)
                if close:
                    candidates = in_section(by_name.get(close[0], []))
            if len(candidates) == 1:
                product = candidates[0]
            elif len(candidates) > 1:
                ambiguous.append((name_hint, len(candidates)))
                continue
        if product is None:
            unmatched.append(name_hint)
            continue
        if product["objectId"] in used:
            continue
        if args.only_missing and product.get("Image"):
            used.add(product["objectId"])   # has a cover: leave it alone
            continue
        used.add(product["objectId"])
        picked = choices.get(product.get("Name"))
        if picked:
            chosen = next((p for p in photos if str(p) == picked), None)
            if chosen:
                photos = [chosen] + [p for p in photos if p != chosen]
            else:
                print("  note: chosen photo for %r is not in this folder — keeping default"
                      % product.get("Name"))
        matched.append((product, photos))

    print("Folder:    %s" % folder)
    print("Products:  %d (%s / %s)" % (len(products), args.brand, args.series))
    print("Matched:   %d of %d photo groups" % (len(matched), len(groups)))
    print("Photos:    %d total" % sum(len(p) for _, p in matched))
    overridden = sum(1 for p, _ in matched if choices.get(p.get("Name")))
    if overridden:
        print("Using your chosen card image for %d product(s)" % overridden)
    already = sum(1 for p, _ in matched if p.get("Image"))
    if already and not args.slides_only:
        print("Note:      %d matched products already have an Image (it will be replaced)" % already)
    elif args.slides_only:
        print("Mode:      slides only — existing card images are kept as they are")
    for name_hint, n in ambiguous:
        print("  ambiguous: %r matches %d products — skipped" % (name_hint, n))
    for name_hint in unmatched:
        print("  unmatched: %r" % name_hint)
    missing = [p["Name"] for p in products if p["objectId"] not in used]
    if missing:
        print("  no photos found for %d product(s): %s" % (len(missing), ", ".join(missing[:8])))

    if not args.commit:
        print("\nWould attach:")
        for product, photos in matched:
            print("  %-16s %-28s <- %s  (%d photo%s)" % (
                product.get("Category"), product.get("Name"),
                photos[0].parent.relative_to(folder), len(photos), "" if len(photos) == 1 else "s"))
        print("\nDry run. Re-run with --commit to upload.")
        return

    for n, (product, photos) in enumerate(matched, 1):
        update = {}
        if args.slides_only:
            # Keep whatever cover is already live — including one chosen by hand —
            # and never repeat it among the slides.
            current = (product.get("Image") or {}).get("name", "")
            extras = [p for p in photos
                      if not current.endswith(uploaded_name_for(p, not args.no_square))]
            extras = extras[:args.max_extra]
        else:
            main_photo, extras = photos[0], photos[1:args.max_extra + 1]
            update["Image"] = dict(upload_file(main_photo, args.max_dim, args.quality,
                                               not args.no_square), __type="File")
        media_pointers = []
        for extra in extras:
            media = call("POST", "/classes/Media", {
                "Type": "image",
                "File": dict(upload_file(extra, args.max_dim, args.quality, not args.no_square), __type="File"),
                "Product": product["Name"],
            })
            media_pointers.append({"__type": "Pointer", "className": "Media",
                                   "objectId": media["objectId"]})
        if media_pointers:
            update["Media"] = media_pointers
        if not update:
            print("[%d/%d] %s  (no new slides)" % (n, len(matched), product["Name"]))
            continue
        call("PUT", "/classes/Products/%s" % product["objectId"], update)
        shown = len(media_pointers) + (0 if args.slides_only else 1)
        print("[%d/%d] %s  (%s%d image%s)"
              % (n, len(matched), product["Name"],
                 "+" if args.slides_only else "", shown, "" if shown == 1 else "s"))

    print("\nDone. %d products updated." % len(matched))


if __name__ == "__main__":
    try:
        main()
    except ParseError as exc:
        sys.exit(str(exc))
