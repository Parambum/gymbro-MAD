# Contributing

Four people, one app, one semester. These rules exist so none of us spends an
evening resolving a merge conflict instead of building a screen.

## Own your folder

| Owner | Folders |
| --- | --- |
| Aditya | `anatomy/`, `feature/deck/`, `feature/body/`, `ui/`, `data/` |
| Arnav | `feature/train/`, `feature/cardio/`, `feature/history/`, `feature/stats/`, `feature/profile/`, `data/db/`, `.github/` |
| John | `feature/fuel/` |

Inside your own folders, build what you like. Outside them, there are four
shared files, and all four are shared on purpose:

- **`GymBroApp.kt`** — the nav graph. Adding a route is a few lines; tell the
  group chat when you do, because it is the one file we all touch.
- **`ui/components/`** — the component kit. **Add** components freely. Changing
  an existing one changes every screen, so say so in the PR title.
- **`data/repo/Repositories.kt`** — the interfaces. Adding a method is fine.
  Changing or removing one breaks somebody else's screen: ask first.
- **`project/Roadmap.kt`** — flip your own ticket to `done`. Nothing else.

If you need a change in someone else's folder, message them. Do not fix it
yourself, even if it is one line — they may be mid-rewrite of that file.

## Branches and commits

```bash
git checkout main
git pull
git checkout -b prem/p3-active-session     # <you>/<ticket>-<what>
```

One branch per ticket. Branch from an up-to-date `main`, and pull `main` into
your branch every couple of days — a week-old branch is where conflicts come
from.

Commit messages: a line saying what changed and why, in the present tense, no
ticket number needed in the subject.

```
Log a set without losing the keyboard

The weight field lost focus on every recomposition because the row
rebuilt its state holder, so logging five sets meant five taps back
into the field. State moved up to the session; the row is now stateless.

P-2
```

Commit when something works, not when the day ends. A commit that does not
compile is a commit that breaks everyone who pulls it.

## Pull requests

- Open a PR into `main`. Title: `P-3 Active session`.
- Describe what you built, what you deliberately left out, and anything you
  were unsure about.
- **Attach a screenshot or a screen recording.** It is a UI project; a diff
  does not show that the spacing is wrong.
- One review from anyone else on the team, then merge your own PR.
- Reviewers: check it against the ticket's acceptance list in TASKS.md. "Looks
  good" is not a review.
- Never force-push a branch somebody has reviewed, and never push to `main`.

## Style

Kotlin official style, which Android Studio does by default. Beyond that:

- **Colours from `LocalPalette.current`.** No hex literals in a screen.
- **Components from the kit.** If the kit lacks something, add it to the kit.
- **State hoisted.** A screen takes values and lambdas; it does not reach for a
  repository through a global. The defaults in the signature are for previews.
- **`suspend` for writes, `Flow` for reads.** Nothing blocking on the main
  thread, ever.
- **Comments explain why, not what.** `// loop over sets` is noise. `// only
  completed sets count, so an abandoned set cannot inflate volume` is worth
  its line.
- Name things after what they are in the gym — `sets`, `reps`, `volumeKg` — not
  after their Compose role.

## The two rules that are not style

**No invented data.** Ever. Not in a screen, not "just for the demo", not
behind a flag. If there is nothing to show, say so on screen. We are shipping
a tracker; a number nobody earned is the one bug that destroys trust in all
the others.

**Kilograms and metres inside the app.** Convert at the edge for display. A
mixed-unit store produces a personal best that is silently wrong, and nobody
notices until it matters.

## Before you ask for a review

```bash
./gradlew testDebugUnitTest   # must pass
./gradlew assembleDebug       # must produce no new warnings
```

Then run it on a real phone, not just the preview. Check the small screen, and
turn the system font size up — both have caught layout bugs in this repo
already.

## Stuck?

- The brief is in the app: open your tab and read it.
- The acceptance list is in [TASKS.md](TASKS.md).
- The worked example is `feature/deck/DeckScreen.kt`.
- The 3D renderer is explained in [docs/3D-MODEL.md](docs/3D-MODEL.md).
- Still stuck after an hour: ask in the group chat. An hour is the right amount
  of time to be stuck for; a day is not.
