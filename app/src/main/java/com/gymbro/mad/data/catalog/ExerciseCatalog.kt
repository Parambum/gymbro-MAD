package com.gymbro.mad.data.catalog

import com.gymbro.mad.data.model.Exercise
import com.gymbro.mad.data.model.MuscleGroup

/**
 * The pre-populated catalog: 11 muscle groups x 191 proven movements.
 *
 * GENERATED from the web app's src/lib/data/exercise-catalog.ts, so the phone
 * and the website agree on slugs, names and group accents. If you add a
 * movement, add it on both sides or the website will not recognise what the
 * app logged. Slugs are the join key and are never renamed in place.
 *
 * This is reference data, not sample data: nothing here pretends to be
 * something the user did. The app ships with zero logged workouts on purpose.
 */
object ExerciseCatalog {

    val groups: List<MuscleGroup> = listOf(
        MuscleGroup("chest", "Chest", 0xFF22FF88),
        MuscleGroup("back", "Back", 0xFF7C3AED),
        MuscleGroup("shoulders", "Shoulders", 0xFF06B6D4),
        MuscleGroup("traps", "Traps", 0xFFFBBF24),
        MuscleGroup("biceps", "Biceps", 0xFF22FF88),
        MuscleGroup("triceps", "Triceps", 0xFF7C3AED),
        MuscleGroup("forearms", "Forearms", 0xFFE879F9),
        MuscleGroup("quads", "Quads", 0xFF06B6D4),
        MuscleGroup("hams-glutes", "Hamstrings / Glutes", 0xFFFF2D55),
        MuscleGroup("calves", "Calves", 0xFF22FF88),
        MuscleGroup("abs", "Abs", 0xFF06B6D4),
    )

