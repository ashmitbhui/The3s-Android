#!/usr/bin/env python3
"""
Build a local web page for reviewing the photos that would be uploaded.

    python3 build_review.py --brand 3S --series 26-27 --folder ~/Downloads/3S
    open review/index.html

Generates small thumbnails into review/thumbs/ and an index.html that groups
every product with the images selected for it, in slider order, flagging which
products would end up with a lifestyle shot as their card image.
"""

import argparse
import base64
import html
import importlib.util
import json
import os
import pathlib
import urllib.parse

HERE = pathlib.Path(__file__).parent
spec = importlib.util.spec_from_file_location("attach_images", HERE / "attach_images.py")
AI = importlib.util.module_from_spec(spec)
spec.loader.exec_module(AI)

THUMB = 320


def thumb_for(path, out_dir, index):
    name = "%05d.jpg" % index
    dest = out_dir / name
    if not dest.exists():
        from PIL import Image
        with AI.open_image(path) as im:
            im = im.convert("RGB")
            im.thumbnail((THUMB, THUMB), Image.LANCZOS)
            im.save(dest, "JPEG", quality=78, optimize=True)
    return name


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--brand", required=True)
    ap.add_argument("--series", required=True)
    ap.add_argument("--folder", required=True)
    ap.add_argument("--max-extra", type=int, default=4)
    args = ap.parse_args()

    folder = pathlib.Path(os.path.expanduser(args.folder))
    out = HERE / "review" / args.brand
    thumbs = out / "thumbs"
    thumbs.mkdir(parents=True, exist_ok=True)

    where = json.dumps({"Brand": args.brand, "Series": args.series})
    params = urllib.parse.urlencode({"where": where, "limit": "1000",
                                     "keys": "Name,Category,Order,Price"})
    products = AI.call("GET", "/classes/Products?%s" % params)["results"]
    categories = {p.get("Category") for p in products if p.get("Category")}
    by_order = {p.get("Order"): p for p in products if p.get("Order") is not None}
    by_name = {}
    for p in products:
        by_name.setdefault(AI.norm(p.get("Name")), []).append(p)

    groups = AI.collect(folder)
    entries, used, counter = [], set(), 0

    for item_no, hint, section, photos in groups:
        product = by_order.get(item_no)
        if product is None:
            cands = by_name.get(AI.norm(AI.strip_category_words(hint, categories)), [])
            product = cands[0] if len(cands) == 1 else None
        if product is None or product["objectId"] in used:
            continue
        used.add(product["objectId"])
        chosen = photos[:args.max_extra + 1]
        shots = []
        for i, ph in enumerate(chosen):
            counter += 1
            studio = AI.is_cutout(ph)
            shots.append({
                "thumb": thumb_for(ph, thumbs, counter),
                "studio": studio,
                "main": i == 0,
                "path": str(ph),
                "name": ph.name,
            })
        entries.append({
            "item": product.get("Order"),
            "name": product.get("Name"),
            "category": product.get("Category"),
            "price": product.get("Price"),
            "shots": shots,
            "main_is_live": not shots[0]["studio"],
            "extra": len(photos) - len(chosen),
        })

    entries.sort(key=lambda e: (e["category"] or "", e["item"] or 0))
    missing = [p for p in products if p["objectId"] not in used]
    total_shots = sum(len(e["shots"]) for e in entries)
    needs_work = sum(1 for e in entries if e["main_is_live"])

    cards = []
    for e in entries:
        strip = "".join(
            '<figure class="shot{cls}" data-src="{src}" data-idx="{i}">'
            '<img src="thumbs/{thumb}" loading="lazy" alt="">'
            '<button class="pick" type="button">Use as card</button>'
            '<figcaption><b class="b role">{role}</b>'
            '{kind}<a class="fn" href="file://{full}" target="_blank">{fn}</a>'
            '</figcaption></figure>'.format(
                cls=" is-main" if s["main"] else "",
                i=i,
                src=html.escape(s["path"], quote=True),
                full=urllib.parse.quote(s["path"]),
                thumb=s["thumb"],
                role="card image" if s["main"] else "slide",
                kind='<b class="b studio">studio</b>' if s["studio"] else '<b class="b live">lifestyle</b>',
                fn=html.escape(s["name"]),
            ) for i, s in enumerate(e["shots"]))
        cards.append(
            '<article class="product{flag}" data-cat="{cat}" data-flag="{fl}"'
            ' data-item="{item}" data-name="{name}">'
            '<header><span class="num">#{item}</span><h3>{name}</h3>'
            '<span class="meta">{cat} · ₹{price}</span>{warn}</header>'
            '<div class="strip">{strip}</div></article>'.format(
                flag=" flagged" if e["main_is_live"] else "",
                cat=html.escape(e["category"] or ""),
                fl="live" if e["main_is_live"] else "studio",
                item=e["item"], name=html.escape(e["name"] or ""),
                price=e["price"],
                warn='<span class="warn">card image is a lifestyle shot</span>' if e["main_is_live"] else "",
                strip=strip))

    cat_buttons = "".join('<button data-filter="cat:%s">%s</button>' % (html.escape(c), html.escape(c))
                          for c in sorted({e["category"] for e in entries if e["category"]}))
    missing_list = "".join("<li>%s <span>#%s · %s</span></li>"
                           % (html.escape(p.get("Name") or ""), p.get("Order"), html.escape(p.get("Category") or ""))
                           for p in missing)

    page = TEMPLATE.format(
        brand=html.escape(args.brand), series=html.escape(args.series),
        products=len(entries), shots=total_shots, needs_work=needs_work,
        missing_n=len(missing), cat_buttons=cat_buttons,
        cards="\n".join(cards),
        missing_block=('<section class="missing"><h2>No photos found (%d)</h2><ul>%s</ul></section>'
                       % (len(missing), missing_list)) if missing else "")
    (out / "index.html").write_text(page, encoding="utf-8")

    print("Review page: %s" % (out / "index.html"))
    print("  %d products, %d images, %d thumbnails" % (len(entries), total_shots, counter))
    print("  %d products whose card image is a lifestyle shot" % needs_work)
    print("  %d products with no photos at all" % len(missing))


