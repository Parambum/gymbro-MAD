# GymBro — Android (MAD project)

A native Android strength tracker in Kotlin and Jetpack Compose: log a lifting
session, see the volume land on a 3D body map, and track what you ate against
what you were aiming for.

This repository is the **mobile app**, built for the Mobile Application
Development course. It is a native rewrite, not a wrapper: the GymBro website
(Next.js + MongoDB) is a separate project, and the only things that cross over
are the design tokens and the exercise catalog, both ported deliberately.

**This repo is a base.** Four of us are building on it. Roughly 40% is done —
the parts everything else needs — and the remaining work is split between
John and Arnav. See **[TASKS.md](TASKS.md)** for who owns what, and
**[CONTRIBUTING.md](CONTRIBUTING.md)** before your first commit.

---

## Run it

You need **Android Studio** (any recent version) and **SDK Platform 37**
(Tools → SDK Manager → SDK Platforms → check *Android SDK Platform 37*).

```bash
git clone <this repo>
cd gymbro-mad-android
cp local.properties.example local.properties   # then point sdk.dir at your SDK
```

Open the folder in Android Studio and press Run, or from the terminal:

```bash
./gradlew installDebug          # build and install on a connected device
./gradlew testDebugUnitTest     # 32 unit tests, no device needed
./gradlew assembleDebug         # APK in app/build/outputs/apk/debug/
```

Gradle compiles with **JDK 17** through a toolchain, whatever JDK launched it,
so you do not have to match anyone else's Java install. The first build
downloads about 200MB of dependencies; after that it is cached.

On a laptop with 8GB of RAM, close the emulator before building — the heap
settings in `gradle.properties` are deliberately small for that reason.

---

## What is already built

| Area | What it does |
| --- | --- |
| **Theme** | Three palettes (Molten, Court, Classic) ported from the website, animated on switch. Type scale, shapes, 13 colour tokens. |
| **Component kit** | `GymBroCard`, `StatTile`, `GymBroChip`, `AccentButton`, `GhostButton`, `EmptyState`, `MeterBar`, `SectionHeader`, `MicroLabel`. |
| **Navigation** | Six tabs + five inner routes, one graph, back stack behaving like a tab bar should. |
| **Data layer** | Models, repository interfaces, analytics derived from logged sessions, and local persistence for workouts and cardio activities, including GPS routes. Fuel remains in memory. |
| **Catalog** | 11 muscle groups × 191 movements, generated from the website's catalog so both agree on slugs. |
| **3D body map** | A low-poly figure drawn by a software renderer written from scratch — no 3D engine, no model files. Drag, pinch, tap a muscle. |
| **Train** | Start and discard sessions, find catalog movements, edit weight/reps, mark completed sets, rest timer, and finish-session summaries. |
| **Cardio** | Log activities manually or record a GPS route in the background with an ongoing notification. |
| **History** | Strength and cardio activity timeline grouped by week, with all-time/date filters, workout summaries, and full strength-session details. |
| **Stats** | 4/8/12-week summaries, completed-set volume by muscle, weekly volume and exercise load charts, personal bests, and a body-weight trend when at least two weigh-ins exist. |
| **Fuel** | Day screen with a day switcher, macro progress and meal sections; food search and a barcode scanner (CameraX + ML Kit); a portion sheet with live macro scaling; Mifflin-St Jeor targets onboarding; water and body-weight quick log. |
| **Deck** | The home screen, wired to the repositories, with honest empty states. |
| **Tests** | JVM tests cover geometry, repository rules, analytics, catalog integrity, workout backup encoding, Fuel day and portion maths, the barcode scanner, and the water and weight log. |

## What is still in progress

Profile/settings, complete body-map analytics and Room persistence are still
roadmap work. Workout sessions and their active draft, plus cardio activities
and GPS routes, are stored locally on-device; Fuel entries and targets remain
in memory. The app does not sign in or sync with the website.

---

## Layout

```
app/src/main/java/com/gymbro/mad/
├── MainActivity.kt          the only Activity
├── GymBroApp.kt             theme + chrome + the whole nav graph
├── ui/
│   ├── theme/               palettes, type, shapes
│   ├── components/          the component kit, charts, and UnbuiltScreen
│   └── nav/                 tabs, top bar, bottom bar, wordmark
├── anatomy/                 the 3D figure
│   ├── Geometry.kt          Vec3, Mesh, box/sphere/capsule/cylinder/icosphere
│   ├── AnatomyRig.kt        the body, part by part, ported from the website
│   └── Renderer.kt          project → cull → shade → sort → draw, plus picking
├── data/
│   ├── model/               every stored shape, free of Compose and Android
│   ├── catalog/             GENERATED exercise catalog
│   └── repo/                interfaces, derived analytics, local workout store
├── project/Roadmap.kt       the task list, in code
└── feature/<area>/          one folder per owner's screens
```

**One file per screen, one folder per area.** It is not tidiness: four people
editing four folders almost never produces a merge conflict, and four people
editing one `Screens.kt` always does.

## How the pieces fit

Screens never touch a database or each other. They read a repository interface
from `data/repo/Repositories.kt` and render what it gives them:

```
Screen ──collects──> Repository (interface) ──> SharedPreferences workout and cardio stores
                                            └─> in-memory Fuel store
```

Analytics are **derived**, never stored — muscle volume, weekly volume and
personal bests are computed from logged sessions, so the charts cannot drift
from History. The workout store uses a versioned binary snapshot; the planned
Room migration remains open for all repository types and relational queries.

## Two house rules

**1. No invented data.** Every number on screen comes from something the user
did. A screen with nothing to show says so; it does not borrow a plausible
figure to look finished. A fake 2,000 kcal target or a demo workout is a bug
that nobody can see, and it will be the thing a marker taps first.

**2. Colours come from the palette.** `LocalPalette.current`, never a hex
literal. A hard-coded colour is invisible in review and shows up as the one
thing that does not retone when the theme changes.

---

## The 3D model

There is no Filament, no SceneView, no `.glb`. The figure is about 1,800
triangles of generated geometry, drawn with a software renderer in
`anatomy/Renderer.kt`: a look-at camera, back-face culling, flat shading with
one light and a rim term, a depth sort, and `Canvas.drawPath` per triangle.

It is maths instead of megabytes, and the tricky parts are unit-tested on the
JVM — mesh winding in `GeometryTest`, camera and picking in `RendererTest`.
**[docs/3D-MODEL.md](docs/3D-MODEL.md)** explains how it works, which is also
the file to read before changing it.

## Credits

- **Aditya** — base, 3D body map, Deck
- **Arnav** — Train, Cardio, History, Stats and platform
- **John** — Fuel

Design tokens and the exercise catalog are ported from the GymBro website,
which is the same project at a different end.
