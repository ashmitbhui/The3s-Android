# The 3S — season data import

## Files
- `products_template.csv` — one row per product. `image_file` is a filename inside `images/`.
- `categories_template.csv` — one row per category. `icon_file` is a filename inside `icons/`.

## Layout
    tools/import/
      products.csv
      categories.csv
      images/    <- product photos named exactly as image_file
      icons/     <- category icons named exactly as icon_file

## Field notes (matches the live Parse schema)
- `Price`, `Order`, `MOQ`, `MasterPacking` — numbers (no currency symbol, no commas)
- `isNew` — True/False. `isEnabled` is set to True by the script.
- `Detail` — multi-line is fine; keep it quoted in the CSV. Shown on the product page and cards.
- `Series` is NOT a column: the script sets it from `--series` for every row.
- `Brand` must be exactly `3S`, `Trophyverse`, or `Verbose`.
- `Category` must match a Category row's `Name` for that brand.

## Credentials
Read from `~/.the3s_upload.env` (never committed):
    PARSE_SERVER_URL=https://parseapi.back4app.com
    PARSE_APP_ID=...
    PARSE_MASTER_KEY=...
