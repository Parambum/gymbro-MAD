# Tasks

Everything left to build, who owns it, and what "done" means.

The same list lives in code at `app/src/main/java/com/gymbro/mad/project/Roadmap.kt`.
That is what the unbuilt tabs and the Deck's build-status card read, so **when
you finish a ticket, flip `done = true` there in the same commit** and update
this file if the scope changed. A unit test checks the two stay plausible.

| Owner | Area | Tickets | Hours | Share |
| --- | --- | --- | --- | --- |
| Aditya | Base, 3D body map, Deck | B-1 … B-8 | 40 | 39% — **done** |
| **John** | Fuel | J-1 … J-6 | **20** | 20% |
| **Arnav** | Train, Cardio, History, Stats, platform | P-1 … P-7, A-1 … A-8 | **42** | 41% |

The three of you own **62 of 102 hours — 61% of the app**. Hours are a rough
guide to keep the split even, not a deadline.

---

## Before you start

1. Read [CONTRIBUTING.md](CONTRIBUTING.md). Branch naming and the "own your
   folder" rule are what keep four people out of each other's way.
2. Open the app and tap your tab. The brief is on screen: what the screen does,
   your tickets, and what is already wired for you.
3. Read `feature/deck/DeckScreen.kt`. It is the worked example — real data from
   a repository, honest empty states, components from the kit. Copy its shape.
4. Read the two house rules in the README. The no-invented-data one is the rule
   most likely to cost you a rewrite if you ignore it.

## Definition of done — every ticket

- [ ] The screen reads real data from a repository. No sample rows, no stub
      numbers, no "demo mode".
- [ ] Empty, loading and error states all exist and say something useful. Empty
      is the state users see first.
- [ ] Colours come from `LocalPalette.current`; spacing and components from the
      kit. No hex literals, no bespoke cards.
- [ ] It works at 360dp wide (a small phone) and with the system font scaled up.
- [ ] Nothing blocks the main thread. Repository calls are `suspend`; collect
      with `collectAsStateWithLifecycle()`.
- [ ] `./gradlew testDebugUnitTest` passes, and anything with arithmetic in it
      has a test of its own.
- [ ] `./gradlew assembleDebug` produces no new warnings.
- [ ] `done = true` in `Roadmap.kt`, and the `UnbuiltScreen` call is deleted.

---

# Arnav — Train and Cardio (21h)

The logging engine. Everything else in the app displays what this produces, so
you are first in the dependency chain: the sooner P-3 lands, the sooner Arnav's
charts and body map have anything to show.

Your files: `feature/train/`, `feature/cardio/`.

### P-1 — Exercise picker (3h)
`feature/train/ExercisePickerScreen.kt`

- [ ] Search field filtering as the user types, over all 191 movements.
- [ ] A row of the 11 muscle-group chips; tapping one filters, tapping it again
      clears.
- [ ] Each result shows the name and a dot in its group accent colour.
- [ ] Tapping a result calls `onPicked(exerciseId)` and pops back.
- [ ] A no-match state naming the search term.

`ExerciseCatalog.search(query, groupSlug)` already does the filtering. Use
`LazyColumn` — 191 rows in a `Column` inside a scroll container will drop
frames, and that is the most common Compose mistake there is.

### P-2 — Set logger rows (4h)
`feature/train/` — a `SetRow` composable of your own

- [ ] Weight, reps and optional RPE per row, each editable.
- [ ] A tick that marks the set complete. Only completed sets count anywhere.
- [ ] Numeric keyboard; decimals allowed for weight, integers only for reps.
- [ ] Rejecting a bad entry rather than storing it — no negative weights, no
      30-rep typo swallowed silently.
- [ ] Add-set copies the weight and reps of the previous set, which is what a
      lifter nearly always wants.
- [ ] Delete a set, with the row sliding out rather than vanishing.

Keep the edit state local to the row and write through `putSet` on change.
`putSet` replaces a set with the same id, so editing is the same call as adding.

### P-3 — Active session (5h) — *the keystone ticket*
`feature/train/TrainScreen.kt` and a `TrainViewModel`

