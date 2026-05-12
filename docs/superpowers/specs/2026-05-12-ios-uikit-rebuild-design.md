# iOS UIKit Rebuild Design

Date: 2026-05-12
Project: The3S iOS rebuild
Source reference: existing Android app in `/Users/ashmitbhui/AndroidStudioProjects/The3S`

## Goal

Rebuild the lost iOS app as a native UIKit application using storyboards/xibs, connected to the same live Parse/Back4App backend used by the Android app.

The rebuild should preserve feature parity with the current Android app while adapting navigation and interaction patterns to standard iOS behavior.

## Scope

In scope:

- Home screen with series toggle and category browsing
- Search screen with product search
- Favorites screen with local persistence
- Videos screen with search and playback
- Category detail with sorting, filtering, and multi-select sharing
- Product detail with media carousel and favorites
- Login/logout flow using Parse user auth
- Catalogue builder and review flow
- PDF catalogue export and system sharing

Out of scope for the first pass:

- Trophyverse brand migration
- Backend schema changes
- New product behavior beyond Android parity
- Offline-first sync beyond local favorites/state

## Product Constraints

- Backend remains the same as the Android app.
- Current backend model should be preserved exactly in the first build.
- `3S` and `Verbose` remain the active brand structure in the first version.
- `Trophies` remains a category under `3S` for now.
- The iOS app should feel native to iOS, not like a direct Android UI port.

## Recommended Approach

Use a UIKit-native rebuild on top of the existing backend.

This keeps the Android app as the functional specification while avoiding a literal port of Android UI patterns. Data model, business rules, and feature coverage are reused conceptually, but navigation, view composition, sharing, and media playback are rebuilt with iOS-first patterns.

## App Architecture

### Root Structure

- `UITabBarController` as the root app shell
- Four primary tabs:
  - `Home`
  - `Search`
  - `Favorites`
  - `Videos`
- Each tab embedded in its own `UINavigationController`

This replaces the Android fragment swapping model with a more idiomatic iOS navigation structure.

### Module Layout

- `App`
  - app delegate / scene delegate
  - dependency bootstrap
- `Models`
  - `Category`
  - `Product`
  - `Video`
  - `MediaItem`
  - `FavoriteProduct`
- `Services`
  - `ParseClientProvider`
  - `AuthService`
  - `CategoryService`
  - `ProductService`
  - `VideoService`
  - `MediaService`
  - `CatalogueService`
  - `PDFService`
- `Persistence`
  - favorites store
  - local app settings store
- `UI`
  - storyboard scenes
  - view controllers
  - collection/table view cells
  - reusable views
- `Presentation`
  - view models or presenter-style screen coordinators

### Design Boundary Rules

- View controllers own view lifecycle and event wiring only.
- Parse queries must live in service types, not inside controllers.
- Filtering, sorting, and selection state should be handled by view models or presenters.
- Local persistence must be accessed through a repository/store layer.

## Screen Design

### Home

Derived from Android `Fragment_Home`.

Behavior:

- Show current series selector for `3S` and `Verbose`
- Show category grid based on current selection
- Expose login/logout entry point
- Expose catalogue flow entry point
- Expose WhatsApp action when logged in

iOS adaptation:

- `UISegmentedControl` for series selection
- `UICollectionView` for category grid
- modal login screen
- native action handling for WhatsApp or sharing entry points

### Search

Derived from Android `Fragment_Search`.

Behavior:

- Load products from Parse for the current series
- Filter locally by name, category, or brand
- Show empty state when no results exist

iOS adaptation:

- `UISearchController`
- grid-based `UICollectionView`
- controller-driven empty state view

### Favorites

Derived from Android `Fragment_Favourite`.

Behavior:

- Persist favorite products locally
- Display a product grid from local persistence
- Show empty state when there are no favorites

iOS adaptation:

- local store-backed collection view
- no Parse dependency for favorite membership itself

### Videos

Derived from Android `Fragment_Videos`.

Behavior:

- Fetch videos from Parse
- Search by name or category
- Show grid of available videos
- Play videos on selection

iOS adaptation:

- `UICollectionView`
- `UISearchController`
- `AVPlayerViewController` for playback

### Category Detail

Derived from Android `CategoryDetail`.

Behavior:

