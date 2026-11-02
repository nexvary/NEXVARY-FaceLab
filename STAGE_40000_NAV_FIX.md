# Stage 40000 — Bottom Navigation Runtime Fix

Root cause: ElectricNavCell used fillMaxSize inside a bottom bar child whose height was not explicitly bounded, allowing each navigation cell to expand vertically at runtime.

Fix:
- Outer nav cell: fixed 78.dp height.
- Inner surface: fixed 74.dp height + fillMaxWidth only.
- Content column: fixed 70.dp height + fillMaxWidth only.
- Removed fillMaxSize from navigation cells.

This preserves the 5-item responsive bottom bar and prevents full-screen vertical stretching.