    /** Every movement, flattened, in catalog order. */
    val exercises: List<Exercise> = listOf(
        // Chest
        Exercise("chest-flat-barbell-bench-press", "Flat Barbell Bench Press", "chest"),
        Exercise("chest-incline-barbell-bench-press", "Incline Barbell Bench Press", "chest"),
        Exercise("chest-decline-barbell-bench-press", "Decline Barbell Bench Press", "chest"),
        Exercise("chest-flat-dumbbell-bench-press", "Flat Dumbbell Bench Press", "chest"),
        Exercise("chest-incline-dumbbell-bench-press", "Incline Dumbbell Bench Press", "chest"),
        Exercise("chest-dumbbell-chest-fly", "Dumbbell Chest Fly", "chest"),
        Exercise("chest-cable-chest-fly-high-to-low", "Cable Chest Fly (High-to-Low)", "chest"),
        Exercise("chest-cable-chest-fly-low-to-high", "Cable Chest Fly (Low-to-High)", "chest"),
        Exercise("chest-pec-deck-fly", "Pec Deck Fly", "chest"),
        Exercise("chest-seated-machine-chest-press", "Seated Machine Chest Press", "chest"),
        Exercise("chest-incline-machine-chest-press", "Incline Machine Chest Press", "chest"),
        Exercise("chest-dips-chest-focus", "Dips (Chest Focus)", "chest"),
        Exercise("chest-push-ups", "Push-ups", "chest"),
        Exercise("chest-weighted-push-ups", "Weighted Push-ups", "chest"),
        Exercise("chest-smith-machine-flat-bench", "Smith Machine Flat Bench", "chest"),
        Exercise("chest-smith-machine-incline-bench", "Smith Machine Incline Bench", "chest"),
        Exercise("chest-close-grip-bench-press", "Close-Grip Bench Press", "chest"),
        Exercise("chest-floor-press", "Floor Press", "chest"),
        Exercise("chest-landmine-press", "Landmine Press", "chest"),
        Exercise("chest-cable-crossover", "Cable Crossover", "chest"),
        // Back
        Exercise("back-conventional-deadlift", "Conventional Deadlift", "back"),
        Exercise("back-pull-up", "Pull-up", "back"),
        Exercise("back-chin-up", "Chin-up", "back"),
        Exercise("back-weighted-pull-up", "Weighted Pull-up", "back"),
        Exercise("back-lat-pulldown-wide-grip", "Lat Pulldown (Wide Grip)", "back"),
        Exercise("back-lat-pulldown-close-grip", "Lat Pulldown (Close Grip)", "back"),
        Exercise("back-single-arm-lat-pulldown", "Single-Arm Lat Pulldown", "back"),
        Exercise("back-barbell-bent-over-row", "Barbell Bent-Over Row", "back"),
        Exercise("back-pendlay-row", "Pendlay Row", "back"),
        Exercise("back-one-arm-dumbbell-row", "One-Arm Dumbbell Row", "back"),
        Exercise("back-seated-cable-row", "Seated Cable Row", "back"),
        Exercise("back-t-bar-row", "T-Bar Row", "back"),
        Exercise("back-chest-supported-row", "Chest-Supported Row", "back"),
        Exercise("back-machine-high-row", "Machine High Row", "back"),
        Exercise("back-meadows-row", "Meadows Row", "back"),
        Exercise("back-smith-machine-row", "Smith Machine Row", "back"),
        Exercise("back-straight-arm-pulldown", "Straight-Arm Pulldown", "back"),
        Exercise("back-rack-pull", "Rack Pull", "back"),
        Exercise("back-inverted-row", "Inverted Row", "back"),
        Exercise("back-renegade-row", "Renegade Row", "back"),
        // Shoulders
        Exercise("shoulders-standing-overhead-barbell-press", "Standing Overhead Barbell Press", "shoulders"),
        Exercise("shoulders-seated-barbell-shoulder-press", "Seated Barbell Shoulder Press", "shoulders"),
        Exercise("shoulders-seated-dumbbell-shoulder-press", "Seated Dumbbell Shoulder Press", "shoulders"),
        Exercise("shoulders-arnold-press", "Arnold Press", "shoulders"),
        Exercise("shoulders-push-press", "Push Press", "shoulders"),
        Exercise("shoulders-machine-shoulder-press", "Machine Shoulder Press", "shoulders"),
        Exercise("shoulders-smith-machine-shoulder-press", "Smith Machine Shoulder Press", "shoulders"),
        Exercise("shoulders-landmine-shoulder-press", "Landmine Shoulder Press", "shoulders"),
        Exercise("shoulders-dumbbell-lateral-raise", "Dumbbell Lateral Raise", "shoulders"),
        Exercise("shoulders-cable-lateral-raise", "Cable Lateral Raise", "shoulders"),
        Exercise("shoulders-machine-lateral-raise", "Machine Lateral Raise", "shoulders"),
        Exercise("shoulders-dumbbell-front-raise", "Dumbbell Front Raise", "shoulders"),
        Exercise("shoulders-cable-front-raise", "Cable Front Raise", "shoulders"),
        Exercise("shoulders-barbell-front-raise", "Barbell Front Raise", "shoulders"),
        Exercise("shoulders-rear-delt-dumbbell-fly", "Rear Delt Dumbbell Fly", "shoulders"),
        Exercise("shoulders-reverse-pec-deck", "Reverse Pec Deck", "shoulders"),
        Exercise("shoulders-face-pull", "Face Pull", "shoulders"),
        Exercise("shoulders-upright-row", "Upright Row", "shoulders"),
        // Traps
        Exercise("traps-barbell-shrug", "Barbell Shrug", "traps"),
        Exercise("traps-dumbbell-shrug", "Dumbbell Shrug", "traps"),
        Exercise("traps-trap-bar-shrug", "Trap Bar Shrug", "traps"),
        Exercise("traps-smith-machine-shrug", "Smith Machine Shrug", "traps"),
        Exercise("traps-cable-shrug", "Cable Shrug", "traps"),
        Exercise("traps-behind-the-back-barbell-shrug", "Behind-the-Back Barbell Shrug", "traps"),
        // Biceps
        Exercise("biceps-barbell-curl", "Barbell Curl", "biceps"),
        Exercise("biceps-ez-bar-curl", "EZ-Bar Curl", "biceps"),
        Exercise("biceps-standing-dumbbell-curl", "Standing Dumbbell Curl", "biceps"),
        Exercise("biceps-alternating-dumbbell-curl", "Alternating Dumbbell Curl", "biceps"),
        Exercise("biceps-hammer-curl", "Hammer Curl", "biceps"),
        Exercise("biceps-cross-body-hammer-curl", "Cross-Body Hammer Curl", "biceps"),
        Exercise("biceps-incline-dumbbell-curl", "Incline Dumbbell Curl", "biceps"),
        Exercise("biceps-preacher-curl-ez-bar", "Preacher Curl (EZ-Bar)", "biceps"),
        Exercise("biceps-preacher-curl-machine", "Preacher Curl (Machine)", "biceps"),
        Exercise("biceps-concentration-curl", "Concentration Curl", "biceps"),
        Exercise("biceps-spider-curl", "Spider Curl", "biceps"),
        Exercise("biceps-cable-curl-straight-bar", "Cable Curl (Straight Bar)", "biceps"),
        Exercise("biceps-cable-rope-hammer-curl", "Cable Rope Hammer Curl", "biceps"),
        Exercise("biceps-bayesian-cable-curl", "Bayesian Cable Curl", "biceps"),
        Exercise("biceps-zottman-curl", "Zottman Curl", "biceps"),
        Exercise("biceps-reverse-curl", "Reverse Curl", "biceps"),
        Exercise("biceps-drag-curl", "Drag Curl", "biceps"),
        Exercise("biceps-chin-up-bicep-focus", "Chin-up (Bicep Focus)", "biceps"),
        Exercise("biceps-barbell-21s", "Barbell 21s", "biceps"),
        Exercise("biceps-machine-bicep-curl", "Machine Bicep Curl", "biceps"),
        // Triceps
        Exercise("triceps-close-grip-bench-press", "Close-Grip Bench Press", "triceps"),
        Exercise("triceps-smith-machine-close-grip-bench", "Smith Machine Close-Grip Bench", "triceps"),
        Exercise("triceps-skull-crusher-ez-bar", "Skull Crusher (EZ-Bar)", "triceps"),
        Exercise("triceps-floor-skull-crusher", "Floor Skull Crusher", "triceps"),
        Exercise("triceps-cable-rope-pushdown", "Cable Rope Pushdown", "triceps"),
        Exercise("triceps-cable-straight-bar-pushdown", "Cable Straight-Bar Pushdown", "triceps"),
        Exercise("triceps-reverse-grip-pushdown", "Reverse-Grip Pushdown", "triceps"),
        Exercise("triceps-single-arm-cable-pushdown", "Single-Arm Cable Pushdown", "triceps"),
        Exercise("triceps-overhead-cable-extension", "Overhead Cable Extension", "triceps"),
        Exercise("triceps-overhead-dumbbell-extension", "Overhead Dumbbell Extension", "triceps"),
        Exercise("triceps-dips-triceps-focus", "Dips (Triceps Focus)", "triceps"),
        Exercise("triceps-weighted-dip", "Weighted Dip", "triceps"),
        Exercise("triceps-bench-dip", "Bench Dip", "triceps"),
        Exercise("triceps-diamond-push-up", "Diamond Push-up", "triceps"),
        Exercise("triceps-dumbbell-kickback", "Dumbbell Kickback", "triceps"),
        Exercise("triceps-cable-kickback", "Cable Kickback", "triceps"),
        Exercise("triceps-machine-triceps-extension", "Machine Triceps Extension", "triceps"),
        Exercise("triceps-jm-press", "JM Press", "triceps"),
        Exercise("triceps-tate-press", "Tate Press", "triceps"),
        Exercise("triceps-board-press", "Board Press", "triceps"),
        // Forearms
        Exercise("forearms-barbell-wrist-curl", "Barbell Wrist Curl", "forearms"),
        Exercise("forearms-barbell-reverse-wrist-curl", "Barbell Reverse Wrist Curl", "forearms"),
        Exercise("forearms-dumbbell-wrist-curl", "Dumbbell Wrist Curl", "forearms"),
        Exercise("forearms-behind-the-back-wrist-curl", "Behind-the-Back Wrist Curl", "forearms"),
        Exercise("forearms-wrist-roller", "Wrist Roller", "forearms"),
        Exercise("forearms-farmer-s-carry", "Farmer's Carry", "forearms"),
        Exercise("forearms-sam-sulek-forearm-curls", "Sam Sulek Forearm Curls", "forearms"),
        // Quads
        Exercise("quads-barbell-back-squat", "Barbell Back Squat", "quads"),
        Exercise("quads-front-squat", "Front Squat", "quads"),
        Exercise("quads-box-squat", "Box Squat", "quads"),
        Exercise("quads-zercher-squat", "Zercher Squat", "quads"),
        Exercise("quads-goblet-squat", "Goblet Squat", "quads"),
        Exercise("quads-smith-machine-squat", "Smith Machine Squat", "quads"),
        Exercise("quads-hack-squat-machine", "Hack Squat (Machine)", "quads"),
        Exercise("quads-pendulum-squat", "Pendulum Squat", "quads"),
        Exercise("quads-belt-squat", "Belt Squat", "quads"),
        Exercise("quads-landmine-squat", "Landmine Squat", "quads"),
        Exercise("quads-leg-press", "Leg Press", "quads"),
        Exercise("quads-narrow-stance-leg-press", "Narrow-Stance Leg Press", "quads"),
        Exercise("quads-bulgarian-split-squat", "Bulgarian Split Squat", "quads"),
        Exercise("quads-walking-lunge", "Walking Lunge", "quads"),
        Exercise("quads-reverse-lunge", "Reverse Lunge", "quads"),
        Exercise("quads-step-up", "Step-Up", "quads"),
        Exercise("quads-leg-extension", "Leg Extension", "quads"),
        Exercise("quads-sissy-squat", "Sissy Squat", "quads"),
        Exercise("quads-cyclist-squat", "Cyclist Squat", "quads"),
        Exercise("quads-weighted-wall-sit", "Weighted Wall Sit", "quads"),
        // Hamstrings / Glutes
        Exercise("hams-glutes-romanian-deadlift-barbell", "Romanian Deadlift (Barbell)", "hams-glutes"),
        Exercise("hams-glutes-romanian-deadlift-dumbbell", "Romanian Deadlift (Dumbbell)", "hams-glutes"),
        Exercise("hams-glutes-single-leg-romanian-deadlift", "Single-Leg Romanian Deadlift", "hams-glutes"),
        Exercise("hams-glutes-stiff-leg-deadlift", "Stiff-Leg Deadlift", "hams-glutes"),
        Exercise("hams-glutes-sumo-deadlift", "Sumo Deadlift", "hams-glutes"),
        Exercise("hams-glutes-good-morning", "Good Morning", "hams-glutes"),
        Exercise("hams-glutes-barbell-hip-thrust", "Barbell Hip Thrust", "hams-glutes"),
        Exercise("hams-glutes-machine-hip-thrust", "Machine Hip Thrust", "hams-glutes"),
        Exercise("hams-glutes-glute-bridge", "Glute Bridge", "hams-glutes"),
        Exercise("hams-glutes-frog-pump", "Frog Pump", "hams-glutes"),
        Exercise("hams-glutes-lying-leg-curl", "Lying Leg Curl", "hams-glutes"),
        Exercise("hams-glutes-seated-leg-curl", "Seated Leg Curl", "hams-glutes"),
        Exercise("hams-glutes-nordic-ham-curl", "Nordic Ham Curl", "hams-glutes"),
        Exercise("hams-glutes-cable-pull-through", "Cable Pull-Through", "hams-glutes"),
        Exercise("hams-glutes-cable-glute-kickback", "Cable Glute Kickback", "hams-glutes"),
        Exercise("hams-glutes-kettlebell-swing", "Kettlebell Swing", "hams-glutes"),
        Exercise("hams-glutes-45-hyperextension", "45° Hyperextension", "hams-glutes"),
        Exercise("hams-glutes-reverse-hyperextension", "Reverse Hyperextension", "hams-glutes"),
        Exercise("hams-glutes-curtsy-lunge", "Curtsy Lunge", "hams-glutes"),
        Exercise("hams-glutes-machine-hip-abduction", "Machine Hip Abduction", "hams-glutes"),
        // Calves
        Exercise("calves-standing-calf-raise-machine", "Standing Calf Raise (Machine)", "calves"),
        Exercise("calves-seated-calf-raise", "Seated Calf Raise", "calves"),
        Exercise("calves-leg-press-calf-raise", "Leg Press Calf Raise", "calves"),
        Exercise("calves-smith-machine-calf-raise", "Smith Machine Calf Raise", "calves"),
        Exercise("calves-hack-squat-calf-raise", "Hack Squat Calf Raise", "calves"),
        Exercise("calves-donkey-calf-raise", "Donkey Calf Raise", "calves"),
        Exercise("calves-barbell-standing-calf-raise", "Barbell Standing Calf Raise", "calves"),
        Exercise("calves-dumbbell-standing-calf-raise", "Dumbbell Standing Calf Raise", "calves"),
        Exercise("calves-seated-dumbbell-calf-raise", "Seated Dumbbell Calf Raise", "calves"),
        Exercise("calves-single-leg-standing-calf-raise", "Single-Leg Standing Calf Raise", "calves"),
        Exercise("calves-calf-raise-on-step-bodyweight", "Calf Raise on Step (Bodyweight)", "calves"),
        Exercise("calves-weighted-calf-raise-on-step", "Weighted Calf Raise on Step", "calves"),
        Exercise("calves-bent-knee-calf-raise", "Bent-Knee Calf Raise", "calves"),
        Exercise("calves-reverse-calf-raise", "Reverse Calf Raise", "calves"),
        Exercise("calves-tibialis-raise", "Tibialis Raise", "calves"),
        Exercise("calves-explosive-calf-raise-plyo", "Explosive Calf Raise (Plyo)", "calves"),
        Exercise("calves-isometric-calf-hold", "Isometric Calf Hold", "calves"),
        Exercise("calves-farmer-s-walk-on-toes", "Farmer's Walk on Toes", "calves"),
        Exercise("calves-jump-rope-calf-focus", "Jump Rope (Calf Focus)", "calves"),
        Exercise("calves-single-leg-seated-calf-raise", "Single-Leg Seated Calf Raise", "calves"),
        // Abs
        Exercise("abs-cable-crunch", "Cable Crunch", "abs"),
        Exercise("abs-machine-crunch", "Machine Crunch", "abs"),
        Exercise("abs-decline-sit-up", "Decline Sit-up", "abs"),
        Exercise("abs-reverse-crunch", "Reverse Crunch", "abs"),
        Exercise("abs-bicycle-crunch", "Bicycle Crunch", "abs"),
        Exercise("abs-hanging-leg-raise", "Hanging Leg Raise", "abs"),
        Exercise("abs-hanging-knee-raise", "Hanging Knee Raise", "abs"),
        Exercise("abs-toes-to-bar", "Toes-to-Bar", "abs"),
        Exercise("abs-ab-wheel-rollout", "Ab Wheel Rollout", "abs"),
        Exercise("abs-plank", "Plank", "abs"),
        Exercise("abs-side-plank", "Side Plank", "abs"),
        Exercise("abs-l-sit-hold", "L-Sit Hold", "abs"),
        Exercise("abs-dragon-flag", "Dragon Flag", "abs"),
        Exercise("abs-v-up", "V-Up", "abs"),
        Exercise("abs-russian-twist", "Russian Twist", "abs"),
        Exercise("abs-cable-woodchopper", "Cable Woodchopper", "abs"),
        Exercise("abs-landmine-rotation", "Landmine Rotation", "abs"),
        Exercise("abs-pallof-press", "Pallof Press", "abs"),
        Exercise("abs-dead-bug", "Dead Bug", "abs"),
        Exercise("abs-mountain-climber", "Mountain Climber", "abs"),
    )

    private val byGroup: Map<String, List<Exercise>> = exercises.groupBy { it.groupSlug }
    private val byId: Map<String, Exercise> = exercises.associateBy { it.id }
    private val groupBySlug: Map<String, MuscleGroup> = groups.associateBy { it.slug }

    fun group(slug: String): MuscleGroup? = groupBySlug[slug]

    fun exercise(id: String): Exercise? = byId[id]

    fun forGroup(slug: String): List<Exercise> = byGroup[slug] ?: emptyList()

    /**
     * Case-insensitive substring match over names, optionally pinned to one
     * group. Good enough for 191 rows; if the catalog ever reaches the
     * website's 13k foods, this becomes a database query instead.
     */
    fun search(query: String, groupSlug: String? = null): List<Exercise> {
        val pool = groupSlug?.let(::forGroup) ?: exercises
        val q = query.trim()
        if (q.isEmpty()) return pool
        return pool.filter { it.name.contains(q, ignoreCase = true) }
    }
}
