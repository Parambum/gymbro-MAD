[//]: # (USE THIS FOR PROMPTS )
#PREM
# Project: GymBro Android App — Complete Website-to-Android Replication

## Reference Links

**Original GitHub Repository:**  
https://github.com/Parambum/gymbro-MAD.git

**Live Reference Website:**  
https://gymbro-dun.vercel.app/

## Objective

I want to recreate the GymBro platform as closely as possible in a native Android application using **Kotlin and Jetpack Compose**.

Use the existing `gymbro-MAD` GitHub repository as the starting point. Thoroughly inspect the source code and the live website, understand the existing implementation, and modify the project to reproduce the reference platform's functionality and design.

The goal is to achieve a complete, functional Android application, not just a visual mockup.

## 1. Analyze the Existing Project

Before making any changes:

- Clone the existing GitHub repository.
- Inspect the entire project structure, source code, dependencies, and configuration.
- Identify the existing screens, navigation, data models, and implemented features.
- Determine which features are complete, partially implemented, or missing.
- Understand the current architecture and preserve working functionality wherever possible.

Do not replace the entire project blindly. Build upon the existing implementation.

## 2. Analyze the Reference Website A–Z

Thoroughly inspect https://gymbro-dun.vercel.app/.

Explore every accessible page, screen, section, navigation element, button, interaction, and relevant user flow.

Pay particular attention to:

- Workout history
- Fitness statistics and analytics
- Workout details and exercise information
- Navigation and overall platform structure
- Charts, graphs, and progress indicators
- Workout records and historical data
- Filters, date selectors, and interactive controls
- Loading, empty, and error states
- Visual styling, spacing, typography, icons, and colors

Test the website's interactive elements wherever possible rather than relying exclusively on screenshots.

Create an inventory of the website's features and compare it against the existing Android application.

## 3. Recreate the History Feature

The workout history functionality is a major priority.

Replicate the reference website's history experience as closely as possible, including all applicable features:

- Workout history listing
- Workout dates and timestamps
- Workout duration
- Exercises performed during each session
- Sets, repetitions, and weights
- Individual workout details
- Historical workout records
- Date-based filtering and navigation
- Workout summaries
- Personal records, where applicable
- Empty states and other relevant interactions

Users should be able to open a previous workout and inspect its details.

Preserve the actual historical workout data available in the application. Do not populate the interface with fabricated statistics merely to make it look complete.

## 4. Recreate the Statistics Feature

Reproduce the website's statistics and analytics experience.

Inspect the reference to determine which metrics are available and implement the corresponding features, including applicable elements such as:

- Total workouts
- Workout frequency and consistency
- Total training volume
- Weight and repetition progression
- Exercise-specific progress
- Personal records
- Progress charts and graphs
- Date-range selection
- Historical comparisons
- Workout duration statistics
- Weekly and monthly summaries

Only include metrics that are supported by the reference platform or existing application data.

Statistics must be calculated from actual workout records and update when new workouts are added or existing records change.

Charts must display meaningful data, use appropriate labels, and handle missing data gracefully.

## 5. Reproduce the Platform's UI and UX

The Android application should closely match the reference website's visual design and user experience.

Reproduce the following wherever applicable:

- Overall layout and screen structure
- Color palette and backgrounds
- Typography and font hierarchy
- Cards, containers, and borders
- Icons and navigation elements
- Charts and progress indicators
- Spacing, margins, and alignment
- Buttons, tabs, and interactive components
- Scrolling behavior
- Screen transitions
- Loading, empty, and error states

Use native Android components implemented with Jetpack Compose while maintaining visual consistency with the website.

The result should feel like a polished, production-quality Android application rather than a basic website displayed inside a WebView.

## 6. Technology Requirements

Use the following technology stack:

- **Language:** Kotlin
- **UI framework:** Jetpack Compose
- **Design system:** Material 3 where appropriate
- **Architecture:** Follow the existing project architecture where practical; otherwise, use a maintainable architecture with clear separation of concerns.
- **Navigation:** Implement proper navigation between screens.
- **State management:** Use appropriate Compose state management and ViewModels.
- **Data persistence:** Preserve and extend the existing persistence solution.
- **Charts:** Use a suitable Android-compatible charting solution to reproduce the reference graphs.
- **Asynchronous operations:** Use Kotlin coroutines and Flow where appropriate.

Avoid unnecessary dependencies, duplicated business logic, and large, unmaintainable composable functions.

## 7. Data and Functionality

All screens must be functional.

Ensure that:

- History is derived from actual workout records.
- Statistics are calculated from the underlying data.
- Selecting a historical workout opens the correct details.
- Filters and date selectors work correctly.
- Navigation works consistently across screens.
- Data remains available after restarting the application, using the existing persistence mechanism.
- Existing functionality is not accidentally broken.
- Empty datasets do not cause crashes.
- UI state updates correctly when workout data changes.

If the existing project uses Firebase or another backend, inspect its implementation before changing the data layer.

Do not introduce a new backend or replace an existing database without a clear technical reason.

## 8. Development Workflow

Follow this sequence:

1. Clone the GitHub repository.
2. Inspect the project and identify the relevant modules and files.
3. Explore the reference website and document its functionality.
4. Compare the reference with the existing application.
5. Create an implementation plan based on the identified gaps.
6. Implement the missing and incomplete features.
7. Reproduce the History and Statistics screens.
8. Implement the remaining relevant platform functionality.
9. Run Gradle builds and resolve compilation errors.
10. Test navigation, data persistence, calculations, and UI behavior.
11. Verify the application on an Android emulator or device.
12. Review the completed implementation against the reference website.
13. Commit the changes and push them to the original GitHub repository.

Work incrementally and verify each major feature before moving to the next.

## 9. Testing and Quality Assurance

Before considering the project complete:

- Verify that the project compiles successfully.
- Resolve Kotlin, Gradle, and Jetpack Compose errors.
- Test all major navigation flows.
- Verify workout history and workout detail screens.
- Check statistics calculations against the underlying records.
- Test date filters and chart updates.
- Test empty and populated datasets.
- Verify persistence across application restarts.
- Check layouts on different screen sizes.
- Remove temporary placeholders and unnecessary debugging code.
- Ensure no existing functionality has been unintentionally removed.

Do not claim that a feature works unless it has been implemented and appropriately tested.

## 10. GitHub Requirements

Use the existing repository:

https://github.com/Parambum/gymbro-MAD.git

All changes must be made in this repository.

- Preserve the existing Git history.
- Work on an appropriate feature branch unless the repository's workflow requires otherwise.
- Commit meaningful, logically grouped changes.
- Do not commit credentials, API keys, secrets, or local machine configuration.
- Push the completed changes to the original repository.
- Do not force-push or overwrite unrelated work.

If GitHub authentication or repository permissions prevent pushing, complete the implementation locally and explain exactly what is required to finish the push.

## 11. Final Deliverables

At completion, provide:

1. The updated Android Studio project.
2. A working Kotlin and Jetpack Compose implementation.
3. The recreated History functionality.
4. The recreated Statistics and analytics functionality.
5. The other relevant replicated platform features.
6. A successful build, with any remaining limitations clearly documented.
7. A summary of the files changed and features implemented.
8. Testing results and any outstanding issues.
9. The GitHub branch and commit details.
10. An installable APK if the build environment supports generating one.

## Final Acceptance Criteria

The project is complete only when:

- The existing repository has been inspected and modified.
- The reference website has been analyzed comprehensively.
- History and Statistics reproduce the reference's functionality and visual design as closely as reasonably possible.
- The implemented features work with real application data.
- The Android application builds successfully.
- The major user flows have been tested.
- The changes have been pushed to the original GitHub repository, or any access-related blocker has been explicitly identified.

**Important:** Prioritize accurate replication, functional completeness, maintainable Kotlin code, and a polished native Android experience. Do not stop after creating the UI. Inspect, implement, test, and verify the complete experience.



#ARNAV

## Training Log and Session Completion
- Build the exercise picker and set logger with search, muscle-group filters, editable weight/reps/RPE, completion state, and validation.
- Ensure active workout state survives screen changes, completed sets are persisted, and finishing a session saves it and opens a useful summary.
- Include duration, volume, completed-set and muscle-group totals, personal bests, previous-session comparison, and appropriately labeled Epley estimates.
- Add a rest timer that starts when a set is completed, remains accurate from timestamps, and can notify when rest ends.

## Exercise Demonstrations
- Find and use the website's paired exercise-demo images, map them to catalog exercises, and display the frames in the exercise logging flow so the selected movement is visible while logging.

## Cardio and GPS Routes
- Support manual cardio entries and history, then add opt-in GPS activity tracking with runtime location permission, background recording, saved route points, and a Leaflet route map.
- Keep manual logging available when location permission or GPS is unavailable. GPX file import was not part of the implemented request.

## Build and CI Reliability
- Resolve Android Gradle/Kotlin source-set and GitHub Actions Android SDK setup failures, then make the workflow invoke the Gradle wrapper through Bash so CI does not depend on the wrapper's executable bit.
- Correct unit-test expectations to match zero-volume normalization and the existing Epley formula after CI reported failures.

## History and Dashboard
- Organize workout and cardio activity into Monday-based weekly groups with date ranges, session counts, and volume totals; show useful workout metrics and visually distinct cardio entries.
- Connect the timeline and dashboard to repository data, with honest empty/error states, responsive layouts, and the shared app styling.


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

## J-2: Food Search and the Repository
1. Repository & Network:
   - Add Retrofit or Ktor dependencies cleanly in gradle/libs.versions.toml.
   - Implement production FoodRepository pointing to /api/fuel/foods with base URL in a single constant.
   - Swap the implementation into Repos.food.
2. Search Screen:
   - Implement ~300ms debounce on search input field.
   - Display results with name, brand, and kcal per 100g using standard kit components.
   - Implement explicit error states: a failed network lookup must state the error clearly and never appear as an empty cupboard.
   - Display an honest no-results state specifying the searched term.
3. UI & Design System Rules:
   - Use LocalPalette.current for colors.
   - Collect flow states via collectAsStateWithLifecycle().
   - Support 360dp width and system font scaling.
4. Constraints & Anti-Patterns:
   - Confine changes to data/repo/, feature/fuel/FoodSearchScreen.kt, and gradle/libs.versions.toml.
   - Mark done = true for ticket J-2 in project/Roadmap.kt.

## J-1: Fuel Day Screen & Dashboard
1. **Day Navigation & Macro Overview:**
   - Day switcher at the top to move between days (yesterday, today, past dates) and tap to return to today.
   - Intake vs Target card showing total consumed calories vs Mifflin-St Jeor target.
   - Individual macro stat tiles for Protein, Carbs, and Fat aligned cleanly on a single baseline (intake / target g) without wrapping numbers across lines.
   - Visual progress meter bars for Protein (accent), Carbs (info), and Fat (warn).
2. **Meal Sections:**
   - Four sections: Breakfast, Lunch, Dinner, and Snack.
   - Each section shows its subtotal calories.
   - Honest empty state per section when no food is logged.
   - Swipe-to-dismiss gesture or trash icon to delete logged food items.
3. **Design & State:**
   - Colors from `LocalPalette.current`.
   - Reactive collection with `collectAsStateWithLifecycle()`.
   - Mark `done = true` for ticket J-1 in `project/Roadmap.kt`.

## J-4: Portion Sheet, Unit Scaling & Meal Routing
1. **Live Macro Calculation:**
   - Bottom sheet for adding or editing an entry.
   - Grams / volume input with live macro and calorie scaling: `foodItem.per100g.scaled(grams / 100.0)`.
   - Quick portion chips: 50g, 100g, 150g (1 katori), 200g (1 cup/bowl), 250g.
2. **Target Meal Routing:**
   - When tapping "+ Add" on a specific meal section (e.g. Breakfast), pre-select that meal rather than defaulting strictly to the current time of day.
   - If opened generally, infer default meal from current device time.
3. **Edit Support & Immutable Log:**
   - Tapping an existing log row reuses the same portion sheet pre-populated with its current grams and meal.
   - Macros stored directly on the entry so editing food databases later does not silently change past historical logs.
4. **Design & State:**
   - Mark `done = true` for ticket J-4 in `project/Roadmap.kt`.

## J-6: Water and Body-Weight Quick Log
1. **Water Quick Log Tile:**
   - Display total water logged today in ml with water droplet icon.
   - Two quick-tap increment buttons: "+250 ml" and "+500 ml".
   - Persist water intake asynchronously per date in `FuelRepository`.
2. **Body Weight Tile & Sheet:**
   - Display today's weigh-in in kg (or "--" if none logged).
   - Tap opens `WeightLoggingSheet` to record or update today's weight.
   - Enforce one weigh-in per day (logging again replaces today's earlier entry).
3. **Tests & Roadmap:**
   - Pure JVM unit test verifying water accumulation and weight updates.
   - Mark `done = true` for ticket J-6 in `project/Roadmap.kt`.

## J-3: Barcode Scanner (CameraX + Google ML Kit)
1. **CameraX & ML Kit Integration:**
   - Add CameraX (`1.4.1`) and Google ML Kit Barcode Scanning (`17.3.0`) in `libs.versions.toml` and `app/build.gradle.kts`.
   - Add `<uses-permission android:name="android.permission.CAMERA"/>` to `AndroidManifest.xml`.
   - Scan button in search screen opening camera preview with scanner reticle overlay and flashlight toggle.
   - Reads EAN-13, UPC-A, EAN-8, UPC-E, and QR codes.
2. **Permission Flow & Fallbacks:**
   - Request `CAMERA` permission only when user taps scan, never on screen launch.
   - Denying permission leaves manual text search fully functional with an explanatory banner.
   - Scanned barcode queries `foodRepository.byBarcode(code)`.
   - If barcode is unknown, display dialog offering to add the food by hand (`ManualFoodEntrySheet`) instead of dead-ending.
   - Release and unbind camera cleanly when leaving composition.
3. **Tests & Roadmap:**
   - JVM unit test in `BarcodeScannerTest.kt` verifying barcode lookup and manual fallback.
   - Mark `done = true` for ticket J-3 in `project/Roadmap.kt`.

#ADI

## The Base (B-1 … B-8)
Build a native Kotlin + Jetpack Compose app, not a WebView wrapper around the website. Build only the part everything else depends on and nobody can build in parallel — theme, component kit, navigation, data layer, 3D body map, Deck — so Prem, John and Arnav can each own a folder without colliding. Port the palettes and the exercise catalog from the website rather than reinventing them, so the phone and the site agree on muscle-group slugs and on what a colour means.

## B-1: Gradle Build, JDK 17 Toolchain, Wrapper
1. **Build Setup:**
   - Gradle wrapper with a version catalog (`gradle/libs.versions.toml`); compileSdk/targetSdk 37, minSdk 26.
   - Compile with a JDK 17 toolchain (foojay resolver) so it builds the same whatever JDK launched Gradle.
   - Keep the heap small in `gradle.properties` (`-Xmx1536m`) so it builds on an 8 GB laptop with the emulator closed.
2. **Onboarding:**
   - `local.properties.example` for the SDK path; never commit `local.properties`.
   - README "Run it" section: `installDebug`, `testDebugUnitTest`, `assembleDebug`.

## B-2: Theme — Three Palettes, Type Scale, Shapes
1. Port the website's three palettes (Molten, Court, Classic) as 13 colour tokens, animated when the theme switches.
2. Expose them through `LocalPalette.current`; type scale and shapes in `ui/theme/`.
3. House rule: colours come from the palette, never a hex literal, so everything retones with the theme.

## B-3: Component Kit
1. Build `GymBroCard`, `StatTile`, `GymBroChip`, `AccentButton`, `GhostButton`, `EmptyState`, `MeterBar`, `SectionHeader`, `MicroLabel` in `ui/components/`.
2. Add `UnbuiltScreen`: every unfinished tab shows a brief (owner, tickets, what the screen should do, what is already wired) instead of a blank placeholder.

## B-4: Navigation Shell
1. Six bottom tabs (Deck, Train, Fuel, Body, Stats, History) plus inner routes (Cardio, Profile, Exercise Picker, Food Search, Session Detail) in one nav graph in `GymBroApp.kt`.
2. Back stack should behave like a tab bar: re-selecting a tab restores its state, back from a tab root returns to Deck.
3. Each tab carries its owner so the app knows who is building what.

## B-5: Models, Repository Interfaces, In-Memory Store, Derived Analytics
1. **Models:** every stored shape in `data/model/`, free of Compose and Android imports.
2. **Repositories:** interfaces for workouts, cardio, fuel, food and analytics in `data/repo/Repositories.kt`; screens talk to these and nothing else.
3. **Implementations:** in-memory for now, swappable for Room (ticket A-5) behind the same interfaces, so the seam never moves while four people build against it.
4. **Analytics are derived, never stored:** muscle volume, weekly volume and personal bests are computed from logged sessions, so the charts, History and the body map cannot disagree.
5. Zero seeded data anywhere, not even for the demo.

## B-6: Exercise Catalog
1. Generate `data/catalog/ExerciseCatalog.kt` from the website's `src/lib/data/exercise-catalog.ts`: 11 muscle groups, 191 movements.
2. Key exercises by group-prefixed ids (e.g. `chest-flat-barbell-bench-press`) because some movements, like Close-Grip Bench Press, are filed under two groups.
3. Unit test catalog integrity: ids unique and resolvable, every group has movements, search narrows them, and every muscle group has a zone on the 3D figure.

## B-7: 3D Anatomy — Mesh Builders, Software Renderer, Body Screen
1. **No 3D engine, no model files:** Filament would add megabytes of native libraries and a `.glb` pipeline for a figure made of ~30 primitives.
2. `anatomy/Geometry.kt`: `Vec3`, `Mesh`, and builders for box, sphere, cylinder, capsule, icosphere.
3. `anatomy/AnatomyRig.kt`: the body ported part for part from the website's `anatomy-config.ts`, with 11 tappable muscle groups and a camera angle per group.
4. `anatomy/Renderer.kt`: project → cull → shade → depth-sort → fill about 1,800 triangles a frame, plus picking so a tap hits the front-most muscle (the torso blocks taps reaching muscles behind it).
5. `feature/body/`: Body screen with drag to orbit, pinch to zoom, tap a muscle; tint muscles by trained volume.
6. Unit test the maths that fails silently: mesh winding, camera projection and hit testing. Document it in `docs/3D-MODEL.md`.

## B-8: Deck Wired to the Repositories
1. Home screen reading real data from the repositories, with honest empty states.
2. A "build status" card driven by `project/Roadmap.kt`, so the app always shows how far along the project is.
3. Make Deck the worked example teammates copy: repository data, kit components, palette colours.

## Team Process and Docs
1. `project/Roadmap.kt`: every ticket in code with owner, hours, done flag and route; `RoadmapTest` keeps the split even (about 60% of hours for the three of them, shares within a few hours of each other).
2. `TASKS.md` with acceptance criteria per ticket and a shared definition of done (real data, empty/error states, palette colours, 360dp width, nothing on the main thread, tests for arithmetic, no new warnings).
3. `CONTRIBUTING.md`: branch naming and "own your folder" (one file per screen, one folder per area) to keep merge conflicts away.
4. README house rules: no invented data; colours come from the palette.

## Website API for the Android App (gymbro repo)
1. Let `/api/fuel/foods/search` and `/api/fuel/barcode` answer without a NextAuth session, so the Android app's food search (J-2) and barcode scanner (J-3) work.
2. Signed out, search covers the global catalog only and skips the history/favourites boosts; signed in, it behaves as before.
3. `FUEL_ENABLED="false"` still turns both off; every other `/api/fuel` route stays behind `fuelGuard()`.

## Integration: Merging Prem's History and Stats into Main
1. Merge `prem/history-stats-platform` into `main` alongside John's completed Fuel track.
2. Resolve the `RoadmapTest` conflict as the union of both sides (J-1 … J-6 and A-1 … A-3 done).
3. Keep `main` as a single straight-line history, delete the merged branch, and update the README so Fuel is listed as built.
4. Verify: the app compiles and all unit tests pass; Stats reads body weight from Fuel's weigh-in log.


#PREM
