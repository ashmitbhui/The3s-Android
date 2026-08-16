# The 3S — Google Play listing

Package: `in.appoholics.the3s` · Category: Business · Content rating: Everyone · Contains ads: No · In-app purchases: No

## App name (30 chars max)
The 3S

(Keep the name plain — Play flags names padded with keywords. "Corporate Gifting" goes in the short description.)

## Short description (80 chars max)
Product catalogue for The 3S: clocks, trophies & merchandise. Browse & share.

## Full description (4000 chars max)
The 3S is the official product catalogue app of The 3S, a manufacturer of corporate gifts in India. It shows the current season's range across our three brands, with photos and specifications, and lets you share products directly with your customers.

WHAT YOU CAN DO WITHOUT AN ACCOUNT
• Browse the full range by brand and category:
  – 3S: wall clocks, table clocks, photo frames, wrist watches
  – TrophyVerse: trophies, awards, plaques and mementos
  – Verbose: notebooks, pens, bags, drinkware, caps, T-shirts, shirts, jackets, gift combos
• Open any product to see its photo gallery and full specifications (size, dial size, weight, material, printing area, warranty)
• Search products by name across all brands
• Mark products as favourites and find them again on the Favourites tab
• Share a product (photo + specifications) to WhatsApp or any other app; select several products and share them together
• Send a WhatsApp enquiry about a product to our sales team in one tap
• Watch and forward marketing videos, product PDFs and images from the Marketing tab
• Follow our WhatsApp channels for ready-stock updates and new designs

WHAT YOU CAN DO AS A REGISTERED DEALER
Dealers who buy from The 3S get a login from our sales team (there is no sign-up inside the app). After signing in you can additionally:
• See dealer prices on every product
• Sort and filter categories by price
• Build your own customer catalogue: choose products across brands and categories, set a customer budget, add your margin (percentage or fixed amount) or hide prices, and export it as a PDF to share
• Choose, when sharing a product, whether the price is included, marked up with your margin, or left out

OTHER DETAILS
• The catalogue updates automatically when a new season is published — no app update needed
• Notifications are used only to announce new collections and offers
• The app is in English and prices are shown in Indian Rupees (₹)
• To request a dealer account, contact us through the WhatsApp enquiry option in the app

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
