# The 3S — Google Play listing

Package: `in.appoholics.the3s` · Category: Business · Content rating: Everyone · Contains ads: No · In-app purchases: No

## App name (30 chars max)
The 3S — Corporate Gifting

## Short description (80 chars max)
Catalogue for 3S clocks, TrophyVerse awards & Verbose merchandise. Share on WhatsApp.

## Full description (4000 chars max)
The 3S is the catalogue app for India's corporate gifting trade — three brands, one place.

3S — wall clocks, table clocks, photo frames and wrist watches, ready for your branding.
TROPHYVERSE — awards, trophies, plaques and mementos with quick engraving and printing options.
VERBOSE — notebooks, pens, bags, drinkware, caps and apparel for corporate merchandise programmes.

BROWSE
• Every product with full specifications, sizes, weights and pack quantities
• Multi-photo galleries, so you see each item from every angle
• Search across all three brands, sort and filter by price
• Save favourites for quick reordering

SHARE
• Share any product — image and details — straight to WhatsApp or anywhere
• Marketing videos, PDFs and images you can forward to your customers
• Follow our WhatsApp channels for ready-stock updates and new designs

FOR DEALERS
• Sign in to build your own catalogue: pick products across brands and categories, set a customer budget, add your margin, and export a polished PDF in seconds
• One-tap WhatsApp enquiry on every product

Fresh collections land every season — the app updates automatically.

## Graphics (this folder → `out/`)
| Asset | File | Spec |
|---|---|---|
| App icon | `icon-512.png` | 512×512 PNG, full-bleed (Play applies its own mask) |
| Feature graphic | `feature-graphic-1024x500.png` | 1024×500 PNG |
| Phone screenshots (6) | `01-home-3s.png` … `06-whatsapp.png` | 1080×1920 (9:16), framed with captions |

Raw, unframed emulator captures are in `raw/` (1080×2424 — Play rejects these directly because
each side must be ≤ 2× the other; use the framed versions).

Tablet screenshots (7" / 10") are optional unless you opt in to tablet distribution; the app is
phone-first, so I've left them out.

## Other listing fields
- Contact email: (your support address)
- Website: https://the3s.in
- Privacy policy URL: required — host a short page (same text as the iOS one).
- App access: "All functionality is available without special access" is NOT true — dealer
  features need a login. Choose "All or some functionality is restricted" and supply a demo
  dealer account (create e.g. `playstore-review` in the portal → Users before submitting).
- Data safety: no data collected for tracking; dealer login = username only (authentication);
  push token stored to deliver notifications; no third-party analytics/ads SDKs.
- Ads: none.