- Fetch products by category and optional brand
- Respect current series filter
- Sort by default, price low-to-high, and high-to-low
- Filter by price range
- Allow multi-select
- Allow share-selected action
- Navigate to product detail

iOS adaptation:

- collection view grid
- native filter/sort controls, likely bar button actions with sheets
- bottom action area or toolbar for multi-select share

### Product Detail

Derived from Android `ProductDetail`.

Behavior:

- Fetch product and related media
- Show product media carousel
- Show/hide price based on login state
- Toggle favorite status
- Support product sharing

iOS adaptation:

- paged media gallery
- `UIPageControl`
- image/video mixed media presentation
- standard iOS share sheet

### Catalogue Builder

Derived from Android `CatalogueBuilder` and review/export flow.

Behavior:

- Load enabled products
- Group by brand and category
- Support product selection
- Review selected products
- Generate PDF catalogue
- Share exported file

iOS adaptation:

- multi-step wizard style flow
- cleaner iOS selection and review screens
- native export/share behavior

## Data Layer Design

### Backend Integration

The iOS app will connect to the same Parse backend used by Android.

Entities to support:

- `Category`
- `Products`
- `Videos`
- related `Media`
- `ParseUser`

The first version should mirror current schema assumptions instead of introducing a translation layer that changes business meaning.

### Parse Service Rules

- All Parse initialization should be centralized.
- Keys/configuration should not be hardcoded in UI resources.
- Object mapping should convert Parse records into typed Swift models.
- Controllers should receive models, not raw Parse objects.

### Local Persistence

Persistence needs:

- favorites
- lightweight settings such as current series

Recommended split:

- `Core Data` for favorites
- `UserDefaults` for current series and small app settings

Reasoning:

- Favorites behave like a small local dataset and need predictable querying.
- `UserDefaults` is sufficient for simple state but not for item collections.

## Error Handling

### Network/Backend Errors

- Show explicit empty/error/retry states for fetch failures.
- Reserve modal alerts for action failures such as login or export.
- Avoid toast-style transient messaging as a primary failure mechanism.

### Login State

- Centralize login state observation.
- Prices and gated actions should refresh when auth state changes.
- Avoid duplicating login state logic independently in every controller.

### Media and Sharing Fallbacks

- Missing images should degrade to placeholders.
- Missing video/media relationships should still allow detail rendering.
- Share/export failures should surface actionable error messages.

## Sharing And PDF Export

### Product Sharing

- Use `UIActivityViewController`
- Preserve current share-selected concept from Android
- Build share payloads from selected products and available media

### PDF Catalogue Export

Preserve Android behavior:

- group selected products by category
- optional price visibility
- include product image, name, detail, and optional price

iOS implementation:

- use native PDF generation APIs
- export to temporary file URL
- share through `UIActivityViewController`

## Testing Strategy

### Unit Tests

- Parse model mapping
- product filtering
- product sorting
- price-range filtering
- favorites persistence behavior
- catalogue grouping rules
- login-gated price visibility rules

### UI Tests

- root tab shell loads correctly
- login flow opens and updates state
- navigation from category list to product detail
- favorite toggle updates favorites screen
- catalogue selection flow reaches review/export entry

### Verification Expectations

- Each implemented feature should be verified against Android behavior, not just against compilation success.
- High-risk flows are auth, favorites persistence, media rendering, and PDF export.

## Migration Notes

### Trophyverse

Do not include Trophyverse migration logic in the first pass.

The first build preserves the current backend shape:

- `3S`
- `Verbose`
- `Trophies` still under `3S`

Any future conversion of `Trophies` into `Trophyverse` should be treated as a separate schema and product migration after parity is restored.

## Delivery Sequence

1. Establish iOS project shell and dependency setup
2. Implement Parse bootstrap and model/service layer
3. Build tab shell and navigation structure
4. Build Home, Search, Favorites, Videos
5. Build Category Detail and Product Detail
6. Build auth flow and gated behaviors
7. Build catalogue selection/review/export
8. Add tests and regression verification

## Implementation Notes

- UIKit with storyboards/xibs is the chosen UI strategy.
- Native iOS conventions take priority over Android UI fidelity when both satisfy the same product behavior.
- The Android project remains the functional reference during rebuild.
