[//]: # (USE THIS FOR PROMPTS )
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


#PREM