- [ ] No session: a Start button plus the last session as a card.
- [ ] Running: title, elapsed time, running volume, the exercise list.
- [ ] Add exercise opens P-1 and appends the result.
- [ ] Finish calls `finishSession` and goes to the summary (P-5).
- [ ] Discard asks first, then calls `discardSession`.
- [ ] Rotating the phone or backgrounding the app does not lose the session.
- [ ] Elapsed time keeps counting while the screen is open.

`Repos.workouts.activeSession` is a `StateFlow<WorkoutSession?>` — collect it,
do not keep a second copy in the ViewModel. `finishSession` already discards a
session with nothing completed in it; do not add your own check for that.

### P-4 — Rest timer (3h)
`feature/train/`

- [ ] Starts when a set is marked complete, with a default (90s) the user can
      change per exercise.
- [ ] Counts down visibly on the Train screen; `MeterBar` is there for it.
- [ ] Posts a notification when the rest is up if the app is backgrounded.
- [ ] Declares `POST_NOTIFICATIONS` in the manifest **in this commit**, and
      asks for it at the moment the first timer starts — not on app launch.
- [ ] Still correct if the user leaves the app for five minutes: compute from a
      timestamp, never by counting ticks.
- [ ] Refusing the permission leaves the on-screen timer working.

Create the notification channel in `GymBroApplication` — that is what it is
there for.

### P-5 — Finish-session summary (2h)
`feature/train/`

- [ ] Volume, completed sets, duration, and volume per muscle group.
- [ ] Any personal best set in this session, named.
- [ ] A line about how it compares with the previous session of the same title,
      or nothing if there is no previous one — not "+0%".
- [ ] Done returns to the Deck, which should already show the session.

### P-6 — Cardio logging (3h)
`feature/cardio/CardioScreen.kt`

- [ ] A form: kind (`CardioKind`), duration, optional distance, optional
      average heart rate.
- [ ] The list of past activities below, newest first, pace shown where there
      is a distance and a dash where there is not.
- [ ] Long-press to delete via `CardioRepository.delete`.
- [ ] Empty state for a fresh install.

No live GPS tracking. It needs a foreground service and background location
permission, which is a project of its own — raise a new ticket if you want it.

### P-7 — Estimated 1RM (1h)
`feature/train/`, plus a test

- [ ] Shown per exercise in the session and in the summary.
- [ ] Uses `PersonalBest.estimated1RmKg` (Epley) rather than a second formula.
- [ ] Marked as an estimate, and suppressed above ~10 reps where Epley stops
      being honest.
- [ ] A unit test with three known values.

---

# John — Fuel (20h)

The nutrition half. It is the most self-contained area in the app: nothing else
depends on it, so you can build it in any order you like.

Your files: `feature/fuel/`.

### J-1 — Fuel day screen (5h)
`feature/fuel/FuelScreen.kt`

- [ ] Day header: kcal against target, and three `MeterBar`s for protein, carbs
      and fat.
- [ ] Four meal sections — breakfast, lunch, dinner, snack — each listing its
      entries with portion and kcal, each with an add button.
- [ ] A day switcher defaulting to today, with yesterday one tap away. People
      log dinner the next morning.
- [ ] Tapping an entry opens it for editing (J-4); swipe or long-press deletes.
- [ ] With no targets set, show intake alone and say there is no target yet.
      **Never invent a 2,000 kcal default and present it as the user's.**
- [ ] Empty state per meal, not one for the whole day.

`Repos.fuel.entriesFor(day)` is a `Flow<List<MealEntry>>`. A day total is
`entries.fold(Macros.ZERO) { acc, e -> acc + e.macros }`. `Macros.ZERO` as the
target means "not set".

### J-2 — Food search and the repository (4h)
`feature/fuel/FoodSearchScreen.kt` + a real `FoodRepository`

- [ ] Search field with a ~300ms debounce, so typing does not fire a request
      per keystroke.
