#!/usr/bin/env python3
"""Re-upload catalogue photos that were baked in sideways.

Photos carrying an EXIF orientation flag were uploaded before attach_images.py
applied it, so they show rotated in the apps. This re-prepares each affected
photo from its source file (now upright) and points the product / media row at
the new file. Run with --commit; without it you get a dry run.
"""
import argparse, json, pathlib, sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import attach_images as A


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("plan", help="rotated_live.json produced by the scan")
    ap.add_argument("--root", default="~/Downloads", help="folder the source paths are relative to")
    ap.add_argument("--commit", action="store_true")
    ap.add_argument("--max-dim", type=int, default=1000)
    ap.add_argument("--quality", type=int, default=85)
    args = ap.parse_args()

    root = pathlib.Path(args.root).expanduser()
    items = json.load(open(pathlib.Path(args.plan).expanduser()))
    missing = [i for i in items if not (root / i["src"]).exists()]
    todo = [i for i in items if (root / i["src"]).exists()]
    print("To fix: %d image(s) — %d cover(s), %d slide(s)"
          % (len(todo), sum(1 for i in todo if i["slot"] == "cover"),
             sum(1 for i in todo if i["slot"] != "cover")))
    for i in missing:
        print("  source missing, skipped: %s" % i["src"])
    if not args.commit:
        print("\nDry run. Re-run with --commit to upload.")
        return

    for n, i in enumerate(todo, 1):
        src = root / i["src"]
        uploaded = A.upload_file(src, args.max_dim, args.quality, True)
        f = dict(uploaded, __type="File")
        if i["slot"] == "cover":
            A.call("PUT", "/classes/Products/%s" % i["objectId"], {"Image": f})
        else:
            A.call("PUT", "/classes/Media/%s" % i["slot"].split(":", 1)[1], {"File": f})
        print("[%d/%d] %s · %s (%s)" % (n, len(todo), i["name"], i["cat"], i["slot"].split(":")[0]))

    print("\nDone. %d image(s) re-uploaded upright." % len(todo))


if __name__ == "__main__":
    try:
        main()
    except A.ParseError as exc:
        sys.exit(str(exc))
