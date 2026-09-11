# Third-party notices

## Vendored code

### `app/src/main/java/org/osmdroid/mapsforge/`

The classes in this package are a vendored copy of the `osmdroid-mapsforge`
module (osmdroid's MapsForge adapter), adapted for mapsforge 0.30.0.
The osmdroid project was archived in November 2024, so the adapter is
maintained here instead of consumed as a Maven artifact.

- Source: https://github.com/osmdroid/osmdroid, module `osmdroid-mapsforge`,
  tag `osmdroid-parent-6.1.20` (the final release)
- License: Apache License 2.0 — https://www.apache.org/licenses/LICENSE-2.0
- The original file headers note earlier heritage from
  [osmbonuspack](https://github.com/MKergall/osmbonuspack) (LGPL), from which
  the code was originally adapted.

## Map stack dependencies

- `com.github.mapsforge.mapsforge:mapsforge-*:0.30.0` (JitPack build of
  https://github.com/mapsforge/mapsforge, tag `0.30.0`) — LGPL-3.0
- `org.osmdroid:osmdroid-android:6.1.20` — Apache License 2.0
- `com.github.MKergall:osmbonuspack:6.9.0` — Apache License 2.0
