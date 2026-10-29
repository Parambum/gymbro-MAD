package com.gymbro.mad.project

/**
 * The build plan, in code.
 *
 * It lives here rather than only in TASKS.md for two reasons: the unbuilt
 * screens name their own owner and tickets instead of saying "coming soon",
 * and the Deck can show the team where the app actually is. Keep this and
 * TASKS.md in step - TASKS.md is the detail (acceptance criteria, file paths,
 * hints), this is the index.
 *
 * When you finish a ticket, flip `done = true` in the same commit as the code.
 * That is the only status report this project needs.
 */

enum class Owner(val displayName: String, val initials: String, val area: String) {
    ADITYA("Aditya", "AP", "Base, 3D, Deck"),
    PREM("Prem", "PR", "Train and Cardio"),
    JOHN("John", "JO", "Fuel"),
    ARNAV("Arnav", "AR", "History, Stats, platform"),
}

data class Ticket(
    val id: String,
    val title: String,
    val owner: Owner,
    /** rough hours, used only to keep the three shares even */
    val hours: Int,
    val done: Boolean = false,
    /** route in ui/nav/Destinations.kt this ticket lands on, if any */
    val route: String? = null,
)

object Roadmap {

    val tickets: List<Ticket> = listOf(
        // ── the base, already in the repo ─────────────────────────
        Ticket("B-1", "Gradle build, JDK 17 toolchain, wrapper", Owner.ADITYA, 3, done = true),
        Ticket("B-2", "Theme: three palettes, type scale, shapes", Owner.ADITYA, 4, done = true),
        Ticket("B-3", "Component kit: cards, tiles, chips, buttons, empty states", Owner.ADITYA, 5, done = true),
        Ticket("B-4", "Navigation shell: six tabs plus inner routes", Owner.ADITYA, 3, done = true),
        Ticket("B-5", "Models, repository interfaces, in-memory store, derived analytics", Owner.ADITYA, 7, done = true),
        Ticket("B-6", "Exercise catalog ported from the web app (11 groups, 191 movements)", Owner.ADITYA, 2, done = true),
        Ticket("B-7", "3D anatomy: mesh builders, software renderer, Body screen", Owner.ADITYA, 12, done = true, route = Routes.BODY),
        Ticket("B-8", "Deck wired to the repositories", Owner.ADITYA, 4, done = true, route = Routes.DECK),

        // ── Prem: the lifting engine ──────────────────────────────
        Ticket("P-1", "Exercise picker: search and group filter over the catalog", Owner.PREM, 3, route = Routes.EXERCISE_PICKER),
        Ticket("P-2", "Set logger rows: weight, reps, RPE, completed toggle", Owner.PREM, 4, route = Routes.TRAIN),
        Ticket("P-3", "Active session: start, add, edit, finish, discard", Owner.PREM, 5, route = Routes.TRAIN),
        Ticket("P-4", "Rest timer with a notification when the app is backgrounded", Owner.PREM, 3, route = Routes.TRAIN),
        Ticket("P-5", "Finish-session summary: volume, sets, PRs hit, duration", Owner.PREM, 2, route = Routes.TRAIN),
        Ticket("P-6", "Cardio logging: kind, duration, distance, average heart rate", Owner.PREM, 3, route = Routes.CARDIO),
        Ticket("P-7", "Estimated 1RM per exercise, with unit tests", Owner.PREM, 1, route = Routes.TRAIN),

        // ── John: fuel ────────────────────────────────────────────
        Ticket("J-1", "Fuel day screen: macro rings, meal sections, day switcher", Owner.JOHN, 5, done = true, route = Routes.FUEL),
        Ticket("J-2", "Food search and FoodRepository against the web API", Owner.JOHN, 4, done = true, route = Routes.FOOD_SEARCH),
        Ticket("J-3", "Barcode scanner behind a camera permission request", Owner.JOHN, 4, route = Routes.FOOD_SEARCH),
        Ticket("J-4", "Add and edit an entry: portion size, unit conversion", Owner.JOHN, 3, done = true, route = Routes.FUEL),
        Ticket("J-5", "Targets onboarding: Mifflin-St Jeor, with unit tests", Owner.JOHN, 2, done = true, route = Routes.FUEL),
        Ticket("J-6", "Water and body-weight quick log", Owner.JOHN, 2, done = true, route = Routes.FUEL),

        // ── Arnav: history, stats, platform ───────────────────────
        Ticket("A-1", "History grouped by week, plus the session detail route", Owner.ARNAV, 4, route = Routes.HISTORY),
        Ticket("A-2", "Stats screen: volume per muscle group, PR table", Owner.ARNAV, 3, route = Routes.STATS),
        Ticket("A-3", "Charts on Canvas: weekly volume bars, body-weight line", Owner.ARNAV, 4, route = Routes.STATS),
        // the tint itself is wired; what is missing is the reading of it
        Ticket("A-4", "Body heat map: window switch, volume legend, undertrained nudges", Owner.ARNAV, 2, route = Routes.BODY),
        Ticket("A-5", "Room persistence behind the existing repository interfaces", Owner.ARNAV, 4),
        Ticket("A-6", "Profile and settings: theme switch, units, bundled fonts", Owner.ARNAV, 2, route = Routes.PROFILE),
        Ticket("A-7", "Tests: stats maths and a Compose test for the tab bar", Owner.ARNAV, 1),
        Ticket("A-8", "GitHub Actions: assembleDebug and unit tests on every PR", Owner.ARNAV, 1),
    )

    fun forOwner(owner: Owner): List<Ticket> = tickets.filter { it.owner == owner }

    fun forRoute(route: String): List<Ticket> = tickets.filter { it.route == route && !it.done }

    val hoursTotal: Int = tickets.sumOf { it.hours }
    val hoursDone: Int = tickets.filter { it.done }.sumOf { it.hours }
    val hoursLeft: Int = hoursTotal - hoursDone

    /** 0f..1f, by hours rather than ticket count - the tickets are not equal. */
    val progress: Float = if (hoursTotal == 0) 0f else hoursDone.toFloat() / hoursTotal

    fun shareOf(owner: Owner): Float {
        val owned = forOwner(owner).sumOf { it.hours }
        return if (hoursTotal == 0) 0f else owned.toFloat() / hoursTotal
    }
}

/**
 * Route names, kept out of the Compose layer so [Roadmap] and the unbuilt
 * screens can both refer to them without a UI import.
 */
object Routes {
    const val DECK = "deck"
    const val TRAIN = "train"
    const val CARDIO = "cardio"
    const val FUEL = "fuel"
    const val BODY = "body"
    const val STATS = "stats"
    const val HISTORY = "history"

    const val PROFILE = "profile"
    const val EXERCISE_PICKER = "train/picker"
    const val FOOD_SEARCH = "fuel/search"

    /** History -> one session. Built by A-1. */
    const val SESSION_DETAIL = "history/{sessionId}"
    fun sessionDetail(sessionId: String) = "history/$sessionId"
}
