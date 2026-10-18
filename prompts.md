[//]: # (USE THIS FOR PROMPTS )
#ARNAV

1. **Weekly Grouping:**
    - Group workouts and cardio logs by week starting on Monday using `TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)`.
    - Each week header must display:
        - Date range (e.g., `Oct 12 – Oct 18`)
        - Total session count for that week
        - Total combined volume lifted (kg)
2. **Feed List Items:**
    - **Strength Sessions:** Show title, date, duration, total volume, total completed sets, and colored accent dots for all muscle groups trained in that session.
    - **Cardio Activities:** Render in the same timeline, but distinctly styled (e.g., cardio icon, distance/time stats, unique layout) to clearly distinguish them from weightlifting sessions.
3. **UI & Design System Rules:**
    - Use `LocalPalette.current` for colors (no hex literals or custom hardcoded colors).
    - Use shared components from the design system/kit.
    - Support small screens (down to 360dp width) and scaled system fonts seamlessly.
4. **Data & State Management:**
    - Connect directly to `Repos.workouts` / `DerivedAnalyticsRepository` (or equivalent history repositories).
    - Collect state asynchronously using `collectAsStateWithLifecycle()`.
    - Implement honest **Empty**, **Loading**, and **Error** states.
    - **Zero Mock Data:** Do not use sample rows, stub numbers, or hardcoded fallback lists.

### Constraints & Anti-Patterns:
- Do not block the main thread; handle all repository queries asynchronously.
- Do not modify files outside your designated scope (`feature/history/`, `feature/stats/`, `feature/profile/`, `data/repo/`, `.github/`).
- Ensure no new compiler or linter warnings are generated.


#ZESHAWN




#ADI


#PREM