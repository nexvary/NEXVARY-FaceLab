# NEXVARY FaceLab — Stage 34500 FIXED

Compiler stabilization release.

Fixes:
- HeritageScreen favorites state now uses explicit `Set<String>` Compose state.
- Removed mutable collection type inference from SharedPreferences state.
- Favorite mutations use copy-on-write and immutable assignment.
- Saved looks mutation normalized to immutable `Set<String>` assignment.
- SharedPreferences writes use `HashSet(...)` copies.

This release intentionally prioritizes Kotlin/Compose compiler stability over new features.