- [ ] Results: name, brand, kcal per 100g.
- [ ] A real `FoodRepository` implementation, swapped into `Repos.food`.
- [ ] Network failure says the lookup failed. An error must not look like an
      empty cupboard.
- [ ] No-results state naming the term searched for.

The website already serves ~13k Indian products with barcodes behind
`/api/fuel/foods`; point the repository at it, with the base URL in one
constant. Add Retrofit or Ktor in this commit — neither is in the build yet, so
the dependency arrives with its first use.

### J-3 — Barcode scanner (4h)
`feature/fuel/`

- [ ] A scan button on the search screen opening a camera preview.
- [ ] Reads EAN-13 and UPC-A, then looks the code up with `byBarcode`.
- [ ] Asks for `CAMERA` when the user taps scan, never on screen open.
- [ ] Refusing the permission leaves search working, with one line explaining
      what scanning would need.
- [ ] An unknown barcode offers to add the food by hand instead of dead-ending.
- [ ] Releases the camera when the screen leaves composition.

CameraX + ML Kit barcode scanning. Both go in `gradle/libs.versions.toml` in
this commit.

### J-4 — Add and edit an entry (3h)
`feature/fuel/`

- [ ] Portion in grams or millilitres, with the macros updating live.
- [ ] Common portions where the food has them (one egg, one slice, one cup).
- [ ] Meal selector, defaulting to the one implied by the time of day.
- [ ] Editing an existing entry reuses the same sheet.
- [ ] Macros stored on the entry, so changing a food later never silently
      rewrites a logged day.

`foodItem.per100g.scaled(grams / 100.0)` is the whole calculation.

### J-5 — Targets onboarding (2h)
`feature/fuel/`, plus a test

- [ ] Asks height, weight, age, sex and activity level on first open of Fuel.
- [ ] Mifflin-St Jeor for BMR, times an activity factor, for the kcal target.
- [ ] Protein target at ~2g per kg of body weight; fat at ~25% of kcal; carbs
      take the remainder.
- [ ] Targets editable afterwards from the same place.
- [ ] A unit test with two worked examples, checked against the formula by
      hand.
- [ ] Skippable. Someone who just wants to log a banana should be able to.

Put the maths in its own file with no Compose imports, so the test is plain
JVM — `data/` is the right place.

### J-6 — Water and body weight (2h)
`feature/fuel/`

- [ ] A water tile with +250ml and +500ml, showing today's total.
- [ ] A weigh-in tile taking one weight per day (a second replaces the first —
      `logWeight` already behaves that way).
- [ ] Both visible on the Fuel day screen without scrolling to the bottom.
- [ ] The weight list feeds Arnav's trend chart (A-3), so store in kilograms.

---

# Arnav — History, Stats and platform (21h)

The half of the app that makes the logging worth doing, plus the two platform
tickets nobody else can do while they are building screens.

Your files: `feature/history/`, `feature/stats/`, `feature/profile/`,
`data/repo/` (for A-5), `.github/` (for A-8).

### A-1 — History and session detail (4h)
`feature/history/HistoryScreen.kt`, `SessionDetailScreen.kt`

- [x] Sessions grouped by week, newest first, with a week header carrying the
      date range, session count and total volume.
- [x] Each row: title, date, duration, volume, sets, and a dot per muscle group
      trained in its accent colour.
- [x] Tapping a row opens the detail route.
- [x] Detail: header stats, then each exercise with its sets; incomplete sets
      shown as skipped rather than hidden.
- [x] Cardio activities in the same timeline, visibly different from lifts.
- [x] A not-found state for an id that no longer exists.
- [x] Empty state pointing at Train.

Group with `TemporalAdjusters.previousOrSame(MONDAY)` — the same boundary
`DerivedAnalyticsRepository` uses, so History and the charts agree on what a
week is.

### A-2 — Stats screen (3h)
`feature/stats/StatsScreen.kt`

- [x] Volume per muscle group as a ranked bar list in group accent colours.
- [x] A personal-best table: exercise, best set, estimated 1RM, date.
- [x] A window selector (4 / 8 / 12 weeks) driving both.
- [x] Under about three sessions, say there is not enough logged to chart yet
      and show the numbers instead.

