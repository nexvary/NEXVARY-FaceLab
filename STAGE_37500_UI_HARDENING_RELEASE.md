# Stage 37500 — UI Hardening
- Replaced horizontally clipped 8-item bottom bar with 4 primary destinations + More sheet.
- Preserved Back behavior: inner screen -> Editor; Editor -> system exit.
- Kept RTL-aware back arrow and labels.
- Minimum 48/56/64dp touch targets for navigation.
- Extracted navigation into NavigationComponents.kt to continue reducing MainActivity.kt.
- No AI feature additions in this stage; stability and responsive UI only.