TEMPLATE = """<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{brand} {series} — photo review</title>
<style>
  :root {{
    --ink:#171A23; --muted:#6B7A71; --line:#E2E8E4; --bg:#F4F7F5;
    --green:#0B3D2E; --warn:#B45309; --warnbg:#FEF3C7;
  }}
  * {{ box-sizing:border-box; }}
  body {{ margin:0; background:var(--bg); color:var(--ink);
    font:15px/1.55 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif; }}
  header.top {{ position:sticky; top:0; z-index:10; background:var(--green); color:#fff;
    padding:16px 24px; box-shadow:0 2px 12px rgba(0,0,0,.15); }}
  header.top h1 {{ margin:0; font-size:19px; font-weight:600; }}
  .stats {{ margin-top:6px; font-size:13px; color:#C9DED5; }}
  .stats b {{ color:#fff; }}
  .filters {{ display:flex; flex-wrap:wrap; gap:8px; padding:14px 24px; background:#fff;
    border-bottom:1px solid var(--line); position:sticky; top:78px; z-index:9; }}
  .filters button {{ font:inherit; font-size:13px; padding:7px 14px; border-radius:18px;
    border:1px solid var(--line); background:#fff; color:var(--ink); cursor:pointer; }}
  .filters button:hover {{ border-color:var(--green); }}
  .filters button.on {{ background:var(--green); color:#fff; border-color:var(--green); }}
  main {{ padding:20px 24px 60px; display:flex; flex-direction:column; gap:14px; }}
  .product {{ background:#fff; border:1px solid var(--line); border-radius:14px; padding:14px 16px; }}
  .product.flagged {{ border-color:#F2C97D; background:#FFFCF5; }}
  .product header {{ display:flex; align-items:baseline; gap:10px; flex-wrap:wrap; margin-bottom:12px; }}
  .num {{ font-size:12px; color:var(--muted); font-variant-numeric:tabular-nums; }}
  .product h3 {{ margin:0; font-size:16px; font-weight:600; }}
  .meta {{ font-size:13px; color:var(--muted); }}
  .warn {{ font-size:12px; color:var(--warn); background:var(--warnbg);
    padding:3px 9px; border-radius:10px; }}
  .strip {{ display:flex; gap:10px; overflow-x:auto; padding-bottom:4px; }}
  .shot {{ margin:0; flex:0 0 auto; width:150px; }}
  .shot img {{ width:150px; height:150px; object-fit:contain; background:#fff;
    border:1px solid var(--line); border-radius:10px; display:block; }}
  .shot.is-main img {{ border-color:var(--green); border-width:2px; }}
  .shot {{ position:relative; }}
  .pick {{ position:absolute; left:50%; top:60px; transform:translateX(-50%);
    font:600 11px/1 inherit; padding:7px 12px; border-radius:14px; border:0;
    background:rgba(11,61,46,.92); color:#fff; cursor:pointer; opacity:0;
    transition:opacity .12s; white-space:nowrap; }}
  .shot:hover .pick {{ opacity:1; }}
  .shot.is-main .pick {{ display:none; }}
  .b.role {{ background:#EEF2EF; color:var(--muted); }}
  .shot.is-main .b.role {{ background:var(--green); color:#fff; }}
  .product.saved {{ border-color:var(--green); background:#F3F9F5; }}
  .status {{ font-size:12px; padding:3px 9px; border-radius:10px; }}
  .status.busy {{ background:#E8EEF9; color:#2A4A8B; }}
  .status.ok {{ background:#E3F2EA; color:#0B3D2E; }}
  .status.err {{ background:#FDE7E7; color:#9B1C1C; }}
  .shot.busy img {{ opacity:.45; }}
  a.fn {{ text-decoration:none; }}
  a.fn:hover {{ text-decoration:underline; }}
  .bar {{ position:fixed; right:20px; bottom:20px; display:flex; gap:10px; align-items:center;
    background:#fff; border:1px solid var(--line); border-radius:30px; padding:10px 14px;
    box-shadow:0 6px 24px rgba(0,0,0,.14); z-index:20; }}
  .bar span {{ font-size:13px; color:var(--muted); }}
  .bar button {{ font:600 13px/1 inherit; padding:9px 16px; border-radius:18px; border:0;
    background:var(--green); color:#fff; cursor:pointer; }}
  .bar button.ghost {{ background:#fff; color:var(--muted); border:1px solid var(--line); }}
  .bar button:disabled {{ opacity:.45; cursor:default; }}
  figcaption {{ margin-top:5px; display:flex; flex-wrap:wrap; gap:4px; align-items:center; }}
  .b {{ font-size:10px; font-weight:600; padding:2px 6px; border-radius:6px;
    background:#EEF2EF; color:var(--muted); }}
  .b.main {{ background:var(--green); color:#fff; }}
  .b.studio {{ background:#E3F2EA; color:#0B3D2E; }}
  .b.live {{ background:#FDECD8; color:#8A5A12; }}
  .fn {{ font-size:10px; color:#9AA6A0; width:100%; overflow:hidden;
    text-overflow:ellipsis; white-space:nowrap; }}
  .missing {{ margin:10px 24px 50px; background:#fff; border:1px solid var(--line);
    border-radius:14px; padding:16px 20px; }}
  .missing h2 {{ margin:0 0 10px; font-size:15px; }}
  .missing ul {{ margin:0; padding-left:18px; columns:2; font-size:13px; }}
  .missing span {{ color:var(--muted); font-size:12px; }}
  @media (max-width:640px) {{ .filters {{ top:88px; }} .missing ul {{ columns:1; }} }}
</style></head><body>
<header class="top">
  <a href="../" style="color:#9FC7B5;text-decoration:none;font-size:12px">&larr; all brands</a>
  <h1>{brand} {series} — photo review</h1>
  <div class="stats"><b>{products}</b> products · <b>{shots}</b> images ·
    <b>{needs_work}</b> need a proper card shot · <b>{missing_n}</b> with no photos</div>
</header>
<div class="filters">
  <button data-filter="all" class="on">All</button>
  <button data-filter="flag:live">Card image is lifestyle ({needs_work})</button>
  <button data-filter="flag:studio">Card image is studio</button>
  {cat_buttons}
</div>
<main id="list" data-brand="{brand}" data-series="{series}">
{cards}
</main>
<div class="bar" id="bar">
  <span id="count">Click "Use as card" on any photo — it uploads straight away</span>
</div>
{missing_block}
<script>
  const buttons = document.querySelectorAll('.filters button');
  const items = document.querySelectorAll('.product');
  const countEl = document.getElementById('count');

  function setStatus(card, text, cls) {{
    let el = card.querySelector('.status');
    if (!el) {{
      el = document.createElement('span');
      el.className = 'status';
      card.querySelector('header').appendChild(el);
    }}
    el.className = 'status ' + cls;
    el.textContent = text;
  }}

  async function useAsCard(card, shot) {{
    const product = card.dataset.name;
    const path = shot.dataset.src;
    shot.classList.add('busy');
    setStatus(card, 'uploading…', 'busy');
    try {{
      const res = await fetch('/api/set-card', {{
        method: 'POST',
        headers: {{'Content-Type': 'application/json'}},
        body: JSON.stringify({{
          product, path,
          brand: document.getElementById('list').dataset.brand,
          series: document.getElementById('list').dataset.series
        }})
      }});
      const data = await res.json();
      if (!res.ok || data.error) throw new Error(data.error || ('HTTP ' + res.status));

      card.querySelectorAll('.shot').forEach(s => {{
        const isMain = s === shot;
        s.classList.toggle('is-main', isMain);
        s.querySelector('.b.role').textContent = isMain ? 'card image' : 'slide';
      }});
      card.classList.add('saved');
      setStatus(card, 'card image updated', 'ok');
      countEl.textContent = 'Updated ' + product + ' — live in Parse';
    }} catch (err) {{
      setStatus(card, 'failed: ' + err.message, 'err');
      countEl.textContent = 'Update failed — is serve_review.py running?';
    }} finally {{
      shot.classList.remove('busy');
    }}
  }}

  items.forEach(card => {{
    card.querySelectorAll('.pick').forEach(btn => {{
      btn.addEventListener('click', () => useAsCard(card, btn.closest('.shot')));
    }});
  }});

  buttons.forEach(b => b.addEventListener('click', () => {{
    buttons.forEach(x => x.classList.toggle('on', x === b));
    const f = b.dataset.filter;
    items.forEach(el => {{
      let show = true;
      if (f.startsWith('cat:')) show = el.dataset.cat === f.slice(4);
      else if (f.startsWith('flag:')) show = el.dataset.flag === f.slice(5);
      el.style.display = show ? '' : 'none';
    }});
  }}));
</script>
</body></html>"""


if __name__ == "__main__":
    main()
