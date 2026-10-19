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
## Part 1: Targets Onboarding & Mifflin-St Jeor (J-5)
1. **Formula & Calculation:**
   - Implement Mifflin-St Jeor BMR in a pure Kotlin file with zero Compose/Android imports.
   - Calculate daily kcal target: BMR multiplied by selected activity factor.
   - Set macro targets: protein ~2g per kg body weight; fat ~25% of kcal; carbs take the remainder.
   - Add a pure JVM unit test verifying against at least two hand-calculated examples.
2. **Onboarding & Configuration UI:**
   - Prompt for height, weight, age, sex, and activity level.
   - Allow user to skip immediately without setting targets.
   - Ensure targets can be re-opened and updated later from the same flow.
3. **UI & Design System Rules:**
   - Use `LocalPalette.current` for colors (no hex literals).
   - Use components from `ui/components/`.
   - Support small screens (down to 360dp width) and scaled fonts.
4. **Data & State Management:**
   - Connect asynchronously without blocking the main thread.
   - Zero Mock Data: Do not use sample rows or hardcoded numbers.
   - Mark `done = true` for ticket J-5 in `project/Roadmap.kt`.

### Constraints & Anti-Patterns:
- Do not modify files outside `feature/fuel/`, `data/`, or `app/src/test/`.
- Ensure no new compiler or linter warnings are generated.



#ADI


#PREM