### A-3 — Charts on Canvas (4h)
`ui/components/Charts.kt` — new, shared

- [x] A bar chart: eight weeks of volume, including the empty weeks.
- [x] A line chart: body weight over time, from J-6's data.
- [x] Axis labels in the mono micro style, grid lines at ~12% ink.
- [x] Touch-and-hold shows the value for that bar or point.
- [x] Both animate in once, not on every recomposition.
- [x] Both read fine in all three themes — check Court, it is the awkward one.

No chart library. `drawLine`, `drawPath` and `drawRect` in a `Canvas`, in
`ui/components/` because Fuel will want them too. `weeklyVolume` already pads
empty weeks, so the list is continuous and in order.

### A-4 — Body heat map (2h)
`feature/body/BodyScreen.kt`

The tint is already wired to real volume. What is missing is the reading of it:

- [ ] A 7 / 28 / 90-day window switch feeding `muscleVolume(days)`.
- [ ] A legend saying what brighter means, with the scale shown.
- [ ] Groups with no volume in the window called out as untrained — a nudge,
      not a scold.
- [ ] Tapping a group in the legend selects it on the figure.

Read `docs/3D-MODEL.md` first. You should not need to touch `Renderer.kt`.

### A-5 — Room persistence (4h)
`data/repo/`, `data/db/` — new

- [ ] Room entities and DAOs for sessions, sets, cardio, meal entries, water,
      weights and targets.
- [ ] `RoomWorkoutRepository` etc. implementing the **existing** interfaces.
- [ ] `Repos` swapped over. **No screen changes** — if a screen needs editing,
      the interface was wrong and that is worth discussing first.
- [ ] Data survives force-stop and reinstall-over-the-top.
- [ ] A migration strategy from version 1, even if version 1 is empty.
- [ ] The existing repository tests pass against the Room versions too.

The seam exists precisely so this ticket is invisible from the UI. Keep the
in-memory implementations — they are what the unit tests use.

### A-6 — Profile and settings (2h)
`feature/profile/ProfileScreen.kt`

- [ ] Weight unit switch, kg or lb. Store kilograms, convert at display only.
- [ ] Theme choice persisted in DataStore — it resets on relaunch today.
- [ ] Space Grotesk and IBM Plex bundled via downloadable fonts and wired into
      `Type.kt`, which already has the slots.
- [ ] Export logged sessions as JSON or CSV. Users should be able to leave with
      their own data.
- [ ] No sign-in button unless the app really talks to an account system.

The working theme picker at the top of that file is the pattern: state hoisted,
screen reports the choice.

### A-7 — Tests (1h)
`app/src/test/`, `app/src/androidTest/`

- [ ] Unit tests for whatever arithmetic A-2 and A-3 add.
- [ ] A Compose UI test: tapping each of the six tabs lands on that screen and
      the bar reflects it.
- [ ] The suite runs in under a minute.

`ui-test-junit4` and the debug manifest are already in the build.

### A-8 — CI (1h)
`.github/workflows/android.yml` — new

- [ ] On every PR: `assembleDebug` and `testDebugUnitTest`.
- [ ] Gradle cache keyed on the lock files, so a run takes ~2 minutes.
- [ ] A failing test fails the check, visibly, on the PR.
- [ ] Debug APK uploaded as an artifact, so anyone can install a branch without
      building it.
- [ ] README badge.

Pin `actions/setup-java` to JDK 17 to match the toolchain.

---

## Unassigned ideas

Not part of the 60%. Raise a ticket and claim one if you finish early, rather
than widening a ticket you are already on.

- A home-screen widget showing this week's volume.
- Wear OS companion for logging sets at the rack.
- Plate calculator: given a target weight and a bar, which plates per side.
- Programme templates (push/pull/legs) that pre-fill a session.
- Sync with the website account, so the phone and the site show one history.
- Dark-on-light theme, for anyone who trains outdoors in daylight.
